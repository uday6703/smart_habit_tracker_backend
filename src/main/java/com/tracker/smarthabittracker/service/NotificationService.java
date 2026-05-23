package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.exception.ResourceNotFoundException;
import com.tracker.smarthabittracker.model.Notification;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<Notification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Notification markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
        unread.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type) // REMINDER, STREAK_ALERT, WEEKLY_SUMMARY, SYSTEM
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long id, User user) {
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
        notificationRepository.delete(notification);
    }
}
