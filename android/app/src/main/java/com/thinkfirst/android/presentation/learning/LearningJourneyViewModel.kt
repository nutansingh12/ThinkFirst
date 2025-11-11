package com.thinkfirst.android.presentation.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.model.LearningPath
import com.thinkfirst.android.data.repository.LearningPathRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningJourneyViewModel @Inject constructor(
    private val learningPathRepository: LearningPathRepository
) : ViewModel() {

    private val _learningPath = MutableStateFlow<LearningPath?>(null)
    val learningPath: StateFlow<LearningPath?> = _learningPath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var childId: Long = 0

    fun setLearningPath(learningPath: LearningPath, childId: Long) {
        this.childId = childId
        _learningPath.value = learningPath
    }

    fun completeLesson(lessonId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val updatedPath = learningPathRepository.completeLesson(lessonId, childId)
                _learningPath.value = updatedPath

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to complete lesson"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLearningPath(learningPathId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val path = learningPathRepository.getLearningPath(learningPathId, childId)
                _learningPath.value = path

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load learning path"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

