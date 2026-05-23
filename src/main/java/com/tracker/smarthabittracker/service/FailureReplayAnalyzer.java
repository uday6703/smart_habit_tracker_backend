package com.tracker.smarthabittracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.smarthabittracker.dto.FailureReplayDto;
import com.tracker.smarthabittracker.model.*;
import com.tracker.smarthabittracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailureReplayAnalyzer {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final MoodLogRepository moodLogRepository;
    private final FailureAnalysisRepository failureAnalysisRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<FailureReplayDto> getOrAnalyzeFailures(User user, boolean forceRecalculate) {
        if (!forceRecalculate) {
            List<FailureAnalysis> existing = failureAnalysisRepository.findByUserId(user.getId());
            if (!existing.isEmpty()) {
                return existing.stream().map(this::mapToDto).toList();
            }
        }

        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByHabitUserId(user.getId());
        List<MoodLog> moodLogs = moodLogRepository.findByUserIdOrderByLoggedDateAsc(user.getId());

        List<FailureReplayDto> dtos = new ArrayList<>();
        failureAnalysisRepository.deleteByUserId(user.getId());

        for (Habit habit : habits) {
            double rate = calculateHabitCompletionRate(habit, logs);
            if (rate >= 80.0) {
                // Highly consistent, no failure analysis needed
                continue;
            }

            String trigger = determinePrimaryTrigger(habit, logs, moodLogs);
            FailureReplayDto details = generateFailureReplayWithGemini(habit, trigger, logs, moodLogs);

            FailureAnalysis fa = FailureAnalysis.builder()
                    .user(user)
                    .habit(habit)
                    .primaryTrigger(trigger)
                    .aiExplanation(details.getAiExplanation())
                    .recoveryAdvice(details.getRecoveryAdvice())
                    .build();

            failureAnalysisRepository.save(fa);
            dtos.add(details);
        }

        return dtos;
    }

    private double calculateHabitCompletionRate(Habit habit, List<HabitLog> logs) {
        long count = logs.stream()
                .filter(l -> l.getHabit().getId().equals(habit.getId()))
                .count();
        // Assume active window is 14 days
        return (count / 14.0) * 100.0;
    }

    private String determinePrimaryTrigger(Habit habit, List<HabitLog> logs, List<MoodLog> moodLogs) {
        Set<LocalDate> compDates = logs.stream()
                .filter(l -> l.getHabit().getId().equals(habit.getId()))
                .map(HabitLog::getCompletedDate)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(13);

        int weekendSkips = 0;
        int weekdaySkips = 0;
        int stressedSkips = 0;
        int totalSkips = 0;

        LocalDate curr = start;
        while (!curr.isAfter(today)) {
            if (!compDates.contains(curr)) {
                totalSkips++;
                DayOfWeek dow = curr.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    weekendSkips++;
                } else {
                    weekdaySkips++;
                }

                // Check stress level on that day
                LocalDate logDate = curr;
                Optional<MoodLog> moodOpt = moodLogs.stream()
                        .filter(m -> m.getLoggedDate().equals(logDate))
                        .findFirst();
                if (moodOpt.isPresent() && moodOpt.get().getStressLevel() >= 4) {
                    stressedSkips++;
                }
            }
            curr = curr.plusDays(1);
        }

        if (totalSkips == 0) return "NONE";

        if (stressedSkips > 0.4 * totalSkips) {
            return "STRESS_INDUCED_BURNOUT";
        }
        if (weekendSkips > 0.6 * totalSkips) {
            return "WEEKEND_SLUMP";
        }
        if (totalSkips > 8) {
            return "CONSISTENCY_DECAY";
        }

        return "SCHEDULE_FRICTION";
    }

    private FailureReplayDto generateFailureReplayWithGemini(Habit habit, String trigger, List<HabitLog> logs, List<MoodLog> moodLogs) {
        String prompt = String.format(
                "You are an expert behavior scientist and personal coach inspired by James Clear's 'Atomic Habits'.\n" +
                "Analyze why the habit '%s' (Category: %s) is experiencing skips.\n" +
                "Calculated Primary Failure Trigger: %s\n\n" +
                "Requirements:\n" +
                "1. Provide a narrative 'explanation' explaining what behavioral trigger or obstacle is blocking consistency (max 3 sentences).\n" +
                "2. Provide 'recoveryAdvice' containing exactly 2 actionable tips inspired by Atomic Habits (friction reduction, stacking, reward cues).\n" +
                "3. You MUST format the output as a valid JSON object containing exactly the keys: 'explanation' and 'recoveryAdvice'. Do NOT write markdown, backticks, or any pre/post text.\n" +
                "JSON format example:\n" +
                "{\n" +
                "  \"explanation\": \"explanation text\",\n" +
                "  \"recoveryAdvice\": \"recovery advice text\"\n" +
                "}",
                habit.getName(), habit.getCategory(), trigger
        );

        String json = geminiService.generateContent(prompt);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                String explanation = map.get("explanation");
                String recovery = map.get("recoveryAdvice");

                if (explanation != null && recovery != null) {
                    return FailureReplayDto.builder()
                            .habitId(habit.getId())
                            .habitName(habit.getName())
                            .category(habit.getCategory())
                            .primaryTrigger(trigger)
                            .aiExplanation(explanation)
                            .recoveryAdvice(recovery)
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to parse Gemini response for Failure Replay: {}", e.getMessage());
            }
        }

        // Local Heuristic Fallbacks
        String explanation;
        String advice;

        switch (trigger) {
            case "STRESS_INDUCED_BURNOUT":
                explanation = String.format("The habit '%s' breaks on days with high stress levels. When cognitive load is high, your brain defaults to comfort and avoids friction.", habit.getName());
                advice = "Use the 2-Minute Rule to shrink your habit on stressed days (e.g. read 1 paragraph instead of 15 pages). Make it 'too small to fail' to maintain the neural path.";
                break;
            case "WEEKEND_SLUMP":
                explanation = String.format("Consistency for '%s' drops significantly on weekends. This is due to structural schedule shifts and the absence of structured weekday cues.", habit.getName());
                advice = "Create a dedicated weekend trigger. Link the habit to a weekend cue (e.g. 'After I drink my Saturday morning coffee, I will perform my habit').";
                break;
            case "CONSISTENCY_DECAY":
                explanation = String.format("A general consistency decay is affecting '%s'. The cue-action-reward loops are fading, causing the habit to drop off your priority radar.", habit.getName());
                advice = "Re-establish visual triggers in your environment. Put physical reminders of the habit in high-visibility locations (e.g., placing your book on your pillow).";
                break;
            default:
                explanation = String.format("Friction or schedule misalignment is causing skips in '%s'. You may have loaded too many demands into the same time slot.", habit.getName());
                advice = String.format("Stack the habit directly behind an established routine. Example: 'After I [established routine], I will [do %s]'.", habit.getName());
                break;
        }

        return FailureReplayDto.builder()
                .habitId(habit.getId())
                .habitName(habit.getName())
                .category(habit.getCategory())
                .primaryTrigger(trigger)
                .aiExplanation(explanation)
                .recoveryAdvice(advice)
                .build();
    }

    private FailureReplayDto mapToDto(FailureAnalysis fa) {
        return FailureReplayDto.builder()
                .habitId(fa.getHabit().getId())
                .habitName(fa.getHabit().getName())
                .category(fa.getHabit().getCategory())
                .primaryTrigger(fa.getPrimaryTrigger())
                .aiExplanation(fa.getAiExplanation())
                .recoveryAdvice(fa.getRecoveryAdvice())
                .build();
    }
}
