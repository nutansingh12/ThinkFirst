package com.thinkfirst.android.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.local.TokenManager
import com.thinkfirst.android.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: ThinkFirstApi,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private var currentSessionId: Long? = null
    private var currentChildId: Long? = null
    private var lastFailedMessage: String? = null
    
    fun initializeSession(childId: Long) {
        currentChildId = childId
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Creating session for child: $childId")
                val session = api.createSession(childId, "New Chat")
                Log.d("ChatViewModel", "Session created: ${session.id}")
                currentSessionId = session.id
                loadChatHistory(session.id)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to create session", e)
                _uiState.value = ChatUiState.Error(e.message ?: "Failed to create session: ${e.javaClass.simpleName}")
            }
        }
    }
    
    fun sendMessage(message: String) {
        val childId = currentChildId ?: return
        val sessionId = currentSessionId ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadingMessage.value = "Thinking..."
                _uiState.value = ChatUiState.Loading

                // Add user message to UI immediately
                val userMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    role = MessageRole.USER,
                    content = message,
                    createdAt = System.currentTimeMillis().toString()
                )
                _messages.value = _messages.value + userMessage

                // Send to backend
                val request = ChatRequest(
                    childId = childId,
                    sessionId = sessionId,
                    query = message
                )

                Log.d("ChatViewModel", "Sending query: $message")
                _loadingMessage.value = "Getting your answer..."
                val response = api.sendQuery(request)
                Log.d("ChatViewModel", "Received response: ${response.responseType}")

                // Clear last failed message on success
                lastFailedMessage = null

                when (response.responseType) {
                    ResponseType.QUIZ_REQUIRED -> {
                        _loadingMessage.value = null
                        _isLoading.value = false
                        _uiState.value = ChatUiState.QuizRequired(response.quiz!!)
                    }
                    ResponseType.FULL_ANSWER -> {
                        addAssistantMessage(response.message ?: "")
                        _loadingMessage.value = null
                        _isLoading.value = false
                        if (response.quiz != null) {
                            _uiState.value = ChatUiState.VerificationQuiz(response.quiz)
                        } else {
                            _uiState.value = ChatUiState.Success
                        }
                    }
                    ResponseType.PARTIAL_HINT -> {
                        addAssistantMessage(response.hint ?: response.message ?: "")
                        _loadingMessage.value = null
                        _isLoading.value = false
                        _uiState.value = ChatUiState.Success
                    }
                    ResponseType.GUIDED_QUESTIONS -> {
                        addAssistantMessage(response.message ?: "")
                        _loadingMessage.value = null
                        _isLoading.value = false
                        _uiState.value = ChatUiState.Success
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to send message", e)
                lastFailedMessage = message
                _loadingMessage.value = null
                _isLoading.value = false
                _uiState.value = ChatUiState.Error(e.message ?: "Failed to send message: ${e.javaClass.simpleName}")
            }
        }
    }

    fun retryLastMessage() {
        lastFailedMessage?.let { message ->
            sendMessage(message)
        }
    }
    
    fun submitQuiz(quizId: Long, answers: Map<Long, String>, timeSpent: Int) {
        val childId = currentChildId ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadingMessage.value = "Checking your answers..."
                _uiState.value = ChatUiState.Loading

                val submission = QuizSubmission(
                    childId = childId,
                    quizId = quizId,
                    answers = answers,
                    timeSpentSeconds = timeSpent
                )

                val result = api.submitQuiz(submission)

                // Check if there's a learning path (student failed badly)
                if (result.learningPath != null) {
                    _loadingMessage.value = "Creating your learning journey..."
                    // Small delay to show the message
                    kotlinx.coroutines.delay(500)
                    _loadingMessage.value = null
                    _isLoading.value = false
                    _uiState.value = ChatUiState.LearningPathRequired(result.learningPath)
                } else {
                    _loadingMessage.value = null
                    _isLoading.value = false
                    _uiState.value = ChatUiState.QuizResult(result)

                    // Add feedback message
                    addAssistantMessage(result.feedbackMessage)

                    // If student passed and there's an answer, show it
                    if (result.passed && result.answerMessage != null) {
                        addAssistantMessage(result.answerMessage)
                    }

                    // If student scored 40-69% and there's a hint, add it to chat
                    if (!result.passed && result.hintMessage != null) {
                        addAssistantMessage("💡 Hint: ${result.hintMessage}")
                    }
                }
            } catch (e: Exception) {
                _loadingMessage.value = null
                _isLoading.value = false
                _uiState.value = ChatUiState.Error(e.message ?: "Failed to submit quiz")
            }
        }
    }
    
    private fun loadChatHistory(sessionId: Long) {
        viewModelScope.launch {
            try {
                val history = api.getChatHistory(sessionId)
                _messages.value = history
            } catch (e: Exception) {
                // Ignore error, start with empty history
            }
        }
    }
    
    private fun addAssistantMessage(content: String) {
        val assistantMessage = ChatMessage(
            id = System.currentTimeMillis(),
            role = MessageRole.ASSISTANT,
            content = content,
            createdAt = System.currentTimeMillis().toString()
        )
        _messages.value = _messages.value + assistantMessage
    }
    
    fun dismissQuiz() {
        _uiState.value = ChatUiState.Success
    }

    fun logout() {
        viewModelScope.launch {
            try {
                tokenManager.clearTokens()
                Log.d("ChatViewModel", "User logged out successfully")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error during logout", e)
            }
        }
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    object Success : ChatUiState()
    data class QuizRequired(val quiz: Quiz) : ChatUiState()
    data class VerificationQuiz(val quiz: Quiz) : ChatUiState()
    data class QuizResult(val result: com.thinkfirst.android.data.model.QuizResult) : ChatUiState()
    data class LearningPathRequired(val learningPath: LearningPath) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

