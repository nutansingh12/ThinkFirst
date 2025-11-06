package com.thinkfirst.android.data.api

import com.thinkfirst.android.data.model.*
import retrofit2.http.*

interface ThinkFirstApi {
    
    // Authentication
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    // Chat
    @POST("chat/query")
    suspend fun sendQuery(@Body request: ChatRequest): ChatResponse
    
    @POST("chat/session")
    suspend fun createSession(
        @Query("childId") childId: Long,
        @Query("title") title: String?
    ): ChatSession
    
    @GET("chat/session/{sessionId}/history")
    suspend fun getChatHistory(@Path("sessionId") sessionId: Long): List<ChatMessage>
    
    @GET("chat/child/{childId}/sessions")
    suspend fun getChildSessions(@Path("childId") childId: Long): List<ChatSession>
    
    // Quiz
    @POST("quiz/submit")
    suspend fun submitQuiz(@Body submission: QuizSubmission): QuizResult
    
    @GET("quiz/{quizId}")
    suspend fun getQuiz(@Path("quizId") quizId: Long): Quiz
    
    // Dashboard
    @GET("dashboard/child/{childId}/progress")
    suspend fun getProgress(@Path("childId") childId: Long): ProgressReport
    
    @GET("dashboard/child/{childId}/achievements")
    suspend fun getAchievements(@Path("childId") childId: Long): List<Achievement>
}

