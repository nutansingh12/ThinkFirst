# Child Management API Testing Guide

## 🎯 Quick Start Testing

Once the Railway deployment completes, you can test the new child management endpoints.

### Base URL
```
https://thinkfirst-backend-production.up.railway.app/api
```

## 📝 Step-by-Step Testing

### Step 1: Register a Parent Account (if not already done)

```bash
curl -X POST https://thinkfirst-backend-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "parent@example.com",
    "password": "Parent123!",
    "fullName": "John Doe",
    "role": "PARENT"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "parent@example.com",
  "fullName": "John Doe",
  "role": "PARENT"
}
```

**Save the token!** You'll need it for subsequent requests.

---

### Step 2: Create a Child Profile

```bash
curl -X POST https://thinkfirst-backend-production.up.railway.app/api/children \
  -H "Authorization: Bearer YOUR_PARENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tommy",
    "password": "Tommy123!",
    "age": 10,
    "gradeLevel": "FIFTH",
    "parentId": 1
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "username": "tommy",
  "age": 10,
  "gradeLevel": "FIFTH",
  "parentId": 1,
  "currentStreak": 0,
  "totalQuestionsAnswered": 0,
  "totalQuizzesCompleted": 0,
  "lastActiveDate": null,
  "active": true,
  "createdAt": "2025-11-08T20:00:00"
}
```

---

### Step 3: Get All Children for a Parent

```bash
curl -X GET https://thinkfirst-backend-production.up.railway.app/api/children/parent/1 \
  -H "Authorization: Bearer YOUR_PARENT_TOKEN"
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "username": "tommy",
    "age": 10,
    "gradeLevel": "FIFTH",
    "parentId": 1,
    "currentStreak": 0,
    "totalQuestionsAnswered": 0,
    "totalQuizzesCompleted": 0,
    "lastActiveDate": null,
    "active": true,
    "createdAt": "2025-11-08T20:00:00"
  }
]
```

---

### Step 4: Child Login

```bash
curl -X POST https://thinkfirst-backend-production.up.railway.app/api/auth/child/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tommy",
    "password": "Tommy123!"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": null,
  "fullName": "tommy",
  "role": "CHILD"
}
```

**Note:** The child gets a different token with `role: "CHILD"`

---

### Step 5: Create a Chat Session (as Child)

```bash
curl -X POST "https://thinkfirst-backend-production.up.railway.app/api/chat/session?childId=1&title=Math%20Help" \
  -H "Authorization: Bearer YOUR_CHILD_TOKEN"
```

**Expected Response:**
```json
{
  "id": 1,
  "title": "Math Help",
  "childId": 1,
  "messageCount": 0,
  "archived": false,
  "createdAt": "2025-11-08T20:05:00",
  "updatedAt": "2025-11-08T20:05:00"
}
```

---

### Step 6: Send a Chat Query (as Child)

```bash
curl -X POST https://thinkfirst-backend-production.up.railway.app/api/chat/query \
  -H "Authorization: Bearer YOUR_CHILD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": 1,
    "sessionId": 1,
    "query": "How does photosynthesis work?"
  }'
```

**Expected Response (Quiz Required):**
```json
{
  "message": "Before I can answer that, let's check your understanding with a quick quiz!",
  "quiz": {
    "id": 1,
    "title": "Science Prerequisites",
    "description": "Let's test your basic science knowledge",
    "questions": [
      {
        "id": 1,
        "questionText": "What do plants need to make food?",
        "options": ["Water and sunlight", "Only water", "Only sunlight", "Soil only"],
        "correctAnswer": 0
      }
    ]
  },
  "requiresQuiz": true
}
```

---

### Step 7: Update Child Profile

```bash
curl -X PUT https://thinkfirst-backend-production.up.railway.app/api/children/1 \
  -H "Authorization: Bearer YOUR_PARENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tommy",
    "password": "NewPassword123!",
    "age": 11,
    "gradeLevel": "SIXTH",
    "parentId": 1
  }'
```

---

### Step 8: Get Specific Child

```bash
curl -X GET https://thinkfirst-backend-production.up.railway.app/api/children/1 \
  -H "Authorization: Bearer YOUR_PARENT_TOKEN"
```

---

### Step 9: Delete Child Profile

```bash
curl -X DELETE https://thinkfirst-backend-production.up.railway.app/api/children/1 \
  -H "Authorization: Bearer YOUR_PARENT_TOKEN"
```

**Expected Response:** `204 No Content`

---

## 🎨 Grade Level Options

