# TestSprite Commands for ThinkFirst

## 🎯 Quick Start

Copy and paste these commands into your IDE's AI assistant (with TestSprite MCP Server installed) to generate comprehensive tests for the ThinkFirst project.

---

## 📋 Backend Tests (Spring Boot)

### **1. Content Moderation Service Tests**

```
Create comprehensive unit tests for ContentModerationService in src/main/java/com/thinkfirst/service/ContentModerationService.java. 

Test cases should include:
1. Test moderation with flagged content (sexual, hate, harassment, self-harm, violence)
2. Test moderation with safe content
3. Test fail-open behavior when moderation service is down
4. Test with moderation disabled in config
5. Mock the OpenAI API calls

Use JUnit 5 and Mockito. Place tests in src/test/java/com/thinkfirst/service/ContentModerationServiceTest.java
```

---

### **2. Rate Limiting Service Tests**

```
Create comprehensive unit tests for RateLimitService in src/main/java/com/thinkfirst/service/RateLimitService.java.

Test cases should include:
1. Test chat rate limit (100 requests/hour)
2. Test quiz rate limit (10 requests/hour)
3. Test auth rate limit (5 requests/hour)
4. Test daily question limit (50 requests/day)
5. Test rate limit reset after TTL expires
6. Test concurrent requests
7. Mock Redis operations

Use JUnit 5, Mockito, and embedded Redis for testing. Place tests in src/test/java/com/thinkfirst/service/RateLimitServiceTest.java
```

---

### **3. Authentication Service Tests**

```
Create comprehensive unit tests for AuthService in src/main/java/com/thinkfirst/service/AuthService.java.

Test cases should include:
1. Test user registration with valid data
2. Test user login with correct credentials
3. Test user login with incorrect credentials
4. Test refresh token generation
5. Test refresh token validation
6. Test token rotation on refresh
7. Test expired refresh token handling
8. Mock JwtTokenProvider and UserRepository

Use JUnit 5 and Mockito. Place tests in src/test/java/com/thinkfirst/service/AuthServiceTest.java
```

---

### **4. Chat Service Tests**

```
Create comprehensive unit tests for ChatService in src/main/java/com/thinkfirst/service/ChatService.java.

Test cases should include:
1. Test quiz-gating logic with different score thresholds (70%+, 40-70%, 0-40%)
2. Test adaptive response levels (FULL_ANSWER, PARTIAL_HINT, GUIDED_QUESTIONS)
3. Test content moderation integration
4. Test AI provider fallback (Gemini → Groq → OpenAI)
5. Test chat session creation and message history
6. Mock ContentModerationService, AIProviderService, and repositories

Use JUnit 5 and Mockito. Place tests in src/test/java/com/thinkfirst/service/ChatServiceTest.java
```

---

### **5. API Integration Tests**

```
Create integration tests for the ThinkFirst REST API.

Test the following endpoints:
1. POST /api/auth/register - Test registration flow
2. POST /api/auth/login - Test login flow
3. POST /api/auth/refresh-token - Test token refresh
4. POST /api/chat/query - Test chat with rate limiting and content moderation
5. POST /api/quiz/submit - Test quiz submission with rate limiting
6. GET /api/dashboard/child/{childId}/progress - Test progress retrieval
7. GET /api/dashboard/child/{childId}/achievements - Test achievements retrieval

Use Spring Boot Test, @SpringBootTest, TestRestTemplate, and embedded PostgreSQL + Redis.
Place tests in src/test/java/com/thinkfirst/integration/ApiIntegrationTest.java
```

---

## 📱 Android Tests (Kotlin + Compose)

### **6. Login Screen UI Tests**

```
Create UI tests for LoginScreen in android/app/src/main/java/com/thinkfirst/android/presentation/auth/LoginScreen.kt.

Test cases should include:
1. Test email field validation (empty, invalid format)
2. Test password field validation (empty, too short)
3. Test password visibility toggle
4. Test login button click with valid credentials
5. Test login button disabled when loading
6. Test error message display
7. Test navigation to register screen

Use Jetpack Compose Testing, JUnit 4, and Espresso.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/presentation/auth/LoginScreenTest.kt
```

---

### **7. Register Screen UI Tests**

```
Create UI tests for RegisterScreen in android/app/src/main/java/com/thinkfirst/android/presentation/auth/RegisterScreen.kt.

Test cases should include:
1. Test all field validations (fullName, email, password, confirmPassword)
2. Test password matching validation
3. Test register button click with valid data
4. Test register button disabled when loading
5. Test error message display
6. Test navigation to login screen

Use Jetpack Compose Testing, JUnit 4, and Espresso.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/presentation/auth/RegisterScreenTest.kt
```

---

### **8. Dashboard Screen UI Tests**

