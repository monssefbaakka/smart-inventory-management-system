package com.example.smartinventory.tenant;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.example.smartinventory.security.AuthenticatedUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void bindsTenantOfAuthenticatedCallerForTheRequest() throws Exception {
        authenticate(new AuthenticatedUser("user@acme.example", "hashed", "acme",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(TenantContext.getTenantId());

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isEqualTo("acme");
    }

    @Test
    void clearsTenantAfterTheRequest() throws Exception {
        authenticate(new AuthenticatedUser("user@acme.example", "hashed", "acme",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void clearsTenantWhenTheRequestFails() {
        authenticate(new AuthenticatedUser("user@acme.example", "hashed", "acme",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception expected) {
            // the filter must still unbind the tenant
        }

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void leavesTenantUnboundForAnonymousRequests() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>("sentinel");
        FilterChain chain = (req, res) -> seen.set(TenantContext.getTenantId());

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isNull();
    }

    @Test
    void leavesTenantUnboundWhenPrincipalCarriesNoTenant() throws Exception {
        authenticate(User.withUsername("user@example.com").password("hashed").authorities("ROLE_USER").build());
        AtomicReference<String> seen = new AtomicReference<>("sentinel");
        FilterChain chain = (req, res) -> seen.set(TenantContext.getTenantId());

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isNull();
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

}
