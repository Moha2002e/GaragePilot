package be.garagepilot.dto.admin;

import be.garagepilot.model.InvoiceStatus;
import be.garagepilot.model.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminGarageDetailResponse(
    UUID id,
    String name,
    String vatNumber,
    String email,
    String phone,
    String address,
    boolean active,
    List<AdminUser> users,
    List<AdminInvoice> recentInvoices) {

  public record AdminUser(
      UUID id,
      String firstName,
      String lastName,
      String email,
      String phone,
      UserRole role,
      boolean active,
      Instant createdAt) {}

  public record AdminInvoice(
      UUID id,
      String invoiceNumber,
      String clientName,
      String licensePlate,
      LocalDate issueDate,
      BigDecimal total,
      BigDecimal balanceDue,
      InvoiceStatus status) {}
}
