package be.garagepilot.dto.invoice;

import be.garagepilot.model.InvoiceItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InvoiceItemRequest(
    @NotNull InvoiceItemType type,
    @NotBlank String description,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
    @NotNull @DecimalMin("0.00") BigDecimal vatRate) {}
