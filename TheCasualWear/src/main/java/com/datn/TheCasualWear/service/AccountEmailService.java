package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.entity.AppUser;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Gửi email kích hoạt tài khoản cho khách được cashier tạo sẵn tại quầy.
 * Cùng pattern với OrderEmailService (async qua "mailTaskExecutor", KHÔNG
 * được để lỗi gửi mail làm fail luồng tạo tài khoản — tài khoản đã lưu DB
 * xong trước khi hàm này được gọi, ở đây chỉ log lại nếu gửi thất bại).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Async("mailTaskExecutor")
    public void sendAccountActivationEmailAsync(AppUser user, String rawToken) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Bỏ qua gửi email kích hoạt cho tài khoản #{} — không có email.", user.getId());
            return;
        }

        try {
            String activationUrl = appBaseUrl + "/activate-account?token=" + rawToken;
            String html = renderActivationHtml(user, activationUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setFrom(fromAddress);
            helper.setSubject("Kích hoạt tài khoản TheCasualWear của bạn");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Đã gửi email kích hoạt tài khoản #{} tới {}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Gửi email kích hoạt tài khoản #{} thất bại: {}",
                    user.getId(), e.getMessage(), e);
        }
    }

    private String renderActivationHtml(AppUser user, String activationUrl) {
        Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("activationUrl", activationUrl);
        // Link hết hạn sau 48h — set cứng ở đây để hiển thị cho khách biết
        // hạn chót, khớp với CashierAccountService.getOrCreateAccountForCustomer.
        context.setVariable("expiresInHours", 48);
        return templateEngine.process("email/account-activation", context);
    }
}
