package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.HabitDto;
import com.tracker.smarthabittracker.dto.HabitLogRequest;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.HabitService;
import com.tracker.smarthabittracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final UserService userService;
    private final com.tracker.smarthabittracker.service.HabitStackService habitStackService;

    public HabitController(HabitService habitService, 
                           UserService userService,
                           com.tracker.smarthabittracker.service.HabitStackService habitStackService) {
        this.habitService = habitService;
        this.userService = userService;
        this.habitStackService = habitStackService;
    }

    @GetMapping
    public ResponseEntity<List<HabitDto>> getHabits() {
        User user = userService.getCurrentAuthenticatedUser();
        List<HabitDto> habits = habitService.getHabitDtosForUser(user);
        return ResponseEntity.ok(habits);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitDto> getHabitById(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Habit habit = habitService.getHabitById(id, user);
        return ResponseEntity.ok(habitService.mapToDto(habit));
    }

    @PostMapping
    public ResponseEntity<HabitDto> createHabit(@Valid @RequestBody Habit habit) {
        User user = userService.getCurrentAuthenticatedUser();
        Habit newHabit = habitService.createHabit(habit, user);
        return new ResponseEntity<>(habitService.mapToDto(newHabit), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitDto> updateHabit(@PathVariable Long id, @Valid @RequestBody Habit habit) {
        User user = userService.getCurrentAuthenticatedUser();
        Habit updatedHabit = habitService.updateHabit(id, habit, user);
        return ResponseEntity.ok(habitService.mapToDto(updatedHabit));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHabit(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        habitService.deleteHabit(id, user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Habit deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/log")
    public ResponseEntity<?> logCompletion(
            @PathVariable Long id,
            @RequestBody HabitLogRequest logRequest) {
        User user = userService.getCurrentAuthenticatedUser();
        HabitLog log = habitService.logCompletion(id, logRequest, user);
        
        // Fetch updated habit data for streak info
        Habit habit = habitService.getHabitById(id, user);
        com.tracker.smarthabittracker.dto.HabitDto updatedDto = habitService.mapToDto(habit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("log", log);
        response.put("habitName", updatedDto.getName());
        response.put("currentStreak", updatedDto.getCurrentStreak());
        response.put("longestStreak", updatedDto.getLongestStreak());
        
        habitStackService.getNextSuggestedHabit(id, user.getId())
                .ifPresent(next -> response.put("nextSuggestedHabit", next));
                
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/log")
    public ResponseEntity<?> deleteCompletion(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = userService.getCurrentAuthenticatedUser();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        habitService.deleteCompletion(id, targetDate, user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Completion log deleted successfully");
        return ResponseEntity.ok(response);
    }
}
