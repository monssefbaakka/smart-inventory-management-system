package com.example.smartinventory.config;

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
import com.example.smartinventory.security.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

/** Baseline HTTP security configuration for the REST API, backed by stateless JWT authentication. */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
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
     * @param http the {@link HttpSecurity} builder to configure
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
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
