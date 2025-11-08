# TestSprite Setup Guide for ThinkFirst

## 📖 Overview

This guide will help you set up **TestSprite MCP Server** for comprehensive testing of the ThinkFirst project (Spring Boot backend + Android app).

TestSprite is an AI-powered autonomous testing framework that:
- Generates tests using natural language prompts
- Supports Spring Boot and Android testing
- Provides auto-healing for flaky tests
- Achieves 90%+ code quality with 10x faster testing

---

## 🚀 Installation

### **Step 1: Install TestSprite MCP Server**

TestSprite integrates with your IDE through the Model Context Protocol (MCP).

**For VS Code / Cursor / Windsurf:**

1. Open your IDE settings
2. Navigate to MCP Server configuration
3. Add TestSprite MCP Server:

```json
{
  "mcpServers": {
    "testsprite": {
      "command": "npx",
      "args": ["-y", "@testsprite/mcp-server"],
      "env": {
        "TESTSPRITE_API_KEY": "your-api-key-here"
      }
    }
  }
}
```

4. Get your API key from: https://www.testsprite.com/auth/cognito/sign-in
5. Restart your IDE

### **Step 2: Verify Installation**

In your IDE's AI assistant, type:
```
"Check if TestSprite is installed"
```

You should see TestSprite tools available in the MCP tools list.

---

## 🧪 Backend Testing (Spring Boot)

### **Test Suite 1: Content Moderation Service**

**Natural Language Prompt:**
```
"Create comprehensive unit tests for ContentModerationService in src/main/java/com/thinkfirst/service/ContentModerationService.java. 

Test cases should include:
1. Test moderation with flagged content (sexual, hate, harassment, self-harm, violence)
2. Test moderation with safe content
3. Test fail-open behavior when moderation service is down
4. Test with moderation disabled in config
5. Mock the OpenAI API calls

Use JUnit 5 and Mockito. Place tests in src/test/java/com/thinkfirst/service/ContentModerationServiceTest.java"
```

### **Test Suite 2: Rate Limiting Service**

**Natural Language Prompt:**
```
"Create comprehensive unit tests for RateLimitService in src/main/java/com/thinkfirst/service/RateLimitService.java.

Test cases should include:
1. Test chat rate limit (100 requests/hour)
2. Test quiz rate limit (10 requests/hour)
3. Test auth rate limit (5 requests/hour)
4. Test daily question limit (50 requests/day)
5. Test rate limit reset after TTL expires
6. Test concurrent requests
7. Mock Redis operations

Use JUnit 5, Mockito, and embedded Redis for testing. Place tests in src/test/java/com/thinkfirst/service/RateLimitServiceTest.java"
```

### **Test Suite 3: Authentication Service**

**Natural Language Prompt:**
```
"Create comprehensive unit tests for AuthService in src/main/java/com/thinkfirst/service/AuthService.java.

Test cases should include:
1. Test user registration with valid data
2. Test user login with correct credentials
3. Test user login with incorrect credentials
4. Test refresh token generation
5. Test refresh token validation
6. Test token rotation on refresh
7. Test expired refresh token handling
8. Mock JwtTokenProvider and UserRepository

Use JUnit 5 and Mockito. Place tests in src/test/java/com/thinkfirst/service/AuthServiceTest.java"
```

### **Test Suite 4: Chat Service**

**Natural Language Prompt:**
```
"Create comprehensive unit tests for ChatService in src/main/java/com/thinkfirst/service/ChatService.java.

Test cases should include:
1. Test quiz-gating logic with different score thresholds (70%+, 40-70%, 0-40%)
2. Test adaptive response levels (FULL_ANSWER, PARTIAL_HINT, GUIDED_QUESTIONS)
3. Test content moderation integration
4. Test AI provider fallback (Gemini → Groq → OpenAI)
5. Test chat session creation and message history
6. Mock ContentModerationService, AIProviderService, and repositories

Use JUnit 5 and Mockito. Place tests in src/test/java/com/thinkfirst/service/ChatServiceTest.java"
```

### **Test Suite 5: API Integration Tests**

**Natural Language Prompt:**
```
"Create integration tests for the ThinkFirst REST API.

Test the following endpoints:
1. POST /api/auth/register - Test registration flow
2. POST /api/auth/login - Test login flow
3. POST /api/auth/refresh-token - Test token refresh
4. POST /api/chat/query - Test chat with rate limiting and content moderation
5. POST /api/quiz/submit - Test quiz submission with rate limiting
6. GET /api/dashboard/child/{childId}/progress - Test progress retrieval
7. GET /api/dashboard/child/{childId}/achievements - Test achievements retrieval

Use Spring Boot Test, @SpringBootTest, TestRestTemplate, and embedded PostgreSQL + Redis.
Place tests in src/test/java/com/thinkfirst/integration/ApiIntegrationTest.java"
```

---

## 📱 Android Testing (Kotlin + Compose)

### **Test Suite 6: Login Screen UI Tests**

