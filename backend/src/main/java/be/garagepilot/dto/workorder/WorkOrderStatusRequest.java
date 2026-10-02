package be.garagepilot.dto.workorder;

import be.garagepilot.model.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record WorkOrderStatusRequest(@NotNull WorkOrderStatus status) {}
