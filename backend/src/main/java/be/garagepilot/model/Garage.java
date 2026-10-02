package be.garagepilot.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "garages")
public class Garage extends AuditableEntity {
  @Id private UUID id = UUID.randomUUID();

  @Column(nullable = false)
  private String name;

  private String vatNumber;

  @Column(nullable = false)
  private String email;

  private String phone;
  private String street;
  private String city;
  private String postalCode;

  @Column(nullable = false)
  private String country = "BE";

  @Column(nullable = false)
  private boolean active = true;

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String v) {
    name = v;
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

  public String getCity() {
    return city;
  }

  public void setCity(String v) {
    city = v;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String v) {
    postalCode = v;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String v) {
    country = v;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    active = false;
  }

  public void setActive(boolean value) {
    active = value;
  }
}
