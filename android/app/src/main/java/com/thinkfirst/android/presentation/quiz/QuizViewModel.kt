package com.thinkfirst.android.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.model.Question
import com.thinkfirst.android.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for quiz screen
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var quizId: Long = 0
    private var childId: Long = 0
    private var quizStartTime: Long = 0  // Track when quiz started
    
    /**
     * Load quiz data from API
     */
    fun loadQuiz(quizId: Long, childId: Long) {
        this.quizId = quizId
        this.childId = childId
        this.quizStartTime = System.currentTimeMillis()  // Record start time
        android.util.Log.d("QuizViewModel", "loadQuiz() called - quizId: $quizId, childId: $childId, quizStartTime: $quizStartTime")

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                android.util.Log.d("QuizViewModel", "Fetching quiz $quizId from API...")
                // Fetch quiz from API
                val result = quizRepository.getQuiz(quizId)

                result.fold(
                    onSuccess = { quiz ->
                        android.util.Log.d("QuizViewModel", "Quiz loaded successfully - ${quiz.questions.size} questions")
                        _uiState.value = QuizUiState(
                            questions = quiz.questions,
                            currentQuestionIndex = 0,
                            selectedAnswers = emptyMap(),
                            timeRemaining = quiz.timeLimit?.toLong(),
                            isLoading = false
                        )

                        // Start timer if time limit exists
                        quiz.timeLimit?.let { startTimer(it.toLong()) }
                    },
                    onFailure = { error ->
                        android.util.Log.e("QuizViewModel", "Failed to load quiz: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Exception loading quiz: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load quiz"
                )
            }
        }
    }
    
    /**
     * Load quiz with questions (called from ChatScreen)
     */
    fun loadQuizWithQuestions(
        quizId: Long,
        childId: Long,
        questions: List<Question>,
        timeLimit: Int? = null
    ) {
        this.quizId = quizId
        this.childId = childId
        this.quizStartTime = System.currentTimeMillis()  // Record start time

        _uiState.value = QuizUiState(
            questions = questions,
            currentQuestionIndex = 0,
            selectedAnswers = emptyMap(),
            timeRemaining = timeLimit?.toLong(),
            isLoading = false
        )

        // Start timer if time limit exists
        timeLimit?.let { startTimer(it.toLong()) }
    }
    
    /**
     * Select an answer for a question
     */
    fun selectAnswer(questionId: Long, answer: String) {
        val currentAnswers = _uiState.value.selectedAnswers.toMutableMap()
        currentAnswers[questionId] = answer
        
        _uiState.value = _uiState.value.copy(
            selectedAnswers = currentAnswers
        )
    }
    
    /**
     * Move to next question
     */
    fun nextQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        val totalQuestions = _uiState.value.questions.size
        
        if (currentIndex < totalQuestions - 1) {
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = currentIndex + 1
            )
        }
    }
    
    /**
     * Move to previous question
     */
    fun previousQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = currentIndex - 1
            )
        }
    }
    
    /**
     * Submit quiz
     */
    fun submitQuiz() {
        android.util.Log.d("QuizViewModel", "submitQuiz() called - quizStartTime: $quizStartTime")
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Stop timer
                timerJob?.cancel()

                // Calculate time spent in seconds
                val currentTime = System.currentTimeMillis()
                val elapsedMillis = currentTime - quizStartTime
                val timeSpentSeconds = (elapsedMillis / 1000).toInt()
                android.util.Log.d("QuizViewModel", "Time calculation - quizStartTime: $quizStartTime, currentTime: $currentTime, elapsedMillis: $elapsedMillis, timeSpentSeconds: $timeSpentSeconds")

                val result = quizRepository.submitQuiz(
                    quizId = quizId,
                    childId = childId,
                    answers = _uiState.value.selectedAnswers,
                    timeSpentSeconds = timeSpentSeconds
                )
                
                result.fold(
                    onSuccess = { quizResult ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isComplete = true,
                            score = quizResult.score,
                            passed = quizResult.passed
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to submit quiz"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to submit quiz"
                )
            }
        }
    }
    
    /**
     * Start countdown timer
     */
    private fun startTimer(seconds: Long) {
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            var remaining = seconds
            
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(timeRemaining = remaining)
            }
            
            // Time's up - auto submit
            submitQuiz()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

/**
 * UI state for quiz screen
 */
data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Long, String> = emptyMap(),
    val timeRemaining: Long? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val score: Int = 0,
    val passed: Boolean = false,
    val error: String? = null
)

