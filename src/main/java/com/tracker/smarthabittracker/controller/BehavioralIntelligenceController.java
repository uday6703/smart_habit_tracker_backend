package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.*;
import com.tracker.smarthabittracker.model.MoodLog;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.BehavioralIntelligenceService;
import com.tracker.smarthabittracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence")
public class BehavioralIntelligenceController {

    private final BehavioralIntelligenceService behavioralIntelligenceService;
    private final UserService userService;

    public BehavioralIntelligenceController(BehavioralIntelligenceService behavioralIntelligenceService,
                                            UserService userService) {
        this.behavioralIntelligenceService = behavioralIntelligenceService;
        this.userService = userService;
    }

    @GetMapping("/dna")
    public ResponseEntity<HabitDnaProfileDto> getHabitDna(@RequestParam(defaultValue = "false") boolean forceRecalculate) {
        User user = userService.getCurrentAuthenticatedUser();
        HabitDnaProfileDto dna = behavioralIntelligenceService.getHabitDna(user, forceRecalculate);
        return ResponseEntity.ok(dna);
    }

    @PostMapping("/dna/regenerate")
    public ResponseEntity<HabitDnaProfileDto> regenerateHabitDna() {
        User user = userService.getCurrentAuthenticatedUser();
        HabitDnaProfileDto dna = behavioralIntelligenceService.getHabitDna(user, true);
        return ResponseEntity.ok(dna);
    }

    @GetMapping("/projections")
    public ResponseEntity<List<FutureProjectionDto>> getFutureProjections(@RequestParam(defaultValue = "false") boolean forceRecalculate) {
        User user = userService.getCurrentAuthenticatedUser();
        List<FutureProjectionDto> projections = behavioralIntelligenceService.getFutureProjections(user, forceRecalculate);
        return ResponseEntity.ok(projections);
    }

    @GetMapping("/failures")
    public ResponseEntity<List<FailureReplayDto>> getFailureReplays(@RequestParam(defaultValue = "false") boolean forceRecalculate) {
        User user = userService.getCurrentAuthenticatedUser();
        List<FailureReplayDto> replays = behavioralIntelligenceService.getFailureReplays(user, forceRecalculate);
        return ResponseEntity.ok(replays);
    }

    @PostMapping("/mood")
    public ResponseEntity<MoodLog> logMood(@Valid @RequestBody MoodCheckInRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        MoodLog moodLog = behavioralIntelligenceService.logMood(user, request);
        return ResponseEntity.ok(moodLog);
    }

    @GetMapping("/mood")
    public ResponseEntity<MoodCorrelationResultDto> getMoodCorrelations() {
        User user = userService.getCurrentAuthenticatedUser();
        MoodCorrelationResultDto correlations = behavioralIntelligenceService.getMoodCorrelations(user);
        return ResponseEntity.ok(correlations);
    }

    @GetMapping("/balance")
    public ResponseEntity<LifeBalanceDto> getLifeBalance(@RequestParam(defaultValue = "false") boolean forceRecalculate) {
        User user = userService.getCurrentAuthenticatedUser();
        LifeBalanceDto balance = behavioralIntelligenceService.getLifeBalance(user, forceRecalculate);
        return ResponseEntity.ok(balance);
    }
}
