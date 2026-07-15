package com.example.smartinventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.smartinventory.dto.AuthResponse;
import com.example.smartinventory.dto.LoginRequest;
import com.example.smartinventory.dto.RegisterRequest;
import com.example.smartinventory.exception.DuplicateEmailException;
import com.example.smartinventory.repository.UserRepository;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");
        UserDetails userDetails = User.withUsername("new@example.com").password("hashed").authorities("ROLE_USER")
                .build();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("new@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("token-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any());
    }

    @Test
    void registerThrowsWhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        UserDetails userDetails = User.withUsername("user@example.com").password("hashed").authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("token-456");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token-456");
    }

}
