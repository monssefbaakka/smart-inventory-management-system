package com.example.smartinventory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.AuthResponse;
import com.example.smartinventory.dto.LoginRequest;
import com.example.smartinventory.dto.RegisterRequest;
import com.example.smartinventory.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for user registration and authentication. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Auth", description = "Registration and login (public, no token required)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Creates a user account and returns a JWT for immediate use.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created and token issued"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content)
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in",
            description = "Authenticates an existing user and returns a JWT bearer token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated and token issued"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

}
