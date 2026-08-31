package com.example.smartinventory.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.smartinventory.security.JwtAuthenticationFilter;
import com.example.smartinventory.security.RateLimitFilter;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.RateLimitService;
import com.example.smartinventory.tenant.TenantFilter;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/** Baseline HTTP security configuration for the REST API, backed by stateless JWT authentication. */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/",
        "/index.html",
        "/favicon.ico",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api/auth/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Defines the HTTP security rules applied to incoming requests.
     *
     * @param http            the {@link HttpSecurity} builder to configure
     * @param rateLimitFilter the request throttling filter, placed after authentication
     * @param tenantFilter    the filter binding the caller's tenant to the request thread
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter,
            TenantFilter tenantFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, RateLimitFilter.class);
        return http.build();
    }

    /**
     * Builds the tenant-scoping filter used by the security chain.
     *
     * @return the tenant filter
     */
    @Bean
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    /**
     * Prevents Boot from also registering {@link TenantFilter} in the plain servlet chain, where it
     * would run before authentication and, being a once-per-request filter, suppress the instance
     * inside the security chain that can actually see the authenticated principal.
     *
     * @param tenantFilter the filter to keep out of the servlet chain
     * @return a disabled registration for the filter
     */
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter tenantFilter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(tenantFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Builds the rate-limiting filter used by the security chain.
     *
     * @param rateLimitService the limiter deciding whether a request may proceed
     * @param jsonMapper       mapper used to render the throttled error body
     * @return the rate-limiting filter
     */
    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitService rateLimitService, JsonMapper jsonMapper) {
        return new RateLimitFilter(rateLimitService, jsonMapper);
    }

    /**
     * Prevents Boot from also registering {@link RateLimitFilter} in the plain servlet chain, where
     * it would run before authentication and therefore never see the authenticated principal.
     *
     * @param rateLimitFilter the filter to keep out of the servlet chain
     * @return a disabled registration for the filter
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Wires the {@link DaoAuthenticationProvider} used to validate login credentials.
     *
     * @param passwordEncoder the password hashing strategy
     * @return an authentication provider backed by {@link UserDetailsServiceImpl}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} used to authenticate login requests.
     *
     * @param config the authentication configuration
     * @return the configured authentication manager
     * @throws Exception if the manager cannot be built
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides the password hashing strategy used to store and verify credentials.
     *
     * @return a BCrypt-based {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
