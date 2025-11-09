# Child Management & Authentication UI Guide

## 🎯 Overview

This guide explains how to implement a complete child management system with two authentication modes:
- **Parent Mode**: For parents to manage child profiles and view progress
- **Child Mode**: For children to access the app with simplified login

## 📱 User Flow

### Parent Flow
```
1. Parent Login (email/password)
   ↓
2. Child Management Screen
   - View all children
   - Add new child
   - Edit/Delete children
   ↓
3. Select a child to view their dashboard
   ↓
4. View child's progress, chat history, achievements
```

### Child Flow
```
1. Child Login (username/password)
   ↓
2. Direct to Chat Screen
   - Age-appropriate UI
   - No access to settings
   - No access to other children's data
```

## 🔧 Backend API Endpoints (Already Implemented)

### Authentication
- `POST /api/auth/register` - Parent registration
- `POST /api/auth/login` - Parent login (email/password)
- `POST /api/auth/child/login` - **NEW** Child login (username/password)
- `POST /api/auth/refresh-token` - Refresh JWT token

### Child Management
- `POST /api/children` - Create a child profile
- `GET /api/children/parent/{parentId}` - Get all children for a parent
- `GET /api/children/{childId}` - Get specific child
- `PUT /api/children/{childId}` - Update child profile
- `DELETE /api/children/{childId}` - Delete child profile

## 📋 Implementation Steps

### Step 1: Update Android Data Models

Add child-related DTOs to match backend:

**File**: `android/app/src/main/java/com/thinkfirst/android/data/model/ChildProfile.kt`
```kotlin
data class ChildProfile(
    val id: Long,
    val username: String,
    val age: Int,
    val gradeLevel: String?,
    val parentId: Long,
    val currentStreak: Int,
    val totalQuestionsAnswered: Int,
    val totalQuizzesCompleted: Int,
    val lastActiveDate: String?,
    val active: Boolean,
    val createdAt: String
)

data class CreateChildRequest(
    val username: String,
    val password: String,
    val age: Int,
    val gradeLevel: String?,
    val parentId: Long
)

data class ChildLoginRequest(
    val username: String,
    val password: String
)
```

### Step 2: Update API Service

**File**: `android/app/src/main/java/com/thinkfirst/android/data/remote/ThinkFirstApi.kt`

Add these endpoints:
```kotlin
interface ThinkFirstApi {
    // Existing endpoints...
    
    @POST("auth/child/login")
    suspend fun childLogin(@Body request: ChildLoginRequest): AuthResponse
    
    @POST("children")
    suspend fun createChild(@Body request: CreateChildRequest): ChildProfile
    
    @GET("children/parent/{parentId}")
    suspend fun getParentChildren(@Path("parentId") parentId: Long): List<ChildProfile>
    
    @GET("children/{childId}")
    suspend fun getChild(@Path("childId") childId: Long): ChildProfile
    
    @PUT("children/{childId}")
    suspend fun updateChild(
        @Path("childId") childId: Long,
        @Body request: CreateChildRequest
    ): ChildProfile
    
    @DELETE("children/{childId}")
    suspend fun deleteChild(@Path("childId") childId: Long)
}
```

### Step 3: Create Login Mode Selection Screen

**File**: `android/app/src/main/java/com/thinkfirst/android/presentation/auth/LoginModeScreen.kt`

```kotlin
@Composable
fun LoginModeScreen(
    onParentLoginClick: () -> Unit,
    onChildLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to ThinkFirst",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Parent Login Button
        Button(
            onClick = onParentLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("I'm a Parent/Educator")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Child Login Button
        OutlinedButton(
            onClick = onChildLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Face, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("I'm a Student")
        }
    }
}
```

### Step 4: Create Child Login Screen

**File**: `android/app/src/main/java/com/thinkfirst/android/presentation/auth/ChildLoginScreen.kt`

```kotlin
@Composable
fun ChildLoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val loginState by viewModel.loginState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Student Login",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) 
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility 
                        else Icons.Default.VisibilityOff,
                        "Toggle password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { viewModel.childLogin(username, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank()
        ) {
            Text("Login")
        }
        
        // Handle login state
        when (loginState) {
            is LoginState.Success -> {
                LaunchedEffect(Unit) {
                    onLoginSuccess((loginState as LoginState.Success).userId)
                }
            }
            is LoginState.Error -> {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}
```

### Step 5: Create Child Management Screen (for Parents)

**File**: `android/app/src/main/java/com/thinkfirst/android/presentation/children/ChildManagementScreen.kt`

```kotlin
@Composable
fun ChildManagementScreen(
    viewModel: ChildManagementViewModel = hiltViewModel(),
    onChildSelected: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val children by viewModel.children.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Children") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Child")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(children) { child ->
                ChildCard(
                    child = child,
                    onClick = { onChildSelected(child.id) }
                )
            }
            
            if (children.isEmpty()) {
                item {
                    EmptyState(
                        message = "No children added yet",
                        onAddClick = { showAddDialog = true }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddChildDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { username, password, age, gradeLevel ->
                viewModel.createChild(username, password, age, gradeLevel)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ChildCard(child: ChildProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.username,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Age ${child.age} • ${child.gradeLevel ?: "No grade"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${child.totalQuestionsAnswered} questions • ${child.currentStreak} day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(Icons.Default.ChevronRight, "View")
        }
    }
}
```

## 🔄 Updated Navigation Flow

Update your navigation to include:
```kotlin
sealed class Screen(val route: String) {
    object LoginMode : Screen("login_mode")
    object ParentLogin : Screen("parent_login")
    object ChildLogin : Screen("child_login")
    object Register : Screen("register")
    object ChildManagement : Screen("child_management")
    object Chat : Screen("chat/{childId}")
    object Dashboard : Screen("dashboard/{childId}")
    object Quiz : Screen("quiz/{quizId}/{childId}")
}
```

## 📝 Testing the Flow

### Test Parent Flow:
1. Launch app → Select "I'm a Parent/Educator"
2. Login with parent credentials
3. See child management screen (empty initially)
4. Click "+" to add a child
5. Fill in: username, password, age, grade level
6. Child appears in list
7. Click on child to view their dashboard

### Test Child Flow:
1. Launch app → Select "I'm a Student"
2. Login with child username/password
3. Directly access chat screen
4. Ask questions and complete quizzes

## 🎨 UI/UX Recommendations

1. **Parent Mode**:
   - Professional, clean interface
   - Dashboard with analytics
   - Ability to manage multiple children
   - Progress tracking and reports

2. **Child Mode**:
   - Colorful, friendly interface
   - Large buttons and text
   - Gamification elements
   - Encouraging messages

3. **Security**:
   - Auto-logout after inactivity
   - Require parent password to delete children
   - Parental controls for content filtering

## 🚀 Next Steps

1. Implement the screens in the order listed above
2. Test the complete flow end-to-end
3. Add error handling and loading states
4. Implement offline support for child profiles
5. Add parental controls and content filtering
6. Consider adding profile pictures for children
7. Add achievement badges and rewards

## 📚 Additional Features to Consider

- **Family Sharing**: Multiple parents can manage the same children
- **Progress Reports**: Weekly/monthly email reports to parents
- **Screen Time Limits**: Set daily usage limits per child
- **Content Filtering**: Age-appropriate content controls
- **Rewards System**: Points, badges, and achievements
- **Parent Dashboard**: Analytics and insights

