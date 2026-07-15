package com.example.smartinventory.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.ExpiredJwtException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L);
    }

    @Test
    void generateTokenAndExtractUsernameRoundTrips() {
        UserDetails userDetails = User.withUsername("user@example.com").password("hashed")
                .authorities("ROLE_USER").build();

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValidReturnsTrueForMatchingUnexpiredToken() {
        UserDetails userDetails = User.withUsername("user@example.com").password("hashed")
                .authorities("ROLE_USER").build();
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUser() {
        UserDetails owner = User.withUsername("owner@example.com").password("hashed")
                .authorities("ROLE_USER").build();
        UserDetails other = User.withUsername("other@example.com").password("hashed")
                .authorities("ROLE_USER").build();
        String token = jwtService.generateToken(owner);

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void isTokenValidThrowsForExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1L);
        UserDetails userDetails = User.withUsername("user@example.com").password("hashed")
                .authorities("ROLE_USER").build();
        String token = shortLived.generateToken(userDetails);
        Thread.sleep(20);

        assertThatThrownBy(() -> shortLived.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

}
