package com.tracker.smarthabittracker.config;

import com.tracker.smarthabittracker.model.*;
import com.tracker.smarthabittracker.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@ConditionalOnProperty(name = "app.demo.seed-on-startup", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SuggestionRepository suggestionRepository;
    private final NotificationRepository notificationRepository;
    private final MoodLogRepository moodLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserRepository userRepository,
                               UserSettingsRepository userSettingsRepository,
                               HabitRepository habitRepository,
                               HabitLogRepository habitLogRepository,
                               SuggestionRepository suggestionRepository,
                               NotificationRepository notificationRepository,
                               MoodLogRepository moodLogRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.suggestionRepository = suggestionRepository;
        this.notificationRepository = notificationRepository;
        this.moodLogRepository = moodLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername("demo")) {
            return;
        }

        User demoUser = userRepository.save(User.builder()
                .username("demo")
                .email("demo@smarthabit.com")
                .password(passwordEncoder.encode("password123"))
                .role("USER")
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(demoUser)
                .dailyReminderTime(LocalTime.of(20, 0))
                .enableBrowserNotifications(true)
                .theme("DARK")
                .build());

        Habit workout = habitRepository.save(Habit.builder()
                .user(demoUser)
                .name("Morning Workout")
                .description("30 minutes cardio and stretching")
                .category("FITNESS")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build());

        Habit reading = habitRepository.save(Habit.builder()
                .user(demoUser)
                .name("Read Book")
                .description("Read at least 15 pages of a technical book")
                .category("STUDY")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build());

        Habit water = habitRepository.save(Habit.builder()
                .user(demoUser)
                .name("Drink Water")
                .description("Drink 3 liters of water throughout the day")
                .category("HEALTH")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build());

        Habit meditation = habitRepository.save(Habit.builder()
                .user(demoUser)
                .name("Evening Meditation")
                .description("10 minutes of deep breathing")
                .category("HEALTH")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build());

        Habit yoga = habitRepository.save(Habit.builder()
                .user(demoUser)
                .name("Yoga")
                .description("Morning stretching and vinyasa flow")
                .category("HEALTH")
                .frequency("DAILY")
                .targetCount(1)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build());

        for (int i = 14; i >= 1; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            int dayVal = date.getDayOfWeek().getValue();

            int moodScore = 3;
            int stressLevel = 3;
            int energyLevel = 3;
            int motivationLevel = 3;
            String note = "Normal routine";

            if (dayVal == 1 || dayVal == 3 || dayVal == 5) {
                moodScore = 4;
                stressLevel = 2;
                energyLevel = 4;
                motivationLevel = 4;
                note = "Productive and focused";
            } else if (dayVal == 2 || dayVal == 4) {
                moodScore = 3;
                stressLevel = 4;
                energyLevel = 2;
                motivationLevel = 3;
                note = "Heavy workday load";
            } else {
                moodScore = 5;
                stressLevel = 1;
                energyLevel = 5;
                motivationLevel = 4;
                note = "Relaxing weekend recharging";
            }

            moodLogRepository.save(MoodLog.builder()
                    .user(demoUser)
                    .loggedDate(date)
                    .moodScore(moodScore)
                    .stressLevel(stressLevel)
                    .energyLevel(energyLevel)
                    .motivationLevel(motivationLevel)
                    .notes(note)
                    .build());

            if (dayVal == 1 || dayVal == 3 || dayVal == 5 || dayVal == 6 || dayVal == 7) {
                habitLogRepository.save(HabitLog.builder()
                        .habit(workout)
                        .completedDate(date)
                        .completedTime(LocalTime.of(7, 30))
                        .mood(moodScore >= 4 ? "ENERGETIC" : "NEUTRAL")
                        .notes("Workout completed successfully")
                        .build());
            }

            if (dayVal != 3) {
                habitLogRepository.save(HabitLog.builder()
                        .habit(reading)
                        .completedDate(date)
                        .completedTime(LocalTime.of(22, 15))
                        .mood("HAPPY")
                        .notes("Read chapters")
                        .build());
            }

            habitLogRepository.save(HabitLog.builder()
                    .habit(water)
                    .completedDate(date)
                    .completedTime(LocalTime.of(20, 0))
                    .mood("HAPPY")
                    .notes("Met water target")
                    .build());

            if (dayVal == 1 || dayVal == 3 || dayVal == 5 || dayVal == 7) {
                habitLogRepository.save(HabitLog.builder()
                        .habit(meditation)
                        .completedDate(date)
                        .completedTime(LocalTime.of(21, 30))
                        .mood("HAPPY")
                        .notes("Meditation completed")
                        .build());
            }

            if (dayVal == 2 || dayVal == 4 || dayVal == 6) {
                habitLogRepository.save(HabitLog.builder()
                        .habit(yoga)
                        .completedDate(date)
                        .completedTime(LocalTime.of(6, 15))
                        .mood("ENERGETIC")
                        .notes("Morning stretching")
                        .build());
            }
        }

        suggestionRepository.save(Suggestion.builder()
                .user(demoUser)
                .habit(workout)
                .content("Your Morning Workout consistency is highest before 9:00 AM. Keep maintaining this morning routine.")
                .type("TIME_ANALYSIS")
                .isRead(false)
                .build());

        suggestionRepository.save(Suggestion.builder()
                .user(demoUser)
                .habit(meditation)
                .content("Evening Meditation completion rates drop when you report high stress. Consider a shorter 2-minute session on busy days.")
                .type("MOOD_CORRELATION")
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(demoUser)
                .title("Welcome to Smart Habit Tracker")
                .message("Start building and tracking your habits. Check your suggestions feed for smart daily insights!")
                .type("SYSTEM")
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(demoUser)
                .title("Streak Alert: Drink Water")
                .message("You are on a 14-day streak for Drink Water! Keep it going!")
                .type("STREAK_ALERT")
                .isRead(false)
                .build());
    }
}