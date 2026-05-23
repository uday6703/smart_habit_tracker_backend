package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.LifeBalanceDto;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.LifeBalanceScore;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import com.tracker.smarthabittracker.repository.LifeBalanceScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LifeBalanceService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final LifeBalanceScoreRepository lifeBalanceScoreRepository;
    private final GeminiService geminiService;

    @Transactional
    public LifeBalanceDto getLifeBalance(User user, boolean forceRecalculate) {
        LocalDate today = LocalDate.now();
        if (!forceRecalculate) {
            Optional<LifeBalanceScore> existing = lifeBalanceScoreRepository.findFirstByUserIdOrderByRecordedDateDesc(user.getId());
            if (existing.isPresent() && existing.get().getRecordedDate().equals(today)) {
                return mapToDto(existing.get(), null);
            }
        }

        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByHabitUserId(user.getId());

        // Initialize category scores
        double health = calculateCategoryScore(habits, logs, "HEALTH", 50.0);
        double fitness = calculateCategoryScore(habits, logs, "FITNESS", 50.0);
        double learning = calculateCategoryScore(habits, logs, "STUDY", 50.0);
        double productivity = calculateCategoryScore(habits, logs, "PRODUCTIVITY", 50.0);

        // Sleep score: check habits with "sleep" or "bed" in name, else base on health consistency
        double sleep = calculateKeywordScore(habits, logs, List.of("sleep", "bed", "rest"), health);

        // Mental Wellness: MOOD, SOCIAL categories
        double mentalWellness = calculateCategoryScore(habits, logs, "MOOD", 55.0);
        long socialLogs = logs.stream().filter(l -> l.getHabit().getCategory().equalsIgnoreCase("SOCIAL")).count();
        if (socialLogs > 0) {
            mentalWellness = (mentalWellness + Math.min(100.0, (socialLogs / 14.0) * 100.0)) / 2.0;
        }

        // Discipline: overall average completion rate of all habits
        double discipline = habits.isEmpty() ? 50.0 : habits.stream()
                .mapToDouble(h -> {
                    long comps = logs.stream().filter(l -> l.getHabit().getId().equals(h.getId())).count();
                    return Math.min(100.0, (comps / 14.0) * 100.0);
                }).average().orElse(50.0);

        // Focus: derived from the average active streak length relative to a target of 10 days
        double focus = habits.isEmpty() ? 50.0 : habits.stream()
                .mapToDouble(h -> {
                    // count max consecutive active days (mock/approx via current streak)
                    long currentStreak = logs.stream()
                            .filter(l -> l.getHabit().getId().equals(h.getId()))
                            .count(); // fallback approx
                    return Math.min(100.0, (currentStreak / 10.0) * 100.0);
                }).average().orElse(50.0);

        // Round values
        health = Math.round(health * 10.0) / 10.0;
        fitness = Math.round(fitness * 10.0) / 10.0;
        learning = Math.round(learning * 10.0) / 10.0;
        productivity = Math.round(productivity * 10.0) / 10.0;
        sleep = Math.round(sleep * 10.0) / 10.0;
        mentalWellness = Math.round(mentalWellness * 10.0) / 10.0;
        discipline = Math.round(discipline * 10.0) / 10.0;
        focus = Math.round(focus * 10.0) / 10.0;

        lifeBalanceScoreRepository.deleteByUserId(user.getId());
        LifeBalanceScore score = LifeBalanceScore.builder()
                .user(user)
                .healthScore(health)
                .fitnessScore(fitness)
                .learningScore(learning)
                .productivityScore(productivity)
                .sleepScore(sleep)
                .mentalWellnessScore(mentalWellness)
                .disciplineScore(discipline)
                .focusScore(focus)
                .recordedDate(today)
                .build();

        LifeBalanceScore saved = lifeBalanceScoreRepository.save(score);

        // Fetch AI analysis
        Map<String, Double> mapScores = new LinkedHashMap<>();
        mapScores.put("Health", health);
        mapScores.put("Fitness", fitness);
        mapScores.put("Learning", learning);
        mapScores.put("Productivity", productivity);
        mapScores.put("Sleep", sleep);
        mapScores.put("Mental Wellness", mentalWellness);
        mapScores.put("Discipline", discipline);
        mapScores.put("Focus", focus);

        String analysis = generateBalanceAnalysis(mapScores);
        return mapToDto(saved, analysis);
    }

    private double calculateCategoryScore(List<Habit> habits, List<HabitLog> logs, String category, double baseline) {
        List<Habit> catHabits = habits.stream()
                .filter(h -> h.getCategory().equalsIgnoreCase(category))
                .toList();

        if (catHabits.isEmpty()) return baseline;

        double sum = 0.0;
        for (Habit h : catHabits) {
            long comp = logs.stream().filter(l -> l.getHabit().getId().equals(h.getId())).count();
            sum += (comp / 14.0) * 100.0;
        }

        return Math.min(100.0, sum / catHabits.size());
    }

    private double calculateKeywordScore(List<Habit> habits, List<HabitLog> logs, List<String> keywords, double fallbackValue) {
        List<Habit> keyHabits = habits.stream()
                .filter(h -> keywords.stream().anyMatch(kw -> h.getName().toLowerCase().contains(kw) || h.getDescription().toLowerCase().contains(kw)))
                .toList();

        if (keyHabits.isEmpty()) return fallbackValue;

        double sum = 0.0;
        for (Habit h : keyHabits) {
            long comp = logs.stream().filter(l -> l.getHabit().getId().equals(h.getId())).count();
            sum += (comp / 14.0) * 100.0;
        }
        return Math.min(100.0, sum / keyHabits.size());
    }

    private String generateBalanceAnalysis(Map<String, Double> scores) {
        // Find weakest and strongest
        String weakest = scores.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        String strongest = scores.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");

        String prompt = String.format(
                "You are an expert lifestyle integration coach specialized in James Clear's 'Atomic Habits'.\n" +
                "Evaluate this user's balance scores across 8 life areas:\n" +
                "- Health: %.1f\n" +
                "- Fitness: %.1f\n" +
                "- Learning: %.1f\n" +
                "- Productivity: %.1f\n" +
                "- Sleep: %.1f\n" +
                "- Mental Wellness: %.1f\n" +
                "- Discipline: %.1f\n" +
                "- Focus: %.1f\n\n" +
                "Requirements:\n" +
                "1. Provide a single paragraph (maximum 3 concise sentences) analyzing the balance.\n" +
                "2. Directly address the strongest area (%s) and suggest how to leverage it to lift the weakest area (%s) using Habit Stacking (After [Strong habit], I will [Weak area habit]).\n" +
                "3. Make it encouraging, clear, and action-focused.",
                scores.get("Health"), scores.get("Fitness"), scores.get("Learning"), scores.get("Productivity"),
                scores.get("Sleep"), scores.get("Mental Wellness"), scores.get("Discipline"), scores.get("Focus"),
                strongest, weakest
        );

        String response = geminiService.generateContent(prompt);
        if (response != null && !response.trim().isEmpty()) {
            return response;
        }

        // Local Heuristic Fallbacks
        return String.format(
                "Your life balance is led strongly by your performance in %s (Score: %.1f), while %s (Score: %.1f) remains your most neglected area. " +
                "To lift your %s, apply Habit Stacking: stack a small 2-minute starter habit for %s immediately after your dominant %s habits.",
                strongest, scores.get(strongest), weakest, scores.get(weakest), weakest, weakest, strongest
        );
    }

    private LifeBalanceDto mapToDto(LifeBalanceScore score, String explicitAnalysis) {
        Map<String, Double> catMap = new LinkedHashMap<>();
        catMap.put("Health", score.getHealthScore());
        catMap.put("Fitness", score.getFitnessScore());
        catMap.put("Learning", score.getLearningScore());
        catMap.put("Productivity", score.getProductivityScore());
        catMap.put("Sleep", score.getSleepScore());
        catMap.put("Mental Wellness", score.getMentalWellnessScore());
        catMap.put("Discipline", score.getDisciplineScore());
        catMap.put("Focus", score.getFocusScore());

        String weakest = catMap.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        String strongest = catMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");

        String analysis = explicitAnalysis;
        if (analysis == null) {
            analysis = String.format(
                    "Your performance is led by %s, while %s is currently your weakest area. Leverage your consistency in %s to anchor new behaviors for %s.",
                    strongest, weakest, strongest, weakest
            );
        }

        return LifeBalanceDto.builder()
                .categoryScores(catMap)
                .strongestCategory(strongest)
                .weakestCategory(weakest)
                .aiBalanceAnalysis(analysis)
                .build();
    }
}
