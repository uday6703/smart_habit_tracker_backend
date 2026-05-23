package com.tracker.smarthabittracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.smarthabittracker.dto.FutureProjectionDto;
import com.tracker.smarthabittracker.model.FutureProjection;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.FutureProjectionRepository;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FutureSelfSimulator {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final FutureProjectionRepository futureProjectionRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<FutureProjectionDto> calculateProjections(User user, boolean forceRecalculate) {
        if (!forceRecalculate) {
            List<FutureProjection> existing = futureProjectionRepository.findByUserId(user.getId());
            if (existing.size() >= 3) {
                return existing.stream()
                        .map(this::mapToDto)
                        .sorted(Comparator.comparingInt(FutureProjectionDto::getTimeHorizonDays))
                        .toList();
            }
        }

        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByHabitUserId(user.getId());

        double consistency = calculateBaseConsistency(habits, logs);
        int[] horizons = {30, 90, 180};

        List<FutureProjectionDto> dtos = new ArrayList<>();
        futureProjectionRepository.deleteByUserId(user.getId());

        for (int horizon : horizons) {
            double projectedScore = projectProductivity(consistency, horizon);
            FutureProjectionDto detail = generateSimulatedNarrative(horizon, consistency, projectedScore, habits);

            FutureProjection fp = FutureProjection.builder()
                    .user(user)
                    .timeHorizonDays(horizon)
                    .projectedProductivityScore(projectedScore)
                    .narrativeSummary(detail.getNarrativeSummary())
                    .growthExplanations(detail.getGrowthExplanations())
                    .build();

            futureProjectionRepository.save(fp);
            dtos.add(detail);
        }

        return dtos;
    }

    private double calculateBaseConsistency(List<Habit> habits, List<HabitLog> logs) {
        if (habits.isEmpty() || logs.isEmpty()) return 0.0;
        // Average completion rate of habits
        double totalRate = 0.0;
        for (Habit h : habits) {
            long compCount = logs.stream().filter(l -> l.getHabit().getId().equals(h.getId())).count();
            // simple rate over last 14 days
            double rate = (compCount / 14.0) * 100.0;
            totalRate += Math.min(100.0, rate);
        }
        return totalRate / habits.size();
    }

    private double projectProductivity(double currentConsistency, int horizon) {
        // Compound growth formula
        // If consistency is > 50%, productivity compounds positively
        // If consistency is < 50%, productivity decays
        double netGrowth = (currentConsistency - 55.0) / 100.0; // range from -0.55 to +0.45
        double growthFactor = Math.pow(1.0 + (netGrowth * 0.01), horizon); // compound rate
        double projected = currentConsistency * growthFactor;
        return Math.max(10.0, Math.min(100.0, Math.round(projected * 10.0) / 10.0));
    }

    private FutureProjectionDto generateSimulatedNarrative(int horizon, double currentConsistency, double projectedScore, List<Habit> habits) {
        StringBuilder habitListStr = new StringBuilder();
        for (Habit h : habits) {
            habitListStr.append("- ").append(h.getName()).append(" (").append(h.getCategory()).append(")\n");
        }

        String prompt = String.format(
                "You are an inspiring personal success coach specialized in James Clear's 'Atomic Habits'.\n" +
                "Project the user's progress and life changes over a time horizon of %d days.\n" +
                "Current consistency rate: %.1f%%\n" +
                "Projected productivity score in %d days: %.1f%%\n" +
                "User's habits:\n%s\n" +
                "Requirements:\n" +
                "1. Generate a 'narrativeSummary' describing how their daily identity will change (max 3 sentences).\n" +
                "2. Generate 'growthExplanations' describing what tangible skills or health boosts they will have built (max 3 sentences).\n" +
                "3. You MUST format the output as a valid JSON object containing exactly the keys: 'narrativeSummary' and 'growthExplanations'. Do NOT write markdown, backticks, or any pre/post text.\n" +
                "JSON format example:\n" +
                "{\n" +
                "  \"narrativeSummary\": \"narrative summary text\",\n" +
                "  \"growthExplanations\": \"growth explanations text\"\n" +
                "}",
                horizon, currentConsistency, horizon, projectedScore, habitListStr.toString()
        );

        String json = geminiService.generateContent(prompt);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                String narrative = map.get("narrativeSummary");
                String growth = map.get("growthExplanations");

                if (narrative != null && growth != null) {
                    return FutureProjectionDto.builder()
                            .timeHorizonDays(horizon)
                            .projectedProductivityScore(projectedScore)
                            .narrativeSummary(narrative)
                            .growthExplanations(growth)
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to parse Gemini response for Future Self simulation: {}", e.getMessage());
            }
        }

        // Local Heuristic Fallbacks
        String fallbackNarrative;
        String fallbackGrowth;

        if (currentConsistency >= 70.0) {
            fallbackNarrative = String.format("In %d days, you will have solidified your identity as a highly organized and disciplined individual. Completing these routines will feel as natural as brushing your teeth, requiring minimal friction.", horizon);
            fallbackGrowth = String.format("By maintaining %.1f%% consistency, your habits will compile into significant progress. You will experience increased clarity, better physical stamina, and stronger cognitive focus due to your compounding daily wins.", projectedScore);
        } else if (currentConsistency >= 45.0) {
            fallbackNarrative = String.format("In %d days, your habits will be moderately stable. You will have built a foundation of positive routines, though minor schedule changes still cause friction.", horizon);
            fallbackGrowth = String.format("Your projected productivity score of %.1f%% suggests steady improvement. To unlock exponential growth, design your environment to make skipping habits harder, aiming to never miss twice.", projectedScore);
        } else {
            fallbackNarrative = String.format("In %d days, your current routine structure will likely collapse unless you scale down. The friction in your daily environment is too high for long-term consistency.", horizon);
            fallbackGrowth = String.format("Your projected score of %.1f%% is decaying. Simplify your routines to '2-minute versions' (e.g. read 1 page instead of 15) to build initial momentum and trust in your identity.", projectedScore);
        }

        return FutureProjectionDto.builder()
                .timeHorizonDays(horizon)
                .projectedProductivityScore(projectedScore)
                .narrativeSummary(fallbackNarrative)
                .growthExplanations(fallbackGrowth)
                .build();
    }

    private FutureProjectionDto mapToDto(FutureProjection fp) {
        return FutureProjectionDto.builder()
                .timeHorizonDays(fp.getTimeHorizonDays())
                .projectedProductivityScore(fp.getProjectedProductivityScore())
                .narrativeSummary(fp.getNarrativeSummary())
                .growthExplanations(fp.getGrowthExplanations())
                .build();
    }
}
