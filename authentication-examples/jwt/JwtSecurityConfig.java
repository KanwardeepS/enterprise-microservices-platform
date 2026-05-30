package com.authentication.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT Security Configuration
 * 
 * This configuration:
 * - Disables CSRF (not needed with stateless JWT)
 * - Sets session management to STATELESS (JWT is stateless)
 * - Adds JWT authentication filter
 * - Configures public and protected endpoints
 * 
 * Key difference from session-based auth:
 * - No server-side session storage
 * - Token includes all necessary information
 * - Highly scalable for microservices
 */
@Configuration
@EnableWebSecurity
public class JwtSecurityConfig {
    
    /**
     * Security filter chain for JWT authentication
     * 
     * @param http HttpSecurity configuration
     * @return     Configured SecurityFilterChain
     * @throws Exception If configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection
            // CSRF tokens are not needed with stateless JWT authentication
            .csrf()
                .disable()
            
            // Set session management to STATELESS
            // Each request contains the JWT token, no server session needed
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Configure request authorization
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/", "/public/**").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT authentication filter
            // This filter runs before UsernamePasswordAuthenticationFilter
            .addFilterBefore(
                new JwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
            )
            
            // Exception handling for authentication failures
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" +
                            authException.getMessage() + "\"}");
                })
            .and()
            
            // Logout configuration
            .logout()
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID");
        
        return http.build();
    }
}
