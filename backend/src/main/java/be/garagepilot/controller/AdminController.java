package be.garagepilot.controller;

import be.garagepilot.dto.admin.ActivationRequest;
import be.garagepilot.dto.admin.AdminGarageDetailResponse;
import be.garagepilot.dto.admin.AdminOverviewResponse;
import be.garagepilot.dto.admin.AdminUserRequest;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.AdminService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

  private final AdminService service;

  public AdminController(AdminService service) {
    this.service = service;
  }

  @GetMapping("/overview")
  public AdminOverviewResponse overview(
      @RequestParam(defaultValue = "") String search,
      @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return service.overview(search, pageable);
  }

  @GetMapping("/garages/{garageId}")
  public AdminGarageDetailResponse garage(@PathVariable UUID garageId) {
    return service.garageDetail(garageId);
  }

  @PatchMapping("/garages/{garageId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setGarageStatus(
      @PathVariable UUID garageId, @Valid @RequestBody ActivationRequest request) {
    service.setGarageActive(garageId, request.active());
  }

  @PostMapping("/garages/{garageId}/users")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminGarageDetailResponse.AdminUser createUser(
      @PathVariable UUID garageId, @Valid @RequestBody AdminUserRequest request) {
    return service.createUser(garageId, request);
  }

  @PutMapping("/users/{userId}")
  public AdminGarageDetailResponse.AdminUser updateUser(
      @PathVariable UUID userId, @Valid @RequestBody AdminUserRequest request) {
    return service.updateUser(userId, request);
  }

  @PatchMapping("/users/{userId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setUserStatus(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID userId,
      @Valid @RequestBody ActivationRequest request) {
    service.setUserActive(currentUser.id(), userId, request.active());
  }
}
