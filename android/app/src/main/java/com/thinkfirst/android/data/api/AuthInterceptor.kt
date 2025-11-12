package com.thinkfirst.android.data.api

import android.util.Log
import com.google.gson.Gson
import com.thinkfirst.android.data.local.TokenManager
import com.thinkfirst.android.data.model.AuthResponse
import com.thinkfirst.android.data.model.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that adds JWT token to all API requests and handles automatic token refresh
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_TYPE = "Bearer"
    }

    // Separate OkHttp client for refresh requests to avoid infinite loop
    private val refreshClient = OkHttpClient.Builder().build()
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for login/register/refresh endpoints
        val url = originalRequest.url.toString()
        if (shouldSkipAuth(url)) {
            return chain.proceed(originalRequest)
        }

        // Get token synchronously (we're already in IO thread)
        val token = runBlocking {
            tokenManager.getAccessToken().first()
        }

        // If no token, proceed without auth header
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Add Authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, "$TOKEN_TYPE $token")
            .build()

        // Execute the request
        val response = chain.proceed(authenticatedRequest)

        // If we get 401 Unauthorized, try to refresh the token
        if (response.code == 401) {
            Log.d(TAG, "Received 401, attempting token refresh")
            response.close()

            // Try to refresh the token
            val newToken = refreshToken()

            if (newToken != null) {
                Log.d(TAG, "Token refreshed successfully, retrying request")

                // Retry the original request with the new token
                val newRequest = originalRequest.newBuilder()
                    .header(HEADER_AUTHORIZATION, "$TOKEN_TYPE $newToken")
                    .build()

                return chain.proceed(newRequest)
            } else {
                Log.e(TAG, "Token refresh failed, user needs to log in again")

                // Clear tokens - user needs to log in again
                runBlocking {
                    tokenManager.clearTokens()
                }
            }
        }

        return response
    }

    private fun shouldSkipAuth(url: String): Boolean {
        return url.contains("/auth/login") ||
               url.contains("/auth/register") ||
               url.contains("/auth/refresh-token")
    }

    /**
     * Attempt to refresh the access token using the refresh token
     * Returns the new access token if successful, null otherwise
     */
    private fun refreshToken(): String? {
        return try {
            val refreshToken = runBlocking {
                tokenManager.getRefreshToken().first()
            }

            if (refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token available")
                return null
            }

            // Get the base URL from the original request
            val baseUrl = runBlocking {
                // We need to construct the base URL - assuming it's stored or we can derive it
                // For now, we'll use BuildConfig or a constant
                com.thinkfirst.android.BuildConfig.API_BASE_URL
            }

            // Create refresh request
            val refreshRequestBody = RefreshTokenRequest(refreshToken)
            val jsonBody = gson.toJson(refreshRequestBody)

            val request = Request.Builder()
                .url("${baseUrl}auth/refresh-token")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            // Execute refresh request
            val response = refreshClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val authResponse = gson.fromJson(responseBody, AuthResponse::class.java)

                    // Update the tokens (preserves other user data like childId)
                    runBlocking {
                        tokenManager.updateTokens(
                            accessToken = authResponse.token,
                            refreshToken = authResponse.refreshToken ?: refreshToken
                        )
                    }

                    Log.d(TAG, "Tokens refreshed and saved successfully")
                    return authResponse.token
                }
            } else {
                Log.e(TAG, "Token refresh failed with code: ${response.code}")
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            null
        }
    }
}

