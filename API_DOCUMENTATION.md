# AWS Cognito Authentication API

This Spring Boot application provides authentication endpoints using AWS Cognito.

## Configuration

The application is configured with the following AWS Cognito settings in `application.properties`:

- **User Pool ID**: eu-central-1_rS9qXL9Ee
- **Client ID**: 3t8p938oufoc1pstif4660vhmq
- **Region**: eu-central-1
- **Domain**: https://eu-central-1rs9qxl9ee.auth.eu-central-1.amazoncognito.com

## API Endpoints

### 1. Login
Authenticate a user with email and password.

**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "YourPassword123!"
}
```

**Success Response (200 OK)**:
```json
{
  "accessToken": "eyJraWQiOiJ...",
  "idToken": "eyJraWQiOiJ...",
  "refreshToken": "eyJjdHkiOiJ...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Error Response (401 Unauthorized)**:
```json
{
  "message": "Invalid email or password",
  "error": "AUTHENTICATION_FAILED",
  "status": 401,
  "timestamp": "2026-05-10T12:00:00"
}
```

### 2. Refresh Token
Get new access and ID tokens using a refresh token.

**Endpoint**: `POST /api/auth/refresh`

**Request Body** (plain text):
```
eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNI...
```

**Success Response (200 OK)**:
```json
{
  "accessToken": "eyJraWQiOiJ...",
  "idToken": "eyJraWQiOiJ...",
  "refreshToken": "eyJjdHkiOiJ...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

### 3. Get User Info
Retrieve user information using an access token.

**Endpoint**: `GET /api/auth/user`

**Authentication**: Supports both methods:
1. **Authorization Header** (Optional):
   ```
   Authorization: Bearer eyJraWQiOiJ...
   ```
2. **HttpOnly Cookie** (Automatic): The endpoint will automatically read the `accessToken` cookie if no Authorization header is provided.

**Frontend Request Example**:
```javascript
// The browser automatically sends cookies, no need to read them manually
fetch('/api/auth/user', {
  credentials: 'include'  // Important: this sends cookies automatically
})
```

**Success Response (200 OK)**:
```json
{
  "username": "user@example.com",
  "userAttributes": [
    {
      "name": "sub",
      "value": "12345678-1234-1234-1234-123456789012"
    },
    {
      "name": "email",
      "value": "user@example.com"
    },
    {
      "name": "email_verified",
      "value": "true"
    }
  ]
}
```

**Error Response (401 Unauthorized)**:
```json
{
  "message": "No access token provided in Authorization header or cookies",
  "error": "NO_ACCESS_TOKEN",
  "status": 401,
  "timestamp": "2026-05-10T12:00:00"
}
```

### 4. Health Check
Check if the authentication service is running.

**Endpoint**: `GET /api/auth/health`

**Success Response (200 OK)**:
```
Authentication service is running
```

## Running the Application

1. **Prerequisites**:
   - Java 17 or higher
   - Maven 3.6+
   - AWS credentials configured (IAM user with Cognito permissions)

2. **Install dependencies**:
   ```bash
   ./mvnw clean install
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

4. **The application will start on**: `http://localhost:8080`

## AWS Credentials Setup

The application uses AWS SDK's `DefaultCredentialsProvider`, which looks for credentials in the following order:

1. Environment variables: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
2. AWS credentials file: `~/.aws/credentials`
3. IAM role (if running on EC2)

### Setting up credentials file:

Create `~/.aws/credentials`:
```
[default]
aws_access_key_id = YOUR_ACCESS_KEY
aws_secret_access_key = YOUR_SECRET_KEY
```

Create `~/.aws/config`:
```
[default]
region = eu-central-1
```

### Or use environment variables:

```bash
export AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY
export AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY
export AWS_DEFAULT_REGION=eu-central-1
```

## CORS Configuration

The application is configured to allow CORS requests from:
- http://localhost:3000
- http://localhost:5173
- https://main.d16f2529r3lhi6.amplifyapp.com
- https://main.d2l13mf6ec69fe.amplifyapp.com

## Error Handling

All errors are returned in a consistent format:

```json
{
  "message": "Error description",
  "error": "ERROR_CODE",
  "status": 401,
  "timestamp": "2026-05-10T12:00:00"
}
```

Common error codes:
- `AUTHENTICATION_FAILED`: Login failed
- `TOKEN_REFRESH_FAILED`: Token refresh failed
- `GET_USER_FAILED`: Getting user info failed

## Testing with cURL

### Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "YourPassword123!"
  }'
```

### Get User Info:
```bash
curl -X GET http://localhost:8080/api/auth/user \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Refresh Token:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: text/plain" \
  -d "YOUR_REFRESH_TOKEN"
```

## Project Structure

```
src/main/java/com/example/cognito_app/
├── config/
│   ├── AwsCognitoConfig.java      # AWS Cognito configuration
│   └── WebConfig.java              # CORS configuration
├── controller/
│   └── AuthController.java         # REST API endpoints
├── dto/
│   ├── LoginRequest.java           # Login request DTO
│   ├── LoginResponse.java          # Login response DTO
│   └── ErrorResponse.java          # Error response DTO
├── service/
│   └── CognitoService.java         # Cognito business logic
└── CognitoAppApplication.java      # Main application class
```

## Important Notes

1. **USER_PASSWORD_AUTH** flow must be enabled in your Cognito User Pool App Client settings
2. Make sure your IAM user has the necessary permissions to call Cognito APIs
3. The refresh token has a longer expiration time (typically 30 days) compared to access tokens (1 hour)
4. Store tokens securely on the client side (preferably in httpOnly cookies or secure storage)
