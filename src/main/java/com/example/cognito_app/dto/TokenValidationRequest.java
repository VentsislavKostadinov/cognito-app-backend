package com.example.cognito_app.dto;

import jakarta.validation.constraints.NotBlank;

public class TokenValidationRequest {
    
    @NotBlank(message = "ID token is required")
    private String idToken;
    
    private String accessToken;
    
    public TokenValidationRequest() {
    }
    
    public TokenValidationRequest(String idToken, String accessToken) {
        this.idToken = idToken;
        this.accessToken = accessToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
