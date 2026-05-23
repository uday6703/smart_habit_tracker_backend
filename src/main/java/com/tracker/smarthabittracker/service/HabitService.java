package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.HabitDto;
import com.tracker.smarthabittracker.dto.HabitLogRequest;
import com.tracker.smarthabittracker.exception.ResourceNotFoundException;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    public HabitService(HabitRepository habitRepository, HabitLogRepository habitLogRepository) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
    }

    public List<Habit> getHabitsForUser(User user) {
        return habitRepository.findByUserId(user.getId());
    }

    public List<HabitDto> getHabitDtosForUser(User user) {
        return getHabitsForUser(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Habit getHabitById(Long id, User user) {
        return habitRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id " + id));
    }

    public Habit createHabit(Habit habit, User user) {
        habit.setUser(user);
        habit.setIsActive(true);
        return habitRepository.save(habit);
    }

    @Transactional
    public Habit updateHabit(Long id, Habit updatedHabit, User user) {
        Habit habit = getHabitById(id, user);
        habit.setName(updatedHabit.getName());
        habit.setDescription(updatedHabit.getDescription());
        habit.setCategory(updatedHabit.getCategory());
        habit.setFrequency(updatedHabit.getFrequency());
        habit.setFrequencyValue(updatedHabit.getFrequencyValue());
        habit.setTargetCount(updatedHabit.getTargetCount());
        habit.setIsActive(updatedHabit.getIsActive());
        return habitRepository.save(habit);
    }

    public void deleteHabit(Long id, User user) {
        Habit habit = getHabitById(id, user);
        habitRepository.delete(habit);
    }

    @Transactional
    public HabitLog logCompletion(Long habitId, HabitLogRequest request, User user) {
        Habit habit = getHabitById(habitId, user);
        LocalDate date = request.getCompletedDate() != null ? request.getCompletedDate() : LocalDate.now();
        LocalTime time = request.getCompletedTime() != null ? request.getCompletedTime() : LocalTime.now();

        Optional<HabitLog> existingLog = habitLogRepository.findByHabitIdAndCompletedDate(habitId, date);
        if (existingLog.isPresent()) {
            HabitLog log = existingLog.get();
            log.setCompletedTime(time);
            log.setMood(request.getMood());
            log.setNotes(request.getNotes());
            return habitLogRepository.save(log);
        } else {
            HabitLog log = HabitLog.builder()
                    .habit(habit)
                    .completedDate(date)
                    .completedTime(time)
                    .mood(request.getMood())
                    .notes(request.getNotes())
                    .build();
            return habitLogRepository.save(log);
        }
    }

    @Transactional
    public void deleteCompletion(Long habitId, LocalDate date, User user) {
        // Verify ownership
        getHabitById(habitId, user);
        Optional<HabitLog> log = habitLogRepository.findByHabitIdAndCompletedDate(habitId, date);
        log.ifPresent(habitLogRepository::delete);
    }

    public HabitDto mapToDto(Habit habit) {
        List<HabitLog> logs = habitLogRepository.findByHabitId(habit.getId());
        
        int currentStreak = calculateCurrentStreak(logs);
        int longestStreak = calculateLongestStreak(logs);
        double completionRate = calculateCompletionRate(logs, habit.getCreatedAt().toLocalDate());
        boolean completedToday = logs.stream()
                .anyMatch(log -> log.getCompletedDate().equals(LocalDate.now()));

        return HabitDto.builder()
                .id(habit.getId())
                .name(habit.getName())
                .description(habit.getDescription())
                .category(habit.getCategory())
                .frequency(habit.getFrequency())
                .frequencyValue(habit.getFrequencyValue())
                .targetCount(habit.getTargetCount())
                .isActive(habit.getIsActive())
                .createdAt(habit.getCreatedAt())
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .completionRate(completionRate)
                .completedToday(completedToday)
                .build();
    }

    public int calculateCurrentStreak(List<HabitLog> logs) {
        if (logs.isEmpty()) return 0;

        Set<LocalDate> dates = logs.stream()
                .map(HabitLog::getCompletedDate)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (!dates.contains(today) && !dates.contains(yesterday)) {
            return 0;
        }

        int streak = 0;
        LocalDate checkDate = dates.contains(today) ? today : yesterday;

        while (dates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    public int calculateLongestStreak(List<HabitLog> logs) {
        if (logs.isEmpty()) return 0;

        List<LocalDate> sortedDates = logs.stream()
                .map(HabitLog::getCompletedDate)
                .distinct()
                .sorted()
                .toList();

        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate prevDate = null;

        for (LocalDate date : sortedDates) {
            if (prevDate == null) {
                currentStreak = 1;
            } else if (date.equals(prevDate.plusDays(1))) {
                currentStreak++;
            } else if (!date.equals(prevDate)) {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
            prevDate = date;
        }

        return Math.max(maxStreak, currentStreak);
    }

    private double calculateCompletionRate(List<HabitLog> logs, LocalDate creationDate) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(29); // 30-day window
        
        // Take the later of the creation date or window start
        LocalDate actualStart = creationDate.isAfter(windowStart) ? creationDate : windowStart;
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(actualStart, today) + 1;

        long completedDays = logs.stream()
                .map(HabitLog::getCompletedDate)
                .filter(d -> !d.isBefore(actualStart) && !d.isAfter(today))
                .distinct()
                .count();

        if (totalDays <= 0) return 0.0;
        double rate = ((double) completedDays / totalDays) * 100.0;
        return Math.round(rate * 10.0) / 10.0; // round to 1 decimal place
    }
}
