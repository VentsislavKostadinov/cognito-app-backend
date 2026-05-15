package com.example.cognito_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class AwsCognitoConfig {
    
    @Value("${aws.cognito.region}")
    private String region;
    
    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;
    
    @Value("${aws.cognito.clientId}")
    private String clientId;
    
    @Value("${aws.cognito.domain}")
    private String domain;
    
    @Value("${aws.cognito.redirectUri}")
    private String redirectUri;
    
    @Value("${aws.cognito.scope}")
    private String scope;
    
    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }
    
    public String getUserPoolId() {
        return userPoolId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public String getScope() {
        return scope;
    }
    
    public String getRegion() {
        return region;
    }
}
