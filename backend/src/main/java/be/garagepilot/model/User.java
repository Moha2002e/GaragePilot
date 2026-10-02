package be.garagepilot.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "garage_id")
  private Garage garage;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  @Column(nullable = false)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

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

  public String getEmail() {
    return email;
  }

  public void setEmail(String v) {
    email = v;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String v) {
    passwordHash = v;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String v) {
    phone = v;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole v) {
    role = v;
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
