package com.example.smartinventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.dto.AuthResponse;
import com.example.smartinventory.dto.LoginRequest;
import com.example.smartinventory.dto.RegisterRequest;
import com.example.smartinventory.exception.DuplicateEmailException;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.AuthService;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void registerReturnsCreatedWithToken() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.builder().token("token-123").tokenType("Bearer").build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-123"));
    }

    @Test
    void registerReturnsConflictWhenEmailTaken() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("Email already in use: taken@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"taken@example.com","password":"password123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsOkWithToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.builder().token("token-456").tokenType("Bearer").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-456"));
    }

    @Test
    void registerRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

}
