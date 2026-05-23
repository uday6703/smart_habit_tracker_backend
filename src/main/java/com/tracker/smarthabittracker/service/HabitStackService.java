package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.HabitStackDto;
import com.tracker.smarthabittracker.exception.ResourceNotFoundException;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitStack;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitRepository;
import com.tracker.smarthabittracker.repository.HabitStackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class HabitStackService {

    private final HabitStackRepository habitStackRepository;
    private final HabitRepository habitRepository;

    public HabitStackService(HabitStackRepository habitStackRepository, HabitRepository habitRepository) {
        this.habitStackRepository = habitStackRepository;
        this.habitRepository = habitRepository;
    }

    @Transactional(readOnly = true)
    public List<HabitStackDto> getAllStacks(User user) {
        List<HabitStack> stacks = habitStackRepository.findByUserId(user.getId());
        return stacks.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public HabitStackDto createStack(Long cueHabitId, Long targetHabitId, User user) {
        if (cueHabitId.equals(targetHabitId)) {
            throw new IllegalArgumentException("A habit cannot stack on itself.");
        }

        Habit cueHabit = habitRepository.findByIdAndUserId(cueHabitId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cue habit not found"));

        Habit targetHabit = habitRepository.findByIdAndUserId(targetHabitId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target habit not found"));

        // Check if cue habit already triggers something (due to unique constraint)
        Optional<HabitStack> existingCue = habitStackRepository.findByUserIdAndCueHabitId(user.getId(), cueHabitId);
        if (existingCue.isPresent()) {
            throw new IllegalArgumentException("This cue habit is already leading to another habit in a stack.");
        }

        // Validate cycle detection
        validateNoCycle(cueHabitId, targetHabitId, user.getId());

        HabitStack stack = HabitStack.builder()
                .user(user)
                .cueHabit(cueHabit)
                .targetHabit(targetHabit)
                .build();

        HabitStack saved = habitStackRepository.save(stack);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteStack(Long id, User user) {
        HabitStack stack = habitStackRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Habit stack not found"));
        habitStackRepository.delete(stack);
    }

    public Optional<String> getNextSuggestedHabit(Long completedHabitId, Long userId) {
        List<HabitStack> stacks = habitStackRepository.findByCueHabitId(completedHabitId);
        for (HabitStack stack : stacks) {
            if (stack.getUser().getId().equals(userId) && stack.getTargetHabit().getIsActive()) {
                return Optional.of(stack.getTargetHabit().getName());
            }
        }
        return Optional.empty();
    }

    private void validateNoCycle(Long proposedCueId, Long proposedTargetId, Long userId) {
        // Build graph of existing stacks
        List<HabitStack> existingStacks = habitStackRepository.findByUserId(userId);
        Map<Long, List<Long>> adjacencyList = new HashMap<>();

        for (HabitStack stack : existingStacks) {
            adjacencyList.computeIfAbsent(stack.getCueHabit().getId(), k -> new ArrayList<>())
                    .add(stack.getTargetHabit().getId());
        }

        // Add proposed relationship
        adjacencyList.computeIfAbsent(proposedCueId, k -> new ArrayList<>()).add(proposedTargetId);

        // Run cycle detection (DFS)
        Set<Long> visited = new HashSet<>();
        Set<Long> recStack = new HashSet<>();

        for (Long habitId : adjacencyList.keySet()) {
            if (hasCycleDfs(habitId, adjacencyList, visited, recStack)) {
                throw new IllegalArgumentException("Circular dependency detected! This connection would create a loop in your stacks.");
            }
        }
    }

    private boolean hasCycleDfs(Long current, Map<Long, List<Long>> adj, Set<Long> visited, Set<Long> recStack) {
        if (recStack.contains(current)) {
            return true;
        }
        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        recStack.add(current);

        List<Long> neighbors = adj.get(current);
        if (neighbors != null) {
            for (Long neighbor : neighbors) {
                if (hasCycleDfs(neighbor, adj, visited, recStack)) {
                    return true;
                }
            }
        }

        recStack.remove(current);
        return false;
    }

    private HabitStackDto mapToDto(HabitStack stack) {
        return HabitStackDto.builder()
                .id(stack.getId())
                .cueHabitId(stack.getCueHabit().getId())
                .cueHabitName(stack.getCueHabit().getName())
                .targetHabitId(stack.getTargetHabit().getId())
                .targetHabitName(stack.getTargetHabit().getName())
                .createdAt(stack.getCreatedAt())
                .build();
    }
}
