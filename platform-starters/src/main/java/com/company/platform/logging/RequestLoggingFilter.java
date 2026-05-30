package com.company.platform.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Filter that logs HTTP requests and responses with configurable verbosity.
 * 
 * Logs:
 * - Request: method, URI, query parameters, headers (optional)
 * - Response: status code, duration
 * - Body content (optional, for debugging)
 * 
 * Automatically excludes sensitive headers (Authorization, Cookie) from logs.
 * Can be enabled/disabled via configuration.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token"
    );
    
    private static final int MAX_BODY_LENGTH = 1000; // Max characters to log from body
    
    private final boolean includeHeaders;
    private final boolean includePayload;
    private final Set<String> excludedPaths;
    
    public RequestLoggingFilter() {
        this(false, false, Set.of("/actuator/health", "/actuator/prometheus"));
    }
    
    public RequestLoggingFilter(boolean includeHeaders, boolean includePayload, Set<String> excludedPaths) {
        this.includeHeaders = includeHeaders;
        this.includePayload = includePayload;
        this.excludedPaths = excludedPaths;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip logging for excluded paths (actuator endpoints)
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        // Wrap request/response to enable body reading
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        try {
            // Log incoming request
            logRequest(requestWrapper);
            
            // Process request
            filterChain.doFilter(requestWrapper, responseWrapper);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log response
            logResponse(responseWrapper, duration);
            
            // IMPORTANT: Copy body to actual response
            responseWrapper.copyBodyToResponse();
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
    
    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Request: ")
                  .append(request.getMethod())
                  .append(" ")
                  .append(request.getRequestURI());
        
        // Query parameters
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            logMessage.append("?").append(queryString);
        }
        
        // Headers (optional)
        if (includeHeaders) {
            logMessage.append(" | Headers: ").append(getSafeHeaders(request));
        }
        
        // Body (optional, for debugging)
        if (includePayload) {
            String body = getRequestBody(request);
            if (body != null && !body.isBlank()) {
                logMessage.append(" | Body: ").append(truncate(body));
            }
        }
        
        log.info("{}", logMessage);
    }
    
    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Response: ")
                  .append(response.getStatus())
                  .append(" | Duration: ")
                  .append(durationMs)
                  .append("ms");
        
        // Body (optional, for debugging)
        if (includePayload) {
            String body = getResponseBody(response);
            if (body != null && !body.isBlank()) {
                logMessage.append(" | Body: ").append(truncate(body));
            }
        }
        
        // Log at different levels based on status code
        if (response.getStatus() >= 500) {
            log.error("{}", logMessage);
        } else if (response.getStatus() >= 400) {
            log.warn("{}", logMessage);
        } else {
            log.info("{}", logMessage);
        }
    }
    
    /**
     * Extract headers, excluding sensitive ones.
     */
    private Map<String, String> getSafeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // Skip sensitive headers
            if (SENSITIVE_HEADERS.contains(headerName.toLowerCase())) {
                headers.put(headerName, "***");
            } else {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        
        return headers;
    }
    
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }
    
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }
    
    /**
     * Truncate long strings to prevent excessive logging.
     */
    private String truncate(String str) {
        if (str.length() <= MAX_BODY_LENGTH) {
            return str;
        }
        return str.substring(0, MAX_BODY_LENGTH) + "... (truncated)";
    }
}
