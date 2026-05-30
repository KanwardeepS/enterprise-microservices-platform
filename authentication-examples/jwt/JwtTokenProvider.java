package com.authentication.jwt;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * JWT Token Provider
 * 
 * Handles:
 * - Creating JWT tokens with claims
 * - Validating token signature and expiration
 * - Extracting claims and user information from tokens
 * 
 * Security Notes:
 * - Secret key should be at least 32 characters (256-bit) for HS512
 * - Never log or expose secret key
 * - Use HTTPS to prevent token interception
 * - Include expiration times to limit token lifetime
 */
@Service
public class JwtTokenProvider {
    
    @Value("${app.jwtSecret}")
    private String jwtSecret;
    
    @Value("${app.jwtExpiration:86400}")  // Default: 24 hours in seconds
    private int jwtExpiration;
    
    /**
     * Create a JWT token
     * 
     * @param userId   User's unique identifier
     * @param username User's username
     * @param roles    List of roles/authorities
     * @return         Signed JWT token string
     */
    public String createToken(String userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("username", username);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)                    // Subject (user ID)
            .setIssuedAt(new Date())               // Issue time (now)
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)  // Sign with secret
            .compact();
    }
    
    /**
     * Create a refresh token with longer expiration
     * 
     * @param userId User's unique identifier
     * @param expirationDays Days until token expires
     * @return       Signed JWT refresh token
     */
    public String createRefreshToken(String userId, int expirationDays) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationDays * 24 * 60 * 60 * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * Validate JWT token
     * 
     * Checks:
     * - Signature validity (not tampered with)
     * - Expiration time (not expired)
     * - Proper format
     * 
     * @param token JWT token to validate
     * @return      true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            System.err.println("Invalid JWT token: " + ex.getMessage());
        } catch (ExpiredJwtException ex) {
            System.err.println("Expired JWT token: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            System.err.println("Unsupported JWT token: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT claims string is empty: " + ex.getMessage());
        }
        return false;
    }
    
    /**
     * Extract all claims from token
     * 
     * @param token JWT token
     * @return      Claims object containing all token information
     * @throws JwtException If token is invalid or expired
     */
    public Claims getClaimsFromToken(String token) throws JwtException {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Extract user ID (subject) from token
     * 
     * @param token JWT token
     * @return      User ID
     */
    public String getUserIdFromToken(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }
    
    /**
     * Extract username from token claims
     * 
     * @param token JWT token
     * @return      Username
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("username", String.class);
        } catch (JwtException e) {
            return null;
        }
    }
    
    /**
     * Extract roles from token claims
     * 
     * @param token JWT token
     * @return      List of roles
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }
            return new ArrayList<>();
        } catch (JwtException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if token is expired
     * 
     * @param token JWT token
     * @return      true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
    
    /**
     * Get time remaining until token expiration
     * 
     * @param token JWT token
     * @return      Milliseconds until expiration, or -1 if expired/invalid
     */
    public long getTimeUntilExpiration(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            long expirationTime = claims.getExpiration().getTime();
            long currentTime = System.currentTimeMillis();
            return Math.max(0, expirationTime - currentTime);
        } catch (JwtException e) {
            return -1;
        }
    }
}
