# AI Provider Service Tests

This directory contains comprehensive tests for the AIProviderService to help debug and verify AI provider functionality.

## Test Files

### 1. `AIProviderServiceDebugTest.java` (Unit Tests)
**Purpose**: Fast unit tests with mocked dependencies

**What it tests**:
- ✅ Provider availability and fallback logic
- ✅ Question generation with proper format
- ✅ Educational response generation
- ✅ Hint generation
- ✅ Subject analysis
- ✅ Cache integration
- ✅ Error handling and recovery
- ✅ Provider status monitoring

**How to run**:
```bash
# Run just this test
mvn test -Dtest=AIProviderServiceDebugTest

# Or using your IDE: Right-click on the file and select "Run Test"
```

**Expected result**: All 16 tests should pass in < 1 second

---

### 2. `AIProviderServiceIntegrationTest.java` (Integration Tests)
**Purpose**: Real API calls to verify actual AI provider functionality

⚠️ **IMPORTANT**: This makes REAL API calls and requires API keys!

**What it tests**:
- ✅ Real API calls to configured providers
- ✅ Verifies questions have REAL answer text (not just A,B,C,D)
- ✅ Educational response quality
- ✅ Hint generation quality
- ✅ Subject classification accuracy
- ✅ Caching effectiveness
- ✅ Provider availability

**Prerequisites**:
1. Set up at least one AI provider API key:
   ```bash
   # Groq (FREE - Recommended for testing)
   export GROQ_API_KEY="your-groq-api-key"
   export GROQ_ENABLED=true
   
   # OR Gemini (FREE)
   export GEMINI_API_KEY="your-gemini-api-key"
   export GEMINI_ENABLED=true
   
   # OR OpenAI (PAID)
   export OPENAI_API_KEY="your-openai-api-key"
   export OPENAI_ENABLED=true
   ```

2. Get a FREE Groq API key:
   - Go to https://console.groq.com
   - Sign up (free)
   - Create an API key
   - Copy and export it as shown above

**How to run**:
```bash
# Set your API key first
export GROQ_API_KEY="your-key-here"
export GROQ_ENABLED=true

# Run the integration test
mvn test -Dtest=AIProviderServiceIntegrationTest

# Or using your IDE: Right-click on the file and select "Run Test"
```

**Expected result**: 
- All 7 tests should pass
- You should see actual quiz questions with real answer text
- Example output:
  ```
  --- Question 1 ---
  Text: What is the value of x in the equation 2 + x = 5?
  Options:
    1. 1
    2. 2
    3. 3  ✅ (Correct)
    4. 4
  Explanation: 2 + 3 = 5, so x = 3
  ```

**NOT this** (which would indicate the bug):
  ```
  --- Question 1 ---
  Text: What is the value of x in the equation 2 + x = 5?
  Options:
    1. A
    2. B
    3. C  ✅ (Correct)
    4. D
  ```

---

## Quick Start Guide

### Option 1: Run Unit Tests (No API key needed)
```bash
cd /Users/nsingh/IdeaProjects/ThinkFirst
mvn test -Dtest=AIProviderServiceDebugTest
```

### Option 2: Run Integration Tests (Requires API key)
```bash
# 1. Get a FREE Groq API key from https://console.groq.com
# 2. Set the environment variable
export GROQ_API_KEY="gsk_your_key_here"
export GROQ_ENABLED=true

# 3. Run the test
cd /Users/nsingh/IdeaProjects/ThinkFirst
mvn test -Dtest=AIProviderServiceIntegrationTest
```

### Option 3: Run All Tests
```bash
cd /Users/nsingh/IdeaProjects/ThinkFirst
mvn test
```

---

## Debugging Tips

### If tests fail:

1. **Check provider availability**:
   - Look for "Provider Status" in the test output
   - Make sure at least one provider shows `Available: true`

