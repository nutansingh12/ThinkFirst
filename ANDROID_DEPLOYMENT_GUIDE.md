# 📱 Android App Deployment Guide

## ✅ Configuration Complete

Your Android app has been configured to use the Railway production backend!

### Changes Made:
- ✅ **Production URL**: `https://thinkfirst-backend-production.up.railway.app/api/`
- ✅ **Debug URL**: `http://10.0.2.2:8080/api/` (local emulator)
- ✅ **Build Configuration**: Separate URLs for debug and release builds

---

## 🚀 Option 1: Build APK with Android Studio (Recommended)

### Prerequisites:
- Android Studio installed
- Android SDK configured
- Java 17 installed

### Steps:

#### 1. Open Project in Android Studio
```bash
# Open Android Studio
# File → Open → Select: /Users/nsingh/IdeaProjects/ThinkFirst/android
```

#### 2. Sync Gradle
- Android Studio will automatically sync Gradle dependencies
- Wait for sync to complete (may take 5-10 minutes first time)

#### 3. Build Release APK
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Or use the terminal in Android Studio:
```bash
./gradlew assembleRelease
```

#### 4. Find Your APK
```
Location: android/app/build/outputs/apk/release/app-release.apk
```

#### 5. Install on Device
```bash
# Connect your Android device via USB
# Enable USB debugging on device

# Install APK
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 🚀 Option 2: Build APK with Command Line

### Prerequisites:
- Java 17 installed
- Android SDK installed
- ANDROID_HOME environment variable set

### Steps:

#### 1. Set Environment Variables
```bash
# Add to ~/.zshrc or ~/.bash_profile
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

#### 2. Create Gradle Wrapper (if not exists)
```bash
cd android
gradle wrapper --gradle-version 8.5
```

#### 3. Build Release APK
```bash
cd android
./gradlew assembleRelease
```

#### 4. Find Your APK
```
Location: android/app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 Testing the App

### 1. Test on Emulator
```bash
# Start Android emulator
emulator -avd <your_avd_name>

# Install APK
adb install app/build/outputs/apk/release/app-release.apk
```

### 2. Test on Physical Device
```bash
# Enable USB debugging on your Android device
# Connect device via USB

# Verify device is connected
adb devices

# Install APK
adb install app/build/outputs/apk/release/app-release.apk
```

### 3. Verify Backend Connection
- Open the app
- Try to register/login
- Check if API calls reach Railway backend
- Monitor Railway logs for incoming requests

---

## 📦 Option 3: Publish to Google Play Store

### Prerequisites:
- Google Play Developer account ($25 one-time fee)
- Signed APK or AAB (Android App Bundle)

### Steps:

#### 1. Generate Signing Key
```bash
cd android/app
keytool -genkey -v -keystore thinkfirst-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias thinkfirst
```

#### 2. Configure Signing in build.gradle.kts
Add to `android/app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("thinkfirst-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "thinkfirst"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### 3. Build Signed AAB (App Bundle)
```bash
cd android
./gradlew bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`

#### 4. Upload to Play Console
1. Go to https://play.google.com/console
2. Create new app
3. Fill in app details
4. Upload AAB file
5. Complete store listing
6. Submit for review

---

## 🔧 Troubleshooting

### Issue: Gradle Build Fails
**Solution**: 
```bash
cd android
./gradlew clean
./gradlew assembleRelease --stacktrace
```

### Issue: "SDK location not found"
**Solution**: Create `android/local.properties`:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### Issue: "Java version mismatch"
**Solution**: Ensure Java 17 is installed:
```bash
java -version  # Should show Java 17
```

### Issue: "Backend connection failed"
**Solution**: 
1. Check Railway backend is running
2. Test backend URL: `curl https://thinkfirst-backend-production.up.railway.app/actuator/health`
3. Check app logs for network errors

### Issue: "APK not installing"
**Solution**:
```bash
# Uninstall old version first
adb uninstall com.thinkfirst.android

# Then install new version
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 📊 Backend Status Check

### Check Railway Deployment
```bash
# Health check
curl https://thinkfirst-backend-production.up.railway.app/actuator/health

# Expected response:
{"status":"UP"}
```

### Monitor Railway Logs
```bash
railway logs
```

---

## 🎯 Next Steps

### 1. Test the App
- [ ] Install APK on device
- [ ] Test user registration
- [ ] Test login
- [ ] Test chat functionality
- [ ] Test quiz generation
- [ ] Test progress tracking

### 2. Prepare for Production
- [ ] Generate signed APK/AAB
- [ ] Test on multiple devices
- [ ] Prepare Play Store listing
- [ ] Create screenshots
- [ ] Write app description
- [ ] Set up privacy policy

### 3. Deploy to Play Store
- [ ] Create Play Console account
- [ ] Upload AAB
- [ ] Complete store listing
- [ ] Submit for review
- [ ] Monitor reviews and crashes

---

## 📝 Important Notes

### Backend URL Configuration
- **Production**: `https://thinkfirst-backend-production.up.railway.app/api/`
- **Debug**: `http://10.0.2.2:8080/api/` (emulator only)
- **Physical Device Debug**: Use your computer's local IP (e.g., `http://192.168.1.100:8080/api/`)

### Build Variants
- **Debug**: Uses local backend, includes debugging tools
- **Release**: Uses Railway production backend, optimized and minified

### Security
- ⚠️ **Never commit** signing keys to Git
- ⚠️ **Never commit** keystore passwords
- ✅ Use environment variables for sensitive data
- ✅ Add `*.jks` to `.gitignore`

---

## 🆘 Need Help?

### Common Commands
```bash
# Check Gradle version
./gradlew --version

# List all tasks
./gradlew tasks

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installRelease

# Run tests
./gradlew test
```

### Useful Links
- [Android Developer Guide](https://developer.android.com/studio/build/building-cmdline)
- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [Railway Documentation](https://docs.railway.app)

---

## ✅ Summary

Your Android app is now configured and ready to build! The easiest way is to:

1. **Open Android Studio**
2. **Open the `android` folder**
3. **Build → Build APK**
4. **Install on your device**
5. **Test with Railway backend**

Good luck! 🚀

