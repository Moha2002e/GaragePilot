package be.garagepilot.dto.auth;

public record AuthResponse(String accessToken, String tokenType, UserResponse user) {}
