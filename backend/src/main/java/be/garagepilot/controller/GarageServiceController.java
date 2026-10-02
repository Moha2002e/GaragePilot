package be.garagepilot.controller;

import be.garagepilot.dto.servicecatalog.GarageServiceRequest;
import be.garagepilot.dto.servicecatalog.GarageServiceResponse;
import be.garagepilot.model.ServiceCategory;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.GarageServiceCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class GarageServiceController {

  private final GarageServiceCatalogService service;

  public GarageServiceController(GarageServiceCatalogService service) {
    this.service = service;
  }

  @GetMapping
  public List<GarageServiceResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) ServiceCategory category) {
    return service.findAll(user.garageId(), search, category);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public GarageServiceResponse create(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody GarageServiceRequest request) {
    return service.create(user.garageId(), request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public GarageServiceResponse update(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody GarageServiceRequest request) {
    return service.update(user.garageId(), id, request);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    service.deactivate(user.garageId(), id);
  }
}
