package com.example.smartinventory.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.AuthResponse;
import com.example.smartinventory.dto.LoginRequest;
import com.example.smartinventory.dto.RegisterRequest;
import com.example.smartinventory.exception.DuplicateEmailException;
import com.example.smartinventory.model.User;
import com.example.smartinventory.repository.UserRepository;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

/** Handles user registration and authentication, issuing JWTs on success. */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsServiceImpl userDetailsService;

    private final JwtService jwtService;

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

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(DEFAULT_ROLE)
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
