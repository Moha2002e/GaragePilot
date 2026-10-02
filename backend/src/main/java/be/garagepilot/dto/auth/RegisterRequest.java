package be.garagepilot.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank String garageName,
    @Email @NotBlank String garageEmail,
    String vatNumber,
    String garagePhone,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 12, max = 100) String password,
    String phone) {}
