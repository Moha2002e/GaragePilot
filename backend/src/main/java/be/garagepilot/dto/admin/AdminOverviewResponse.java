package be.garagepilot.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record AdminOverviewResponse(
    long garages,
    long activeGarages,
    long users,
    long invoices,
    BigDecimal platformRevenue,
    BigDecimal outstandingTotal,
    Page<GarageSummary> companies) {

  public record GarageSummary(
      UUID id,
      String name,
      String vatNumber,
      String email,
      String city,
      boolean active,
      long users,
      long clients,
      long vehicles,
      long invoices,
      BigDecimal revenue,
      BigDecimal outstanding,
      Instant createdAt) {}
}
