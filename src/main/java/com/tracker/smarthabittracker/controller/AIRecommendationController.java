package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.model.Suggestion;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.AIRecommendationService;
import com.tracker.smarthabittracker.service.SuggestionEngineService;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
public class AIRecommendationController {

    private final AIRecommendationService aiRecommendationService;
    private final SuggestionEngineService suggestionEngineService;
    private final UserService userService;

    public AIRecommendationController(AIRecommendationService aiRecommendationService,
                                      SuggestionEngineService suggestionEngineService,
                                      UserService userService) {
        this.aiRecommendationService = aiRecommendationService;
        this.suggestionEngineService = suggestionEngineService;
        this.userService = userService;
    }

    @PostMapping("/regenerate")
    public ResponseEntity<?> regenerateAISuggestions() {
        User user = userService.getCurrentAuthenticatedUser();
        aiRecommendationService.generateAISuggestions(user);
        
        List<Suggestion> updated = suggestionEngineService.getActiveSuggestions(user);
        return ResponseEntity.ok(updated);
    }
}
