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
 * Gửi email xác nhận đơn hàng (4.3) — ĐÃ ĐỔI (theo yêu cầu mới): trigger ngay
 * trong OrderService.placeOrder()/placeOrderGuest() lúc khách vừa đặt hàng
 * (status PENDING), KHÔNG còn chờ admin xác nhận (CONFIRMED) nữa. Chạy bất
 * đồng bộ qua "mailTaskExecutor" (xem MailAsyncConfig) để không làm chậm
 * response, và KHÔNG được để lỗi gửi mail làm fail luồng đặt hàng — đơn đã
 * lưu DB xong trước khi hàm này được gọi, ở đây chỉ log lại nếu gửi thất bại.
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

    // MỚI: base URL của app để dựng link tuyệt đối trong email (link tương
    // đối như "/order/lookup-guest" không mở được từ trong email client).
    // ⚠️ GIẢ ĐỊNH: chưa có property "app.base-url" trong application.properties
    // — cần thêm, ví dụ app.base-url=http://localhost:8080 lúc dev, đổi thành
    // domain thật lúc deploy VPS. Fallback dưới đây chỉ để không vỡ lúc chưa cấu hình.
    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

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
            helper.setSubject("Đã tiếp nhận đơn hàng #" + order.getOrderCode() + " — TheCasualWear");
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
        // MỚI: hiển thị email liên hệ trong bảng thông tin đơn hàng.
        context.setVariable("recipientEmail", recipient);
        // MỚI: link tra cứu đơn tuyệt đối, chỉ hiển thị cho khách vãng lai
        // (template chỉ render khối này khi isGuest=true, nhưng vẫn set biến
        // luôn cho gọn — không set thì Thymeleaf lỗi biến null trong th:href).
        context.setVariable("guestLookupUrl", appBaseUrl + "/order/lookup-guest");
        return templateEngine.process("email/order-confirmation", context);
    }
}