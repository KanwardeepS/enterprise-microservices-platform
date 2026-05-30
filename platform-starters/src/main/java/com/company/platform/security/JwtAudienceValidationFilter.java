package com.company.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAudienceValidationFilter extends OncePerRequestFilter {

    private final String requiredAudience;

    public JwtAudienceValidationFilter(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (StringUtils.hasText(requiredAudience)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                List<String> audiences = jwt.getAudience();
                if (audiences == null || audiences.stream().noneMatch(requiredAudience::equals)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid token audience");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
