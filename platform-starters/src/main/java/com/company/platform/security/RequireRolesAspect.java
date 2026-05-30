package com.company.platform.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
public class RequireRolesAspect {

    @Before("@within(requireRoles) || @annotation(requireRoles)")
    public void enforce(JoinPoint joinPoint, RequireRoles requireRoles) {
        if (requireRoles == null || requireRoles.value().length == 0) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Missing authentication for " + joinPoint.getSignature());
        }

        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean authorized = Arrays.stream(requireRoles.value())
                .filter(StringUtils::hasText)
                .map(this::normalizeRole)
                .anyMatch(authorities::contains);

        if (!authorized) {
            throw new AccessDeniedException("Role not allowed for " + joinPoint.getSignature());
        }
    }

    private String normalizeRole(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
