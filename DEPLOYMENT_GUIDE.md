# ThinkFirst Deployment Guide

## 🚀 Deployment Options

ThinkFirst can be deployed to various platforms. This guide covers the most common options.

---

## Option 1: Vercel (Recommended for Quick Deploy)

### Prerequisites
- Vercel account (free tier available)
- GitHub repository
- PostgreSQL database (e.g., Supabase, Neon, Railway)
- Redis instance (e.g., Upstash, Redis Cloud)

### Steps

#### 1. **Set up External Services**

**PostgreSQL Database** (Choose one):
- **Supabase** (Free): https://supabase.com
- **Neon** (Free): https://neon.tech
- **Railway** (Free tier): https://railway.app

**Redis** (Choose one):
- **Upstash** (Free): https://upstash.com
- **Redis Cloud** (Free): https://redis.com/try-free

#### 2. **Deploy to Vercel**

```bash
# Install Vercel CLI
npm install -g vercel

# Login to Vercel
vercel login

# Deploy
vercel

# Follow prompts:
# - Link to existing project or create new
# - Set project name: thinkfirst
# - Set framework preset: Other
```

#### 3. **Configure Environment Variables**

In Vercel Dashboard → Settings → Environment Variables, add:

```
# Database
DATABASE_URL=postgresql://user:password@host:5432/thinkfirst
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/thinkfirst
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# Redis
REDIS_HOST=your-redis-host.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT
JWT_SECRET=your-super-secret-jwt-key-min-256-bits

# AI Providers (Get FREE API keys)
GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key
OPENAI_API_KEY=your_openai_api_key  # Optional fallback

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

#### 4. **Deploy**

```bash
vercel --prod
```

---

## Option 2: Railway (Easiest Full-Stack Deploy)

### Steps

#### 1. **Create Railway Account**
- Go to https://railway.app
- Sign up with GitHub

#### 2. **Deploy from GitHub**

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Initialize project
railway init

# Link to GitHub repo
railway link

# Add PostgreSQL
railway add --plugin postgresql

# Add Redis
railway add --plugin redis

# Deploy
railway up
```

#### 3. **Set Environment Variables**

In Railway Dashboard → Variables:

```
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key
OPENAI_API_KEY=your_openai_api_key
SPRING_PROFILES_ACTIVE=prod
```

Railway automatically sets `DATABASE_URL` and `REDIS_URL`.

---

## Option 3: Heroku

### Steps

#### 1. **Install Heroku CLI**

```bash
# macOS
brew tap heroku/brew && brew install heroku

# Or download from https://devcenter.heroku.com/articles/heroku-cli
```

#### 2. **Create Heroku App**

```bash
# Login
heroku login

# Create app
heroku create thinkfirst-app

# Add PostgreSQL
heroku addons:create heroku-postgresql:mini

# Add Redis
heroku addons:create heroku-redis:mini
```

#### 3. **Configure Environment Variables**

```bash
heroku config:set JWT_SECRET=your-super-secret-jwt-key-min-256-bits
heroku config:set GEMINI_API_KEY=your_gemini_api_key
heroku config:set GROQ_API_KEY=your_groq_api_key
heroku config:set OPENAI_API_KEY=your_openai_api_key
heroku config:set SPRING_PROFILES_ACTIVE=prod
```

#### 4. **Deploy**

```bash
git push heroku main
```

---

## Option 4: Docker + Cloud (AWS, GCP, Azure)

### Prerequisites
- Docker installed
- Cloud account (AWS/GCP/Azure)

### Steps

#### 1. **Build Docker Image**

```bash
# Build
docker build -t thinkfirst:latest .

# Test locally
docker-compose up
```

#### 2. **Push to Container Registry**

**AWS ECR**:
```bash
aws ecr create-repository --repository-name thinkfirst
docker tag thinkfirst:latest <account-id>.dkr.ecr.<region>.amazonaws.com/thinkfirst:latest
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/thinkfirst:latest
```

**Google Container Registry**:
```bash
docker tag thinkfirst:latest gcr.io/<project-id>/thinkfirst:latest
docker push gcr.io/<project-id>/thinkfirst:latest
```

#### 3. **Deploy to Cloud Service**

**AWS ECS/Fargate**:
- Create ECS cluster
- Create task definition with environment variables
- Create service

**Google Cloud Run**:
```bash
gcloud run deploy thinkfirst \
  --image gcr.io/<project-id>/thinkfirst:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

**Azure Container Instances**:
```bash
az container create \
  --resource-group myResourceGroup \
  --name thinkfirst \
  --image <registry>/thinkfirst:latest \
  --dns-name-label thinkfirst \
  --ports 8080
