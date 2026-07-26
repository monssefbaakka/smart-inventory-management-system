package com.example.smartinventory.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.smartinventory.dto.RateLimitDecision;
import com.example.smartinventory.exception.ErrorResponse;
import com.example.smartinventory.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Rejects API requests from a caller that exceeds its request budget.
 *
 * <p>Runs after authentication so an authenticated caller is limited per user rather than per
 * source address; unauthenticated callers fall back to their remote address. Every API response
 * carries the current budget, and throttled responses add {@code Retry-After}.
 *
 * <p>Wired into the security filter chain by {@code SecurityConfig} rather than component-scanned,
 * so it always sees the authenticated principal resolved by {@link JwtAuthenticationFilter}.
 */
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /** Header advertising the maximum requests allowed per window. */
    static final String HEADER_LIMIT = "X-RateLimit-Limit";

    /** Header advertising the requests still available in the current window. */
    static final String HEADER_REMAINING = "X-RateLimit-Remaining";

    /** Header telling a throttled caller how long to wait, in seconds. */
    static final String HEADER_RETRY_AFTER = "Retry-After";

    private static final String API_PATH_PREFIX = "/api/";

    private static final String ANONYMOUS_KEY_PREFIX = "ip:";

    private static final String USER_KEY_PREFIX = "user:";

    private final RateLimitService rateLimitService;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !rateLimitService.isEnabled() || !request.getRequestURI().startsWith(API_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        RateLimitDecision decision = rateLimitService.check(clientKey(request));

        response.setHeader(HEADER_LIMIT, String.valueOf(decision.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(decision.remaining()));

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        writeTooManyRequests(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null && !authentication.getName().isBlank()) {
            return USER_KEY_PREFIX + authentication.getName();
        }
        return ANONYMOUS_KEY_PREFIX + request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Rate limit exceeded; retry later")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

}
