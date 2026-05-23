package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.HeatmapDto;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.HeatmapService;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/heatmap")
public class HeatmapController {

    private final HeatmapService heatmapService;
    private final UserService userService;

    public HeatmapController(HeatmapService heatmapService, UserService userService) {
        this.heatmapService = heatmapService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<HeatmapDto>> getHeatmapData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        User user = userService.getCurrentAuthenticatedUser();
        
        // Defaults: last 60 days if not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(59);
        }

        List<HeatmapDto> data = heatmapService.getHeatmapData(startDate, endDate, user);
        return ResponseEntity.ok(data);
    }
}
