package com.thinkfirst.android.data.api

import com.thinkfirst.android.data.model.*
import retrofit2.http.*

interface ThinkFirstApi {

    // Authentication
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/child/login")
    suspend fun childLogin(@Body request: ChildLoginRequest): AuthResponse

    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): AuthResponse

    // Child Management
    @POST("children")
    suspend fun createChild(@Body request: CreateChildRequest): ChildProfile

    @GET("children/parent/{parentId}")
    suspend fun getParentChildren(@Path("parentId") parentId: Long): List<ChildProfile>

    @GET("children/{childId}")
    suspend fun getChild(@Path("childId") childId: Long): ChildProfile

    @PUT("children/{childId}")
    suspend fun updateChild(
        @Path("childId") childId: Long,
        @Body request: CreateChildRequest
    ): ChildProfile

    @DELETE("children/{childId}")
    suspend fun deleteChild(@Path("childId") childId: Long)

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

    // Learning Path
    @POST("learning-path/lesson/{lessonId}/complete")
    suspend fun completeLesson(
        @Path("lessonId") lessonId: Long,
        @Query("childId") childId: Long
    ): LearningPath

    @GET("learning-path/{learningPathId}")
    suspend fun getLearningPath(
        @Path("learningPathId") learningPathId: Long,
        @Query("childId") childId: Long
    ): LearningPath

    // Profile and Badges
    @GET("profile/{childId}")
    suspend fun getLearningProfile(@Path("childId") childId: Long): LearningProfileDTO

    @GET("profile/{childId}/badges")
    suspend fun getBadges(@Path("childId") childId: Long): List<BadgeDTO>

    @GET("profile/{childId}/badges/new")
    suspend fun getNewBadges(@Path("childId") childId: Long): List<BadgeDTO>

    @POST("profile/{childId}/badges/mark-seen")
    suspend fun markBadgesAsSeen(@Path("childId") childId: Long)
}

