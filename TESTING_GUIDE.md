# ThinkFirst - Testing Guide

## 🧪 Complete Testing Workflow

This guide walks you through testing all major features of the ThinkFirst application.

---

## Prerequisites

1. **Backend Running**: `docker-compose up -d` or `mvn spring-boot:run`
2. **Database Seeded**: Flyway migrations should have run automatically
3. **OpenAI API Key**: Set in environment variables
4. **API Tool**: Use curl, Postman, or Swagger UI

---

## Test Suite 1: Authentication Flow

### 1.1 Register Parent Account

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test.parent@example.com",
    "password": "SecurePass123!",
    "fullName": "Test Parent",
    "role": "PARENT"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "test.parent@example.com",
  "fullName": "Test Parent",
  "role": "PARENT"
}
```

**Save the token** for subsequent requests!

### 1.2 Login

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.parent@example.com",
    "password": "SecurePass123!"
  }'
```

**Expected**: Same response as registration

### 1.3 Test Invalid Credentials

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.parent@example.com",
    "password": "WrongPassword"
  }'
```

**Expected**: 401 Unauthorized

---

## Test Suite 2: Child Profile Setup

### 2.1 Create Child Profile (via Database)

Since we haven't implemented the child creation endpoint yet, use SQL:

```sql
-- Connect to database
docker exec -it thinkfirst-postgres psql -U thinkfirst -d thinkfirst

-- Create a child
INSERT INTO children (username, password, age, grade_level, parent_id, active, created_at, updated_at)
VALUES (
  'tommy_test',
  '$2a$10$dummypasswordhash',
  10,
  'FIFTH',
  1,  -- Your parent user ID
  true,
  NOW(),
  NOW()
);

-- Verify
SELECT id, username, age, grade_level FROM children;
```

**Note the child ID** for subsequent tests!

---

## Test Suite 3: Quiz-Gated Chat Flow

### 3.1 Create Chat Session

**Request:**
```bash
curl -X POST "http://localhost:8080/api/chat/session?childId=1&title=Science%20Questions" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "id": 1,
  "title": "Science Questions",
  "messageCount": 0,
  "archived": false,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### 3.2 Send Query (No Prerequisites - Should Trigger Quiz)

**Request:**
```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "sessionId": 1,
    "query": "How does photosynthesis work?"
  }'
```

**Expected Response:**
```json
{
  "message": null,
  "responseType": "QUIZ_REQUIRED",
  "quiz": {
    "id": 1,
    "title": "Science Prerequisite Quiz",
    "description": "Complete this quiz to unlock the answer",
    "questions": [
      {
        "id": 1,
        "questionText": "What is the primary source of energy for photosynthesis?",
        "type": "MULTIPLE_CHOICE",
        "options": ["Water", "Sunlight", "Soil", "Air"],
        "correctOptionIndex": 1,
        "explanation": null
      },
      // ... 4 more questions
    ],
    "passingScore": 70,
    "timeLimit": null,
    "type": "PREREQUISITE",
    "difficulty": "BEGINNER"
  },
  "hint": null,
  "messageId": null
}
```

### 3.3 Submit Quiz - Low Score (< 40%)

**Request:**
```bash
curl -X POST http://localhost:8080/api/quiz/submit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "quizId": 1,
    "answers": {
      "1": "0",
      "2": "0",
      "3": "0",
      "4": "0",
      "5": "0"
    },
    "timeSpentSeconds": 120
  }'
```

**Expected Response:**
```json
{
  "attemptId": 1,
  "score": 20,
  "passed": false,
  "responseLevel": "GUIDED_QUESTIONS",
  "feedbackMessage": "Let's work on the basics together! Try thinking about...",
  "questionResults": [
    {
      "questionId": 1,
      "questionText": "What is the primary source of energy...",
      "userAnswer": "Water",
      "correctAnswer": "Sunlight",
      "correct": false,
      "explanation": "Sunlight is the primary energy source..."
    }
    // ... more results
  ],
  "totalQuestions": 5,
  "correctAnswers": 1
}
```

### 3.4 Submit Quiz - Medium Score (40-70%)

**Request:**
```bash
curl -X POST http://localhost:8080/api/quiz/submit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "quizId": 1,
    "answers": {
      "1": "1",
      "2": "1",
      "3": "0",
      "4": "1",
      "5": "0"
    },
    "timeSpentSeconds": 180
  }'
```

**Expected Response:**
```json
{
  "score": 60,
  "passed": false,
  "responseLevel": "PARTIAL_HINT",
  "feedbackMessage": "Good effort! You're on the right track. Here's a hint..."
}
```

### 3.5 Submit Quiz - High Score (70%+)

**Request:**
```bash
curl -X POST http://localhost:8080/api/quiz/submit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "quizId": 1,
    "answers": {
      "1": "1",
      "2": "1",
      "3": "2",
      "4": "1",
      "5": "0"
    },
    "timeSpentSeconds": 200
  }'
```

**Expected Response:**
```json
{
  "score": 80,
  "passed": true,
  "responseLevel": "FULL_ANSWER",
  "feedbackMessage": "Excellent work! You've unlocked the full answer."
}
```

### 3.6 Ask Same Question Again (Should Get Full Answer)

**Request:**
```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "sessionId": 1,
    "query": "How does photosynthesis work?"
  }'
```

**Expected Response:**
```json
{
  "message": "Photosynthesis is the process by which plants...",
  "responseType": "FULL_ANSWER",
  "quiz": {
    // Verification quiz (optional)
  },
  "hint": null,
  "messageId": 2
}
```

---

## Test Suite 4: Progress Tracking

### 4.1 Get Child Progress

