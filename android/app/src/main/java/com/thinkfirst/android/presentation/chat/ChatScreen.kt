package com.thinkfirst.android.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thinkfirst.android.data.model.ChatMessage
import com.thinkfirst.android.data.model.MessageRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    childId: Long,
    onNavigateToDashboard: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    
    LaunchedEffect(childId) {
        viewModel.initializeSession(childId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ThinkFirst Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = rememberLazyListState(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
                
                // Loading indicator
                if (uiState is ChatUiState.Loading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask a question...") },
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank() && uiState !is ChatUiState.Loading
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
        
        // Quiz overlay
        when (val state = uiState) {
            is ChatUiState.QuizRequired -> {
                QuizDialog(
                    quiz = state.quiz,
                    onSubmit = { answers, timeSpent ->
                        viewModel.submitQuiz(state.quiz.id, answers, timeSpent)
                    },
                    onDismiss = { viewModel.dismissQuiz() }
                )
            }
            is ChatUiState.VerificationQuiz -> {
                QuizDialog(
                    quiz = state.quiz,
                    onSubmit = { answers, timeSpent ->
                        viewModel.submitQuiz(state.quiz.id, answers, timeSpent)
                    },
                    onDismiss = { viewModel.dismissQuiz() }
                )
            }
            is ChatUiState.QuizResult -> {
                QuizResultDialog(
                    result = state.result,
                    onDismiss = { viewModel.dismissQuiz() }
                )
            }
            is ChatUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissQuiz() },
                    title = { Text("Error") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissQuiz() }) {
                            Text("OK")
                        }
                    }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
fun QuizDialog(
    quiz: com.thinkfirst.android.data.model.Quiz,
    onSubmit: (Map<Long, String>, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var answers by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(quiz.title ?: "Quiz") },
        text = {
            Column {
                Text(quiz.description ?: "Complete this quiz to continue")
                Spacer(modifier = Modifier.height(16.dp))
                
                quiz.questions.forEachIndexed { index, question ->
                    Text("${index + 1}. ${question.questionText}")
                    
                    question.options?.forEachIndexed { optIndex, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = answers[question.id] == optIndex.toString(),
                                onClick = {
                                    answers = answers + (question.id to optIndex.toString())
                                }
                            )
                            Text(option)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(answers, 0) },
                enabled = answers.size == quiz.questions.size
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuizResultDialog(
    result: com.thinkfirst.android.data.model.QuizResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (result.passed) "Great Job! 🎉" else "Keep Trying! 💪") },
        text = {
            Column {
                Text("Score: ${result.score}%")
                Text("Correct: ${result.correctAnswers}/${result.totalQuestions}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(result.feedbackMessage)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue")
            }
        }
    )
}

