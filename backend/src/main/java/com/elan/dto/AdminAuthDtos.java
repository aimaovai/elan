package com.elan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminAuthDtos {
    private AdminAuthDtos() {}

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 200) String password) {}
    public record AdministratorView(String email, String displayName, String role) {}
    public record CsrfView(String token, String headerName) {}
}
