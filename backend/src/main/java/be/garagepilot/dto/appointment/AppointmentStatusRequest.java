package be.garagepilot.dto.appointment;

import be.garagepilot.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {}
