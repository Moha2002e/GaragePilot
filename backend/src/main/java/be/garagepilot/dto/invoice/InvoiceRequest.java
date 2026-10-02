package be.garagepilot.dto.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
    @NotNull UUID clientId,
    @NotNull UUID vehicleId,
    UUID workOrderId,
    UUID quoteId,
    @NotNull LocalDate issueDate,
    LocalDate dueDate,
    @NotEmpty List<@Valid InvoiceItemRequest> items) {}
