package com.membership.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 255) String fullName) { }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) { }

    public record AuthResponse(
            String token,
            UUID userId,
            String email,
            String fullName,
            List<String> roles) { }
}
