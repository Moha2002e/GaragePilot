package be.garagepilot.dto.client;

import be.garagepilot.model.ClientType;
import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
    UUID id,
    ClientType type,
    String firstName,
    String lastName,
    String companyName,
    String email,
    String phone,
    String city,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
