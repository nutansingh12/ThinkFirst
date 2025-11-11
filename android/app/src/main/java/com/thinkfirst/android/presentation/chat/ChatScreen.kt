package com.thinkfirst.android.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thinkfirst.android.data.model.ChatMessage
import com.thinkfirst.android.data.model.MessageRole
import com.thinkfirst.android.presentation.learning.LearningJourneyScreen
import com.thinkfirst.android.presentation.learning.LearningJourneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    childId: Long,
    onNavigateToDashboard: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
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
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout?.invoke()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error display
            if (uiState is ChatUiState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: ${(uiState as ChatUiState.Error).message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

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
            is ChatUiState.LearningPathRequired -> {
                LearningPathDialog(
                    learningPath = state.learningPath,
                    childId = childId,
                    onRetakeQuiz = {
                        // Send the original query again to retake the quiz
                        viewModel.dismissQuiz()
                        viewModel.sendMessage(state.learningPath.originalQuery)
                    },
                    onAskDifferentQuestion = {
                        // Clear the learning path and reset input
                        viewModel.dismissQuiz()
                        messageText = ""
                    }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(quiz.description ?: "Complete this quiz to continue")
                Spacer(modifier = Modifier.height(16.dp))

                quiz.questions.forEachIndexed { index, question ->
                    Text(
                        text = "${index + 1}. ${question.questionText}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    question.options?.forEachIndexed { optIndex, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    answers = answers + (question.id to optIndex.toString())
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = answers[question.id] == optIndex.toString(),
                                onClick = {
                                    answers = answers + (question.id to optIndex.toString())
                                }
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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

@Composable
fun LearningPathDialog(
    learningPath: com.thinkfirst.android.data.model.LearningPath,
    childId: Long,
    onRetakeQuiz: () -> Unit,
    onAskDifferentQuestion: () -> Unit,
    learningJourneyViewModel: LearningJourneyViewModel = hiltViewModel()
) {
    var showFullScreen by remember { mutableStateOf(false) }

    // Observe the updated learning path from ViewModel
    val updatedLearningPath by learningJourneyViewModel.learningPath.collectAsState()
    val currentLearningPath = updatedLearningPath ?: learningPath

    LaunchedEffect(learningPath) {
        learningJourneyViewModel.setLearningPath(learningPath, childId)
    }

    if (showFullScreen) {
        LearningJourneyScreen(
            learningPath = currentLearningPath,
            onCompleteLesson = { lessonId ->
                learningJourneyViewModel.completeLesson(lessonId)
            },
            onRetakeQuiz = onRetakeQuiz,
            onAskDifferentQuestion = onAskDifferentQuestion,
            onBack = { showFullScreen = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onAskDifferentQuestion,
            title = {
                Text(
                    text = "🎓 Learning Journey",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Score
                    Text(
                        text = "Your Score: ${currentLearningPath.score}% (${currentLearningPath.correctAnswers}/${currentLearningPath.totalQuestions})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Motivational message
                    Text(
                        text = currentLearningPath.motivationalMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Divider()

                    // Lessons preview
                    Text(
                        text = "📚 ${currentLearningPath.totalLessons} Lessons to Complete:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    currentLearningPath.lessons.take(3).forEach { lesson ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (lesson.completed) "✅" else if (lesson.locked) "🔒" else "📖",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Divider()

                    // Pro tip
                    Text(
                        text = "💡 ${currentLearningPath.proTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFullScreen = true }) {
                    Text("Start Learning")
                }
            },
            dismissButton = {
                TextButton(onClick = onAskDifferentQuestion) {
                    Text("Ask Different Question")
                }
            }
        )
    }
}

