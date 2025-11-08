package com.thinkfirst.android.presentation.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thinkfirst.android.data.model.Question

/**
 * Full-screen quiz interface with improved UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizId: Long,
    childId: Long,
    onQuizComplete: (Int, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(quizId) {
        viewModel.loadQuiz(quizId, childId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Challenge") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    ErrorView(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadQuiz(quizId, childId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.isComplete -> {
                    QuizResultView(
                        score = uiState.score,
                        passed = uiState.passed,
                        totalQuestions = uiState.questions.size,
                        onContinue = {
                            onQuizComplete(uiState.score, uiState.passed)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    QuizContentView(
                        questions = uiState.questions,
                        currentQuestionIndex = uiState.currentQuestionIndex,
                        selectedAnswers = uiState.selectedAnswers,
                        timeRemaining = uiState.timeRemaining,
                        onAnswerSelected = { questionId, answer ->
                            viewModel.selectAnswer(questionId, answer)
                        },
                        onNext = { viewModel.nextQuestion() },
                        onPrevious = { viewModel.previousQuestion() },
                        onSubmit = { viewModel.submitQuiz() }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizContentView(
    questions: List<Question>,
    currentQuestionIndex: Int,
    selectedAnswers: Map<Long, String>,
    timeRemaining: Long?,
    onAnswerSelected: (Long, String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit
) {
    if (questions.isEmpty()) return
    
    val currentQuestion = questions[currentQuestionIndex]
    val progress = (currentQuestionIndex + 1).toFloat() / questions.size
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                timeRemaining?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Time",
                            modifier = Modifier.size(20.dp),
                            tint = if (it < 60) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTime(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (it < 60) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Question content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = currentQuestion.questionText,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Answer options
            currentQuestion.options.forEach { option ->
                val isSelected = selectedAnswers[currentQuestion.id] == option
                
                AnswerOption(
                    text = option,
                    isSelected = isSelected,
                    onClick = { onAnswerSelected(currentQuestion.id, option) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentQuestionIndex > 0
            ) {
                Icon(Icons.Default.ArrowBack, "Previous")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
            }
            
            if (currentQuestionIndex < questions.size - 1) {
                Button(
                    onClick = onNext,
                    enabled = selectedAnswers.containsKey(currentQuestion.id)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, "Next")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = selectedAnswers.size == questions.size,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, "Submit")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit Quiz")
                }
            }
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder() 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuizResultView(
    score: Int,
    passed: Boolean,
    totalQuestions: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.EmojiEvents else Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (passed) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (passed) "Congratulations!" else "Keep Trying!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You scored $score%",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (passed) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = if (passed) {
                    "Great job! You've unlocked the full answer."
                } else {
                    "Score at least 70% to unlock the full answer. Try again!"
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

