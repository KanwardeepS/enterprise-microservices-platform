package com.authentication.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * 
 * Endpoints:
 * - POST /auth/login    - Authenticate and receive JWT token
 * - POST /auth/register - Register new user
 * - POST /auth/refresh  - Refresh expired JWT token
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserService userService;
    
    /**
     * Login endpoint
     * 
     * Verifies username/password and returns JWT token
     * 
     * @param loginRequest Contains username and password
     * @return             JWT token and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Find user by username
            User user = userService.findByUsername(loginRequest.getUsername());
            
            // Verify user exists and password matches
            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid username or password"));
            }
            
            // Generate JWT access token (short-lived, e.g., 1 hour)
            String accessToken = tokenProvider.createToken(
                    user.getId(),
                    user.getUsername(),
                    user.getRoles()
            );
            
            // Generate refresh token (long-lived, e.g., 7 days)
            String refreshToken = tokenProvider.createRefreshToken(user.getId(), 7);
            
            // Return tokens
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Login failed: " + e.getMessage()));
        }
    }
    
    /**
     * Register new user endpoint
     * 
     * @param registerRequest Contains username, password, email
     * @return                Success message
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Check if user already exists
            if (userService.findByUsername(registerRequest.getUsername()) != null) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Username already exists"));
            }
            
            // Create new user
            User newUser = new User();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            newUser.setRoles(List.of("ROLE_USER"));  // Default role
            
            // Save user
            userService.saveUser(newUser);
            
            return ResponseEntity.ok(new SuccessResponse("User registered successfully"));
            
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    
    /**
     * Refresh JWT token endpoint
     * 
     * Validates refresh token and returns new access token
     * 
     * @param refreshTokenRequest Contains refresh token
     * @return                    New access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();
            
            // Validate refresh token
            if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid refresh token"));
            }
            
            // Extract user ID from refresh token
            String userId = tokenProvider.getUserIdFromToken(refreshToken);
            
            // Load user to get updated roles
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }
            
            // Generate new access token
            String newAccessToken = tokenProvider.createToken(
                    user.getId(),
                    user.getUsername(),
                    user.getRoles()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Token refresh failed: " + e.getMessage()));
        }
    }
    
    // DTOs and Helper Classes
    
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }
    
    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
    
    @Data
    public static class ErrorResponse {
        private String error;
        private long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    @Data
    public static class SuccessResponse {
        private String message;
        private long timestamp;
        
        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