```

---

## Option 5: DigitalOcean App Platform

### Steps

#### 1. **Create DigitalOcean Account**
- Go to https://www.digitalocean.com

#### 2. **Deploy from GitHub**
- Go to App Platform
- Click "Create App"
- Connect GitHub repository
- Select branch: `main`

#### 3. **Configure**
- **Build Command**: `mvn clean package -DskipTests`
- **Run Command**: `java -jar target/thinkfirst-0.0.1-SNAPSHOT.jar`
- **Port**: 8080

#### 4. **Add Database & Redis**
- Add PostgreSQL database
- Add Redis database

#### 5. **Set Environment Variables**
```
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key
OPENAI_API_KEY=your_openai_api_key
SPRING_PROFILES_ACTIVE=prod
```

---

## 🔑 Getting FREE API Keys

### Google Gemini (FREE - 1.5M requests/month)
1. Go to https://makersuite.google.com/app/apikey
2. Click "Create API Key"
3. Copy the key

### Groq (FREE - 14.4K requests/day)
1. Go to https://console.groq.com
2. Sign up with GitHub
3. Go to API Keys → Create API Key
4. Copy the key

### OpenAI (Optional - Paid)
1. Go to https://platform.openai.com/api-keys
2. Create account
3. Add payment method
4. Create API key

---

## 🗄️ Database Setup

### Automatic Migration

ThinkFirst uses Flyway for automatic database migrations. On first deployment:

1. Database tables will be created automatically
2. Initial schema will be applied
3. No manual SQL needed!

### Manual Migration (if needed)

```bash
# Connect to your database
psql $DATABASE_URL

# Check migrations
SELECT * FROM flyway_schema_history;
```

---

## 🧪 Testing Deployment

### Health Check

```bash
# Check if app is running
curl https://your-app-url.vercel.app/actuator/health

# Expected response:
{
  "status": "UP"
}
```

### Test API

```bash
# Test AI provider status
curl https://your-app-url.vercel.app/api/ai-provider/status

# Expected response:
{
  "gemini": "available",
  "groq": "available",
  "openai": "available"
}
```

---

## 📊 Monitoring

### Vercel
- Dashboard → Analytics
- View request logs
- Monitor performance

### Railway
- Dashboard → Metrics
- View logs in real-time
- Monitor resource usage

### Heroku
```bash
# View logs
heroku logs --tail

# Monitor metrics
heroku ps
```

---

## 🔒 Security Checklist

Before deploying to production:

- [ ] Change JWT_SECRET to a strong random value (min 256 bits)
- [ ] Use environment variables for all secrets (never commit)
- [ ] Enable HTTPS (most platforms do this automatically)
- [ ] Set up CORS properly in SecurityConfig.java
- [ ] Enable rate limiting
- [ ] Set up database backups
- [ ] Configure Redis persistence
- [ ] Review and update allowed origins in CORS config

---

## 💰 Cost Estimates

### Free Tier (Recommended for MVP)

**Vercel**:
- Hosting: FREE
- Bandwidth: 100GB/month FREE

**Supabase** (PostgreSQL):
- Database: FREE (500MB)
- Bandwidth: 2GB/month FREE

**Upstash** (Redis):
- Redis: FREE (10K commands/day)

**AI APIs**:
- Gemini: FREE (1.5M requests/month)
- Groq: FREE (14.4K requests/day)

**Total**: $0/month for ~1000 users! 🎉

### Paid Tier (For Scale)

**Railway**:
- $5/month base
- $0.000463/GB-hour for resources

**Heroku**:
- Eco Dyno: $5/month
- PostgreSQL Mini: $5/month
- Redis Mini: $3/month

**Total**: ~$13-20/month for 10K+ users

---

## 🚀 Quick Deploy Commands

### Vercel
```bash
vercel --prod
```

### Railway
```bash
railway up
```

### Heroku
```bash
git push heroku main
```

### Docker
```bash
docker-compose up -d
```

---

## 📝 Post-Deployment

1. **Test all endpoints**
2. **Monitor logs for errors**
3. **Check cache hit rates** (`/api/ai-provider/cache/stats`)
4. **Verify AI providers are working**
5. **Set up monitoring/alerts**
6. **Configure backups**

---

## 🆘 Troubleshooting

### App won't start
- Check environment variables are set
- Verify database connection string
- Check logs for errors

### Database connection failed
- Verify DATABASE_URL format
- Check firewall rules
- Ensure database is running

### Redis connection failed
- Verify REDIS_HOST and REDIS_PORT
- Check Redis password
- Ensure Redis is running

### AI API errors
- Verify API keys are correct
- Check API key quotas
- Review logs for specific errors

---

## 📚 Additional Resources

- [Vercel Documentation](https://vercel.com/docs)
- [Railway Documentation](https://docs.railway.app)
- [Heroku Java Deployment](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku)
- [Spring Boot Production Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)

---

**Your ThinkFirst app is ready for production deployment! 🚀**

