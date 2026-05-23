package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.dto.UserSettingsDto;
import com.tracker.smarthabittracker.model.*;
import com.tracker.smarthabittracker.repository.*;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    private final UserSettingsRepository userSettingsRepository;
    private final UserService userService;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SuggestionRepository suggestionRepository;
    private final NotificationRepository notificationRepository;
    private final MoodLogRepository moodLogRepository;
    private final HabitDnaProfileRepository habitDnaProfileRepository;
    private final FutureProjectionRepository futureProjectionRepository;
    private final FailureAnalysisRepository failureAnalysisRepository;
    private final LifeBalanceScoreRepository lifeBalanceScoreRepository;

    public UserSettingsController(UserSettingsRepository userSettingsRepository, 
                                  UserService userService,
                                  HabitRepository habitRepository,
                                  HabitLogRepository habitLogRepository,
                                  SuggestionRepository suggestionRepository,
                                  NotificationRepository notificationRepository,
                                  MoodLogRepository moodLogRepository,
                                  HabitDnaProfileRepository habitDnaProfileRepository,
                                  FutureProjectionRepository futureProjectionRepository,
                                  FailureAnalysisRepository failureAnalysisRepository,
                                  LifeBalanceScoreRepository lifeBalanceScoreRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.userService = userService;
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.suggestionRepository = suggestionRepository;
        this.notificationRepository = notificationRepository;
        this.moodLogRepository = moodLogRepository;
        this.habitDnaProfileRepository = habitDnaProfileRepository;
        this.futureProjectionRepository = futureProjectionRepository;
        this.failureAnalysisRepository = failureAnalysisRepository;
        this.lifeBalanceScoreRepository = lifeBalanceScoreRepository;
    }

    @GetMapping
    public ResponseEntity<UserSettingsDto> getSettings() {
        User user = userService.getCurrentAuthenticatedUser();
        UserSettings settings = userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSettings defaultSettings = UserSettings.builder()
                            .user(user)
                            .theme("DARK")
                            .enableBrowserNotifications(true)
                            .dailyReminderTime(LocalTime.of(20, 0))
                            .build();
                    return userSettingsRepository.save(defaultSettings);
                });

        String timeStr = settings.getDailyReminderTime() != null 
                ? settings.getDailyReminderTime().format(DateTimeFormatter.ofPattern("HH:mm")) 
                : "20:00";

        UserSettingsDto dto = UserSettingsDto.builder()
                .dailyReminderTime(timeStr)
                .enableBrowserNotifications(settings.getEnableBrowserNotifications())
                .theme(settings.getTheme())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody UserSettingsDto dto) {
        User user = userService.getCurrentAuthenticatedUser();
        UserSettings settings = userSettingsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Settings record not found"));

        if (dto.getDailyReminderTime() != null) {
            settings.setDailyReminderTime(LocalTime.parse(dto.getDailyReminderTime(), DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (dto.getEnableBrowserNotifications() != null) {
            settings.setEnableBrowserNotifications(dto.getEnableBrowserNotifications());
        }
        if (dto.getTheme() != null) {
            settings.setTheme(dto.getTheme().toUpperCase());
        }

        userSettingsRepository.save(settings);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Settings updated successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<?> seedDemoData() {
        User user = userService.getCurrentAuthenticatedUser();
        Long userId = user.getId();

        // 1. Delete existing records for this user to ensure clean seed
        failureAnalysisRepository.deleteByUserId(userId);
        futureProjectionRepository.deleteByUserId(userId);
        habitDnaProfileRepository.deleteByUserId(userId);
        lifeBalanceScoreRepository.deleteByUserId(userId);
        moodLogRepository.deleteByUserId(userId);
        suggestionRepository.deleteByUserId(userId);
        
        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifs);

        List<Habit> habits = habitRepository.findByUserId(userId);
        habitRepository.deleteAll(habits);

        // 2. Create new habits
        List<Habit> seededHabits = new ArrayList<>();
        
        Habit workout = Habit.builder()
                .user(user)
                .name("Morning Workout")
                .description("30 minutes cardio and stretching")
                .category("FITNESS")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();
        seededHabits.add(habitRepository.save(workout));

        Habit book = Habit.builder()
                .user(user)
                .name("Read Book")
                .description("Read at least 15 pages of technical book")
                .category("STUDY")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();
        seededHabits.add(habitRepository.save(book));

        Habit water = Habit.builder()
                .user(user)
                .name("Drink Water")
                .description("Drink 3 Liters of water throughout the day")
                .category("HEALTH")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();
        seededHabits.add(habitRepository.save(water));

        Habit meditation = Habit.builder()
                .user(user)
                .name("Evening Meditation")
                .description("10 minutes of deep breathing")
                .category("HEALTH")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();
        seededHabits.add(habitRepository.save(meditation));

        Habit yoga = Habit.builder()
                .user(user)
                .name("Yoga")
                .description("Morning stretching and Vinyasa flow")
                .category("HEALTH")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();
        seededHabits.add(habitRepository.save(yoga));

        // 3. Seed Mood logs & Habit logs for the last 14 days
        for (int i = 14; i >= 1; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            
            // Seed Mood Log
            int moodScore = 3;
            int stressLevel = 3;
            int energyLevel = 3;
            int motivationLevel = 3;
            String note = "Normal routine";

            int dayVal = date.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)
            if (dayVal == 1 || dayVal == 3 || dayVal == 5) {
                // Mon, Wed, Fri (Good days)
                moodScore = 4;
                stressLevel = 2;
                energyLevel = 4;
                motivationLevel = 4;
                note = "Productive and focused";
            } else if (dayVal == 2 || dayVal == 4) {
                // Tue, Thu (Stressed days)
                moodScore = 3;
                stressLevel = 4;
                energyLevel = 2;
                motivationLevel = 3;
                note = "Heavy workday load";
            } else {
                // Sat, Sun (Great days)
                moodScore = 5;
                stressLevel = 1;
                energyLevel = 5;
                motivationLevel = 4;
                note = "Relaxing weekend recharging";
            }

            MoodLog moodLog = MoodLog.builder()
                    .user(user)
                    .loggedDate(date)
                    .moodScore(moodScore)
                    .stressLevel(stressLevel)
                    .energyLevel(energyLevel)
                    .motivationLevel(motivationLevel)
                    .notes(note)
                    .build();
            moodLogRepository.save(moodLog);

            // Seed completions
            // Morning Workout (Mon, Wed, Fri, Sat, Sun - 5 days/week)
            if (dayVal == 1 || dayVal == 3 || dayVal == 5 || dayVal == 6 || dayVal == 7) {
                HabitLog log = HabitLog.builder()
                        .habit(workout)
                        .completedDate(date)
                        .completedTime(LocalTime.of(7, 30))
                        .mood(moodScore >= 4 ? "ENERGETIC" : "NEUTRAL")
                        .notes("Workout completed successfully")
                        .build();
                habitLogRepository.save(log);
            }

            // Read Book (Mon, Tue, Thu, Fri, Sat, Sun - 6 days/week)
            if (dayVal != 3) {
                HabitLog log = HabitLog.builder()
                        .habit(book)
                        .completedDate(date)
                        .completedTime(LocalTime.of(22, 15))
                        .mood("HAPPY")
                        .notes("Read chapters")
                        .build();
                habitLogRepository.save(log);
            }

            // Drink Water (All 14 days)
            HabitLog logWater = HabitLog.builder()
                    .habit(water)
                    .completedDate(date)
                    .completedTime(LocalTime.of(20, 0))
                    .mood("HAPPY")
                    .notes("Met water target")
                    .build();
            habitLogRepository.save(logWater);

            // Evening Meditation (Mon, Wed, Fri, Sun - 4 days/week)
            if (dayVal == 1 || dayVal == 3 || dayVal == 5 || dayVal == 7) {
                HabitLog log = HabitLog.builder()
                        .habit(meditation)
                        .completedDate(date)
                        .completedTime(LocalTime.of(21, 30))
                        .mood("HAPPY")
                        .notes("Meditation completed")
                        .build();
                habitLogRepository.save(log);
            }

            // Yoga (Tue, Thu, Sat - 3 days/week)
            if (dayVal == 2 || dayVal == 4 || dayVal == 6) {
                HabitLog log = HabitLog.builder()
                        .habit(yoga)
                        .completedDate(date)
                        .completedTime(LocalTime.of(6, 15))
                        .mood("ENERGETIC")
                        .notes("Morning stretching")
                        .build();
                habitLogRepository.save(log);
            }
        }

        // 4. Seed Suggestions
        Suggestion sug1 = Suggestion.builder()
                .user(user)
                .habit(workout)
                .content("Your Morning Workout consistency is highest before 9:00 AM. Keep maintaining this morning routine.")
                .type("TIME_ANALYSIS")
                .isRead(false)
                .build();
        suggestionRepository.save(sug1);

        Suggestion sug2 = Suggestion.builder()
                .user(user)
                .habit(meditation)
                .content("Evening Meditation completion rates drop when you report high stress. Consider a shorter 2-minute session on busy days.")
                .type("MOOD_CORRELATION")
                .isRead(false)
                .build();
        suggestionRepository.save(sug2);

        // 5. Seed Notifications
        Notification notif1 = Notification.builder()
                .user(user)
                .title("Welcome to Smart Habit Tracker")
                .message("Start building and tracking your habits. Check your suggestions feed for smart daily insights!")
                .type("SYSTEM")
                .isRead(false)
                .build();
        notificationRepository.save(notif1);

        Notification notif2 = Notification.builder()
                .user(user)
                .title("Streak Alert: Drink Water")
                .message("You are on a 14-day streak for Drink Water! Keep it going!")
                .type("STREAK_ALERT")
                .isRead(false)
                .build();
        notificationRepository.save(notif2);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Demo data seeded successfully! All charts and widgets are now fully populated.");
        return ResponseEntity.ok(response);
    }
}
