package com.example.smartinventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.smartinventory.dto.AuthResponse;
import com.example.smartinventory.dto.LoginRequest;
import com.example.smartinventory.dto.RegisterRequest;
import com.example.smartinventory.exception.DuplicateEmailException;
import com.example.smartinventory.model.Role;
import com.example.smartinventory.model.Tenant;
import com.example.smartinventory.model.User;
import com.example.smartinventory.repository.UserRepository;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;

/** Handles user registration and authentication, issuing JWTs on success. */
@Service
@Transactional
public class AuthService {

    private static final Role DEFAULT_ROLE = Role.USER;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsServiceImpl userDetailsService;

    private final JwtService jwtService;

    private final TenantService tenantService;

    private final String defaultTenant;

    /**
     * Creates the authentication service.
     *
     * @param userRepository        store holding user accounts
     * @param passwordEncoder       hashing strategy for stored passwords
     * @param authenticationManager manager validating login credentials
     * @param userDetailsService    loader turning accounts into authenticated principals
     * @param jwtService            issuer of access tokens
     * @param tenantService         registry used to resolve the tenant an account joins
     * @param defaultTenant         tenant joined when a registration names none
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, UserDetailsServiceImpl userDetailsService,
            JwtService jwtService, TenantService tenantService,
            @Value("${multitenancy.default-tenant}") String defaultTenant) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.tenantService = tenantService;
        this.defaultTenant = defaultTenant;
    }

    /**
     * Creates a new user account and returns a JWT for it.
     *
     * @param request the registration payload
     * @return an access token for the newly created user
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + request.getEmail());
        }

        String slug = StringUtils.hasText(request.getTenantSlug()) ? request.getTenantSlug() : defaultTenant;
        Tenant tenant = tenantService.findActiveBySlug(slug);

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(DEFAULT_ROLE)
                .tenantId(tenant.getSlug())
                .build();
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return buildAuthResponse(userDetails);
    }

    /**
     * Authenticates an existing user and returns a JWT for it.
     *
     * @param request the login payload
     * @return an access token for the authenticated user
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        return buildAuthResponse(userDetails);
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(userDetails))
                .tokenType("Bearer")
                .build();
    }

}
