package be.garagepilot.controller;

import be.garagepilot.dto.client.*;
import be.garagepilot.model.UserRole;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.ClientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
  private final ClientService service;

  public ClientController(ClientService service) {
    this.service = service;
  }

  @GetMapping
  public Page<ClientResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "") String search,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    if (user.role() == UserRole.SUPER_ADMIN) {
      return service.findAll(null, search, pageable);
    }
    return service.findAll(user.garageId(), search, pageable);
  }

  @GetMapping("/{id}")
  public ClientResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    return service.findById(user.garageId(), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClientResponse create(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ClientRequest request) {
    return service.create(user.garageId(), request);
  }

  @PutMapping("/{id}")
  public ClientResponse update(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody ClientRequest request) {
    return service.update(user.garageId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    service.deactivate(user.garageId(), id);
  }
}
