package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.MoodCheckInRequest;
import com.tracker.smarthabittracker.dto.MoodCorrelationResultDto;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.MoodLog;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.MoodLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MoodIntelligenceService {

    private final MoodLogRepository moodLogRepository;
    private final HabitLogRepository habitLogRepository;
    private final GeminiService geminiService;

    @Transactional
    public MoodLog logMood(User user, MoodCheckInRequest request) {
        LocalDate today = LocalDate.now();
        Optional<MoodLog> existingOpt = moodLogRepository.findByUserIdAndLoggedDate(user.getId(), today);

        MoodLog moodLog;
        if (existingOpt.isPresent()) {
            moodLog = existingOpt.get();
            moodLog.setMoodScore(request.getMoodScore());
            moodLog.setStressLevel(request.getStressLevel());
            moodLog.setEnergyLevel(request.getEnergyLevel());
            moodLog.setMotivationLevel(request.getMotivationLevel());
            moodLog.setNotes(request.getNotes());
        } else {
            moodLog = MoodLog.builder()
                    .user(user)
                    .loggedDate(today)
                    .moodScore(request.getMoodScore())
                    .stressLevel(request.getStressLevel())
                    .energyLevel(request.getEnergyLevel())
                    .motivationLevel(request.getMotivationLevel())
                    .notes(request.getNotes())
                    .build();
        }

        return moodLogRepository.save(moodLog);
    }

    public MoodCorrelationResultDto getMoodCorrelation(User user) {
        List<MoodLog> moodLogs = moodLogRepository.findByUserIdOrderByLoggedDateAsc(user.getId());
        List<HabitLog> habitLogs = habitLogRepository.findByHabitUserId(user.getId());

        // Group completions by date
        Map<LocalDate, Long> completionsByDate = habitLogs.stream()
                .collect(Collectors.groupingBy(HabitLog::getCompletedDate, Collectors.counting()));

        // Lists to divide logs
        List<MoodLog> completionDays = new ArrayList<>();
        List<MoodLog> skipDays = new ArrayList<>();

        for (MoodLog logItem : moodLogs) {
            Long count = completionsByDate.getOrDefault(logItem.getLoggedDate(), 0L);
            if (count > 0) {
                completionDays.add(logItem);
            } else {
                skipDays.add(logItem);
            }
        }

        double avgMoodComp = completionDays.stream().mapToInt(MoodLog::getMoodScore).average().orElse(0.0);
        double avgMoodSkip = skipDays.stream().mapToInt(MoodLog::getMoodScore).average().orElse(0.0);
        double avgStressComp = completionDays.stream().mapToInt(MoodLog::getStressLevel).average().orElse(0.0);
        double avgStressSkip = skipDays.stream().mapToInt(MoodLog::getStressLevel).average().orElse(0.0);

        avgMoodComp = Math.round(avgMoodComp * 10.0) / 10.0;
        avgMoodSkip = Math.round(avgMoodSkip * 10.0) / 10.0;
        avgStressComp = Math.round(avgStressComp * 10.0) / 10.0;
        avgStressSkip = Math.round(avgStressSkip * 10.0) / 10.0;

        List<MoodCorrelationResultDto.MoodLogDto> dtos = moodLogs.stream()
                .sorted((a, b) -> b.getLoggedDate().compareTo(a.getLoggedDate()))
                .limit(10)
                .map(m -> MoodCorrelationResultDto.MoodLogDto.builder()
                        .date(m.getLoggedDate().toString())
                        .moodScore(m.getMoodScore())
                        .stressLevel(m.getStressLevel())
                        .energyLevel(m.getEnergyLevel())
                        .motivationLevel(m.getMotivationLevel())
                        .notes(m.getNotes())
                        .build())
                .collect(Collectors.toList());

        String aiInsight = generateMoodInsights(avgMoodComp, avgMoodSkip, avgStressComp, avgStressSkip);

        return MoodCorrelationResultDto.builder()
                .averageMoodWithCompletions(avgMoodComp)
                .averageMoodWithSkips(avgMoodSkip)
                .averageStressWithCompletions(avgStressComp)
                .averageStressWithSkips(avgStressSkip)
                .recentLogs(dtos)
                .aiInsightSummary(aiInsight)
                .build();
    }

    private String generateMoodInsights(double moodComp, double moodSkip, double stressComp, double stressSkip) {
        String prompt = String.format(
                "You are an expert behavior coach specialized in James Clear's 'Atomic Habits'.\n" +
                "Evaluate the correlation between a user's logged emotional stats and habit completions:\n" +
                "- Average Mood on completion days: %.1f/5 vs non-completion days: %.1f/5\n" +
                "- Average Stress on completion days: %.1f/5 vs non-completion days: %.1f/5\n\n" +
                "Requirements:\n" +
                "1. Provide a single paragraph (maximum 3 concise sentences) summarizing how emotional wellness is linked to daily routines.\n" +
                "2. Apply Atomic Habits principles (e.g., priming environment, reducing friction during stress, tracking triggers).\n" +
                "3. Make it highly motivational, direct, and action-oriented.",
                moodComp, moodSkip, stressComp, stressSkip
        );

        String response = geminiService.generateContent(prompt);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // Local fallback heuristics
        if (moodComp > moodSkip + 0.5) {
            return "Your completions are closely linked to a positive mental state. On days you skip your routines, your average mood drops. Focus on making your habits tiny and incredibly easy to perform on low-energy days to protect your consistency.";
        } else if (stressSkip > stressComp + 0.5) {
            return "Higher stress level correlates with days your habits are skipped. Reduce environmental friction and establish stress-recovery habits like taking 2 deep breaths before starting tasks.";
        } else {
            return "Keep logging your moods and routines daily. By checking in consistently, you create behavior mapping lines that make it easier to design stress-resilient triggers.";
        }
    }
}
