package be.garagepilot.dto.appointment;

import be.garagepilot.model.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID clientId,
    String clientName,
    UUID vehicleId,
    String licensePlate,
    String vehicleDescription,
    OffsetDateTime startDateTime,
    OffsetDateTime endDateTime,
    String reason,
    String notes,
    AppointmentStatus status) {}
