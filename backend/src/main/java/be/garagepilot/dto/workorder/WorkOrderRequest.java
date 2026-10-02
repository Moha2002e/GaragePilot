package be.garagepilot.dto.workorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record WorkOrderRequest(
    @NotNull UUID clientId,
    @NotNull UUID vehicleId,
    UUID appointmentId,
    @PositiveOrZero Long mileageIn,
    @PositiveOrZero Long mileageOut,
    @NotBlank @Size(max = 4000) String description,
    @Size(max = 4000) String technicianNotes,
    @Size(max = 4000) String customerNotes,
    @NotNull List<@Valid WorkOrderItemRequest> items) {}
