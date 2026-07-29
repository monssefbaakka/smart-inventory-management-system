package com.example.smartinventory.tenant;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.smartinventory.security.AuthenticatedUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Binds the authenticated caller's tenant to the request thread, so every JPA statement issued
 * while handling the request is scoped to that tenant. Anonymous requests keep the default tenant.
 *
 * <p>Registered in the security chain after authentication; see
 * {@code SecurityConfig#securityFilterChain}.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            TenantContext.setTenantId(user.getTenantId());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

}
