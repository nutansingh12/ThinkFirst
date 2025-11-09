package com.thinkfirst.android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.local.TokenManager
import com.thinkfirst.android.data.model.ChildLoginRequest
import com.thinkfirst.android.data.model.LoginRequest
import com.thinkfirst.android.data.model.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val userId: Long? = null,
    val childId: Long? = null,
    val email: String? = null,
    val fullName: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ThinkFirstApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check if user is already authenticated on app start
        checkAuthStatus()
    }

    /**
     * Check if user is already authenticated
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            val isAuthenticated = tokenManager.isAuthenticated().first()
            if (isAuthenticated) {
                // Load user data from token manager
                val token = tokenManager.getAccessToken().first()
                val refreshToken = tokenManager.getRefreshToken().first()
                val userId = tokenManager.getUserId().first()
                val childId = tokenManager.getChildId().first()
                val email = tokenManager.getEmail().first()
                val fullName = tokenManager.getFullName().first()

                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    token = token,
                    refreshToken = refreshToken,
                    userId = userId,
                    childId = childId,
                    email = email,
                    fullName = fullName
                )
            }
        }
    }

    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val response = api.login(LoginRequest(email, password))

                // Save tokens to DataStore
                // For now, we'll use a hardcoded child ID
                // In production, you'd fetch the user's children and let them select
                val childId = 1L // TODO: Fetch actual child ID from API

                tokenManager.saveTokens(
                    accessToken = response.token,
                    refreshToken = response.refreshToken ?: "",
                    userId = response.userId,
                    childId = childId,
                    email = response.email ?: "",
                    fullName = response.fullName
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    token = response.token,
                    refreshToken = response.refreshToken,
                    userId = response.userId,
                    childId = childId,
                    email = response.email,
                    fullName = response.fullName
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed. Please try again."
                )
            }
        }
    }
    
    /**
     * Register new user
     */
    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val response = api.register(
                    RegisterRequest(
                        fullName = fullName,
                        email = email,
                        password = password,
                        role = "PARENT"
                    )
                )

                // Save tokens to DataStore
                // For now, we'll use a hardcoded child ID
                // In production, you'd create a child profile after registration
                val childId = 1L // TODO: Create child profile and get ID from API

                tokenManager.saveTokens(
                    accessToken = response.token,
                    refreshToken = response.refreshToken ?: "",
                    userId = response.userId,
                    childId = childId,
                    email = response.email ?: "",
                    fullName = response.fullName
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    token = response.token,
                    refreshToken = response.refreshToken,
                    userId = response.userId,
                    childId = childId,
                    email = response.email,
                    fullName = response.fullName
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registration failed. Please try again."
                )
            }
        }
    }
    
    /**
     * Child login with username and password
     */
    fun childLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = api.childLogin(ChildLoginRequest(username, password))

                // For child login, the userId is actually the child ID
                val childId = response.userId

                tokenManager.saveTokens(
                    accessToken = response.token,
                    refreshToken = response.refreshToken ?: "",
                    userId = response.userId,
                    childId = childId,
                    email = response.email ?: "",
                    fullName = response.fullName
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    token = response.token,
                    refreshToken = response.refreshToken,
                    userId = response.userId,
                    childId = childId,
                    email = response.email,
                    fullName = response.fullName
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed. Please check your username and password."
                )
            }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            _uiState.value = AuthUiState()
        }
    }
}

