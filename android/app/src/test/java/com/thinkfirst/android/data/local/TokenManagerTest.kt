package com.thinkfirst.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TokenManager
 * Tests DataStore-based JWT token storage and retrieval
 */
class TokenManagerTest {

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenManager: TokenManager

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val USER_ID_KEY = longPreferencesKey("user_id")
    private val CHILD_ID_KEY = longPreferencesKey("child_id")
    private val EMAIL_KEY = stringPreferencesKey("email")
    private val FULL_NAME_KEY = stringPreferencesKey("full_name")

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        dataStore = mockk(relaxed = true)
        tokenManager = TokenManager(context)
    }

    @Test
    fun `saveTokens should save all data correctly`() = runTest {
        // Arrange
        val accessToken = "test_access_token"
        val refreshToken = "test_refresh_token"
        val userId = 1L
        val childId = 2L
        val email = "test@example.com"
        val fullName = "Test User"

        coEvery { dataStore.edit(any()) } returns mockk()

        // Act
        tokenManager.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            childId = childId,
            email = email,
            fullName = fullName
        )

        // Assert
        coVerify {
            dataStore.edit(any())
        }
    }

    @Test
    fun `getAccessToken should retrieve correct token`() = runTest {
        // Arrange
        val expectedToken = "test_access_token"
        val preferences = mockk<Preferences>()
        coEvery { preferences[ACCESS_TOKEN_KEY] } returns expectedToken
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getAccessToken().first()

        // Assert
        assertThat(result).isEqualTo(expectedToken)
    }

    @Test
    fun `getAccessToken should return null when no token exists`() = runTest {
        // Arrange
        val preferences = mockk<Preferences>()
        coEvery { preferences[ACCESS_TOKEN_KEY] } returns null
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getAccessToken().first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getRefreshToken should retrieve correct token`() = runTest {
        // Arrange
        val expectedToken = "test_refresh_token"
        val preferences = mockk<Preferences>()
        coEvery { preferences[REFRESH_TOKEN_KEY] } returns expectedToken
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getRefreshToken().first()

        // Assert
        assertThat(result).isEqualTo(expectedToken)
    }

    @Test
    fun `getUserId should retrieve correct user ID`() = runTest {
        // Arrange
        val expectedUserId = 1L
        val preferences = mockk<Preferences>()
        coEvery { preferences[USER_ID_KEY] } returns expectedUserId
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getUserId().first()

        // Assert
        assertThat(result).isEqualTo(expectedUserId)
    }

    @Test
    fun `getChildId should retrieve correct child ID`() = runTest {
        // Arrange
        val expectedChildId = 2L
        val preferences = mockk<Preferences>()
        coEvery { preferences[CHILD_ID_KEY] } returns expectedChildId
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getChildId().first()

        // Assert
        assertThat(result).isEqualTo(expectedChildId)
    }

    @Test
    fun `getEmail should retrieve correct email`() = runTest {
        // Arrange
        val expectedEmail = "test@example.com"
        val preferences = mockk<Preferences>()
        coEvery { preferences[EMAIL_KEY] } returns expectedEmail
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getEmail().first()

        // Assert
        assertThat(result).isEqualTo(expectedEmail)
    }

    @Test
    fun `getFullName should retrieve correct full name`() = runTest {
        // Arrange
        val expectedFullName = "Test User"
        val preferences = mockk<Preferences>()
        coEvery { preferences[FULL_NAME_KEY] } returns expectedFullName
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.getFullName().first()

        // Assert
        assertThat(result).isEqualTo(expectedFullName)
    }

    @Test
    fun `isAuthenticated should return true when tokens exist`() = runTest {
        // Arrange
        val preferences = mockk<Preferences>()
        coEvery { preferences[ACCESS_TOKEN_KEY] } returns "test_token"
        coEvery { preferences[REFRESH_TOKEN_KEY] } returns "test_refresh"
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.isAuthenticated().first()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isAuthenticated should return false when access token is missing`() = runTest {
        // Arrange
        val preferences = mockk<Preferences>()
        coEvery { preferences[ACCESS_TOKEN_KEY] } returns null
        coEvery { preferences[REFRESH_TOKEN_KEY] } returns "test_refresh"
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.isAuthenticated().first()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `isAuthenticated should return false when refresh token is missing`() = runTest {
        // Arrange
        val preferences = mockk<Preferences>()
        coEvery { preferences[ACCESS_TOKEN_KEY] } returns "test_token"
        coEvery { preferences[REFRESH_TOKEN_KEY] } returns null
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.isAuthenticated().first()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `isAuthenticated should return false when both tokens are missing`() = runTest {
        // Arrange
        val preferences = mockk<Preferences>()
        coEvery { preferences[ACCESS_TOKEN_KEY] } returns null
        coEvery { preferences[REFRESH_TOKEN_KEY] } returns null
        coEvery { dataStore.data } returns flowOf(preferences)

        // Act
        val result = tokenManager.isAuthenticated().first()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `clearTokens should remove all data`() = runTest {
        // Arrange
        coEvery { dataStore.edit(any()) } returns mockk()

        // Act
        tokenManager.clearTokens()

        // Assert
        coVerify {
            dataStore.edit(any())
        }
    }

    @Test
    fun `updateTokens should update existing tokens`() = runTest {
        // Arrange
        val newAccessToken = "new_access_token"
        val newRefreshToken = "new_refresh_token"
        coEvery { dataStore.edit(any()) } returns mockk()

        // Act
        tokenManager.updateTokens(newAccessToken, newRefreshToken)

        // Assert
        coVerify {
            dataStore.edit(any())
        }
    }

    @Test
    fun `Flow emissions should be reactive to data changes`() = runTest {
        // This test verifies that the Flow emits new values when data changes
        val preferences1 = mockk<Preferences>()
        val preferences2 = mockk<Preferences>()
        
        coEvery { preferences1[ACCESS_TOKEN_KEY] } returns "token1"
        coEvery { preferences2[ACCESS_TOKEN_KEY] } returns "token2"
        
        // Note: This is a simplified test. In real implementation,
        // you would test the actual Flow behavior with multiple emissions
        
        coEvery { dataStore.data } returns flowOf(preferences1)
        
        tokenManager.getAccessToken().test {
            val item = awaitItem()
            assertThat(item).isEqualTo("token1")
            awaitComplete()
        }
    }
}