**Natural Language Prompt:**
```
"Create UI tests for LoginScreen in android/app/src/main/java/com/thinkfirst/android/presentation/auth/LoginScreen.kt.

Test cases should include:
1. Test email field validation (empty, invalid format)
2. Test password field validation (empty, too short)
3. Test password visibility toggle
4. Test login button click with valid credentials
5. Test login button disabled when loading
6. Test error message display
7. Test navigation to register screen

Use Jetpack Compose Testing, JUnit 4, and Espresso.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/presentation/auth/LoginScreenTest.kt"
```

### **Test Suite 7: Register Screen UI Tests**

**Natural Language Prompt:**
```
"Create UI tests for RegisterScreen in android/app/src/main/java/com/thinkfirst/android/presentation/auth/RegisterScreen.kt.

Test cases should include:
1. Test all field validations (fullName, email, password, confirmPassword)
2. Test password matching validation
3. Test register button click with valid data
4. Test register button disabled when loading
5. Test error message display
6. Test navigation to login screen

Use Jetpack Compose Testing, JUnit 4, and Espresso.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/presentation/auth/RegisterScreenTest.kt"
```

### **Test Suite 8: Token Manager Tests**

**Natural Language Prompt:**
```
"Create unit tests for TokenManager in android/app/src/main/java/com/thinkfirst/android/data/local/TokenManager.kt.

Test cases should include:
1. Test saveTokens() saves all data correctly
2. Test getAccessToken() retrieves correct token
3. Test getRefreshToken() retrieves correct token
4. Test isAuthenticated() returns true when tokens exist
5. Test isAuthenticated() returns false when tokens don't exist
6. Test clearTokens() removes all data
7. Test updateTokens() updates existing tokens

Use JUnit 4, Mockito, and Turbine for Flow testing.
Place tests in android/app/src/test/java/com/thinkfirst/android/data/local/TokenManagerTest.kt"
```

### **Test Suite 9: Auth ViewModel Tests**

**Natural Language Prompt:**
```
"Create unit tests for AuthViewModel in android/app/src/main/java/com/thinkfirst/android/presentation/auth/AuthViewModel.kt.

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
Place tests in android/app/src/test/java/com/thinkfirst/android/presentation/auth/AuthViewModelTest.kt"
```

### **Test Suite 10: Navigation Tests**

**Natural Language Prompt:**
```
"Create navigation tests for the ThinkFirst Android app.

Test cases should include:
1. Test navigation from Login to Chat after successful login
2. Test navigation from Login to Register
3. Test navigation from Register to Chat after successful registration
4. Test navigation from Chat to Dashboard
5. Test back navigation from Dashboard to Chat
6. Test deep linking to specific screens
7. Test auto-login navigation on app start

Use Jetpack Compose Testing, Navigation Testing, and JUnit 4.
Place tests in android/app/src/androidTest/java/com/thinkfirst/android/navigation/NavigationTest.kt"
```

---

## 🔄 Running Tests

### **Backend Tests (Maven)**

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ContentModerationServiceTest

# Run with coverage
./mvnw test jacoco:report
```

### **Android Tests (Gradle)**

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests TokenManagerTest
```

---

## 📊 Continuous Monitoring

### **Deploy Tests to TestSprite Cloud**

**Natural Language Prompt:**
```
"Deploy all ThinkFirst tests to TestSprite cloud for continuous monitoring.

Configure:
1. Run tests every hour
2. Alert on failures via email
3. Auto-heal flaky tests
4. Generate daily test reports
5. Monitor test execution time and cost
```

### **View Test Results**

Access the TestSprite Web Portal:
- URL: https://www.testsprite.com/
- View test execution history
- Monitor test health and coverage
- Analyze failure patterns
- Review auto-healing actions

---

## 🛠️ Troubleshooting

### **Issue: TestSprite MCP Server not found**

**Solution:**
1. Verify MCP configuration in IDE settings
2. Restart IDE after adding configuration
3. Check API key is valid
4. Run `npx -y @testsprite/mcp-server` manually to verify installation

### **Issue: Tests failing due to missing dependencies**

**Solution:**
1. Add test dependencies to `pom.xml` (backend) or `build.gradle.kts` (Android)
2. For backend: JUnit 5, Mockito, Spring Boot Test, Embedded Redis
3. For Android: JUnit 4, Mockito, Compose Testing, Turbine, Coroutines Test

### **Issue: Compilation errors preventing test execution**

**Solution:**
1. Fix pre-existing Lombok annotation processing issues
2. Configure IDE to enable annotation processing
3. Verify Java runtime is correctly configured
4. Run `./mvnw clean compile` to verify build

---

## 📚 Resources

- **TestSprite Documentation:** https://docs.testsprite.com/mcp/overview
- **TestSprite MCP Server:** https://docs.testsprite.com/mcp/getting-started/installation
- **TestSprite Web Portal:** https://www.testsprite.com/
- **Spring Boot Testing:** https://spring.io/guides/gs/testing-web/
- **Android Testing:** https://developer.android.com/training/testing
- **Jetpack Compose Testing:** https://developer.android.com/jetpack/compose/testing

---

**Last Updated:** 2025-11-07
**Status:** Ready for Test Generation