2. **Check API keys**:
   ```bash
   echo $GROQ_API_KEY  # Should show your key
   echo $GROQ_ENABLED  # Should show "true"
   ```

3. **Check the logs**:
   - Tests output detailed logs showing which provider is being used
   - Look for "Attempting generateQuestions with provider: Groq"

4. **Verify question format**:
   - Integration test will FAIL if questions have A,B,C,D instead of real text
   - Look for the error message: "CRITICAL BUG: Option is just a single letter"

5. **Test individual providers**:
   ```bash
   # Test just Groq
   export GROQ_ENABLED=true
   export GEMINI_ENABLED=false
   export OPENAI_ENABLED=false
   mvn test -Dtest=AIProviderServiceIntegrationTest
   ```

---

## Expected Test Output

### Successful Unit Test Output:
```
>>> TEST 1: All providers available
✅ TEST 1 PASSED: Gemini was used as first priority

>>> TEST 2: Gemini unavailable, fallback to Groq
✅ TEST 2 PASSED: Groq was used as fallback

... (14 more tests)

ALL TESTS COMPLETED
Summary:
  ✅ Provider availability and fallback logic
  ✅ Question generation with proper format
  ✅ Educational response generation
  ✅ Hint generation
  ✅ Subject analysis
  ✅ Cache integration
  ✅ Error handling and recovery
  ✅ Provider status monitoring
```

### Successful Integration Test Output:
```
>>> INTEGRATION TEST 1: Check provider availability
Provider Status:
  Gemini (gemini) - Available: false
  Groq (groq) - Available: true
  OpenAI (openai) - Available: false
✅ TEST 1 PASSED: At least one provider is available

>>> INTEGRATION TEST 2: Generate quiz questions with REAL API
Generated 3 questions:

--- Question 1 ---
Text: What is the value of x in the equation 2 + x = 5?
Type: MULTIPLE_CHOICE
Options:
  1. 1
  2. 2
  3. 3
  4. 4
Correct Answer: 3 (index 2)
Explanation: 2 + 3 = 5, so x = 3

✅ TEST 2 PASSED: Questions generated with proper format (NOT just A,B,C,D)

... (5 more tests)

INTEGRATION TEST SUMMARY
All integration tests completed successfully!

Verified:
  ✅ At least one AI provider is available and working
  ✅ Quiz questions are generated with REAL answer text (not A,B,C,D)
  ✅ Educational responses are generated correctly
  ✅ Hints are generated correctly
  ✅ Subject classification works
  ✅ Caching prevents duplicate API calls
  ✅ Provider testing functionality works
```

---

## Troubleshooting

### "No AI providers are available"
- Make sure you've set the API key environment variable
- Check that the provider is enabled: `export GROQ_ENABLED=true`
- Verify the API key is valid

### "CRITICAL BUG: Option is just a single letter"
- This means the AI is still generating A,B,C,D instead of real answers
- Check that the latest code changes are deployed
- Verify the prompt in GroqService.java and PromptOptimizer.java

### "Rate limit exceeded"
- Groq free tier: 14,400 requests/day
- Wait a few minutes and try again
- Or use a different provider

### Tests are slow
- First run will be slower (real API calls)
- Subsequent runs should be faster (caching)
- Unit tests should always be fast (< 1 second)

---

## CI/CD Integration

To run these tests in CI/CD:

```yaml
# GitHub Actions example
- name: Run Unit Tests
  run: mvn test -Dtest=AIProviderServiceDebugTest

- name: Run Integration Tests
  env:
    GROQ_API_KEY: ${{ secrets.GROQ_API_KEY }}
    GROQ_ENABLED: true
  run: mvn test -Dtest=AIProviderServiceIntegrationTest
```

---

## Questions?

If you encounter any issues:
1. Check the logs - they're very detailed
2. Run the unit tests first (no API key needed)
3. Then run integration tests with a valid API key
4. Look for specific error messages in the output

Happy testing! 🚀

