package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request payload for registering a new user account. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details for registering a new user account")
public class RegisterRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    @Schema(description = "Email address, unique per account", example = "admin@example.com")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(description = "Password, 8-100 characters", example = "password123")
    private String password;

}
