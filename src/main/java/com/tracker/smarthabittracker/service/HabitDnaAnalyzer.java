package com.tracker.smarthabittracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.smarthabittracker.dto.HabitDnaProfileDto;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.HabitDnaProfile;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitDnaProfileRepository;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HabitDnaAnalyzer {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitDnaProfileRepository habitDnaProfileRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public HabitDnaProfileDto getOrCalculateDna(User user, boolean forceRecalculate) {
        if (!forceRecalculate) {
            Optional<HabitDnaProfile> existing = habitDnaProfileRepository.findByUserId(user.getId());
            if (existing.isPresent()) {
                return mapToDto(existing.get());
            }
        }

        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByHabitUserId(user.getId());

        String archetype = determineLocalArchetype(habits, logs);
        HabitDnaProfileDto details = generateDnaWithGemini(archetype, habits, logs);

        habitDnaProfileRepository.deleteByUserId(user.getId());
        HabitDnaProfile profile = HabitDnaProfile.builder()
                .user(user)
                .personalityType(details.getPersonalityType())
                .strengths(String.join("||", details.getStrengths()))
                .weaknesses(String.join("||", details.getWeaknesses()))
                .aiCoachingAdvice(details.getAiCoachingAdvice())
                .build();

        HabitDnaProfile saved = habitDnaProfileRepository.save(profile);
        return mapToDto(saved);
    }

    private String determineLocalArchetype(List<Habit> habits, List<HabitLog> logs) {
        if (habits.isEmpty() || logs.isEmpty()) {
            return "Balanced Tracker";
        }

        // Night Builder: > 60% of completions between 6 PM and 5 AM
        long nightLogs = logs.stream()
                .filter(l -> {
                    int hr = l.getCompletedTime().getHour();
                    return hr >= 18 || hr < 5;
                }).count();
        if ((double) nightLogs / logs.size() > 0.6) {
            return "Night Builder";
        }

        // Weekend Warrior: > 60% of completions on Saturday/Sunday
        long weekendLogs = logs.stream()
                .filter(l -> {
                    DayOfWeek dow = l.getCompletedDate().getDayOfWeek();
                    return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
                }).count();
        if ((double) weekendLogs / logs.size() > 0.6) {
            return "Weekend Warrior";
        }

        // Fast Starter: High completions Mon-Wed but drop > 50% on Thu-Sun
        long monWed = logs.stream().filter(l -> {
            DayOfWeek dow = l.getCompletedDate().getDayOfWeek();
            return dow == DayOfWeek.MONDAY || dow == DayOfWeek.TUESDAY || dow == DayOfWeek.WEDNESDAY;
        }).count();
        long thuSun = logs.stream().filter(l -> {
            DayOfWeek dow = l.getCompletedDate().getDayOfWeek();
            return dow == DayOfWeek.THURSDAY || dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        }).count();
        if (monWed > thuSun * 1.8 && monWed > 3) {
            return "Fast Starter";
        }

        // Deep Focus Learner: Predominant category is STUDY/PRODUCTIVITY
        Map<String, Long> categoryCount = habits.stream()
                .collect(Collectors.groupingBy(Habit::getCategory, Collectors.counting()));
        String topCategory = categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
        if (topCategory.equalsIgnoreCase("STUDY") || topCategory.equalsIgnoreCase("PRODUCTIVITY")) {
            return "Deep Focus Learner";
        }

        // Consistency Master: average completions per day > 85% of total active habits
        LocalDate today = LocalDate.now();
        LocalDate firstLogDate = logs.stream().map(HabitLog::getCompletedDate).min(LocalDate::compareTo).orElse(today);
        long days = firstLogDate.datesUntil(today.plusDays(1)).count();
        double avgCompletionsPerDay = (double) logs.size() / Math.max(1, days);
        if (avgCompletionsPerDay > habits.size() * 0.85) {
            return "Consistency Master";
        }

        // Recovery Struggler: If user skips, takes > 2 days on average to recover
        // Let's compute average gaps
        return "Balanced Tracker";
    }

    private HabitDnaProfileDto generateDnaWithGemini(String archetype, List<Habit> habits, List<HabitLog> logs) {
        String prompt = String.format(
                "You are an expert behavior coach and psychologist. Analyze this habit data and generate a 'Habit DNA Personality Profile'.\n" +
                "Archetype: %s\n" +
                "Total Habits Active: %d\n" +
                "Total Logs Checked-in: %d\n\n" +
                "Requirements:\n" +
                "1. Generate exactly 3 key strengths for this archetype.\n" +
                "2. Generate exactly 3 key weaknesses/vulnerabilities.\n" +
                "3. Provide exactly 1 paragraph (maximum 3 sentences) of coaching advice inspired by 'Atomic Habits' (friction, environment, identity).\n" +
                "4. You MUST format the output as a valid JSON object containing exactly the keys: 'archetype', 'strengths', 'weaknesses', and 'coachingAdvice'. Do NOT write markdown, backticks, or any pre/post text.\n" +
                "JSON format example:\n" +
                "{\n" +
                "  \"archetype\": \"Archetype Name\",\n" +
                "  \"strengths\": [\"Strength 1\", \"Strength 2\", \"Strength 3\"],\n" +
                "  \"weaknesses\": [\"Weakness 1\", \"Weakness 2\", \"Weakness 3\"],\n" +
                "  \"coachingAdvice\": \"coaching text\"\n" +
                "}",
                archetype, habits.size(), logs.size()
        );

        String json = geminiService.generateContent(prompt);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                String arch = (String) map.getOrDefault("archetype", archetype);
                List<String> strs = (List<String>) map.get("strengths");
                List<String> weaks = (List<String>) map.get("weaknesses");
                String advice = (String) map.get("coachingAdvice");

                if (strs != null && weaks != null && advice != null) {
                    return HabitDnaProfileDto.builder()
                            .personalityType(arch)
                            .strengths(strs)
                            .weaknesses(weaks)
                            .aiCoachingAdvice(advice)
                            .lastUpdated(LocalDateTime.now().toString())
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to parse Gemini response for DNA: {}", e.getMessage());
            }
        }

        // Local Heuristic Fallbacks
        List<String> fallBackStrengths = new ArrayList<>();
        List<String> fallBackWeaknesses = new ArrayList<>();
        String fallBackAdvice;

        switch (archetype) {
            case "Night Builder":
                fallBackStrengths = List.of("Excellent concentration in evening hours", "Strong sleep-adjacent triggers", "Consistent end-of-day reflection");
                fallBackWeaknesses = List.of("Prone to late-night fatigue and skips", "Slower morning starts", "Highly vulnerable to sleep schedule shifts");
                fallBackAdvice = "Design an easy shut-down routine. Shift heavy study habits slightly earlier, and set a tiny boundary habit (e.g. read 1 page) to complete before midnight.";
                break;
            case "Weekend Warrior":
                fallBackStrengths = List.of("High weekend dedication", "Substantial blocks of free-time utilization", "Strong performance under low stress");
                fallBackWeaknesses = List.of("Prone to mid-week drop-offs", "Lacks weekday consistency", "Difficulty creating small daily loops");
                fallBackAdvice = "Scale weekday goals down to '2-minute actions'. If you do not have time for a full workout, commit to doing just 5 push-ups on weekdays to keep the neural loop alive.";
                break;
            case "Fast Starter":
                fallBackStrengths = List.of("Strong start-of-week momentum", "Clear organization early on", "High initial motivation");
                fallBackWeaknesses = List.of("Prone to mid-week burnout", "Vulnerable to Thursday-Friday fatigue", "Struggles with sustainable pacing");
                fallBackAdvice = "Pace yourself early in the week. Avoid loading 100% of your energy into Monday; reduce initial scope slightly to build sustainable daily reservoirs.";
                break;
            case "Deep Focus Learner":
                fallBackStrengths = List.of("High learning discipline", "Consistent study hours", "Dedicated intellectual focus");
                fallBackWeaknesses = List.of("Neglects physical health/movement", "Risk of mental overload", "Overlooks social/wellness balance");
                fallBackAdvice = "Anchor physical habits directly to study blocks. Try 'Habit Stacking': After I finish my 30-minute reading block, I will do 10 squats immediately.";
                break;
            case "Consistency Master":
                fallBackStrengths = List.of("Phenomenal completion habits", "Exceptional streak persistence", "Highly structured daily schedules");
                fallBackWeaknesses = List.of("Vulnerable to over-rigidity", "High stress when streaks break", "Difficulty resting/recharging");
                fallBackAdvice = "Celebrate rest as part of growth. To build resilience, practice doing a micro-version of habits to keep streaks alive on vacation, but accept flexibility.";
                break;
            default:
                fallBackStrengths = List.of("Flexible routine adjustments", "Balanced category distribution", "Healthy integration of habits");
                fallBackWeaknesses = List.of("Struggles to lock in high streaks", "Vulnerable to slight drops in motivation", "Scattered timing triggers");
                fallBackAdvice = "Anchor your key habits to pre-existing cues. Focus on building one core streak to 15 days to experience the power of compounding momentum.";
                break;
        }

        return HabitDnaProfileDto.builder()
                .personalityType(archetype)
                .strengths(fallBackStrengths)
                .weaknesses(fallBackWeaknesses)
                .aiCoachingAdvice(fallBackAdvice)
                .lastUpdated(LocalDateTime.now().toString())
                .build();
    }

    private HabitDnaProfileDto mapToDto(HabitDnaProfile profile) {
        return HabitDnaProfileDto.builder()
                .personalityType(profile.getPersonalityType())
                .strengths(Arrays.asList(profile.getStrengths().split("\\|\\|")))
                .weaknesses(Arrays.asList(profile.getWeaknesses().split("\\|\\|")))
                .aiCoachingAdvice(profile.getAiCoachingAdvice())
                .lastUpdated(profile.getUpdatedAt().toString())
                .build();
    }
}
