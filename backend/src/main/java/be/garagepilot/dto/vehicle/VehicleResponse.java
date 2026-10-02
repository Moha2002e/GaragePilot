package be.garagepilot.dto.vehicle;

import be.garagepilot.model.FuelType;
import java.time.*;
import java.util.UUID;

public record VehicleResponse(
    UUID id,
    UUID clientId,
    String licensePlate,
    String vin,
    String brand,
    String model,
    String version,
    FuelType fuelType,
    LocalDate firstRegistrationDate,
    Long mileage,
    String color,
    String notes,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
