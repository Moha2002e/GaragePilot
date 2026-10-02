package be.garagepilot.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client extends AuditableEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "garage_id")
  private Garage garage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ClientType type;

  private String firstName,
      lastName,
      companyName,
      vatNumber,
      email,
      phone,
      street,
      postalCode,
      city;

  @Column(nullable = false)
  private String country = "BE";

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

  public ClientType getType() {
    return type;
  }

  public void setType(ClientType v) {
    type = v;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String v) {
    firstName = v;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String v) {
    lastName = v;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String v) {
    companyName = v;
  }

  public String getVatNumber() {
    return vatNumber;
  }

  public void setVatNumber(String v) {
    vatNumber = v;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String v) {
    email = v;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String v) {
    phone = v;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String v) {
    street = v;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String v) {
    postalCode = v;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String v) {
    city = v;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String v) {
    country = v;
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
