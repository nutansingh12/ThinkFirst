package com.thinkfirst.android.data.model

import com.google.gson.annotations.SerializedName

// Auth Models
data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String = "PARENT"
)

data class AuthResponse(
    val token: String,
    val refreshToken: String?,
    val type: String = "Bearer",
    val userId: Long,
    val email: String,
    val fullName: String,
    val role: String
)

// Chat Models
data class ChatRequest(
    val childId: Long,
    val sessionId: Long,
    val query: String
)

data class ChatResponse(
    val message: String?,
    val responseType: ResponseType,
    val quiz: Quiz?,
    val hint: String?,
    val messageId: Long?
)

enum class ResponseType {
    @SerializedName("FULL_ANSWER")
    FULL_ANSWER,
    
    @SerializedName("PARTIAL_HINT")
    PARTIAL_HINT,
    
    @SerializedName("GUIDED_QUESTIONS")
    GUIDED_QUESTIONS,
    
    @SerializedName("QUIZ_REQUIRED")
    QUIZ_REQUIRED
}

data class ChatSession(
    val id: Long,
    val title: String?,
    val messageCount: Int,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val createdAt: String
)

enum class MessageRole {
    @SerializedName("USER")
    USER,
    
    @SerializedName("ASSISTANT")
    ASSISTANT
}

// Quiz Models
data class Quiz(
    val id: Long,
    val title: String?,
    val description: String?,
    val questions: List<Question>,
    val passingScore: Int,
    val timeLimit: Int?,
    val type: QuizType,
    val difficulty: DifficultyLevel
)

data class Question(
    val id: Long,
    val questionText: String,
    val type: QuestionType,
    val options: List<String>?,
    val correctOptionIndex: Int?,
    val explanation: String?
)

enum class QuizType {
    @SerializedName("PREREQUISITE")
    PREREQUISITE,
    
    @SerializedName("VERIFICATION")
    VERIFICATION,
    
    @SerializedName("CHALLENGE")
    CHALLENGE,
    
    @SerializedName("DIAGNOSTIC")
    DIAGNOSTIC
}

enum class QuestionType {
    @SerializedName("MULTIPLE_CHOICE")
    MULTIPLE_CHOICE,
    
    @SerializedName("SHORT_ANSWER")
    SHORT_ANSWER,
    
    @SerializedName("TRUE_FALSE")
    TRUE_FALSE,
    
    @SerializedName("FILL_BLANK")
    FILL_BLANK
}

enum class DifficultyLevel {
    @SerializedName("BEGINNER")
    BEGINNER,
    
    @SerializedName("INTERMEDIATE")
    INTERMEDIATE,
    
    @SerializedName("ADVANCED")
    ADVANCED,
    
    @SerializedName("EXPERT")
    EXPERT
}

data class QuizSubmission(
    val childId: Long,
    val quizId: Long,
    val answers: Map<Long, String>,
    val timeSpentSeconds: Int?
)

data class QuizResult(
    val attemptId: Long,
    val score: Int,
    val passed: Boolean,
    val responseLevel: ResponseType,
    val feedbackMessage: String,
    val questionResults: List<QuestionResult>,
    val totalQuestions: Int,
    val correctAnswers: Int
)

data class QuestionResult(
    val questionId: Long,
    val questionText: String,
    val userAnswer: String?,
    val correctAnswer: String,
    val correct: Boolean,
    val explanation: String?
)

// Progress Models
data class ProgressReport(
    val childId: Long,
    val childUsername: String,
    val currentStreak: Int,
    val totalQuizzesCompleted: Int,
    val totalQuestionsAnswered: Int,
    val averageScore: Double,
    val skillLevels: List<SkillLevel>,
    val recentAchievements: List<Achievement>,
    val subjectProgress: Map<String, SubjectProgress>
)

data class SkillLevel(
    val id: Long,
    val proficiencyScore: Int,
    val currentLevel: DifficultyLevel,
    val quizzesCompleted: Int,
    val averageScore: Int
)

data class SubjectProgress(
    val subjectName: String,
    val proficiencyScore: Int,
    val currentLevel: String,
    val quizzesCompleted: Int,
    val averageScore: Int
)

data class Achievement(
    val id: Long,
    val badgeName: String,
    val description: String?,
    val iconUrl: String?,
    val type: AchievementType,
    val points: Int,
    val earnedAt: String
)

enum class AchievementType {
    @SerializedName("SUBJECT_MASTERY")
    SUBJECT_MASTERY,
    
    @SerializedName("STREAK_MILESTONE")
    STREAK_MILESTONE,
    
    @SerializedName("QUIZ_PERFECT")
    QUIZ_PERFECT,
    
    @SerializedName("QUICK_LEARNER")
    QUICK_LEARNER,
    
    @SerializedName("PERSISTENT_LEARNER")
    PERSISTENT_LEARNER,
    
    @SerializedName("FIRST_STEPS")
    FIRST_STEPS
}