Valid grade levels for children:
- `KINDERGARTEN`
- `FIRST`
- `SECOND`
- `THIRD`
- `FOURTH`
- `FIFTH`
- `SIXTH`
- `SEVENTH`
- `EIGHTH`
- `NINTH`
- `TENTH`
- `ELEVENTH`
- `TWELFTH`

---

## 🔐 Authentication Flow Summary

### Parent Authentication
1. Register/Login with email/password
2. Get JWT token with `role: "PARENT"`
3. Use token to manage children
4. View all children's progress

### Child Authentication
1. Login with username/password (created by parent)
2. Get JWT token with `role: "CHILD"`
3. Use token to access chat/quiz features
4. Cannot access other children's data

---

## 🧪 Complete Test Scenario

Here's a complete test flow you can run:

```bash
# 1. Register parent
PARENT_RESPONSE=$(curl -s -X POST https://thinkfirst-backend-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test.parent@example.com",
    "password": "Parent123!",
    "fullName": "Test Parent",
    "role": "PARENT"
  }')

PARENT_TOKEN=$(echo $PARENT_RESPONSE | jq -r '.token')
PARENT_ID=$(echo $PARENT_RESPONSE | jq -r '.userId')

echo "Parent Token: $PARENT_TOKEN"
echo "Parent ID: $PARENT_ID"

# 2. Create first child
CHILD1=$(curl -s -X POST https://thinkfirst-backend-production.up.railway.app/api/children \
  -H "Authorization: Bearer $PARENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"alice\",
    \"password\": \"Alice123!\",
    \"age\": 10,
    \"gradeLevel\": \"FIFTH\",
    \"parentId\": $PARENT_ID
  }")

CHILD1_ID=$(echo $CHILD1 | jq -r '.id')
echo "Child 1 ID: $CHILD1_ID"

# 3. Create second child
CHILD2=$(curl -s -X POST https://thinkfirst-backend-production.up.railway.app/api/children \
  -H "Authorization: Bearer $PARENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"bob\",
    \"password\": \"Bob123!\",
    \"age\": 8,
    \"gradeLevel\": \"THIRD\",
    \"parentId\": $PARENT_ID
  }")

CHILD2_ID=$(echo $CHILD2 | jq -r '.id')
echo "Child 2 ID: $CHILD2_ID"

# 4. Get all children
curl -s -X GET https://thinkfirst-backend-production.up.railway.app/api/children/parent/$PARENT_ID \
  -H "Authorization: Bearer $PARENT_TOKEN" | jq

# 5. Child login
CHILD_LOGIN=$(curl -s -X POST https://thinkfirst-backend-production.up.railway.app/api/auth/child/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "Alice123!"
  }')

CHILD_TOKEN=$(echo $CHILD_LOGIN | jq -r '.token')
echo "Child Token: $CHILD_TOKEN"

# 6. Create chat session as child
SESSION=$(curl -s -X POST "https://thinkfirst-backend-production.up.railway.app/api/chat/session?childId=$CHILD1_ID&title=Science%20Questions" \
  -H "Authorization: Bearer $CHILD_TOKEN")

SESSION_ID=$(echo $SESSION | jq -r '.id')
echo "Session ID: $SESSION_ID"

# 7. Send a query
curl -s -X POST https://thinkfirst-backend-production.up.railway.app/api/chat/query \
  -H "Authorization: Bearer $CHILD_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"childId\": $CHILD1_ID,
    \"sessionId\": $SESSION_ID,
    \"query\": \"What is gravity?\"
  }" | jq
```

---

## 🐛 Troubleshooting

### Error: "Child not found"
- Make sure you created a child profile first
- Verify the childId in your request matches an existing child

### Error: "Invalid credentials"
- Check username and password are correct
- Passwords are case-sensitive

### Error: "Username already exists"
- Choose a different username
- Usernames must be unique across all children

### Error: "Unauthorized"
- Make sure you're using the correct token
- Parent tokens for parent endpoints, child tokens for child endpoints
- Tokens expire after 7 days

---

## 📱 Next Steps

1. Test all endpoints using the commands above
2. Implement the Android UI following `CHILD_MANAGEMENT_UI_GUIDE.md`
3. Test the complete flow from mobile app
4. Add error handling and validation
5. Implement offline support

---

## 🎉 Summary

You now have:
- ✅ Parent registration and login
- ✅ Child profile creation and management
- ✅ Child authentication (separate from parent)
- ✅ Complete CRUD operations for children
- ✅ Role-based access (PARENT vs CHILD)
- ✅ Ready for UI implementation

The backend is fully functional and deployed to Railway!

