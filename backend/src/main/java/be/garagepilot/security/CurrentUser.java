package be.garagepilot.security;

import be.garagepilot.model.UserRole;
import java.util.UUID;

public record CurrentUser(UUID id, UUID garageId, UserRole role) {}
