# TestSprite Test Generation Script

## 🎯 Overview

This document contains the exact prompts to use with TestSprite MCP Server to generate comprehensive tests for the ThinkFirst project.

**Prerequisites:**
- TestSprite MCP Server is installed in your IDE ✅
- Lombok compilation errors are fixed (see `LOMBOK_FIX_GUIDE.md`)

---

## 📋 Test Generation Sequence

### **Phase 1: Backend Unit Tests (30 minutes)**

Copy and paste these prompts one by one into your IDE's AI assistant:

---

#### **Test 1: Content Moderation Service**

```
Create comprehensive unit tests for ContentModerationService located at src/main/java/com/thinkfirst/service/ContentModerationService.java.

Requirements:
- Test moderation with flagged content for each category: sexual, hate, harassment, self-harm, violence
- Test moderation with safe content returns approved status
- Test fail-open behavior when OpenAI API is down (should allow content)
- Test with moderation disabled in config (moderation.enabled=false)
- Mock WebClient and OpenAI API responses
- Use JUnit 5, Mockito, and AssertJ
- Achieve 100% code coverage

Place tests in: src/test/java/com/thinkfirst/service/ContentModerationServiceTest.java
```

---

#### **Test 2: Rate Limiting Service**

```
Create comprehensive unit tests for RateLimitService located at src/main/java/com/thinkfirst/service/RateLimitService.java.

Requirements:
- Test chat rate limit: 100 requests/hour per child
- Test quiz rate limit: 10 requests/hour per child
- Test auth rate limit: 5 requests/hour per IP
- Test daily question limit: 50 requests/day per child
- Test rate limit reset after TTL expires
- Test concurrent requests don't exceed limits
- Test RateLimitException is thrown when limit exceeded
- Mock RedisTemplate operations
- Use JUnit 5, Mockito, and AssertJ
- Achieve 100% code coverage

Place tests in: src/test/java/com/thinkfirst/service/RateLimitServiceTest.java
```

---

#### **Test 3: Authentication Service**

```
Create comprehensive unit tests for AuthService located at src/main/java/com/thinkfirst/service/AuthService.java.

Requirements:
- Test user registration with valid data creates user and returns tokens
- Test user login with correct credentials returns access and refresh tokens
- Test user login with incorrect credentials throws exception
- Test refresh token generation creates 7-day token
- Test refresh token validation with valid token returns new tokens
- Test refresh token validation with expired token throws exception
- Test refresh token validation with invalid token throws exception
- Test token rotation on refresh (old refresh token should be invalidated)
- Mock JwtTokenProvider, UserRepository, PasswordEncoder
- Use JUnit 5, Mockito, and AssertJ
- Achieve 100% code coverage

Place tests in: src/test/java/com/thinkfirst/service/AuthServiceTest.java
```

---

#### **Test 4: Chat Service**

```
Create comprehensive unit tests for ChatService located at src/main/java/com/thinkfirst/service/ChatService.java.

Requirements:
- Test quiz-gating logic with score >= 70% returns FULL_ANSWER
- Test quiz-gating logic with score 40-70% returns PARTIAL_HINT
- Test quiz-gating logic with score < 40% returns GUIDED_QUESTIONS
- Test content moderation integration blocks flagged content
- Test content moderation integration allows safe content
- Test AI provider fallback: Gemini → Groq → OpenAI
- Test chat session creation and message history
- Test quiz generation after chat query
- Mock ContentModerationService, AIProviderService, QuizService, repositories
- Use JUnit 5, Mockito, and AssertJ
- Achieve 100% code coverage

Place tests in: src/test/java/com/thinkfirst/service/ChatServiceTest.java
```

---

### **Phase 2: Backend Integration Tests (20 minutes)**

---

#### **Test 5: API Integration Tests**

