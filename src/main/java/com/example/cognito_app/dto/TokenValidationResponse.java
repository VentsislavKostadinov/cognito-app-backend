package com.example.cognito_app.dto;

import java.util.Map;

public class TokenValidationResponse {
    
    private boolean valid;
    private String username;
    private String email;
    private String sub;
    private Long exp;
    private Long iat;
    private String tokenUse;
    private Map<String, Object> claims;
    private String message;
    
    public TokenValidationResponse() {
    }
    
    public TokenValidationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getSub() {
        return sub;
    }
    
    public void setSub(String sub) {
        this.sub = sub;
    }
    
    public Long getExp() {
        return exp;
    }
    
    public void setExp(Long exp) {
        this.exp = exp;
    }
    
    public Long getIat() {
        return iat;
    }
    
    public void setIat(Long iat) {
        this.iat = iat;
    }
    
    public String getTokenUse() {
        return tokenUse;
    }
    
    public void setTokenUse(String tokenUse) {
        this.tokenUse = tokenUse;
    }
    
    public Map<String, Object> getClaims() {
        return claims;
    }
    
    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
