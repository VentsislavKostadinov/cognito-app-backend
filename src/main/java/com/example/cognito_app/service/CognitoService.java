package com.example.cognito_app.service;

import com.example.cognito_app.config.AwsCognitoConfig;
import com.example.cognito_app.dto.LoginRequest;
import com.example.cognito_app.dto.LoginResponse;
import com.example.cognito_app.dto.UserInfoResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class CognitoService {
    
    private final CognitoIdentityProviderClient cognitoClient;
    private final AwsCognitoConfig awsCognitoConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public CognitoService(CognitoIdentityProviderClient cognitoClient, AwsCognitoConfig awsCognitoConfig) {
        this.cognitoClient = cognitoClient;
        this.awsCognitoConfig = awsCognitoConfig;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Authenticate user with email and password using AWS Cognito
     * @param loginRequest LoginRequest containing email and password
     * @return LoginResponse containing tokens
     * @throws Exception if authentication fails
     */
    public LoginResponse authenticateUser(LoginRequest loginRequest) throws Exception {
        try {
            // Prepare authentication parameters
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", loginRequest.getEmail());
            authParams.put("PASSWORD", loginRequest.getPassword());
            
            // Create InitiateAuth request
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(awsCognitoConfig.getClientId())
                    .authParameters(authParams)
                    .build();
            
            // Execute authentication
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            
            // Extract authentication result
            AuthenticationResultType authResult = authResponse.authenticationResult();
            
            if (authResult == null) {
                throw new RuntimeException("Authentication failed: No authentication result returned");
            }
            
            // Extract email from ID token
            String email = extractEmailFromIdToken(authResult.idToken());
            
            // Create and return login response
            return new LoginResponse(
                    email,
                    authResult.accessToken(),
                    authResult.idToken(),
                    authResult.refreshToken(),
                    authResult.expiresIn(),
                    authResult.tokenType()
            );
            
        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Invalid email or password", e);
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found", e);
        } catch (TooManyRequestsException e) {
            throw new RuntimeException("Too many requests. Please try again later", e);
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get user tokens using refresh token
     * @param refreshToken The refresh token
     * @return LoginResponse containing new tokens
     * @throws Exception if token refresh fails
     */
    public LoginResponse refreshTokens(String refreshToken) throws Exception {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);
            
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(awsCognitoConfig.getClientId())
                    .authParameters(authParams)
                    .build();
            
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            AuthenticationResultType authResult = authResponse.authenticationResult();
            
            if (authResult == null) {
                throw new RuntimeException("Token refresh failed: No authentication result returned");
            }
            
            // Extract email from ID token
            String email = extractEmailFromIdToken(authResult.idToken());
            
            return new LoginResponse(
                    email,
                    authResult.accessToken(),
                    authResult.idToken(),
                    authResult.refreshToken() != null ? authResult.refreshToken() : refreshToken,
                    authResult.expiresIn(),
                    authResult.tokenType()
            );
            
        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Invalid or expired refresh token", e);
        } catch (Exception e) {
            throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get user information from access token
     * @param accessToken The access token
     * @return UserInfoResponse containing user attributes
     * @throws Exception if getting user info fails
     */
    public UserInfoResponse getUserInfo(String accessToken) throws Exception {
        try {
            GetUserRequest getUserRequest = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();
            
            GetUserResponse response = cognitoClient.getUser(getUserRequest);
            
            // Convert attributes to a map
            Map<String, String> attributes = response.userAttributes().stream()
                    .collect(Collectors.toMap(
                            AttributeType::name,
                            AttributeType::value
                    ));
            
            return new UserInfoResponse(response.username(), attributes);
            
        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Invalid or expired access token", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user info: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract email from ID token JWT
     * @param idToken The ID token
     * @return Email address
     */
    private String extractEmailFromIdToken(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return (String) claims.getClaim("email");
        } catch (Exception e) {
            return null; // Return null if email cannot be extracted
        }
    }
    
    /**
     * Generate OAuth authorization URL for a specific provider
     * @param provider The OAuth provider (e.g., "Google")
     * @param state Optional state parameter for CSRF protection
     * @return Authorization URL
     */
    public String getOAuthAuthorizationUrl(String provider, String state) {
        try {
            StringBuilder url = new StringBuilder(awsCognitoConfig.getDomain());
            url.append("/oauth2/authorize");
            url.append("?client_id=").append(URLEncoder.encode(awsCognitoConfig.getClientId(), StandardCharsets.UTF_8));
            url.append("&response_type=code");
            url.append("&scope=").append(URLEncoder.encode(awsCognitoConfig.getScope(), StandardCharsets.UTF_8));
            url.append("&redirect_uri=").append(URLEncoder.encode(awsCognitoConfig.getOauthCallbackUri(), StandardCharsets.UTF_8));
            url.append("&identity_provider=").append(provider);
            
            if (state != null && !state.isEmpty()) {
                url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }
            
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OAuth URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Exchange OAuth authorization code for tokens
     * @param code The authorization code from OAuth callback
     * @return LoginResponse containing tokens
     * @throws Exception if token exchange fails
     */
    public LoginResponse exchangeCodeForTokens(String code) throws Exception {
        try {
            // Prepare the token endpoint URL
            String tokenUrl = awsCognitoConfig.getDomain() + "/oauth2/token";
            
            // Prepare the request body
            String requestBody = "grant_type=authorization_code" +
                    "&client_id=" + URLEncoder.encode(awsCognitoConfig.getClientId(), StandardCharsets.UTF_8) +
                    "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(awsCognitoConfig.getOauthCallbackUri(), StandardCharsets.UTF_8);
            
            // Build the HTTP request (no client secret required for this app client)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Token exchange failed with status: " + response.statusCode() + ", body: " + response.body());
            }
            
            // Parse the response
            JsonNode jsonResponse = objectMapper.readTree(response.body());
            
            String accessToken = jsonResponse.get("access_token").asText();
            String idToken = jsonResponse.get("id_token").asText();
            String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;
            int expiresIn = jsonResponse.get("expires_in").asInt();
            String tokenType = jsonResponse.get("token_type").asText();
            
            // Extract email from ID token
            String email = extractEmailFromIdToken(idToken);
            
            return new LoginResponse(
                    email,
                    accessToken,
                    idToken,
                    refreshToken,
                    expiresIn,
                    tokenType
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for tokens: " + e.getMessage(), e);
        }
    }
}
