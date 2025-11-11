package com.thinkfirst.android.data.repository

import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.model.LearningPath
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningPathRepository @Inject constructor(
    private val api: ThinkFirstApi
) {
    suspend fun completeLesson(lessonId: Long, childId: Long): LearningPath {
        return api.completeLesson(lessonId, childId)
    }

    suspend fun getLearningPath(learningPathId: Long, childId: Long): LearningPath {
        return api.getLearningPath(learningPathId, childId)
    }
}

