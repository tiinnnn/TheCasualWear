package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Notification;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    public NotificationService(NotificationRepository notificationRepository, AppUserRepository appUserRepository) {
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
    }

    public void createNotification(AppUser user, String message, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public int countUnread(Integer userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // Đánh dấu đã đọc tất cả
    public void markAllRead(Integer userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    // Đánh dấu đã đọc 1 thông báo
    public void markRead(Integer id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void createNotificationForAdmins(String message, String link) {
        // Lấy tất cả user có role ADMIN hoặc OWNER
        appUserRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().equals("ROLE_ADMIN")
                                || r.getName().equals("ROLE_OWNER")))
                .forEach(admin -> createNotification(admin, message, link));
    }

    public void createNotificationForDeliveries(String message, String link) {
        appUserRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().equals("ROLE_DELIVERY")))
                .forEach(delivery -> createNotification(delivery, message, link));
    }
}