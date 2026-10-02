package be.garagepilot.service;

import be.garagepilot.dto.auth.*;
import be.garagepilot.exception.BusinessException;
import be.garagepilot.model.*;
import be.garagepilot.repository.*;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  private final GarageRepository garages;
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwtService;
  private final GarageServiceCatalogService garageServices;

  public AuthService(
      GarageRepository garages,
      UserRepository users,
      PasswordEncoder encoder,
      JwtService jwtService,
      GarageServiceCatalogService garageServices) {
    this.garages = garages;
    this.users = users;
    this.encoder = encoder;
    this.jwtService = jwtService;
    this.garageServices = garageServices;
  }

  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (users.existsByEmailIgnoreCase(email))
      throw new BusinessException("An account with this email already exists");
    Garage garage = new Garage();
    garage.setName(request.garageName());
    garage.setEmail(normalizeEmail(request.garageEmail()));
    garage.setVatNumber(request.vatNumber());
    garage.setPhone(request.garagePhone());
    garages.save(garage);
    User user = new User();
    user.setGarage(garage);
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setEmail(email);
    user.setPasswordHash(encoder.encode(request.password()));
    user.setPhone(request.phone());
    user.setRole(UserRole.OWNER);
    users.save(user);
    garageServices.initializeDefaults(garage);
    return response(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        users
            .findByEmailIgnoreCase(normalizeEmail(request.email()))
            .filter(candidate -> candidate.isActive() && candidate.getGarage().isActive())
            .orElseThrow(() -> new BusinessException("Invalid email or password"));
    if (!encoder.matches(request.password(), user.getPasswordHash()))
      throw new BusinessException("Invalid email or password");
    return response(user);
  }

  private AuthResponse response(User user) {
    return new AuthResponse(
        jwtService.createToken(user),
        "Bearer",
        new UserResponse(
            user.getId(),
            user.getGarage().getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole().name()));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
