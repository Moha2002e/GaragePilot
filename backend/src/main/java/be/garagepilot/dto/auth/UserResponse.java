package be.garagepilot.dto.auth;

import java.util.UUID;

public record UserResponse(
    UUID id, UUID garageId, String firstName, String lastName, String email, String role) {}
