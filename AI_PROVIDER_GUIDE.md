# AI Provider Integration Guide

## Overview

ThinkFirst now uses a **hybrid AI provider architecture** with automatic fallback to minimize costs while maintaining reliability.

### Provider Priority (Default)
1. **Gemini** (Google) - FREE 1.5M requests/month
2. **Groq** - FREE 14,400 requests/day  
3. **OpenAI** - PAID (Most reliable fallback)

The system automatically tries providers in order. If one fails (rate limit, error, etc.), it falls back to the next.

---

## Getting API Keys

### 1. Gemini (Google) - Recommended Primary

**Free Tier**: 1.5 million requests per month

**Get API Key**:
1. Visit: https://makersuite.google.com/app/apikey
2. Sign in with Google account
3. Click "Create API Key"
4. Copy the key

**Models Available**:
- `gemini-1.5-flash` (default) - Fast, efficient
- `gemini-1.5-pro` (advanced) - More capable

**Set in `.env`**:
```bash
GEMINI_ENABLED=true
GEMINI_API_KEY=AIzaSy...your-key-here
```

---

### 2. Groq - Recommended Secondary

**Free Tier**: 14,400 requests per day (600/hour)

**Get API Key**:
1. Visit: https://console.groq.com/keys
2. Sign up/Sign in
3. Click "Create API Key"
4. Copy the key

**Models Available**:
- `llama-3.1-8b-instant` (default) - Very fast
- `llama-3.1-70b-versatile` (advanced) - More capable
- `mixtral-8x7b-32768` (alternative) - Good balance

**Set in `.env`**:
```bash
GROQ_ENABLED=true
GROQ_API_KEY=gsk_...your-key-here
```

---

### 3. OpenAI - Fallback Only

**Pricing**: Pay-per-use
- GPT-3.5-Turbo: ~$0.002 per 1K tokens
- GPT-4: ~$0.03-0.06 per 1K tokens
- GPT-4o-mini: ~$0.0001 per 1K tokens (cheapest)

**Get API Key**:
1. Visit: https://platform.openai.com/api-keys
2. Sign up/Sign in
3. Click "Create new secret key"
4. Copy the key
5. Add credits: https://platform.openai.com/account/billing

**Models Available**:
- `gpt-3.5-turbo` (default) - Fast, cheap
- `gpt-4` (advanced) - Most capable
- `gpt-4-turbo-preview` (turbo) - Fast GPT-4
- `gpt-4o-mini` (mini) - Cheapest GPT-4

**Set in `.env`**:
```bash
OPENAI_ENABLED=true
OPENAI_API_KEY=sk-...your-key-here
```

---

## Configuration

### Change Provider Priority

Edit `src/main/resources/application.yml`:

```yaml
ai:
  provider-priority:
    - gemini    # Try Gemini first
    - groq      # Then Groq
    - openai    # Finally OpenAI
```

You can reorder or remove providers as needed.

---

### Change Models

Edit `src/main/resources/application.yml`:

**Gemini Models**:
```yaml
ai:
  gemini:
    models:
      default: gemini-1.5-flash      # Fast, efficient
      advanced: gemini-1.5-pro       # More capable
```

**Groq Models**:
```yaml
ai:
  groq:
    models:
      default: llama-3.1-8b-instant
      advanced: llama-3.1-70b-versatile
      alternative: mixtral-8x7b-32768
```

**OpenAI Models**:
```yaml
ai:
  openai:
    models:
      default: gpt-3.5-turbo
      advanced: gpt-4
      turbo: gpt-4-turbo-preview
      mini: gpt-4o-mini
```

---

## Runtime Model Switching

### Change OpenAI Model via API

```bash
# Switch to GPT-4
curl -X POST "http://localhost:8080/api/ai-provider/openai/model?modelKey=advanced" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Switch to GPT-4o-mini (cheapest)
curl -X POST "http://localhost:8080/api/ai-provider/openai/model?modelKey=mini" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Check current model
curl -X GET "http://localhost:8080/api/ai-provider/openai/model" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Programmatically in Code

```java
@Autowired
private AIProviderService aiProviderService;

// Switch to GPT-4
aiProviderService.setOpenAIModel("advanced");

// Switch to GPT-4o-mini
aiProviderService.setOpenAIModel("mini");

