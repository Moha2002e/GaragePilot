package be.garagepilot.service;

import be.garagepilot.dto.admin.AdminGarageDetailResponse;
import be.garagepilot.dto.admin.AdminOverviewResponse;
import be.garagepilot.dto.admin.AdminUserRequest;
import be.garagepilot.exception.BusinessException;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.model.Client;
import be.garagepilot.model.Garage;
import be.garagepilot.model.Invoice;
import be.garagepilot.model.InvoiceStatus;
import be.garagepilot.model.User;
import be.garagepilot.model.UserRole;
import be.garagepilot.repository.ClientRepository;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.InvoiceRepository;
import be.garagepilot.repository.UserRepository;
import be.garagepilot.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

  private static final EnumSet<InvoiceStatus> OPEN_INVOICE_STATUSES =
      EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE);

  private final GarageRepository garages;
  private final UserRepository users;
  private final ClientRepository clients;
  private final VehicleRepository vehicles;
  private final InvoiceRepository invoices;
  private final PasswordEncoder passwordEncoder;

  public AdminService(
      GarageRepository garages,
      UserRepository users,
      ClientRepository clients,
      VehicleRepository vehicles,
      InvoiceRepository invoices,
      PasswordEncoder passwordEncoder) {
    this.garages = garages;
    this.users = users;
    this.clients = clients;
    this.vehicles = vehicles;
    this.invoices = invoices;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public AdminOverviewResponse overview(String search, Pageable pageable) {
    Page<Garage> garagePage = garages.search(normalizeSearch(search), pageable);
    List<UUID> garageIds = garagePage.stream().map(Garage::getId).toList();
    Map<UUID, Long> userCounts =
        garageIds.isEmpty() ? Map.of() : countMap(users.countByGarageIds(garageIds));
    Map<UUID, Long> clientCounts =
        garageIds.isEmpty() ? Map.of() : countMap(clients.countByGarageIds(garageIds));
    Map<UUID, Long> vehicleCounts =
        garageIds.isEmpty() ? Map.of() : countMap(vehicles.countByGarageIds(garageIds));
    Map<UUID, InvoiceMetrics> invoiceMetrics =
        garageIds.isEmpty() ? Map.of() : invoiceMetrics(garageIds);
    Page<AdminOverviewResponse.GarageSummary> companies =
        garagePage.map(
            garage ->
                toSummary(
                    garage,
                    userCounts.getOrDefault(garage.getId(), 0L),
                    clientCounts.getOrDefault(garage.getId(), 0L),
                    vehicleCounts.getOrDefault(garage.getId(), 0L),
                    invoiceMetrics.getOrDefault(garage.getId(), InvoiceMetrics.EMPTY)));
    return new AdminOverviewResponse(
        garages.count(),
        garages.countByActiveTrue(),
        users.count(),
        invoices.count(),
        invoices.sumPlatformRevenue(),
        invoices.sumPlatformBalanceDue(OPEN_INVOICE_STATUSES),
        companies);
  }

  @Transactional(readOnly = true)
  public AdminGarageDetailResponse garageDetail(UUID garageId) {
    Garage garage = garage(garageId);
    List<AdminGarageDetailResponse.AdminUser> garageUsers =
        users.findByGarageIdOrderByLastNameAscFirstNameAsc(garageId).stream()
            .map(this::toAdminUser)
            .toList();
    List<AdminGarageDetailResponse.AdminInvoice> recentInvoices =
        invoices
            .findByGarageId(
                garageId,
                PageRequest.of(
                    0, 30, Sort.by(Sort.Order.desc("issueDate"), Sort.Order.desc("createdAt"))))
            .stream()
            .map(this::toAdminInvoice)
            .toList();
    return new AdminGarageDetailResponse(
        garage.getId(),
        garage.getName(),
        garage.getVatNumber(),
        garage.getEmail(),
        garage.getPhone(),
        address(garage),
        garage.isActive(),
        garageUsers,
        recentInvoices);
  }

  public void setGarageActive(UUID garageId, boolean active) {
    garage(garageId).setActive(active);
  }

  public AdminGarageDetailResponse.AdminUser createUser(UUID garageId, AdminUserRequest request) {
    Garage garage = garage(garageId);
    validateManagedRole(request.role());
    String email = normalizeEmail(request.email());
    if (users.existsByEmailIgnoreCase(email)) {
      throw new BusinessException("An account with this email already exists");
    }
    if (request.password() == null || request.password().isBlank()) {
      throw new BusinessException("A password is required when creating a user");
    }
    User user = new User();
    user.setGarage(garage);
    applyUser(user, request, email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    return toAdminUser(users.save(user));
  }

  public AdminGarageDetailResponse.AdminUser updateUser(UUID userId, AdminUserRequest request) {
    User user =
        users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    validateManagedRole(request.role());
    String email = normalizeEmail(request.email());
    users
        .findByEmailIgnoreCase(email)
        .filter(existing -> !existing.getId().equals(userId))
        .ifPresent(
            existing -> {
              throw new BusinessException("An account with this email already exists");
            });
    applyUser(user, request, email);
    if (request.password() != null && !request.password().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.password()));
    }
    return toAdminUser(user);
  }

  public void setUserActive(UUID currentUserId, UUID userId, boolean active) {
    if (currentUserId.equals(userId) && !active) {
      throw new BusinessException("You cannot deactivate your own account");
    }
    User user =
        users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (user.getRole() == UserRole.SUPER_ADMIN) {
      throw new BusinessException("Platform administrators cannot be deactivated here");
    }
    user.setActive(active);
  }

  private AdminOverviewResponse.GarageSummary toSummary(
      Garage garage,
      long userCount,
      long clientCount,
      long vehicleCount,
      InvoiceMetrics invoiceMetrics) {
    return new AdminOverviewResponse.GarageSummary(
        garage.getId(),
        garage.getName(),
        garage.getVatNumber(),
        garage.getEmail(),
        garage.getCity(),
        garage.isActive(),
        userCount,
        clientCount,
        vehicleCount,
        invoiceMetrics.count(),
        invoiceMetrics.revenue(),
        invoiceMetrics.outstanding(),
        garage.getCreatedAt());
  }

  private Map<UUID, Long> countMap(List<Object[]> rows) {
    Map<UUID, Long> result = new HashMap<>();
    rows.forEach(row -> result.put((UUID) row[0], ((Number) row[1]).longValue()));
    return result;
  }

  private Map<UUID, InvoiceMetrics> invoiceMetrics(List<UUID> garageIds) {
    Map<UUID, InvoiceMetrics> result = new HashMap<>();
    invoices
        .summarizeByGarageIds(garageIds, OPEN_INVOICE_STATUSES)
        .forEach(
            row ->
                result.put(
                    (UUID) row[0],
                    new InvoiceMetrics(
                        ((Number) row[1]).longValue(), (BigDecimal) row[2], (BigDecimal) row[3])));
    return result;
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }

  private record InvoiceMetrics(long count, BigDecimal revenue, BigDecimal outstanding) {
    private static final InvoiceMetrics EMPTY =
        new InvoiceMetrics(0, BigDecimal.ZERO, BigDecimal.ZERO);
  }

  private AdminGarageDetailResponse.AdminUser toAdminUser(User user) {
    return new AdminGarageDetailResponse.AdminUser(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhone(),
        user.getRole(),
        user.isActive(),
        user.getCreatedAt());
  }

  private AdminGarageDetailResponse.AdminInvoice toAdminInvoice(Invoice invoice) {
    return new AdminGarageDetailResponse.AdminInvoice(
        invoice.getId(),
        invoice.getInvoiceNumber(),
        clientName(invoice.getClient()),
        invoice.getVehicle().getLicensePlate(),
        invoice.getIssueDate(),
        invoice.getTotal(),
        invoice.getBalanceDue(),
        invoice.getStatus());
  }

  private void applyUser(User user, AdminUserRequest request, String email) {
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setEmail(email);
    user.setPhone(request.phone());
    user.setRole(request.role());
  }

  private void validateManagedRole(UserRole role) {
    if (role == UserRole.SUPER_ADMIN) {
      throw new BusinessException("SUPER_ADMIN cannot be assigned through this endpoint");
    }
  }

  private Garage garage(UUID id) {
    return garages
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Garage not found"));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String address(Garage garage) {
    return String.join(
            " ",
            garage.getStreet() == null ? "" : garage.getStreet(),
            garage.getPostalCode() == null ? "" : garage.getPostalCode(),
            garage.getCity() == null ? "" : garage.getCity())
        .trim();
  }

  private String clientName(Client client) {
    if (client.getCompanyName() != null && !client.getCompanyName().isBlank()) {
      return client.getCompanyName();
    }
    return String.join(
            " ",
            client.getFirstName() == null ? "" : client.getFirstName(),
            client.getLastName() == null ? "" : client.getLastName())
        .trim();
  }
}