```
Create integration tests for the ThinkFirst REST API.

Requirements:
Test the following endpoints with real Spring context:

1. POST /api/auth/register
   - Test successful registration returns 200 and tokens
   - Test duplicate email returns 400
   - Test invalid email format returns 400

2. POST /api/auth/login
   - Test successful login returns 200 and tokens
   - Test wrong password returns 401
   - Test non-existent user returns 401

3. POST /api/auth/refresh-token
   - Test valid refresh token returns new tokens
   - Test expired refresh token returns 401
   - Test invalid refresh token returns 401

4. POST /api/chat/query (with authentication)
   - Test chat query with valid data returns response
   - Test rate limiting blocks after 100 requests/hour
   - Test content moderation blocks inappropriate content

5. POST /api/quiz/submit (with authentication)
   - Test quiz submission with correct answers returns high score
   - Test quiz submission with wrong answers returns low score
   - Test rate limiting blocks after 10 requests/hour

6. GET /api/dashboard/child/{childId}/progress (with authentication)
   - Test returns progress data for valid child
   - Test returns 404 for non-existent child

7. GET /api/dashboard/child/{childId}/achievements (with authentication)
   - Test returns achievements for valid child
   - Test returns empty list for child with no achievements

Use:
- @SpringBootTest with WebEnvironment.RANDOM_PORT
- TestRestTemplate for HTTP requests
- @Testcontainers with PostgreSQL and Redis containers
- @BeforeEach to set up test data
- @AfterEach to clean up test data
- JUnit 5 and AssertJ

Place tests in: src/test/java/com/thinkfirst/integration/ApiIntegrationTest.java
```

---

### **Phase 3: Android Unit Tests (25 minutes)**

---

#### **Test 6: Token Manager Tests**

```
Create unit tests for TokenManager located at android/app/src/main/java/com/thinkfirst/android/data/local/TokenManager.kt.

Requirements:
- Test saveTokens() saves all data correctly to DataStore
- Test getAccessToken() retrieves correct token
- Test getRefreshToken() retrieves correct token
- Test getUserId() retrieves correct user ID
- Test getChildId() retrieves correct child ID
- Test getEmail() retrieves correct email
- Test getFullName() retrieves correct full name
- Test isAuthenticated() returns true when tokens exist
- Test isAuthenticated() returns false when tokens don't exist
- Test clearTokens() removes all data
- Test updateTokens() updates existing tokens
- Use Turbine for Flow testing
- Use JUnit 4, MockK, and Truth assertions
- Achieve 100% code coverage

Place tests in: android/app/src/test/java/com/thinkfirst/android/data/local/TokenManagerTest.kt
```

---

#### **Test 7: Auth ViewModel Tests**

```
Create unit tests for AuthViewModel located at android/app/src/main/java/com/thinkfirst/android/presentation/auth/AuthViewModel.kt.

Requirements:
- Test checkAuthStatus() loads user data when authenticated
- Test checkAuthStatus() sets unauthenticated state when no tokens
- Test login() with valid credentials saves tokens and sets authenticated state
- Test login() with invalid credentials shows error message
- Test login() with network error shows error message
- Test register() with valid data saves tokens and sets authenticated state
- Test register() with invalid data shows error message
- Test register() with duplicate email shows error message
- Test logout() clears tokens and resets state
- Mock TokenManager and ThinkFirstApi
- Use JUnit 4, MockK, Turbine, and Coroutines Test
- Achieve 100% code coverage

Place tests in: android/app/src/test/java/com/thinkfirst/android/presentation/auth/AuthViewModelTest.kt
```

---

#### **Test 8: Dashboard ViewModel Tests**

```
Create unit tests for DashboardViewModel located at android/app/src/main/java/com/thinkfirst/android/presentation/dashboard/DashboardViewModel.kt.

Requirements:
- Test loadProgress() fetches data successfully and updates UI state
- Test loadProgress() handles API error (4xx) and shows error message
- Test loadProgress() handles network error and shows error message
- Test loadProgress() handles server error (5xx) and shows error message
- Test UI state updates correctly with progress data
- Test UI state updates correctly with achievements data
- Mock ThinkFirstApi
- Use JUnit 4, MockK, Turbine, and Coroutines Test
- Achieve 100% code coverage

Place tests in: android/app/src/test/java/com/thinkfirst/android/presentation/dashboard/DashboardViewModelTest.kt
```