// Get current model
String currentModel = aiProviderService.getCurrentOpenAIModel();
```

---

## Monitoring & Testing

### Check Provider Status

```bash
curl -X GET "http://localhost:8080/api/ai-provider/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "gemini": {
    "name": "Gemini",
    "available": true,
    "key": "gemini"
  },
  "groq": {
    "name": "Groq",
    "available": true,
    "key": "groq"
  },
  "openai": {
    "name": "OpenAI",
    "available": false,
    "key": "openai"
  }
}
```

### Test a Specific Provider

```bash
# Test Gemini
curl -X POST "http://localhost:8080/api/ai-provider/test/gemini" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Test Groq
curl -X POST "http://localhost:8080/api/ai-provider/test/groq" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Cost Optimization Strategies

### Strategy 1: Free Only (Recommended for MVP)
```yaml
ai:
  provider-priority:
    - gemini
    - groq
  openai:
    enabled: false
```

**Cost**: $0/month  
**Capacity**: ~1.5M requests/month

---

### Strategy 2: Free Primary + Paid Fallback (Recommended for Production)
```yaml
ai:
  provider-priority:
    - gemini
    - groq
    - openai
  openai:
    models:
      default: gpt-4o-mini  # Use cheapest OpenAI model
```

**Cost**: ~$10-50/month (depending on fallback usage)  
**Reliability**: High

---

### Strategy 3: OpenAI Only (Most Reliable)
```yaml
ai:
  provider-priority:
    - openai
  gemini:
    enabled: false
  groq:
    enabled: false
```

**Cost**: ~$100-500/month (1000 active users)  
**Reliability**: Highest

---

## Troubleshooting

### Gemini Rate Limit Exceeded
**Error**: `RateLimitException: Gemini - Rate limit exceeded`

**Solution**: System automatically falls back to Groq/OpenAI. No action needed.

**Manual Fix**: Wait 1 minute or disable Gemini temporarily:
```yaml
ai:
  gemini:
    enabled: false
```

---

### Groq Rate Limit Exceeded
**Error**: `RateLimitException: Groq - Rate limit exceeded`

**Solution**: System falls back to OpenAI.

**Manual Fix**: Groq resets every hour (600 requests/hour limit)

---

### All Providers Failed
**Error**: `AIProviderException: ALL_PROVIDERS - All AI providers failed`

**Causes**:
1. No API keys configured
2. All providers hit rate limits
3. Network issues

**Solution**:
1. Check API keys in `.env`
2. Verify keys are valid
3. Check provider status endpoint
4. Wait for rate limits to reset

---

## Best Practices

### 1. Use Free Providers First
Always prioritize Gemini and Groq to minimize costs.

### 2. Monitor Usage
Check logs for which provider is being used:
```
INFO: Attempting generateQuestions with provider: Gemini
INFO: Successfully executed generateQuestions with provider: Gemini
```

### 3. Set Up Alerts
Monitor for "All providers failed" errors in production.

### 4. Test Regularly
Use the test endpoint to verify all providers are working:
```bash
curl -X POST "http://localhost:8080/api/ai-provider/test/gemini"
curl -X POST "http://localhost:8080/api/ai-provider/test/groq"
curl -X POST "http://localhost:8080/api/ai-provider/test/openai"
```

### 5. Use Appropriate Models
- **Quiz Generation**: Use default models (fast, cheap)
- **Complex Explanations**: Use advanced models
- **Simple Queries**: Use mini/flash models

---

## Example Usage

### Generate Quiz with Automatic Fallback

```java
// The service automatically tries Gemini → Groq → OpenAI
List<Question> questions = aiProviderService.generateQuestions(
    "Photosynthesis",
    "Science",
    5,
    "INTERMEDIATE"
);
```

### Generate Response with Specific Model

```java
// Switch to GPT-4 for complex topics
aiProviderService.setOpenAIModel("advanced");

String response = aiProviderService.generateEducationalResponse(
    "Explain quantum mechanics",
    14,
    "Physics"
);

// Switch back to default
aiProviderService.setOpenAIModel("default");
```

---

## API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ai-provider/status` | GET | Get all provider statuses |
| `/api/ai-provider/test/{provider}` | POST | Test a specific provider |
| `/api/ai-provider/openai/model` | POST | Change OpenAI model |
| `/api/ai-provider/openai/model` | GET | Get current OpenAI model |

---

## Support

For issues or questions:
- Check logs for detailed error messages
- Verify API keys are valid
- Test each provider individually
- Contact support@thinkfirst.com

---

**Happy Learning with Hybrid AI! 🚀**

