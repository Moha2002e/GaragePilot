package be.garagepilot.controller;

import be.garagepilot.dto.vehicle.*;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.VehicleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
  private final VehicleService service;

  public VehicleController(VehicleService service) {
    this.service = service;
  }

  @GetMapping
  public Page<VehicleResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) UUID clientId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return service.findAll(user.garageId(), search, clientId, pageable);
  }

  @GetMapping("/{id}")
  public VehicleResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    return service.findById(user.garageId(), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VehicleResponse create(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody VehicleRequest request) {
    return service.create(user.garageId(), request);
  }

  @PutMapping("/{id}")
  public VehicleResponse update(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody VehicleRequest request) {
    return service.update(user.garageId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    service.deactivate(user.garageId(), id);
  }
}
