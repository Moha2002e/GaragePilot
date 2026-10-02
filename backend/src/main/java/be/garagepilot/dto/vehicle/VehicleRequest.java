package be.garagepilot.dto.vehicle;

import be.garagepilot.model.FuelType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleRequest(
    @NotNull UUID clientId,
    @NotBlank @Size(max = 20) String licensePlate,
    @Size(max = 17) String vin,
    @NotBlank String brand,
    @NotBlank String model,
    String version,
    FuelType fuelType,
    LocalDate firstRegistrationDate,
    @PositiveOrZero Long mileage,
    String color,
    String notes) {}
