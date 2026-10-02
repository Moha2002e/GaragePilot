package be.garagepilot.dto.invoice;

import be.garagepilot.model.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    String garageName,
    String garageVatNumber,
    String garageEmail,
    String garagePhone,
    String garageAddress,
    UUID clientId,
    String clientName,
    String clientVatNumber,
    String clientEmail,
    String clientPhone,
    String clientAddress,
    UUID vehicleId,
    String licensePlate,
    String vehicleDescription,
    String vin,
    Long mileage,
    UUID workOrderId,
    UUID quoteId,
    String invoiceNumber,
    InvoiceStatus status,
    LocalDate issueDate,
    LocalDate dueDate,
    BigDecimal subtotal,
    BigDecimal vatAmount,
    BigDecimal total,
    BigDecimal amountPaid,
    BigDecimal balanceDue,
    List<InvoiceItemResponse> items,
    Instant createdAt,
    Instant updatedAt) {}
