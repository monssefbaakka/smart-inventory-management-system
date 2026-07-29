package com.example.smartinventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.example.smartinventory.exception.InactiveTenantException;
import com.example.smartinventory.model.Tenant;
import com.example.smartinventory.repository.UserRepository;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DEFAULT_TENANT = "default";

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

    @Mock
    private TenantService tenantService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, userDetailsService,
                jwtService, tenantService, DEFAULT_TENANT);
    }

    @Test
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", null);
        UserDetails userDetails = User.withUsername("new@example.com").password("hashed").authorities("ROLE_USER")
                .build();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(tenantService.findActiveBySlug(DEFAULT_TENANT))
                .thenReturn(Tenant.builder().slug(DEFAULT_TENANT).name("Default").build());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("new@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("token-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any());
    }

    @Test
    void registerJoinsRequestedTenant() {
        RegisterRequest request = new RegisterRequest("new@acme.example", "password123", "acme");
        UserDetails userDetails = User.withUsername("new@acme.example").password("hashed").authorities("ROLE_USER")
                .build();
        when(userRepository.existsByEmail("new@acme.example")).thenReturn(false);
        when(tenantService.findActiveBySlug("acme"))
                .thenReturn(Tenant.builder().slug("acme").name("Acme").build());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("new@acme.example")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("token-789");

        authService.register(request);

        ArgumentCaptor<com.example.smartinventory.model.User> captor =
                ArgumentCaptor.forClass(com.example.smartinventory.model.User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("acme");
    }

    @Test
    void registerThrowsWhenTenantIsInactive() {
        RegisterRequest request = new RegisterRequest("new@acme.example", "password123", "acme");
        when(userRepository.existsByEmail("new@acme.example")).thenReturn(false);
        when(tenantService.findActiveBySlug("acme")).thenThrow(new InactiveTenantException("Tenant is not active"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(InactiveTenantException.class);
        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void registerThrowsWhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123", null);
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
