package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.AnalyticsDto;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.AnalyticsService;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;

    public AnalyticsController(AnalyticsService analyticsService, UserService userService) {
        this.analyticsService = analyticsService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<AnalyticsDto> getAnalytics() {
        User user = userService.getCurrentAuthenticatedUser();
        AnalyticsDto analytics = analyticsService.getAnalyticsForUser(user);
        return ResponseEntity.ok(analytics);
    }
}
