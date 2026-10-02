package be.garagepilot.dto.workorder;

import be.garagepilot.model.WorkOrderItemType;
import java.math.BigDecimal;
import java.util.UUID;

public record WorkOrderItemResponse(
    UUID id,
    WorkOrderItemType type,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal vatRate,
    BigDecimal totalExcludingVat) {}
