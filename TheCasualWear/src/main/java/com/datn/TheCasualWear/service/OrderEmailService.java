package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.entity.AppOrder;
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
 * Gửi email xác nhận đơn hàng (4.3) — trigger từ AdminOrderController sau khi
 * admin xác nhận đơn (PENDING → CONFIRMED). Chạy bất đồng bộ qua
 * "mailTaskExecutor" (xem MailAsyncConfig) để không làm chậm response, và
 * KHÔNG được để lỗi gửi mail làm fail luồng xác nhận đơn — đơn đã CONFIRMED
 * xong trước khi hàm này được gọi, ở đây chỉ log lại nếu gửi thất bại.
 *
 * ⚠️ GIẢ ĐỊNH: application.properties đã có spring.mail.username (theo xác
 * nhận SMTP đã cấu hình sẵn) — dùng làm địa chỉ "From". Nếu key tên khác,
 * đổi lại @Value bên dưới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Thymeleaf engine có sẵn (dùng chung với web view)

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async("mailTaskExecutor")
    public void sendOrderConfirmationAsync(AppOrder order) {
        String recipient = resolveRecipientEmail(order);

        if (recipient == null || recipient.isBlank()) {
            // Không có email để gửi — user không nhập email lúc đăng ký, hoặc
            // đơn COUNTER (POS) không có guestEmail. Không phải lỗi, chỉ log.
            log.warn("Bỏ qua gửi email xác nhận đơn #{} — không có địa chỉ email nhận.",
                    order.getOrderCode());
            return;
        }

        try {
            String html = renderOrderConfirmationHtml(order, recipient);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setFrom(fromAddress);
            helper.setSubject("Xác nhận đơn hàng #" + order.getOrderCode() + " — TheCasualWear");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Đã gửi email xác nhận đơn #{} tới {}", order.getOrderCode(), recipient);
        } catch (Exception e) {
            log.error("Gửi email xác nhận đơn #{} thất bại: {}",
                    order.getOrderCode(), e.getMessage(), e);
        }
    }

    // User đã login -> email tài khoản. Guest -> guestEmail (AppOrder, thêm ở 4.1).
    private String resolveRecipientEmail(AppOrder order) {
        if (order.getCustomer() != null) {
            return order.getCustomer().getEmail();
        }
        return order.getGuestEmail();
    }

    private String renderOrderConfirmationHtml(AppOrder order, String recipient) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("recipientName",
                order.getShippingAddress() != null ? order.getShippingAddress().getFullName()
                        : (order.getCustomer() != null ? order.getCustomer().getUsername() : "Quý khách"));
        context.setVariable("isGuest", order.getCustomer() == null);
        return templateEngine.process("email/order-confirmation", context);
    }
}
