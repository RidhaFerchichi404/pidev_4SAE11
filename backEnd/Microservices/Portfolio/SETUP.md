# Portfolio Microservice Setup Guide

## Problem Summary

The "Generating Your Test" feature was **taking too long and not returning a response** to the frontend, even though the test was successfully created in the database.

### Root Causes Identified:

1. **Lazy Loading Serialization Issue**: The `skill` property in `EvaluationTest` was marked as `LAZY`, causing JSON serialization to fail after the Hibernate session closed
2. **API Gateway Timeout**: No explicit timeout configuration for AI generation requests (which can take 30-60 seconds)
3. **Missing API Key**: The Portfolio service requires an `API_KEY` environment variable for AI question generation

## Fixes Applied

### 1. Backend Entity Fix (EvaluationTest.java)
- Changed `skill` from `FetchType.LAZY` to `FetchType.EAGER`
- Added `@JsonIgnoreProperties` to handle Hibernate proxy serialization
- This ensures the response can be properly serialized to JSON

### 2. API Gateway Timeout Configuration (application.yml)
- Added 120-second response timeout for Portfolio service
- Added 10-second connection timeout
- This gives the AI API enough time to generate questions

### 3. Enhanced Logging (EvaluationTestController.java)
- Added detailed logging for test generation requests
- Added error logging with stack traces
- Helps identify issues during development

### 4. Frontend Error Handling (skill-management.ts)
- Added specific error messages for different error types
- Added detailed console logging for debugging
- Improved user feedback with timeout messages

## Setup Instructions

### Step 1: Set API Key Environment Variable

The Portfolio service needs an API key for the AI service (https://api.apifree.ai).

**Option A: Set environment variable before starting**
```bash
# Linux/Mac
export API_KEY="your-actual-api-key-here"

# Windows PowerShell
$env:API_KEY="your-actual-api-key-here"

# Windows CMD
set API_KEY=your-actual-api-key-here
```

**Option B: Create a .env file** (in `backEnd/Microservices/Portfolio/`)
```properties
API_KEY=your-actual-api-key-here
```

**Note:** Get your free API key from: https://api.apifree.ai (or use your own OpenAI-compatible endpoint)

### Step 2: Build the Portfolio Service

```bash
cd backEnd/Microservices/Portfolio
mvn clean package -DskipTests
```

### Step 3: Start the Portfolio Service

Make sure the following are running first:
1. MySQL (port 3306)
2. Eureka Service (port 8420)
3. API Gateway (port 8078)

Then start Portfolio:

```bash
# Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run

# Or with explicit API key
API_KEY=your-key ./mvnw spring-boot:run
```

The service will start on **port 8086** and register with Eureka as **PORTFOLIO**.

### Step 4: Verify Service is Running

Check the logs for:
```
✓ "Loaded API Key from .env manually" (if using .env)
✓ "Registering application PORTFOLIO with eureka"
✓ "Tomcat started on port 8086"
```

Check Eureka dashboard: http://localhost:8420
- You should see **PORTFOLIO** listed under registered services

### Step 5: Restart API Gateway

After Portfolio is registered with Eureka, restart the API Gateway to pick up the new route configuration with timeout settings:

```bash
cd backEnd/apiGateway
./mvnw spring-boot:run
```

### Step 6: Test the Feature

1. Go to your Angular app (http://localhost:4200)
2. Navigate to Skills Management
3. Click "Verify Skill" on any skill
4. Watch the browser console for detailed logs:
   - "Generating test for skill ID: X"
   - "HTTP Response" (should show test data)

## Troubleshooting

### Issue: "Cannot connect to Portfolio service"
**Solution:**
- Check if Portfolio is running: `ps aux | grep -i portfolio`
- Check port 8086: `netstat -an | grep 8086`
- Check Eureka: http://localhost:8420 (PORTFOLIO should be listed)

### Issue: "AI service error" or 500 status
**Solution:**
- Verify API_KEY is set: Check logs for "Using API Key: xxxxx..."
- Check if the AI API is accessible
- Try a different AI model or endpoint

### Issue: "Request timed out"
**Solution:**
- AI generation can take 30-60 seconds for complex skills
- The timeout is now set to 120 seconds
- If still timing out, check your internet connection or AI API status

### Issue: Still hanging with no response
**Check:**
1. Browser Network tab - is the request pending?
2. Backend logs - is the test being created?
3. Database - check `evaluation_tests` table for new entries
4. API Gateway logs - any routing errors?

## Database Schema

The Portfolio service uses these tables:
- `skills` - User skills
- `evaluation_tests` - Generated tests
- `test_questions` - Questions for each test
- `evaluations` - User test submissions and results
- `experiences` - User work/project experience

## API Endpoints

All endpoints are accessed via API Gateway: `http://localhost:8078/portfolio/api/...`

- `POST /evaluation-tests/generate/{skillId}` - Generate AI test for skill
- `POST /evaluations/submit` - Submit test answers
- `GET /evaluations/freelancer/{userId}/skill/{skillId}` - Get evaluation results
- `GET /skills/user/{userId}` - Get user skills
- `POST /skills` - Create new skill
- `DELETE /skills/{id}` - Delete skill

## Configuration Files

- `application.properties` - Database, Eureka, AI API configuration
- `pom.xml` - Maven dependencies (includes java-dotenv for .env support)
- `.env` (create this) - API key and optional overrides

## Next Steps

After fixing the issue, consider:
1. ✅ Add loading indicators in the UI
2. ✅ Add timeout messages for users
3. 🔄 Cache generated tests to avoid regenerating
4. 🔄 Add retry logic for failed AI requests
5. 🔄 Add progress updates during generation

## Support

For issues or questions, check:
- Backend logs: `backEnd/Microservices/Portfolio/logs/`
- Frontend console: Browser DevTools > Console
- Network requests: Browser DevTools > Network
