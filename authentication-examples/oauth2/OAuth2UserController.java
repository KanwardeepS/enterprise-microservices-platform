package com.authentication.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.rest.annotation.RestController;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * OAuth2 User Information Controller
 * 
 * Demonstrates how to:
 * - Access OAuth2 authorized client
 * - Extract access token
 * - Call OAuth2 resource server (e.g., Google API, GitHub API)
 */
@RestController
@RequestMapping("/api")
public class OAuth2UserController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Get user information from OAuth2 provider
     * 
     * The @RegisteredOAuth2AuthorizedClient annotation automatically injects
     * the OAuth2AuthorizedClient for the specified provider registration.
     * 
     * @param authorizedClient OAuth2 authorized client for Google
     * @return User information from Google
     */
    @GetMapping("/user-info")
    public Map<String, Object> getUserInfo(
            @RegisteredOAuth2AuthorizedClient("google") 
            OAuth2AuthorizedClient authorizedClient) {
        
        // Extract access token from authorized client
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        
        // Use access token to call Google API (UserInfo endpoint)
        String userInfo = restTemplate.getForObject(
            "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken,
            String.class
        );
        
        return Map.of(
            "provider", "google",
            "userInfo", userInfo
        );
    }
    
    /**
     * Get GitHub user information
     * 
     * Similar pattern but for GitHub OAuth2 provider
     */
    @GetMapping("/github-user")
    public Map<String, Object> getGitHubUser(
            @RegisteredOAuth2AuthorizedClient("github") 
            OAuth2AuthorizedClient authorizedClient) {
        
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        
        // GitHub API endpoint requires token in header
        String userInfo = restTemplate.getForObject(
            "https://api.github.com/user",
            String.class
        );
        
        return Map.of(
            "provider", "github",
            "userInfo", userInfo
        );
    }
    
    /**
     * Get current authenticated user's OAuth2 info
     * 
     * Returns basic information about the authenticated user from OAuth2
     */
    @GetMapping("/current-user")
    public Map<String, Object> getCurrentUser(
            @RegisteredOAuth2AuthorizedClient("google") 
            OAuth2AuthorizedClient authorizedClient) {
        
        String clientId = authorizedClient.getClientRegistration().getClientId();
        String clientName = authorizedClient.getClientRegistration().getClientName();
        String tokenValue = authorizedClient.getAccessToken().getTokenValue();
        
        return Map.of(
            "clientId", clientId,
            "clientName", clientName,
            "tokenPresent", tokenValue != null && !tokenValue.isEmpty(),
            "tokenType", authorizedClient.getAccessToken().getTokenType().getValue()
        );
    }
}
