package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for authenticating an existing user. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credentials for logging an existing user in")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(description = "Registered email address", example = "admin@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Account password", example = "password123")
    private String password;

}
