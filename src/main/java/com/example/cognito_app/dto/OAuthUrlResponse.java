package com.example.cognito_app.dto;

/**
 * Response containing OAuth authorization URL
 */
public class OAuthUrlResponse {
    private String authorizationUrl;
    private String provider;
    
    public OAuthUrlResponse() {
    }
    
    public OAuthUrlResponse(String authorizationUrl, String provider) {
        this.authorizationUrl = authorizationUrl;
        this.provider = provider;
    }
    
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }
    
    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
}
