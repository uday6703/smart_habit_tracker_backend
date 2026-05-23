package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.HeatmapDto;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HeatmapService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    public HeatmapService(HabitRepository habitRepository, HabitLogRepository habitLogRepository) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
    }

    @Transactional(readOnly = true)
    public List<HeatmapDto> getHeatmapData(LocalDate startDate, LocalDate endDate, User user) {
        List<Habit> activeHabits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByHabitUserId(user.getId());

        // Group logs by completed_date
        Map<LocalDate, List<HabitLog>> logsByDate = logs.stream()
                .filter(l -> !l.getCompletedDate().isBefore(startDate) && !l.getCompletedDate().isAfter(endDate))
                .collect(Collectors.groupingBy(HabitLog::getCompletedDate));

        List<HeatmapDto> data = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            LocalDate date = current;
            List<HabitLog> dayLogs = logsByDate.getOrDefault(date, new ArrayList<>());

            List<String> completed = dayLogs.stream()
                    .map(l -> l.getHabit().getName())
                    .toList();

            // Find habits that were active on or before this day
            List<Habit> habitsScheduledForDay = activeHabits.stream()
                    .filter(h -> !h.getCreatedAt().toLocalDate().isAfter(date))
                    .toList();

            List<String> missed = habitsScheduledForDay.stream()
                    .filter(h -> dayLogs.stream().noneMatch(l -> l.getHabit().getId().equals(h.getId())))
                    .map(Habit::getName)
                    .toList();

            int completedCount = completed.size();
            int totalCount = habitsScheduledForDay.size();
            double compRate = totalCount > 0 ? ((double) completedCount / totalCount) * 100.0 : 0.0;

            // Productivity Score formula:
            // 80% completion rate + 20% consistency bonus - 5% penalty per missed habit
            double score = (compRate * 0.8) - (missed.size() * 5.0);
            if (completedCount == totalCount && totalCount > 0) {
                score += 20.0; // Perfect completion bonus
            }
            double finalScore = Math.max(0.0, Math.min(100.0, Math.round(score * 10.0) / 10.0));

            data.add(HeatmapDto.builder()
                    .date(date.toString())
                    .completionRate(Math.round(compRate * 10.0) / 10.0)
                    .completedCount(completedCount)
                    .totalCount(totalCount)
                    .completedHabits(completed)
                    .missedHabits(missed)
                    .productivityScore(finalScore)
                    .build());

            current = current.plusDays(1);
        }

        return data;
    }
}
