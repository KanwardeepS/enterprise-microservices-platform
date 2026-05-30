
package com.company.platform.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that manages correlation IDs and MDC context for distributed tracing and logging.
 * 
 * Responsibilities:
 * - Generates or extracts correlation ID from X-Correlation-ID header
 * - Stores correlation ID in SLF4J MDC for inclusion in all log statements
 * - Extracts user ID from JWT token and adds to MDC
 * - Stores request metadata (path, method) in MDC
 * - Ensures MDC is properly cleaned up after request completion
 */
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_REQUEST_PATH = "requestPath";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Generate or extract correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Store in MDC for logging
            MDC.put(MDC_CORRELATION_ID, correlationId);
            
            // Add correlation ID to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Extract and store user ID from JWT if authenticated
            extractAndStoreUserId();
            
            // Store request metadata in MDC
            MDC.put(MDC_REQUEST_PATH, request.getRequestURI());
            MDC.put(MDC_REQUEST_METHOD, request.getMethod());
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // CRITICAL: Clear MDC to prevent memory leaks and cross-request contamination
            // in thread pool environments (e.g., Tomcat, Jetty)
            MDC.clear();
        }
    }
    
    /**
     * Extracts user ID from JWT token and stores in MDC.
     * Supports both "sub" (subject) and "preferred_username" claims.
     */
    private void extractAndStoreUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                
                // Try "sub" claim first (standard JWT subject)
                String userId = jwt.getClaimAsString("sub");
                
                // Fallback to "preferred_username" (common in OAuth2/OIDC)
                if (userId == null || userId.isBlank()) {
                    userId = jwt.getClaimAsString("preferred_username");
                }
                
                // Fallback to "email" 
                if (userId == null || userId.isBlank()) {
                    userId = jwt.getClaimAsString("email");
                }
                
                if (userId != null && !userId.isBlank()) {
                    MDC.put(MDC_USER_ID, userId);
                }
            }
        } catch (Exception e) {
            // Don't fail the request if user extraction fails
            // Log at debug level to avoid noise
            logger.debug("Failed to extract user ID from JWT", e);
        }
    }
}
