package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.DashboardSummary;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.DashboardService;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        User user = userService.getCurrentAuthenticatedUser();
        DashboardSummary summary = dashboardService.getDashboardSummary(user);
        return ResponseEntity.ok(summary);
    }
}
