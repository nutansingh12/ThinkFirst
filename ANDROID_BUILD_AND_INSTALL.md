# Android App - Build and Install Guide

## 🎯 Overview

This guide will help you build and install the updated ThinkFirst Android app with the new child management features.

## 📱 What's New in This Version

### New Features:
1. **Dual Authentication System**
   - Parent/Educator login (email/password)
   - Child login (username/password)

2. **Child Management Dashboard** (for parents)
   - View all children
   - Add new child profiles
   - Delete child profiles
   - View child statistics (streak, questions answered)

3. **Improved Navigation Flow**
   - Login mode selection screen
   - Separate flows for parents and children
   - Parents manage children before accessing features
   - Children go directly to chat after login

## 🔧 Prerequisites

Make sure you have:
- Android Studio installed
- Android device or emulator
- USB debugging enabled (for physical device)

## 📦 Build and Install Steps

### Option 1: Using Android Studio (Recommended)

1. **Open the project in Android Studio**
   ```bash
   cd /Users/nsingh/IdeaProjects/ThinkFirst
   # Open Android Studio and select "Open an Existing Project"
   # Navigate to the android/ folder
   ```

2. **Sync Gradle**
   - Android Studio should automatically sync Gradle
   - If not, click "File" → "Sync Project with Gradle Files"

3. **Build the APK**
   - Click "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
   - Wait for the build to complete
   - You'll see a notification with "locate" link when done

4. **Install on Device**
   - Connect your Android device via USB
   - Click the green "Run" button (▶️) in Android Studio
   - Select your device from the list
   - The app will install and launch automatically

### Option 2: Using Command Line

1. **Navigate to android directory**
   ```bash
   cd /Users/nsingh/IdeaProjects/ThinkFirst/android
   ```

2. **Build the APK**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected device**
   ```bash
   ./gradlew installDebug
   ```

4. **Or manually install the APK**
   ```bash
   # The APK will be at:
   # android/app/build/outputs/apk/debug/app-debug.apk
   
   # Install using adb:
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 🧪 Testing the New Features

### Test Parent Flow:

1. **Launch the app**
   - You'll see the "Welcome to ThinkFirst" screen
   - Two buttons: "I'm a Parent/Educator" and "I'm a Student"

2. **Click "I'm a Parent/Educator"**
   - You'll be taken to the parent login screen
   - If you don't have an account, click "Don't have an account? Sign up"

3. **Register a parent account**
   - Enter your full name, email, and password
   - Click "Sign Up"

4. **Child Management Screen**
   - After login, you'll see "My Children" screen
   - Initially empty with "No Children Added Yet" message
   - Click the "+" floating action button

5. **Add a child**
   - Enter username (e.g., "tommy")
   - Enter password (e.g., "Tommy123!")
   - Enter age (e.g., 10)
   - Select grade level (optional)
   - Click "Add Child"

6. **View child in list**
   - The child will appear in the list
   - Shows username, age, grade, streak, and questions answered
   - Click on the child to view their dashboard

### Test Child Flow:

1. **Go back to login mode screen**
   - Click logout from child management screen
   - Or restart the app

2. **Click "I'm a Student"**
   - You'll see the child login screen

3. **Login with child credentials**
   - Enter the username you created (e.g., "tommy")
   - Enter the password (e.g., "Tommy123!")
   - Click "Login"

4. **Direct to Chat**
   - Child is taken directly to the chat screen
   - Can start asking questions immediately
   - Age-appropriate interface

## 🔍 Troubleshooting

### Build Errors

**Error: "SDK location not found"**
```bash
# Create local.properties file in android/ directory
echo "sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk" > android/local.properties
```

**Error: "Gradle sync failed"**
- Click "File" → "Invalidate Caches / Restart"
- Try "Build" → "Clean Project" then "Build" → "Rebuild Project"

**Error: "Unresolved reference"**
- Make sure all files are properly saved
- Try "File" → "Sync Project with Gradle Files"

### Installation Errors

**Error: "INSTALL_FAILED_UPDATE_INCOMPATIBLE"**
```bash
# Uninstall the old version first
adb uninstall com.thinkfirst.android
# Then install again
./gradlew installDebug
```

**Error: "Device not found"**
```bash
# Check if device is connected
adb devices
# If not listed, check USB debugging is enabled on device
```

### Runtime Errors

**Error: "Unable to connect to backend"**
- Check that Railway backend is running
- Verify the API_BASE_URL in `android/app/build.gradle.kts`
- Should be: `https://thinkfirst-backend-production.up.railway.app/api/`

**Error: "Login failed"**
- Make sure Railway deployment is complete
- Check backend logs for errors
- Verify credentials are correct

## 📊 Testing Checklist

- [ ] App launches successfully
- [ ] Login mode selection screen appears
- [ ] Parent login works
- [ ] Parent registration works
- [ ] Child management screen loads
- [ ] Can add a child profile
- [ ] Child appears in the list
- [ ] Can click on child to view dashboard
- [ ] Can delete a child
- [ ] Logout works
- [ ] Child login works
- [ ] Child goes directly to chat
- [ ] Can send chat messages
- [ ] Can complete quizzes

## 🎨 UI Screenshots Locations

After testing, the app should show:

1. **Login Mode Screen**: Two large buttons for parent/child selection
2. **Parent Login**: Email/password fields
3. **Child Management**: List of children with stats
4. **Add Child Dialog**: Form to create child profile
5. **Child Login**: Username/password fields (simpler UI)
6. **Chat Screen**: Same as before but accessible via child login

## 🚀 Next Steps After Installation

1. **Create a parent account** using the registration flow
2. **Add 1-2 child profiles** with different ages
3. **Test child login** with one of the created children
4. **Try the chat feature** as a child
5. **View child progress** from parent dashboard

## 📝 Notes

- The app connects to the Railway backend at:
  `https://thinkfirst-backend-production.up.railway.app/api/`
  
- All data is stored on the backend (PostgreSQL + Redis)

- Child passwords are encrypted on the backend

- JWT tokens are stored securely in Android DataStore

- Offline support is available for chat history (Room database)

## 🐛 Known Issues

None at the moment! If you encounter any issues:
1. Check the logcat in Android Studio
2. Verify backend is running on Railway
3. Check network connectivity
4. Try clearing app data and reinstalling

## ✅ Success Indicators

You'll know everything is working when:
- ✅ You can register a parent account
- ✅ You can add a child profile
- ✅ You can login as that child
- ✅ Child can access chat and ask questions
- ✅ Parent can view child's progress

Enjoy the new child management features! 🎉

