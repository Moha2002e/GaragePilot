package be.garagepilot.controller;

import be.garagepilot.dto.workorder.WorkOrderRequest;
import be.garagepilot.dto.workorder.WorkOrderResponse;
import be.garagepilot.dto.workorder.WorkOrderStatusRequest;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.WorkOrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/work-orders")
public class WorkOrderController {

  private final WorkOrderService service;

  public WorkOrderController(WorkOrderService service) {
    this.service = service;
  }

  @GetMapping
  public Page<WorkOrderResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) be.garagepilot.model.WorkOrderStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return service.findAll(user.garageId(), search, status, pageable);
  }

  @GetMapping("/{id}")
  public WorkOrderResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
    return service.findById(user.garageId(), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WorkOrderResponse create(
      @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody WorkOrderRequest request) {
    return service.create(user.garageId(), request);
  }

  @PutMapping("/{id}")
  public WorkOrderResponse update(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody WorkOrderRequest request) {
    return service.update(user.garageId(), id, request);
  }

  @PatchMapping("/{id}/status")
  public WorkOrderResponse updateStatus(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable UUID id,
      @Valid @RequestBody WorkOrderStatusRequest request) {
    return service.updateStatus(user.garageId(), id, request.status());
  }
}
