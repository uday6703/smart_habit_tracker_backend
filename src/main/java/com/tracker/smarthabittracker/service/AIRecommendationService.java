package com.tracker.smarthabittracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.Suggestion;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import com.tracker.smarthabittracker.repository.SuggestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIRecommendationService {

    private final GeminiService geminiService;
    private final SuggestionEngineService localSuggestionEngine;
    private final SuggestionRepository suggestionRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final ObjectMapper objectMapper;

    public AIRecommendationService(GeminiService geminiService,
                                   SuggestionEngineService localSuggestionEngine,
                                   SuggestionRepository suggestionRepository,
                                   HabitRepository habitRepository,
                                   HabitLogRepository habitLogRepository) {
        this.geminiService = geminiService;
        this.localSuggestionEngine = localSuggestionEngine;
        this.suggestionRepository = suggestionRepository;
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void generateAISuggestions(User user) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        if (habits.isEmpty()) {
            localSuggestionEngine.generateSuggestionsForUser(user);
            return;
        }

        List<HabitLog> logs = habitLogRepository.findByHabitUserId(user.getId());

        // Prepare context for prompt
        StringBuilder habitsContext = new StringBuilder();
        habitsContext.append("User Habits:\n");
        for (Habit h : habits) {
            long compCount = logs.stream().filter(l -> l.getHabit().getId().equals(h.getId())).count();
            habitsContext.append(String.format("- ID: %d, Name: '%s', Category: %s, Target: %d, Frequency: %s, Created: %s, Completions: %d\n",
                    h.getId(), h.getName(), h.getCategory(), h.getTargetCount(), h.getFrequency(), h.getCreatedAt().toLocalDate(), compCount));
        }

        // Recent Moods
        String recentMoods = logs.stream()
                .filter(l -> l.getMood() != null)
                .limit(10)
                .map(HabitLog::getMood)
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "You are an expert behavioral psychologist and executive coach inspired by James Clear's 'Atomic Habits'.\n" +
                "Analyze the following user habit tracking logs and generate exactly 3 highly personalized, actionable suggestions.\n\n" +
                "%s\n" +
                "Recent moods recorded: [%s]\n\n" +
                "Requirements:\n" +
                "1. Focus on identity-based habits, reducing friction, environment design, and recovery after failure.\n" +
                "2. Detect streak breaks, overloading (too many habits), or completion rate drops.\n" +
                "3. You MUST format the output as a valid JSON array of objects, containing ONLY the keys 'content' and 'type'. Do NOT include any markdown formatting, backticks, or text before/after the JSON array.\n" +
                "4. Valid types are: 'STREAK_WARNING', 'OPTIMIZATION', 'OVERLOAD', 'TIME_ANALYSIS', 'MOOD_CORRELATION', 'CONSISTENCY_DROP'.\n\n" +
                "JSON format example:\n" +
                "[\n" +
                "  {\"content\": \"suggestion here\", \"type\": \"OPTIMIZATION\"}\n" +
                "]",
                habitsContext.toString(), recentMoods
        );

        String jsonResponse = geminiService.generateContent(prompt);

        if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
            try {
                List<Map<String, String>> rawSuggestions = objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {});
                
                // Clear old suggestions before saving new ones
                suggestionRepository.deleteByUserId(user.getId());

                List<Suggestion> newSuggestions = new ArrayList<>();
                for (Map<String, String> item : rawSuggestions) {
                    String content = item.get("content");
                    String type = item.get("type");

                    if (content != null && type != null) {
                        newSuggestions.add(Suggestion.builder()
                                .user(user)
                                .content(content)
                                .type(type.toUpperCase())
                                .isRead(false)
                                .build());
                    }
                }

                if (!newSuggestions.isEmpty()) {
                    suggestionRepository.saveAll(newSuggestions);
                    log.info("Successfully updated AI suggestions for user: {}", user.getUsername());
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to parse Gemini response JSON: {}. Response content was: {}", e.getMessage(), jsonResponse);
            }
        }

        // Fallback to local rule-based generator
        log.info("Falling back to local rule-based engine for user: {}", user.getUsername());
        localSuggestionEngine.generateSuggestionsForUser(user);
    }
}
