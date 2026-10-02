package be.garagepilot.controller;

import be.garagepilot.dto.dashboard.DashboardResponse;
import be.garagepilot.security.CurrentUser;
import be.garagepilot.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping
  public DashboardResponse get(@AuthenticationPrincipal CurrentUser user) {
    return service.get(user.garageId());
  }
}
