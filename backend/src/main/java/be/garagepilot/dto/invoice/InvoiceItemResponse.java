package be.garagepilot.dto.invoice;

import be.garagepilot.model.InvoiceItemType;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
    UUID id,
    InvoiceItemType type,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal vatRate,
    BigDecimal totalExcludingVat) {}
