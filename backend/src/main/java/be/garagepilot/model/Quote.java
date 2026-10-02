package be.garagepilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quotes")
@Getter
@Setter
public class Quote extends AuditableEntity {

  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "garage_id", nullable = false)
  private Garage garage;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Column(name = "work_order_id")
  private UUID workOrderId;

  @Column(name = "quote_number", nullable = false)
  private String quoteNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private QuoteStatus status;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal vatAmount;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Column(name = "customer_comment")
  private String customerComment;
}
