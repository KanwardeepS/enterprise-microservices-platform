package com.authentication.kerberos;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Spring Security Kerberos Configuration
 * 
 * This configuration enables Kerberos/SPNEGO authentication, commonly used in:
 * - Windows Active Directory environments
 * - Enterprise authentication scenarios
 * 
 * Requirements:
 * - Kerberos/LDAP server (e.g., Active Directory)
 * - krb5.conf configuration file
 * - Valid Kerberos principals and keytabs
 * 
 * Dependencies:
 * - spring-security-kerberos-core
 * - spring-security-kerberos-web
 */
@Configuration
@EnableWebSecurity
public class KerberosSecurityConfig {
    
    /**
     * SPNEGO Entry Point
     * 
     * SPNEGO (Simple and Protected GSS-API Negotiation Mechanism) is used for
     * browser-based Kerberos authentication.
     * 
     * @return SpnegoEntryPoint configured to redirect to /login on failure
     */
    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }
    
    /**
     * Kerberos Authentication Provider
     * 
     * Handles authentication against Kerberos KDC (Key Distribution Center)
     * 
     * @param userDetailsService Service to load user details after Kerberos auth
     * @return Configured KerberosAuthenticationProvider
     * @throws Exception If configuration fails
     */
    @Bean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider(
            UserDetailsService userDetailsService) throws Exception {
        
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        
        // Link Kerberos authentication with user details loading
        provider.setUserDetailsService(userDetailsService);
        
        return provider;
    }
    
    /**
     * Security Filter Chain for Kerberos
     * 
     * Configuration:
     * - Exception handling with SPNEGO entry point
     * - SPNEGO authentication processing filter
     * - All requests require authentication
     * 
     * @param http HttpSecurity configuration
     * @return Configured SecurityFilterChain
     * @throws Exception If configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .exceptionHandling()
                // SPNEGO handles initial authentication challenge
                .authenticationEntryPoint(spnegoEntryPoint())
            .and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login", "/public/**").permitAll()
                .anyRequest().authenticated()
            )
            // Add SPNEGO filter before Basic Auth filter
            .addFilterBefore(
                spnegoAuthenticationProcessingFilter(),
                BasicAuthenticationFilter.class
            )
            .logout(logout -> logout.logoutSuccessUrl("/"));
        
        return http.build();
    }
    
    /**
     * SPNEGO Authentication Processing Filter
     * 
     * Handles the SPNEGO token exchange and Kerberos ticket validation
     * 
     * @return Configured SpnegoAuthenticationProcessingFilter
     */
    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter() {
        SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }
    
    /**
     * Authentication Manager for Kerberos
     * 
     * @return AuthenticationManager configured with KerberosAuthenticationProvider
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(kerberosAuthenticationProvider(userDetailsService()));
    }
}
