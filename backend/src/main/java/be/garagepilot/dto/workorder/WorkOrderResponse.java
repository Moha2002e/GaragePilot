package be.garagepilot.dto.workorder;

import be.garagepilot.model.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkOrderResponse(
    UUID id,
    UUID clientId,
    String clientName,
    UUID vehicleId,
    String licensePlate,
    String vehicleDescription,
    UUID appointmentId,
    WorkOrderStatus status,
    Long mileageIn,
    Long mileageOut,
    String description,
    String technicianNotes,
    String customerNotes,
    Instant startedAt,
    Instant completedAt,
    BigDecimal subtotal,
    BigDecimal vatAmount,
    BigDecimal total,
    List<WorkOrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt) {}
