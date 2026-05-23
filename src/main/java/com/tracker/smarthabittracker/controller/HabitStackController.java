package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.HabitStackDto;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.HabitStackService;
import com.tracker.smarthabittracker.service.UserService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habit-stacks")
public class HabitStackController {

    private final HabitStackService habitStackService;
    private final UserService userService;

    public HabitStackController(HabitStackService habitStackService, UserService userService) {
        this.habitStackService = habitStackService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<HabitStackDto>> getAllStacks() {
        User user = userService.getCurrentAuthenticatedUser();
        List<HabitStackDto> list = habitStackService.getAllStacks(user);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<?> createStack(@RequestBody CreateStackRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        try {
            HabitStackDto dto = habitStackService.createStack(request.getCueHabitId(), request.getTargetHabitId(), user);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStack(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        habitStackService.deleteStack(id, user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Habit stack deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Data
    public static class CreateStackRequest {
        private Long cueHabitId;
        private Long targetHabitId;
    }
}
