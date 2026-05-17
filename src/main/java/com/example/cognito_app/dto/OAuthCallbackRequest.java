package com.example.cognito_app.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request containing OAuth callback parameters
 */
public class OAuthCallbackRequest {
    @NotBlank(message = "Authorization code is required")
    private String code;
    
    private String state;
    
    public OAuthCallbackRequest() {
    }
    
    public OAuthCallbackRequest(String code, String state) {
        this.code = code;
        this.state = state;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
}
