package be.garagepilot.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle extends AuditableEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "garage_id")
  private Garage garage;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id")
  private Client client;

  @Column(nullable = false)
  private String licensePlate;

  private String vin;

  @Column(nullable = false)
  private String brand;

  @Column(nullable = false)
  private String model;

  private String version;

  @Enumerated(EnumType.STRING)
  private FuelType fuelType;

  private LocalDate firstRegistrationDate;
  private Long mileage;
  private String color;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(nullable = false)
  private boolean active = true;

  public UUID getId() {
    return id;
  }

  public Garage getGarage() {
    return garage;
  }

  public void setGarage(Garage v) {
    garage = v;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client v) {
    client = v;
  }

  public String getLicensePlate() {
    return licensePlate;
  }

  public void setLicensePlate(String v) {
    licensePlate = v;
  }

  public String getVin() {
    return vin;
  }

  public void setVin(String v) {
    vin = v;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String v) {
    brand = v;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String v) {
    model = v;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String v) {
    version = v;
  }

  public FuelType getFuelType() {
    return fuelType;
  }

  public void setFuelType(FuelType v) {
    fuelType = v;
  }

  public LocalDate getFirstRegistrationDate() {
    return firstRegistrationDate;
  }

  public void setFirstRegistrationDate(LocalDate v) {
    firstRegistrationDate = v;
  }

  public Long getMileage() {
    return mileage;
  }

  public void setMileage(Long v) {
    mileage = v;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String v) {
    color = v;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String v) {
    notes = v;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    active = false;
  }
}
