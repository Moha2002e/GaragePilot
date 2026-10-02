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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "garage_services")
@Getter
@Setter
public class GarageService extends AuditableEntity {

  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "garage_id", nullable = false)
  private Garage garage;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ServiceCategory category;

  private String description;

  @Column(name = "duration_minutes", nullable = false)
  private Integer durationMinutes;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal vatRate;

  @Column(nullable = false, length = 20)
  private String color = "#167A5B";

  @Column(nullable = false)
  private boolean active = true;
}
