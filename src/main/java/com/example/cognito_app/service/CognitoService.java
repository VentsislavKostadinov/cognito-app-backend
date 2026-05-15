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

@Service
public class CognitoService {
    
    private final CognitoIdentityProviderClient cognitoClient;
    private final AwsCognitoConfig awsCognitoConfig;
    
    @Autowired
    public CognitoService(CognitoIdentityProviderClient cognitoClient, AwsCognitoConfig awsCognitoConfig) {
        this.cognitoClient = cognitoClient;
        this.awsCognitoConfig = awsCognitoConfig;
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
}
