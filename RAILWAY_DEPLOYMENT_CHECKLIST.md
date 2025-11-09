# Railway Backend Deployment Checklist

## 🎯 **Current Status**

- ✅ **Android App**: Configured to use `https://thinkfirst-backend-production.up.railway.app/api/`
- ✅ **APK**: Rebuilt and installed on device RFCX91N92YX
- ⏳ **Railway Backend**: Needs to be deployed/fixed

---

## 🚀 **Railway Deployment Steps**

### **1. Check Railway Dashboard**

Visit [railway.app](https://railway.app) and check:

- [ ] Is the project created?
- [ ] Is the service deployed?
- [ ] What's the deployment status? (Building, Deployed, Failed, Crashed)
- [ ] Are there any error logs?

### **2. Verify Domain/URL**

In Railway dashboard:

- [ ] Check the actual domain assigned to your service
- [ ] Verify it matches: `thinkfirst-backend-production.up.railway.app`
- [ ] If different, update the Android app with the correct URL

### **3. Required Environment Variables**

Make sure these are set in Railway:

```bash
# AI Provider API Keys
GEMINI_API_KEY=your-gemini-api-key
GROQ_API_KEY=your-groq-api-key
OPENAI_API_KEY=your-openai-api-key

# JWT Configuration
JWT_SECRET=your-secret-key-minimum-256-bits

# Database (if not using Railway PostgreSQL)
DB_USERNAME=postgres
DB_PASSWORD=your-db-password

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

### **4. Add PostgreSQL Database**

In Railway dashboard:

1. Click **"New"** → **"Database"** → **"PostgreSQL"**
2. Railway will automatically set `DATABASE_URL` environment variable
3. Your Spring Boot app should auto-configure from `DATABASE_URL`

### **5. Add Redis Database**

In Railway dashboard:

1. Click **"New"** → **"Database"** → **"Redis"**
2. Railway will automatically set `REDIS_URL` environment variable
3. Update `application.yml` to use `REDIS_URL` if needed

### **6. Configure Build Settings**

Railway should auto-detect Maven/Spring Boot, but verify:

**Build Command**: (Usually auto-detected)
```bash
mvn clean package -DskipTests
```

**Start Command**: (Usually auto-detected)
```bash
java -jar target/thinkfirst-0.0.1-SNAPSHOT.jar
```

**Port**: Railway auto-assigns, Spring Boot should use `${PORT}` env variable

### **7. Update application.yml for Railway**

Make sure your `application.yml` supports Railway's environment:

```yaml
server:
  port: ${PORT:8080}  # Railway provides PORT env variable

spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/thinkfirst}
  
  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}
```

---

## 🔍 **Common Railway Issues**

### **Issue 1: "Application not found" (404)**

**Cause**: Service not deployed or crashed

**Solutions**:
- Check deployment logs in Railway dashboard
- Verify the service is running (not crashed)
- Check if domain is correctly configured

### **Issue 2: Build Failures**

**Cause**: Java version mismatch, missing dependencies, Lombok issues

**Solutions**:
- Check Railway build logs
- Verify Java 17 is being used
- Make sure `pom.xml` is correct
- Check Lombok configuration

### **Issue 3: Database Connection Errors**

**Cause**: PostgreSQL not connected or wrong credentials

**Solutions**:
- Verify PostgreSQL service is running
- Check `DATABASE_URL` environment variable
- Verify Flyway migrations are working
- Check database logs

### **Issue 4: Redis Connection Errors**

**Cause**: Redis not connected or wrong URL

**Solutions**:
- Verify Redis service is running
- Check `REDIS_URL` environment variable
- Update Spring configuration to use `REDIS_URL`

### **Issue 5: Missing Environment Variables**

**Cause**: API keys or secrets not set

**Solutions**:
- Add all required environment variables in Railway
- Restart the service after adding variables
- Check logs for "missing" or "null" errors

---

## 🛠️ **Debugging Railway Deployment**

### **View Logs**

In Railway dashboard:
1. Click on your service
2. Go to **"Deployments"** tab
3. Click on the latest deployment
4. View **"Build Logs"** and **"Deploy Logs"**

### **Common Log Errors**

**Error**: `Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin`
- **Fix**: Java version issue, check Railway is using Java 17

**Error**: `Cannot find symbol` (Lombok related)
- **Fix**: Lombok annotation processing issue, verify `pom.xml` configuration

**Error**: `Connection refused` (Database)
- **Fix**: PostgreSQL not connected, add PostgreSQL service

**Error**: `Unable to connect to Redis`
- **Fix**: Redis not connected, add Redis service

**Error**: `Port 8080 already in use`
- **Fix**: Use `${PORT}` environment variable instead of hardcoded 8080

---

## 📋 **Backend Configuration Files to Check**

### **1. pom.xml**

Make sure it's configured for Java 17:

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

### **2. application.yml**

Make sure it uses environment variables:

```yaml
server:
  port: ${PORT:8080}

spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/thinkfirst}
  
  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}

ai:
  gemini:
    api-key: ${GEMINI_API_KEY}
  groq:
    api-key: ${GROQ_API_KEY}
  openai:
    api-key: ${OPENAI_API_KEY}

jwt:
  secret: ${JWT_SECRET}
```

### **3. Procfile (Optional)**

Create a `Procfile` in project root if Railway doesn't auto-detect:

```
web: java -Dserver.port=$PORT -jar target/thinkfirst-0.0.1-SNAPSHOT.jar
```

---

## ✅ **Verification Steps**

After deploying to Railway:

### **1. Test Root Endpoint**
```bash
curl https://thinkfirst-backend-production.up.railway.app/
```
**Expected**: Should not return "Application not found"

### **2. Test API Endpoint**
```bash
curl https://thinkfirst-backend-production.up.railway.app/api/
```
**Expected**: 404 or API documentation (not "Application not found")

### **3. Test Login Endpoint**
```bash
curl -X POST https://thinkfirst-backend-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```
**Expected**: 401 Unauthorized or validation error (not "Application not found")

### **4. Test Swagger UI**
```bash
curl https://thinkfirst-backend-production.up.railway.app/swagger-ui.html
```
**Expected**: HTML page (not "Application not found")

---

## 🔄 **After Railway is Fixed**

Once Railway backend is deployed and working:

1. **Test the backend** using curl commands above
2. **Open the Android app** on your phone
3. **Try to register** a new account
4. **Try to login** with the account
5. **Test chat functionality**
6. **Test quiz functionality**

The Android app is already configured with the Railway URL, so it should work immediately once the backend is deployed!

---

## 📱 **If You Need to Change Backend URL Later**

If Railway assigns a different domain:

1. **Edit** `android/app/build.gradle.kts`
2. **Update** the `API_BASE_URL` in both `defaultConfig` and `buildTypes`
3. **Rebuild**:
   ```bash
   export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
   cd android
   ./gradlew assembleDebug
   ```
4. **Reinstall**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📊 **Summary**

### **Android App Status** ✅
- Backend URL: `https://thinkfirst-backend-production.up.railway.app/api/`
- APK: Built and installed on device
- Ready to connect once Railway backend is deployed

### **Railway Backend Status** ⏳
- Current: Not deployed or crashed (404 error)
- Needed: Deploy Spring Boot app to Railway
- Required: PostgreSQL, Redis, Environment Variables

### **Next Steps**
1. Check Railway dashboard
2. Verify/fix deployment
3. Add PostgreSQL and Redis services
4. Set environment variables
5. Test backend endpoints
6. Test Android app

---

**Good luck with the Railway deployment!** 🚀

Once the backend is running, the Android app should connect automatically.

