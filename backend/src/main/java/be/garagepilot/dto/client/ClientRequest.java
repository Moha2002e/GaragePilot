package be.garagepilot.dto.client;

import be.garagepilot.model.ClientType;
import jakarta.validation.constraints.*;

public record ClientRequest(
    @NotNull ClientType type,
    String firstName,
    String lastName,
    String companyName,
    String vatNumber,
    @Email String email,
    String phone,
    String street,
    String postalCode,
    String city,
    @Size(min = 2, max = 2) String country,
    String notes) {}
