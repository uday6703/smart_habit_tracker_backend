package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.model.Suggestion;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.SuggestionEngineService;
import com.tracker.smarthabittracker.service.UserService;
import com.tracker.smarthabittracker.repository.SuggestionRepository;
import com.tracker.smarthabittracker.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionEngineService suggestionEngineService;
    private final UserService userService;
    private final SuggestionRepository suggestionRepository;

    public SuggestionController(SuggestionEngineService suggestionEngineService,
                                UserService userService,
                                SuggestionRepository suggestionRepository) {
        this.suggestionEngineService = suggestionEngineService;
        this.userService = userService;
        this.suggestionRepository = suggestionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Suggestion>> getActiveSuggestions() {
        User user = userService.getCurrentAuthenticatedUser();
        List<Suggestion> suggestions = suggestionEngineService.getActiveSuggestions(user);
        return ResponseEntity.ok(suggestions);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Suggestion suggestion = suggestionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion not found with id " + id));
        
        suggestion.setIsRead(true);
        suggestionRepository.save(suggestion);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Suggestion marked as read");
        return ResponseEntity.ok(response);
    }
}
