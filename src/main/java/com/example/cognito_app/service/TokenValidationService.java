package com.example.cognito_app.service;

import com.example.cognito_app.config.AwsCognitoConfig;
import com.example.cognito_app.dto.TokenValidationResponse;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenValidationService {
    
    private final AwsCognitoConfig awsCognitoConfig;
    private JWKSet jwkSet;
    private long jwkSetLastFetched = 0;
    private static final long JWK_SET_CACHE_TIME = 3600000; // 1 hour in milliseconds
    
    @Autowired
    public TokenValidationService(AwsCognitoConfig awsCognitoConfig) {
        this.awsCognitoConfig = awsCognitoConfig;
    }
    
    /**
     * Validate JWT token from AWS Cognito
     * @param token The JWT token (ID token or Access token)
     * @return TokenValidationResponse with validation result and claims
     */
    public TokenValidationResponse validateToken(String token) {
        try {
            // Parse the JWT
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // Get the key ID from the token header
            String keyId = signedJWT.getHeader().getKeyID();
            
            // Get the JWK set (with caching)
            JWKSet jwkSet = getJWKSet();
            
            // Find the specific key
            JWK jwk = jwkSet.getKeyByKeyId(keyId);
            if (jwk == null) {
                return new TokenValidationResponse(false, "Token key ID not found in JWK set");
            }
            
            // Verify the signature
            RSAKey rsaKey = (RSAKey) jwk;
            JWSVerifier verifier = new RSASSAVerifier(rsaKey);
            
            if (!signedJWT.verify(verifier)) {
                return new TokenValidationResponse(false, "Invalid token signature");
            }
            
            // Get claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Verify issuer
            String expectedIssuer = String.format("https://cognito-idp.%s.amazonaws.com/%s",
                    awsCognitoConfig.getRegion(), awsCognitoConfig.getUserPoolId());
            if (!expectedIssuer.equals(claims.getIssuer())) {
                return new TokenValidationResponse(false, "Invalid token issuer");
            }
            
            // Verify token is not expired
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                return new TokenValidationResponse(false, "Token has expired");
            }
            
            // Verify token_use claim
            String tokenUse = (String) claims.getClaim("token_use");
            if (tokenUse == null || (!tokenUse.equals("id") && !tokenUse.equals("access"))) {
                return new TokenValidationResponse(false, "Invalid token_use claim");
            }
            
            // For ID tokens, verify audience (client_id)
            if ("id".equals(tokenUse)) {
                String audience = claims.getAudience().get(0);
                if (!awsCognitoConfig.getClientId().equals(audience)) {
                    return new TokenValidationResponse(false, "Invalid token audience");
                }
            }
            
            // Build successful response
            TokenValidationResponse response = new TokenValidationResponse();
            response.setValid(true);
            response.setMessage("Token is valid");
            response.setSub((String) claims.getClaim("sub"));
            response.setEmail((String) claims.getClaim("email"));
            response.setUsername((String) claims.getClaim("cognito:username"));
            response.setTokenUse(tokenUse);
            response.setExp(claims.getExpirationTime().getTime() / 1000);
            response.setIat(claims.getIssueTime().getTime() / 1000);
            
            // Convert all claims to map
            Map<String, Object> allClaims = new HashMap<>();
            claims.getClaims().forEach(allClaims::put);
            response.setClaims(allClaims);
            
            return response;
            
        } catch (Exception e) {
            return new TokenValidationResponse(false, "Token validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Get JWK Set from Cognito with caching
     * @return JWKSet
     * @throws Exception if fetching fails
     */
    private JWKSet getJWKSet() throws Exception {
        long currentTime = System.currentTimeMillis();
        
        // Return cached JWK set if still valid
        if (jwkSet != null && (currentTime - jwkSetLastFetched) < JWK_SET_CACHE_TIME) {
            return jwkSet;
        }
        
        // Fetch new JWK set
        String jwksUrl = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                awsCognitoConfig.getRegion(), awsCognitoConfig.getUserPoolId());
        
        jwkSet = JWKSet.load(new URL(jwksUrl));
        jwkSetLastFetched = currentTime;
        
        return jwkSet;
    }
    
    /**
     * Validate both ID token and Access token
     * @param idToken The ID token
     * @param accessToken The access token
     * @return TokenValidationResponse with combined validation result
     */
    public TokenValidationResponse validateBothTokens(String idToken, String accessToken) {
        // Validate ID token
        TokenValidationResponse idTokenResponse = validateToken(idToken);
        if (!idTokenResponse.isValid()) {
            idTokenResponse.setMessage("ID Token validation failed: " + idTokenResponse.getMessage());
            return idTokenResponse;
        }
        
        // Validate access token if provided
        if (accessToken != null && !accessToken.isEmpty()) {
            TokenValidationResponse accessTokenResponse = validateToken(accessToken);
            if (!accessTokenResponse.isValid()) {
                accessTokenResponse.setMessage("Access Token validation failed: " + accessTokenResponse.getMessage());
                return accessTokenResponse;
            }
        }
        
        // Return ID token response (contains user info)
        idTokenResponse.setMessage("Tokens are valid");
        return idTokenResponse;
    }
}
