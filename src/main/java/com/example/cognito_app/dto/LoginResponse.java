package com.example.cognito_app.dto;

public class LoginResponse {
    
    private String email;
    private String accessToken;
    private String idToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;
    
    public LoginResponse() {
    }
    
    public LoginResponse(String email, String accessToken, String idToken, String refreshToken, Integer expiresIn, String tokenType) {
        this.email = email;
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public Integer getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
