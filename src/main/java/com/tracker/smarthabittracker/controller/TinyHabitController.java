package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.TinyHabitDto;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.TinyHabitService;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tiny-habits")
public class TinyHabitController {

    private final TinyHabitService tinyHabitService;
    private final UserService userService;

    public TinyHabitController(TinyHabitService tinyHabitService, UserService userService) {
        this.tinyHabitService = tinyHabitService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<TinyHabitDto>> getPendingRecommendations() {
        User user = userService.getCurrentAuthenticatedUser();
        List<TinyHabitDto> list = tinyHabitService.getPendingRecommendations(user);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptRecommendation(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        TinyHabitDto dto = tinyHabitService.acceptRecommendation(id, user);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<?> dismissRecommendation(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        tinyHabitService.dismissRecommendation(id, user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Recommendation dismissed");
        return ResponseEntity.ok(response);
    }
}
