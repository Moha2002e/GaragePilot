package be.garagepilot.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
public class WorkOrder extends AuditableEntity {

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

  @Column(name = "appointment_id")
  private UUID appointmentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private WorkOrderStatus status;

  @Column(name = "mileage_in")
  private Long mileageIn;

  @Column(name = "mileage_out")
  private Long mileageOut;

  private String description;

  @Column(name = "technician_notes")
  private String technicianNotes;

  @Column(name = "customer_notes")
  private String customerNotes;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<WorkOrderItem> items = new ArrayList<>();

  public void addItem(WorkOrderItem item) {
    items.add(item);
    item.setWorkOrder(this);
  }
}
