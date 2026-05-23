package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.*;
import com.tracker.smarthabittracker.model.MoodLog;
import com.tracker.smarthabittracker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BehavioralIntelligenceService {

    private final HabitDnaAnalyzer habitDnaAnalyzer;
    private final FutureSelfSimulator futureSelfSimulator;
    private final FailureReplayAnalyzer failureReplayAnalyzer;
    private final MoodIntelligenceService moodIntelligenceService;
    private final LifeBalanceService lifeBalanceService;

    public HabitDnaProfileDto getHabitDna(User user, boolean forceRecalculate) {
        return habitDnaAnalyzer.getOrCalculateDna(user, forceRecalculate);
    }

    public List<FutureProjectionDto> getFutureProjections(User user, boolean forceRecalculate) {
        return futureSelfSimulator.calculateProjections(user, forceRecalculate);
    }

    public List<FailureReplayDto> getFailureReplays(User user, boolean forceRecalculate) {
        return failureReplayAnalyzer.getOrAnalyzeFailures(user, forceRecalculate);
    }

    public MoodLog logMood(User user, MoodCheckInRequest request) {
        return moodIntelligenceService.logMood(user, request);
    }

    public MoodCorrelationResultDto getMoodCorrelations(User user) {
        return moodIntelligenceService.getMoodCorrelation(user);
    }

    public LifeBalanceDto getLifeBalance(User user, boolean forceRecalculate) {
        return lifeBalanceService.getLifeBalance(user, forceRecalculate);
    }
}