```
Create UI tests for DashboardScreen in android/app/src/main/java/com/thinkfirst/android/presentation/dashboard/DashboardScreen.kt.

Test cases should include:
1. Test streak display with different values
2. Test statistics cards display (quizzes, avg score, questions, time)
3. Test achievement list display
4. Test loading state
5. Test error state
6. Test navigation to chat screen

Use Jetpack Compose Testing, JUnit 4, and Espresso.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/presentation/dashboard/DashboardScreenTest.kt
```

---

### **9. Token Manager Tests**

```
Create unit tests for TokenManager in android/app/src/main/java/com/thinkfirst/android/data/local/TokenManager.kt.

Test cases should include:
1. Test saveTokens() saves all data correctly
2. Test getAccessToken() retrieves correct token
3. Test getRefreshToken() retrieves correct token
4. Test isAuthenticated() returns true when tokens exist
5. Test isAuthenticated() returns false when tokens don't exist
6. Test clearTokens() removes all data
7. Test updateTokens() updates existing tokens

Use JUnit 4, Mockito, and Turbine for Flow testing.
Place tests in android/app/src/test/java/com/thinkfirst/android/data/local/TokenManagerTest.kt
```

---

### **10. Auth ViewModel Tests**

```
Create unit tests for AuthViewModel in android/app/src/main/java/com/thinkfirst/android/presentation/auth/AuthViewModel.kt.

Test cases should include:
1. Test checkAuthStatus() loads user data when authenticated
2. Test checkAuthStatus() sets unauthenticated state when no tokens
3. Test login() with valid credentials saves tokens
4. Test login() with invalid credentials shows error
5. Test register() with valid data saves tokens
6. Test register() with invalid data shows error
7. Test logout() clears tokens and resets state
8. Mock TokenManager and ThinkFirstApi

Use JUnit 4, Mockito, Turbine, and Coroutines Test.
Place tests in android/app/src/test/java/com/thinkfirst/android/presentation/auth/AuthViewModelTest.kt
```

---

### **11. Dashboard ViewModel Tests**

```
Create unit tests for DashboardViewModel in android/app/src/main/java/com/thinkfirst/android/presentation/dashboard/DashboardViewModel.kt.

Test cases should include:
1. Test loadProgress() fetches data successfully
2. Test loadProgress() handles API errors
3. Test loadProgress() handles network errors
4. Test UI state updates correctly
5. Mock ThinkFirstApi

Use JUnit 4, Mockito, Turbine, and Coroutines Test.
Place tests in android/app/src/test/java/com/thinkfirst/android/presentation/dashboard/DashboardViewModelTest.kt
```

---

### **12. Navigation Tests**

```
Create navigation tests for the ThinkFirst Android app.

Test cases should include:
1. Test navigation from Login to Chat after successful login
2. Test navigation from Login to Register
3. Test navigation from Register to Chat after successful registration
4. Test navigation from Chat to Dashboard
5. Test back navigation from Dashboard to Chat
6. Test deep linking to specific screens
7. Test auto-login navigation on app start

Use Jetpack Compose Testing, Navigation Testing, and JUnit 4.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/navigation/NavigationTest.kt
```

---

## 🔄 Running All Tests

### **Generate All Backend Tests**

```
Generate all backend tests for ThinkFirst:
1. ContentModerationServiceTest
2. RateLimitServiceTest
3. AuthServiceTest
4. ChatServiceTest
5. ApiIntegrationTest

Use JUnit 5, Mockito, Spring Boot Test, and embedded databases.
```

---

### **Generate All Android Tests**

```
Generate all Android tests for ThinkFirst:
1. LoginScreenTest
2. RegisterScreenTest
3. DashboardScreenTest
4. TokenManagerTest
5. AuthViewModelTest
6. DashboardViewModelTest
7. NavigationTest

Use JUnit 4, Mockito, Jetpack Compose Testing, Turbine, and Coroutines Test.
```

---

## 📊 Deploy to TestSprite Cloud

```
Deploy all ThinkFirst tests to TestSprite cloud for continuous monitoring.

Configure:
1. Run tests every hour
2. Alert on failures via email
3. Auto-heal flaky tests
4. Generate daily test reports
5. Monitor test execution time and cost
```

---

## 🎉 Complete Test Suite

```
Create a complete test suite for the ThinkFirst project covering:

Backend (Spring Boot):
- Content moderation service
- Rate limiting service
- Authentication service with refresh tokens
- Chat service with quiz-gating logic
- All REST API endpoints

Android (Kotlin + Compose):
- Login screen UI
- Register screen UI
- Dashboard screen UI
- Token manager
- Auth view model
- Dashboard view model
- Navigation flow

Use appropriate testing frameworks for each platform and achieve 90%+ code coverage.
```

---

**Last Updated:** 2025-11-07
**Status:** Ready to Execute

