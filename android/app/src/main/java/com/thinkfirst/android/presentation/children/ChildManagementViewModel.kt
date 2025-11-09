package com.thinkfirst.android.presentation.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.local.TokenManager
import com.thinkfirst.android.data.model.ChildProfile
import com.thinkfirst.android.data.model.CreateChildRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildManagementUiState(
    val isLoading: Boolean = false,
    val children: List<ChildProfile> = emptyList(),
    val error: String? = null,
    val parentId: Long? = null
)

@HiltViewModel
class ChildManagementViewModel @Inject constructor(
    private val api: ThinkFirstApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildManagementUiState())
    val uiState: StateFlow<ChildManagementUiState> = _uiState.asStateFlow()

    init {
        loadParentId()
    }

    private fun loadParentId() {
        viewModelScope.launch {
            val parentId = tokenManager.getUserId().first()
            _uiState.value = _uiState.value.copy(parentId = parentId)
            if (parentId != null) {
                loadChildren(parentId)
            }
        }
    }

    fun loadChildren(parentId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val children = api.getParentChildren(parentId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    children = children
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load children"
                )
            }
        }
    }

    fun createChild(
        username: String,
        password: String,
        age: Int,
        gradeLevel: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val parentId = _uiState.value.parentId
            if (parentId == null) {
                onError("Parent ID not found")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val request = CreateChildRequest(
                    username = username,
                    password = password,
                    age = age,
                    gradeLevel = gradeLevel,
                    parentId = parentId
                )
                
                api.createChild(request)
                
                // Reload children list
                loadChildren(parentId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create child"
                )
                onError(e.message ?: "Failed to create child")
            }
        }
    }

    fun updateChild(
        childId: Long,
        username: String,
        password: String,
        age: Int,
        gradeLevel: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val parentId = _uiState.value.parentId
            if (parentId == null) {
                onError("Parent ID not found")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val request = CreateChildRequest(
                    username = username,
                    password = password,
                    age = age,
                    gradeLevel = gradeLevel,
                    parentId = parentId
                )
                
                api.updateChild(childId, request)
                
                // Reload children list
                loadChildren(parentId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update child"
                )
                onError(e.message ?: "Failed to update child")
            }
        }
    }

    fun deleteChild(
        childId: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val parentId = _uiState.value.parentId
            if (parentId == null) {
                onError("Parent ID not found")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                api.deleteChild(childId)
                
                // Reload children list
                loadChildren(parentId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete child"
                )
                onError(e.message ?: "Failed to delete child")
            }
        }
    }
}

