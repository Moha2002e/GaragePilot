package be.garagepilot.dto.appointment;

import be.garagepilot.model.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentRequest(
    @NotNull UUID clientId,
    @NotNull UUID vehicleId,
    @NotNull OffsetDateTime startDateTime,
    @NotNull OffsetDateTime endDateTime,
    @NotBlank String reason,
    String notes,
    AppointmentStatus status) {}
