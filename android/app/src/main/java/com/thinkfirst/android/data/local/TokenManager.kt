package com.thinkfirst.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Manages JWT token storage and retrieval using DataStore
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val CHILD_ID_KEY = longPreferencesKey("child_id")
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val FULL_NAME_KEY = stringPreferencesKey("full_name")
    }
    
    /**
     * Save authentication tokens and user info
     */
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        childId: Long?,
        email: String,
        fullName: String
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            preferences[USER_ID_KEY] = userId
            childId?.let { preferences[CHILD_ID_KEY] = it }
            preferences[EMAIL_KEY] = email
            preferences[FULL_NAME_KEY] = fullName
        }
    }
    
    /**
     * Get access token
     */
    fun getAccessToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }
    
    /**
     * Get refresh token
     */
    fun getRefreshToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    }
    
    /**
     * Get user ID
     */
    fun getUserId(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }
    
    /**
     * Get child ID
     */
    fun getChildId(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[CHILD_ID_KEY]
        }
    }
    
    /**
     * Get email
     */
    fun getEmail(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[EMAIL_KEY]
        }
    }
    
    /**
     * Get full name
     */
    fun getFullName(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[FULL_NAME_KEY]
        }
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY] != null
        }
    }
    
    /**
     * Clear all tokens and user data (logout)
     */
    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Update access token (after refresh)
     */
    suspend fun updateAccessToken(accessToken: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
        }
    }
    
    /**
     * Update both tokens (after refresh with rotation)
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
}

