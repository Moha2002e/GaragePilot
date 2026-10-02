package be.garagepilot.service;

import be.garagepilot.dto.servicecatalog.GarageServiceRequest;
import be.garagepilot.dto.servicecatalog.GarageServiceResponse;
import be.garagepilot.exception.BusinessException;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.model.Garage;
import be.garagepilot.model.GarageService;
import be.garagepilot.model.ServiceCategory;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.GarageServiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GarageServiceCatalogService {

  private final GarageServiceRepository services;
  private final GarageRepository garages;

  public GarageServiceCatalogService(GarageServiceRepository services, GarageRepository garages) {
    this.services = services;
    this.garages = garages;
  }

  @Transactional(readOnly = true)
  public List<GarageServiceResponse> findAll(
      UUID garageId, String search, ServiceCategory category) {
    return services.searchActive(garageId, normalizeSearch(search), category).stream()
        .map(this::toResponse)
        .toList();
  }

  public GarageServiceResponse create(UUID garageId, GarageServiceRequest request) {
    validateUniqueName(garageId, request.name(), null);
    GarageService service = new GarageService();
    service.setGarage(garages.getReferenceById(garageId));
    apply(service, request);
    return toResponse(services.save(service));
  }

  public void initializeDefaults(Garage garage) {
    if (!services.findByGarageIdOrderByCategoryAscNameAsc(garage.getId()).isEmpty()) {
      return;
    }
    services.saveAll(
        List.of(
            defaultService(
                garage, "Entretien annuel", ServiceCategory.MAINTENANCE, 90, "149.00", "#087a55"),
            defaultService(
                garage, "Vidange moteur", ServiceCategory.MAINTENANCE, 45, "89.00", "#12b76a"),
            defaultService(
                garage,
                "Contrôle et remplacement des freins",
                ServiceCategory.MECHANICAL,
                120,
                "219.00",
                "#f79009"),
            defaultService(
                garage, "Montage de quatre pneus", ServiceCategory.TIRES, 75, "96.00", "#2e90fa"),
            defaultService(
                garage,
                "Diagnostic électronique",
                ServiceCategory.DIAGNOSTIC,
                60,
                "79.00",
                "#6172f3"),
            defaultService(
                garage,
                "Contrôle batterie et alternateur",
                ServiceCategory.ELECTRICAL,
                30,
                "45.00",
                "#7f56d9"),
            defaultService(
                garage,
                "Entretien climatisation",
                ServiceCategory.MAINTENANCE,
                60,
                "129.00",
                "#06aed4"),
            defaultService(
                garage,
                "Géométrie et parallélisme",
                ServiceCategory.TIRES,
                60,
                "85.00",
                "#475467")));
  }

  public GarageServiceResponse update(UUID garageId, UUID serviceId, GarageServiceRequest request) {
    GarageService service = findEntity(garageId, serviceId);
    validateUniqueName(garageId, request.name(), service);
    apply(service, request);
    return toResponse(services.save(service));
  }

  public void deactivate(UUID garageId, UUID serviceId) {
    GarageService service = findEntity(garageId, serviceId);
    service.setActive(false);
    services.save(service);
  }

  private GarageService findEntity(UUID garageId, UUID id) {
    return services
        .findByIdAndGarageId(id, garageId)
        .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }

  private void validateUniqueName(UUID garageId, String name, GarageService current) {
    boolean unchanged = current != null && current.getName().equalsIgnoreCase(name.trim());
    if (!unchanged && services.existsByGarageIdAndNameIgnoreCase(garageId, name.trim())) {
      throw new BusinessException("A service with this name already exists");
    }
  }

  private void apply(GarageService service, GarageServiceRequest request) {
    service.setName(request.name().trim());
    service.setCategory(request.category());
    service.setDescription(request.description());
    service.setDurationMinutes(request.durationMinutes());
    service.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
    service.setVatRate(request.vatRate().setScale(2, RoundingMode.HALF_UP));
    service.setColor(
        request.color() == null || request.color().isBlank() ? "#167A5B" : request.color());
  }

  private GarageService defaultService(
      Garage garage,
      String name,
      ServiceCategory category,
      int durationMinutes,
      String price,
      String color) {
    GarageService service = new GarageService();
    service.setGarage(garage);
    service.setName(name);
    service.setCategory(category);
    service.setDescription("Prestation proposée par défaut, entièrement modifiable.");
    service.setDurationMinutes(durationMinutes);
    service.setPrice(new BigDecimal(price));
    service.setVatRate(new BigDecimal("21.00"));
    service.setColor(color);
    return service;
  }

  private GarageServiceResponse toResponse(GarageService service) {
    return new GarageServiceResponse(
        service.getId(),
        service.getName(),
        service.getCategory(),
        service.getDescription(),
        service.getDurationMinutes(),
        service.getPrice(),
        service.getVatRate(),
        service.getColor(),
        service.isActive());
  }
}
