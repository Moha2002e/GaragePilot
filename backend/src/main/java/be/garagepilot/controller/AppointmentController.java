package be.garagepilot.controller;

import be.garagepilot.dto.appointment.AppointmentRequest;
import be.garagepilot.dto.appointment.AppointmentResponse;
import be.garagepilot.dto.appointment.AppointmentStatusRequest;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.AppointmentService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/appointments")
public class AppointmentController {

  private final AppointmentService service;

  public AppointmentController(AppointmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<AppointmentResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam OffsetDateTime from,
      @RequestParam OffsetDateTime to) {
    return service.findRange(user.garageId(), from, to);
  }

  @GetMapping("/{id}")
  public AppointmentResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    return service.findById(user.garageId(), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AppointmentResponse create(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody AppointmentRequest request) {
    return service.create(user.garageId(), request);
  }

  @PutMapping("/{id}")
  public AppointmentResponse update(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody AppointmentRequest request) {
    return service.update(user.garageId(), id, request);
  }

  @PatchMapping("/{id}/status")
  public AppointmentResponse updateStatus(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody AppointmentStatusRequest request) {
    return service.updateStatus(user.garageId(), id, request.status());
  }
}
