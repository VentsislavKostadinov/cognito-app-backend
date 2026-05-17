# Gmail OAuth Login Implementation

## Overview
This implementation adds Google OAuth login support to your AWS Cognito authentication system. Users can now sign in using their Gmail accounts through AWS Cognito's federated identity feature.

## Configuration

### 1. Application Properties
The following configuration has been added to `application.properties`:

```properties
# OAuth Callback Configuration
aws.cognito.oauth.callbackUri=${AWS_COGNITO_OAUTH_CALLBACKURI:http://localhost:8080/api/auth/oauth2/callback}

# Google OAuth Configuration
aws.cognito.google.clientId=${GOOGLE_CLIENT_ID:YOUR-GOOGLE-CLIENT-ID}
aws.cognito.google.clientSecret=${GOOGLE_CLIENT_SECRET:YOUR-GOOGLE-CLIENT-SECRET}
```

### 2. Environment Variables
Set these environment variables for production:

```bash
export AWS_COGNITO_OAUTH_CALLBACKURI=https://your-domain.com/api/auth/oauth2/callback
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
```

## API Endpoints

### 1. Get OAuth Authorization URL
**GET** `/api/auth/oauth2/authorize/{provider}`

Generates the OAuth authorization URL to redirect users to Google login.

**Path Parameters:**
- `provider`: OAuth provider name (e.g., "Google")

**Query Parameters:**
- `state` (optional): CSRF protection token

**Request Example:**
```bash
curl http://localhost:8080/api/auth/oauth2/authorize/Google
```

**Response:**
```json
{
  "authorizationUrl": "https://eu-central-1rs9qxl9ee.auth.eu-central-1.amazoncognito.com/oauth2/authorize?client_id=...&redirect_uri=...",
  "provider": "Google"
}
```

### 2. OAuth Callback Handler
**GET** `/api/auth/oauth2/callback`

Handles the OAuth callback after user authentication and exchanges the authorization code for tokens.

**Query Parameters:**
- `code`: Authorization code from OAuth provider (required)
- `state` (optional): CSRF protection token

**Request Example:**
```bash
curl "http://localhost:8080/api/auth/oauth2/callback?code=AUTHORIZATION_CODE"
```

**Response:**
```json
{
  "email": "user@gmail.com",
  "accessToken": "eyJraWQiOiJ...",
  "idToken": "eyJraWQiOiJ...",
  "refreshToken": "eyJjdHkiOi...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Cookies Set:**
- `accessToken`: HttpOnly, 1 hour expiry
- `idToken`: HttpOnly, 1 hour expiry
- `refreshToken`: HttpOnly, 30 days expiry

## Frontend Integration

### React/Vue Example

```javascript
// 1. Get OAuth authorization URL
const initiateGoogleLogin = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/oauth2/authorize/Google');
    const data = await response.json();
    
    // Redirect user to Google login
    window.location.href = data.authorizationUrl;
  } catch (error) {
    console.error('Failed to initiate Google login:', error);
  }
};

// 2. Handle OAuth callback (on callback page)
const handleOAuthCallback = async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const code = urlParams.get('code');
  
  if (code) {
    try {
      const response = await fetch(`http://localhost:8080/api/auth/oauth2/callback?code=${code}`, {
        credentials: 'include' // Important: include cookies
      });
      
      const data = await response.json();
      
      if (response.ok) {
        // Login successful - redirect to dashboard
        console.log('Logged in as:', data.email);
        window.location.href = '/dashboard';
      } else {
        console.error('OAuth callback failed:', data);
      }
    } catch (error) {
      console.error('OAuth callback error:', error);
    }
  }
};
```

### HTML Example

```html
<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
</head>
<body>
    <h1>Login</h1>
    <button onclick="loginWithGoogle()">Login with Google</button>
    
    <script>
        async function loginWithGoogle() {
            const response = await fetch('http://localhost:8080/api/auth/oauth2/authorize/Google');
            const data = await response.json();
            window.location.href = data.authorizationUrl;
        }
    </script>
</body>
</html>
```

## OAuth Flow

1. **User Initiates Login**
   - Frontend calls `/api/auth/oauth2/authorize/Google`
   - Backend returns authorization URL
   - Frontend redirects user to the authorization URL

2. **User Authenticates**
   - User is redirected to Google login
   - User grants permissions
   - Google redirects back to your callback URI with authorization code

3. **Token Exchange**
   - Frontend receives authorization code
   - Frontend calls `/api/auth/oauth2/callback?code=CODE`
   - Backend exchanges code for tokens with AWS Cognito
   - Backend sets HttpOnly cookies and returns user info

4. **Authenticated Session**
   - User is now logged in
   - Subsequent API calls include authentication cookies
   - Use existing `/api/auth/me` endpoint to verify session

## AWS Cognito Setup

Ensure your AWS Cognito User Pool has the following configured:

### 1. Identity Provider
- Provider: Google
- Client ID: `YOUR-GOOGLE-CLIENT-ID.apps.googleusercontent.com`
- Client Secret: `YOUR-GOOGLE-CLIENT-SECRET`
- Authorized scopes: `profile`, `email`, `openid`

### 2. App Client Settings
- **Callback URLs**: Add your callback URI (e.g., `http://localhost:8080/api/auth/oauth2/callback`)
- **Sign out URLs**: Optional
- **Allowed OAuth Flows**: Authorization code grant
- **Allowed OAuth Scopes**: email, openid, profile

### 3. Domain Name
Configure a Cognito domain for your user pool (e.g., `https://eu-central-1rs9qxl9ee.auth.eu-central-1.amazoncognito.com`)

## Security Considerations

1. **HTTPS in Production**: Always use HTTPS in production. Cookies are set with `Secure` flag in production mode.

2. **CSRF Protection**: Use the `state` parameter for CSRF protection:
   ```javascript
   const state = generateRandomString(); // Store in session
   const response = await fetch(`/api/auth/oauth2/authorize/Google?state=${state}`);
   ```

3. **Cookie Security**:
   - HttpOnly: Prevents JavaScript access
   - Secure: HTTPS only in production
   - SameSite: Prevents CSRF attacks

4. **Environment Variables**: Never commit sensitive credentials. Use environment variables or AWS Secrets Manager.

## Testing

### Local Testing
1. Start your Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

2. Navigate to:
   ```
   http://localhost:8080/api/auth/oauth2/authorize/Google
   ```

3. Copy the `authorizationUrl` from the response and paste it in your browser

4. Complete Google login

5. You'll be redirected to the callback endpoint with tokens set as cookies

### Production Testing
Ensure your callback URI is whitelisted in both:
- Google Cloud Console (OAuth 2.0 Client)
- AWS Cognito User Pool App Client Settings

## Troubleshooting

### Error: "Token exchange failed"
- Verify client ID and client secret in configuration
- Check that the authorization code hasn't expired (codes expire quickly)
- Ensure callback URI matches exactly what's configured in Cognito

### Error: "redirect_uri_mismatch"
- Callback URI in the request must match what's configured in:
  - Google Cloud Console
  - AWS Cognito App Client Settings
  - Your application.properties

### Cookies Not Being Set
- Check CORS configuration allows credentials
- Ensure frontend includes `credentials: 'include'` in fetch requests
- Verify cookie settings match environment (Secure flag for HTTPS)

## Additional Features

The implementation also supports:
- Email/password login: `POST /api/auth/login`
- Token refresh: `POST /api/auth/refresh`
- User info: `GET /api/auth/user`
- Session check: `GET /api/auth/me`
- Logout: `POST /api/auth/logout`

All endpoints work seamlessly with both OAuth and traditional authentication methods.
