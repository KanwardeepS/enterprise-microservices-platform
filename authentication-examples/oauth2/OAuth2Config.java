package com.authentication.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.util.Map;

/**
 * Spring Boot OAuth 2.0 Configuration
 * 
 * This configuration enables OAuth 2.0 authentication with providers like:
 * - Google
 * - GitHub
 * - Other OAuth2 compliant providers
 * 
 * Dependencies:
 * - spring-boot-starter-security
 * - spring-security-oauth2-client
 * - spring-boot-starter-web
 */
@Configuration
public class OAuth2Config {
    
    /**
     * Security filter chain configuration for OAuth 2.0
     * 
     * - Permits access to login page
     * - Requires authentication for all other requests
     * - Configures OAuth 2.0 login with redirects
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login").permitAll()
                .requestMatchers("/", "/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            );
        return http.build();
    }
}
