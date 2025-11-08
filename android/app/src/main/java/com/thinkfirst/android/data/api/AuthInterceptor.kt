package com.thinkfirst.android.data.api

import com.thinkfirst.android.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that adds JWT token to all API requests
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for login/register endpoints
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login") || url.contains("/auth/register")) {
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
            .header("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}