---

### **Phase 4: Android UI Tests (25 minutes)**

---

#### **Test 9: Login Screen UI Tests**

```
Create UI tests for LoginScreen located at android/app/src/main/java/com/thinkfirst/android/presentation/auth/LoginScreen.kt.

Requirements:
- Test email field displays and accepts input
- Test email field shows error for empty input
- Test email field shows error for invalid format
- Test password field displays and accepts input
- Test password field shows error for empty input
- Test password visibility toggle works
- Test login button is enabled with valid input
- Test login button is disabled when loading
- Test login button click triggers login
- Test error message displays correctly
- Test navigation to register screen works
- Use Jetpack Compose Testing (ComposeTestRule)
- Use JUnit 4 and Truth assertions
- Test with both light and dark themes

Place tests in: android/app/src/androidTest/java/com/thinkfirst/android/presentation/auth/LoginScreenTest.kt
```

---

#### **Test 10: Register Screen UI Tests**

```
Create UI tests for RegisterScreen located at android/app/src/main/java/com/thinkfirst/android/presentation/auth/RegisterScreen.kt.

Requirements:
- Test all fields display and accept input (fullName, email, password, confirmPassword)
- Test fullName field shows error for empty input
- Test email field shows error for invalid format
- Test password field shows error for too short password
- Test confirmPassword field shows error when passwords don't match
- Test password visibility toggles work for both fields
- Test register button is enabled with valid input
- Test register button is disabled when loading
- Test register button click triggers registration
- Test error message displays correctly
- Test navigation to login screen works
- Use Jetpack Compose Testing (ComposeTestRule)
- Use JUnit 4 and Truth assertions

Place tests in: android/app/src/androidTest/java/com/thinkfirst/android/presentation/auth/RegisterScreenTest.kt
```

---

#### **Test 11: Dashboard Screen UI Tests**

```
Create UI tests for DashboardScreen located at android/app/src/main/java/com/thinkfirst/android/presentation/dashboard/DashboardScreen.kt.

Requirements:
- Test streak display shows correct value
- Test statistics cards display correct data (quizzes, avg score, questions, time)
- Test achievement list displays correctly
- Test achievement icons display correctly
- Test loading state shows progress indicator
- Test error state shows error message
- Test navigation to chat screen works
- Test pull-to-refresh works
- Use Jetpack Compose Testing (ComposeTestRule)
- Use JUnit 4 and Truth assertions

Place tests in: android/app/src/androidTest/java/com/thinkfirst/android/presentation/dashboard/DashboardScreenTest.kt
```

---

#### **Test 12: Navigation Tests**

```
Create navigation tests for the ThinkFirst Android app.

Requirements:
- Test app starts on Login screen when not authenticated
- Test app starts on Chat screen when authenticated
- Test navigation from Login to Chat after successful login
- Test navigation from Login to Register
- Test navigation from Register to Chat after successful registration
- Test navigation from Chat to Dashboard
- Test back navigation from Dashboard to Chat
- Test back navigation from Register to Login
- Test deep linking to Chat screen
- Test deep linking to Dashboard screen
- Use Jetpack Compose Testing and Navigation Testing
- Use JUnit 4 and Truth assertions

Place tests in: android/app/src/androidTest/java/com/thinkfirst/android/navigation/NavigationTest.kt
```

---

## 🎉 Summary

After running all 12 test generation prompts, you will have:

**Backend Tests:**
- 4 unit test suites (ContentModeration, RateLimit, Auth, Chat)
- 1 integration test suite (API endpoints)

**Android Tests:**
- 3 unit test suites (TokenManager, AuthViewModel, DashboardViewModel)
- 4 UI test suites (Login, Register, Dashboard, Navigation)

**Total Coverage:**
- 90%+ code coverage
- All critical paths tested
- Auto-healing enabled via TestSprite
- Continuous monitoring ready

---

**Last Updated:** 2025-11-07
**Status:** Ready to Generate Tests

