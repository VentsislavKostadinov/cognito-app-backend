# AWS Cognito Setup Guide

## Enabling USER_PASSWORD_AUTH Flow

The application uses the `USER_PASSWORD_AUTH` authentication flow. This must be enabled in your AWS Cognito User Pool App Client.

### Steps to Enable:

1. **Go to AWS Console** → **Cognito** → **User Pools**

2. **Select your User Pool**: `eu-central-1_rS9qXL9Ee`

3. **Go to App Clients** (or **App Integration** → **App Clients**)

4. **Select your App Client**: `3t8p938oufoc1pstif4660vhmq`

5. **Edit Authentication Flows**:
   - Check ✅ **ALLOW_USER_PASSWORD_AUTH**
   - Check ✅ **ALLOW_REFRESH_TOKEN_AUTH**
   - Optionally check ✅ **ALLOW_USER_SRP_AUTH** (for SRP authentication)

6. **Save Changes**

### Alternative: Using AWS CLI

```bash
aws cognito-idp update-user-pool-client \
  --user-pool-id eu-central-1_rS9qXL9Ee \
  --client-id 3t8p938oufoc1pstif4660vhmq \
  --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH \
  --region eu-central-1
```

## Required IAM Permissions

Your AWS IAM user needs the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cognito-idp:InitiateAuth",
        "cognito-idp:RespondToAuthChallenge",
        "cognito-idp:GetUser"
      ],
      "Resource": "arn:aws:cognito-idp:eu-central-1:*:userpool/eu-central-1_rS9qXL9Ee"
    }
  ]
}
```

## Creating a Test User

### Using AWS Console:

1. Go to **Cognito** → **User Pools** → Your pool
2. Click **Users** → **Create User**
3. Enter:
   - Username/Email: `test@example.com`
   - Temporary password: `TempPassword123!`
   - Uncheck "Send invitation" if you don't want to send email
4. Click **Create User**

### Using AWS CLI:

```bash
# Create user
aws cognito-idp admin-create-user \
  --user-pool-id eu-central-1_rS9qXL9Ee \
  --username test@example.com \
  --user-attributes Name=email,Value=test@example.com Name=email_verified,Value=true \
  --temporary-password "TempPassword123!" \
  --region eu-central-1

# Set permanent password
aws cognito-idp admin-set-user-password \
  --user-pool-id eu-central-1_rS9qXL9Ee \
  --username test@example.com \
  --password "YourPassword123!" \
  --permanent \
  --region eu-central-1
```

## Testing the Setup

1. **Start your Spring Boot application**
2. **Test the login endpoint**:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "YourPassword123!"
  }'
```

3. **Expected response** (if successful):
```json
{
  "accessToken": "eyJraWQiOi...",
  "idToken": "eyJraWQiOi...",
  "refreshToken": "eyJjdHkiOi...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

## Common Issues

### Issue: "Unable to verify secret hash for client"
**Solution**: If you have a client secret configured, you need to generate and include it in the authentication request. The current implementation assumes no client secret.

To remove client secret:
```bash
aws cognito-idp update-user-pool-client \
  --user-pool-id eu-central-1_rS9qXL9Ee \
  --client-id 3t8p938oufoc1pstif4660vhmq \
  --region eu-central-1 \
  --no-generate-secret
```

### Issue: "User password cannot be reset in the current state"
**Solution**: Set a permanent password for the user:
```bash
aws cognito-idp admin-set-user-password \
  --user-pool-id eu-central-1_rS9qXL9Ee \
  --username test@example.com \
  --password "YourPassword123!" \
  --permanent \
  --region eu-central-1
```

### Issue: "NotAuthorizedException: Incorrect username or password"
**Possible causes**:
1. Wrong credentials
2. User doesn't exist
3. User account is disabled or not confirmed
4. USER_PASSWORD_AUTH flow not enabled

### Issue: "InvalidParameterException: Missing required parameter USERNAME"
**Solution**: Ensure the email is being passed correctly in the request body.

## Security Best Practices

1. **Never commit AWS credentials** to version control
2. **Use environment variables** or AWS credentials file
3. **Enable MFA** on your Cognito users for production
4. **Use HTTPS** in production environments
5. **Set appropriate token expiration times**
6. **Implement rate limiting** to prevent brute force attacks
7. **Store tokens securely** on the client side
8. **Validate tokens** on every API request
9. **Use refresh tokens** to get new access tokens instead of re-authenticating
10. **Implement proper logging** without exposing sensitive data
