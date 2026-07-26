package com.example.smartinventory.security;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.smartinventory.dto.RateLimitDecision;
import com.example.smartinventory.service.RateLimitService;

import jakarta.servlet.FilterChain;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(rateLimitService, JsonMapper.builder().build());
        request = new MockHttpServletRequest("GET", "/api/products");
        request.setRemoteAddr("203.0.113.7");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestAndAdvertisesRemainingBudget() throws Exception {
        when(rateLimitService.isEnabled()).thenReturn(true);
        when(rateLimitService.check(anyString())).thenReturn(RateLimitDecision.allowed(100, 99));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getHeader(RateLimitFilter.HEADER_LIMIT)).isEqualTo("100");
        assertThat(response.getHeader(RateLimitFilter.HEADER_REMAINING)).isEqualTo("99");
        assertThat(response.getHeader(RateLimitFilter.HEADER_RETRY_AFTER)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rejectsThrottledRequestWithTooManyRequests() throws Exception {
        when(rateLimitService.isEnabled()).thenReturn(true);
        when(rateLimitService.check(anyString())).thenReturn(RateLimitDecision.rejected(100, 42));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getHeader(RateLimitFilter.HEADER_RETRY_AFTER)).isEqualTo("42");
        assertThat(response.getHeader(RateLimitFilter.HEADER_REMAINING)).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("\"status\":429", "/api/products");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void keysAuthenticatedCallersByUsername() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(rateLimitService.isEnabled()).thenReturn(true);
        when(rateLimitService.check(anyString())).thenReturn(RateLimitDecision.allowed(100, 99));

        filter.doFilter(request, response, filterChain);

        assertThat(capturedKey()).isEqualTo("user:user@example.com");
    }

    @Test
    void keysUnauthenticatedCallersByRemoteAddress() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        when(rateLimitService.isEnabled()).thenReturn(true);
        when(rateLimitService.check(anyString())).thenReturn(RateLimitDecision.allowed(100, 99));

        filter.doFilter(request, response, filterChain);

        assertThat(capturedKey()).isEqualTo("ip:203.0.113.7");
    }

    @Test
    void skipsNonApiRequests() throws Exception {
        when(rateLimitService.isEnabled()).thenReturn(true);
        MockHttpServletRequest swaggerRequest = new MockHttpServletRequest("GET", "/swagger-ui.html");

        filter.doFilter(swaggerRequest, response, filterChain);

        verify(rateLimitService, never()).check(anyString());
        verify(filterChain).doFilter(swaggerRequest, response);
    }

    @Test
    void skipsEveryRequestWhenLimitingIsDisabled() throws Exception {
        when(rateLimitService.isEnabled()).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(rateLimitService, never()).check(anyString());
        assertThat(response.getHeader(RateLimitFilter.HEADER_LIMIT)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private String capturedKey() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimitService).check(keyCaptor.capture());
        return keyCaptor.getValue();
    }

}