**Request:**
```bash
curl -X GET http://localhost:8080/api/dashboard/child/1/progress \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
{
  "childId": 1,
  "childUsername": "tommy_test",
  "currentStreak": 1,
  "totalQuizzesCompleted": 1,
  "totalQuestionsAnswered": 5,
  "averageScore": 80.0,
  "skillLevels": [
    {
      "id": 1,
      "proficiencyScore": 80,
      "currentLevel": "INTERMEDIATE",
      "quizzesCompleted": 1,
      "averageScore": 80
    }
  ],
  "recentAchievements": [
    {
      "badgeName": "First Steps",
      "description": "Completed your first quiz!",
      "type": "FIRST_STEPS",
      "points": 10
    }
  ],
  "subjectProgress": {
    "Science": {
      "subjectName": "Science",
      "proficiencyScore": 80,
      "currentLevel": "INTERMEDIATE",
      "quizzesCompleted": 1,
      "averageScore": 80
    }
  }
}
```

### 4.2 Get Achievements

**Request:**
```bash
curl -X GET http://localhost:8080/api/dashboard/child/1/achievements \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "badgeName": "First Steps",
    "description": "Completed your first quiz!",
    "iconUrl": null,
    "type": "FIRST_STEPS",
    "points": 10,
    "earnedAt": "2024-01-15T10:35:00"
  }
]
```

---

## Test Suite 5: Adaptive Difficulty

### 5.1 Complete Multiple Quizzes on Same Subject

Repeat the quiz submission with high scores (80%+) multiple times:

**After 3 high-scoring quizzes:**
- Proficiency should increase to 85-90
- Difficulty level should upgrade to ADVANCED
- Next quiz should have harder questions

**Verify:**
```bash
curl -X GET http://localhost:8080/api/dashboard/child/1/progress \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Check that `currentLevel` has changed from BEGINNER → INTERMEDIATE → ADVANCED

---

## Test Suite 6: Chat History

### 6.1 Get Chat History

**Request:**
```bash
curl -X GET http://localhost:8080/api/chat/session/1/history \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "role": "USER",
    "content": "How does photosynthesis work?",
    "createdAt": "2024-01-15T10:32:00"
  },
  {
    "id": 2,
    "role": "ASSISTANT",
    "content": "Photosynthesis is the process...",
    "createdAt": "2024-01-15T10:35:00"
  }
]
```

### 6.2 Get All Child Sessions

**Request:**
```bash
curl -X GET http://localhost:8080/api/chat/child/1/sessions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Test Suite 7: Edge Cases

### 7.1 Invalid Quiz Submission (Missing Answers)

**Request:**
```bash
curl -X POST http://localhost:8080/api/quiz/submit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "quizId": 1,
    "answers": {
      "1": "0"
    },
    "timeSpentSeconds": 10
  }'
```

**Expected**: Error or 0% score

### 7.2 Unauthorized Access

**Request:**
```bash
curl -X GET http://localhost:8080/api/dashboard/child/1/progress
```

**Expected**: 401 Unauthorized (no token)

### 7.3 Access Another Parent's Child

**Request:**
```bash
# Create another parent and try to access child ID 1
curl -X GET http://localhost:8080/api/dashboard/child/1/progress \
  -H "Authorization: Bearer DIFFERENT_PARENT_TOKEN"
```

**Expected**: 403 Forbidden (if authorization is implemented)

---

## Test Suite 8: Android App Testing

### 8.1 Setup
1. Open Android Studio
2. Start emulator or connect device
3. Run the app

### 8.2 Manual Test Cases

**TC1: Chat Interface**
- [ ] App launches successfully
- [ ] Chat screen displays
- [ ] Can type message
- [ ] Send button is enabled when text is entered

**TC2: Quiz Flow**
- [ ] Send a question
- [ ] Quiz dialog appears
- [ ] Can select answers
- [ ] Submit button enables when all answered
- [ ] Result dialog shows score

**TC3: UI/UX**
- [ ] Messages display in bubbles
- [ ] User messages align right (blue)
- [ ] AI messages align left (gray)
- [ ] Loading indicator shows during API calls
- [ ] Error messages display properly

---

## Performance Testing

### Load Test (Optional)

Use Apache JMeter or similar:

1. **Concurrent Users**: 100
2. **Requests per User**: 10 chat queries
3. **Expected**: < 2s response time for 95th percentile

---

## Checklist: All Features Working

- [ ] User registration
- [ ] User login
- [ ] JWT authentication
- [ ] Create chat session
- [ ] Send query triggers quiz
- [ ] Quiz generation (GPT-4)
- [ ] Quiz submission
- [ ] Score calculation
- [ ] Response level adaptation (GUIDED/PARTIAL/FULL)
- [ ] Skill level tracking
- [ ] Achievement awards
- [ ] Progress dashboard
- [ ] Chat history
- [ ] Android app connects to backend
- [ ] Android app displays chat
- [ ] Android app shows quiz dialog
- [ ] Docker deployment works

---

## Troubleshooting Common Issues

### Issue: Quiz not generated
- **Check**: OpenAI API key is valid
- **Check**: OpenAI API has credits
- **Check**: Model is accessible (try gpt-3.5-turbo)

### Issue: Database errors
- **Check**: Flyway migrations ran successfully
- **Check**: PostgreSQL is running
- **Check**: Connection string is correct

### Issue: 401 Unauthorized
- **Check**: JWT token is included in Authorization header
- **Check**: Token format is "Bearer <token>"
- **Check**: Token hasn't expired (24 hours)

---

## Next Steps

After all tests pass:
1. Write automated tests (JUnit, Mockito)
2. Set up CI/CD pipeline
3. Deploy to staging environment
4. Conduct user acceptance testing
5. Launch MVP!

Happy Testing! 🚀

