package com.example.cognito_app.controller;

import com.example.cognito_app.dto.ErrorResponse;
import com.example.cognito_app.dto.LoginRequest;
import com.example.cognito_app.dto.LoginResponse;
import com.example.cognito_app.dto.TokenValidationRequest;
import com.example.cognito_app.dto.TokenValidationResponse;
import com.example.cognito_app.dto.UserInfoResponse;
import com.example.cognito_app.service.CognitoService;
import com.example.cognito_app.service.TokenValidationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final CognitoService cognitoService;
    private final TokenValidationService tokenValidationService;
    
    @Value("${app.environment:development}")
    private String environment;
    
    @Autowired
    public AuthController(CognitoService cognitoService, TokenValidationService tokenValidationService) {
        this.cognitoService = cognitoService;
        this.tokenValidationService = tokenValidationService;
    }
    
    /**
     * Login endpoint - authenticates user with email and password
     * POST /api/auth/login
     * @param loginRequest LoginRequest containing email and password
     * @param response HttpServletResponse to set cookies
     * @return LoginResponse containing tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            LoginResponse loginResponse = cognitoService.authenticateUser(loginRequest);
            
            // Set HttpOnly cookies for tokens
            setTokenCookies(response, loginResponse);
            
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    "AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    /**
     * Set HttpOnly cookies for tokens
     * @param response HttpServletResponse
     * @param loginResponse LoginResponse containing tokens
     */
    private void setTokenCookies(HttpServletResponse response, LoginResponse loginResponse) {
        boolean isProduction = "production".equalsIgnoreCase(environment);
        
        // Access Token Cookie
        Cookie accessTokenCookie = new Cookie("accessToken", loginResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(isProduction); // Only HTTPS in production
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(3600); // 1 hour
        if (isProduction) {
            accessTokenCookie.setAttribute("SameSite", "None"); // For cross-domain in production
        } else {
            accessTokenCookie.setAttribute("SameSite", "Lax"); // For localhost development
        }
        response.addCookie(accessTokenCookie);
        
        // ID Token Cookie
        Cookie idTokenCookie = new Cookie("idToken", loginResponse.getIdToken());
        idTokenCookie.setHttpOnly(true);
        idTokenCookie.setSecure(isProduction);
        idTokenCookie.setPath("/");
        idTokenCookie.setMaxAge(3600); // 1 hour
        if (isProduction) {
            idTokenCookie.setAttribute("SameSite", "None");
        } else {
            idTokenCookie.setAttribute("SameSite", "Lax");
        }
        response.addCookie(idTokenCookie);
        
        // Refresh Token Cookie
        if (loginResponse.getRefreshToken() != null) {
            Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(isProduction);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(2592000); // 30 days
            if (isProduction) {
                refreshTokenCookie.setAttribute("SameSite", "None");
            } else {
                refreshTokenCookie.setAttribute("SameSite", "Lax");
            }
            response.addCookie(refreshTokenCookie);
        }
    }
    
    /**
     * Refresh token endpoint - gets new tokens using refresh token
     * POST /api/auth/refresh
     * @param refreshToken The refresh token
     * @param response HttpServletResponse to set cookies
     * @return LoginResponse containing new tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken, HttpServletResponse response) {
        try {
            LoginResponse loginResponse = cognitoService.refreshTokens(refreshToken);
            
            // Set HttpOnly cookies for new tokens
            setTokenCookies(response, loginResponse);
            
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    "TOKEN_REFRESH_FAILED",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    /**
     * Get user info endpoint - retrieves user information using access token
     * GET /api/auth/user
     * @param authorization The Authorization header containing the access token (optional)
     * @param request HttpServletRequest to read cookies if Authorization header is not provided
     * @return UserInfoResponse containing user attributes
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        try {
            String accessToken = null;
            
            // First, try to get token from Authorization header
            if (authorization != null && !authorization.isEmpty()) {
                accessToken = authorization;
                if (authorization.startsWith("Bearer ")) {
                    accessToken = authorization.substring(7);
                }
            } else {
                // If no Authorization header, try to get from cookies
                accessToken = getCookieValue(request, "accessToken");
            }
            
            if (accessToken == null || accessToken.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse(
                        "No access token provided in Authorization header or cookies",
                        "NO_ACCESS_TOKEN",
                        HttpStatus.UNAUTHORIZED.value()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            UserInfoResponse response = cognitoService.getUserInfo(accessToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getMessage(),
                    "GET_USER_FAILED",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    /**
     * Check authentication status from cookies
     * GET /api/auth/me
     * @param request HttpServletRequest to read cookies
     * @return TokenValidationResponse with user information if authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        try {
            // Get tokens from cookies
            String accessToken = getCookieValue(request, "accessToken");
            String idToken = getCookieValue(request, "idToken");
            
            if (idToken == null || idToken.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse(
                        "Not authenticated - no tokens found in cookies",
                        "NOT_AUTHENTICATED",
                        HttpStatus.UNAUTHORIZED.value()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // Validate the tokens
            TokenValidationResponse response;
            if (accessToken != null && !accessToken.isEmpty()) {
                response = tokenValidationService.validateBothTokens(idToken, accessToken);
            } else {
                response = tokenValidationService.validateToken(idToken);
            }
            
            if (response.isValid()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Authentication check failed: " + e.getMessage(),
                    "AUTH_CHECK_FAILED",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    /**
     * Get cookie value by name
     * @param request HttpServletRequest
     * @param cookieName Name of the cookie
     * @return Cookie value or null if not found
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Validate token endpoint - validates JWT tokens from Cognito
     * POST /api/auth/validate
     * @param request TokenValidationRequest containing ID token and optionally access token
     * @return TokenValidationResponse with validation result and claims
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody TokenValidationRequest request) {
        TokenValidationResponse response;
        
        if (request.getAccessToken() != null && !request.getAccessToken().isEmpty()) {
            // Validate both tokens
            response = tokenValidationService.validateBothTokens(request.getIdToken(), request.getAccessToken());
        } else {
            // Validate only ID token
            response = tokenValidationService.validateToken(request.getIdToken());
        }
        
        if (response.isValid()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * Logout endpoint - clears authentication cookies
     * POST /api/auth/logout
     * @param response HttpServletResponse to clear cookies
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // Clear all token cookies
        clearCookie(response, "accessToken");
        clearCookie(response, "idToken");
        clearCookie(response, "refreshToken");
        
        return ResponseEntity.ok("Logged out successfully");
    }
    
    /**
     * Clear a specific cookie
     * @param response HttpServletResponse
     * @param cookieName Name of the cookie to clear
     */
    private void clearCookie(HttpServletResponse response, String cookieName) {
        boolean isProduction = "production".equalsIgnoreCase(environment);
        
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete cookie
        if (isProduction) {
            cookie.setAttribute("SameSite", "None");
        } else {
            cookie.setAttribute("SameSite", "Lax");
        }
        response.addCookie(cookie);
    }
    
    /**
     * Health check endpoint
     * GET /api/auth/health
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication service is running");
    }
}
