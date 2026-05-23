package com.tracker.smarthabittracker.controller;

import com.tracker.smarthabittracker.model.Notification;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.service.NotificationService;
import com.tracker.smarthabittracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications() {
        User user = userService.getCurrentAuthenticatedUser();
        List<Notification> notifications = notificationService.getNotificationsForUser(user);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        User user = userService.getCurrentAuthenticatedUser();
        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(user);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Notification notification = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        User user = userService.getCurrentAuthenticatedUser();
        notificationService.markAllAsRead(user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        notificationService.deleteNotification(id, user);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted");
        return ResponseEntity.ok(response);
    }
}
