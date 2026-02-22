package com.school.management.service;

import com.school.management.constant.NotificationStatus;
import com.school.management.constant.NotificationType;
import com.school.management.dto.response.NotificationResponse;
import com.school.management.entity.Notification;
import com.school.management.entity.School;
import com.school.management.entity.User;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.NotificationRepository;
import com.school.management.repository.SchoolRepository;
import com.school.management.repository.UserRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create notification for a user
     */
    @Transactional
    public void createNotification(UUID userId, String title, String message, NotificationType type) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        School school = user.getSchool();

        Notification notification = Notification.builder()
                .school(school)
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .status(NotificationStatus.UNREAD)
                .build();

        notification = notificationRepository.save(notification);

        // Send via WebSocket (best effort)
        try {
            NotificationResponse response = mapToNotificationResponse(notification);
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", response);
        } catch (Exception e) {
            log.error("Failed to send notification via WebSocket", e);
        }
    }

    /**
     * Create notification (public method with all params)
     */
    @Transactional
    public NotificationResponse createNotification(UUID userId, String title, String message) {
        return createNotificationWithType(userId, title, message, NotificationType.INFO);
    }

    @Transactional
    public NotificationResponse createNotificationWithType(UUID userId, String title,
                                                           String message, NotificationType type) {
        createNotification(userId, title, message, type);

        // Fetch the created notification
        List<Notification> notifications = notificationRepository
                .findTop50ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        return mapToNotificationResponse(notifications.get(0));
    }

    /**
     * Get notifications for current user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        UUID userId = SecurityUtils.getCurrentUserId();

        List<Notification> notifications = notificationRepository
                .findTop50ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Notification notification = notificationRepository.findByIdAndUser_IdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setStatus(NotificationStatus.READ);
        notification = notificationRepository.save(notification);

        return mapToNotificationResponse(notification);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();

        List<Notification> notifications = notificationRepository
                .findTop50ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        notifications.forEach(notification -> {
            if (notification.getStatus() == NotificationStatus.UNREAD) {
                notification.setStatus(NotificationStatus.READ);
            }
        });

        notificationRepository.saveAll(notifications);
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Notification notification = notificationRepository.findByIdAndUser_IdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notificationRepository.delete(notification);
    }

    /**
     * Get unread count
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID userId = SecurityUtils.getCurrentUserId();

        List<Notification> notifications = notificationRepository
                .findTop50ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .filter(n -> n.getStatus() == NotificationStatus.UNREAD)
                .count();
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}