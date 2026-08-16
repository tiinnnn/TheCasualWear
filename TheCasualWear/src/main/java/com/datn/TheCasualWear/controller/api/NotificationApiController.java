package com.datn.TheCasualWear.controller.api;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoint JSON riêng cho notifications.js (AJAX polling khu vực chuông
 * thông báo). Không thay thế /notifications/read/{id} và
 * /notifications/read-all hiện có trong NotificationController - 2 route đó
 * vẫn giữ nguyên, notifications.js gọi thẳng tới chúng qua fetch().
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final AppUserService appUserService;

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryDTO> summary(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            // Chưa đăng nhập (guest) -> trả rỗng, JS tự bỏ qua
            return ResponseEntity.ok(new NotificationSummaryDTO(0, Collections.emptyList()));
        }

        AppUser user = appUserService.getUserByUsername(auth.getName());

        int unreadCount = notificationService.countUnread(user.getId());

        List<NotificationDTO> notifications = notificationService.getUserNotifications(user.getId())
                .stream()
                .limit(20) // tránh dropdown quá dài, chỉnh nếu muốn nhiều/ít hơn
                .map(n -> new NotificationDTO(
                        n.getId(),
                        n.getMessage(),
                        // Nếu compile lỗi ở dòng dưới do tên getter khác (vd isRead()
                        // thay vì getIsRead()), đổi lại cho khớp Notification entity.
                        Boolean.TRUE.equals(n.getIsRead()),
                        n.getCreatedAt() != null ? n.getCreatedAt().format(DISPLAY_FORMAT) : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new NotificationSummaryDTO(unreadCount, notifications));
    }

    public record NotificationSummaryDTO(int unreadCount, List<NotificationDTO> notifications) {}

    public record NotificationDTO(Integer id, String message, boolean isRead, String createdAtDisplay) {}
}
