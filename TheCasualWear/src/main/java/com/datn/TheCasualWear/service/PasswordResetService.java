package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.PasswordResetToken;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final AppUserRepository appUserRepository;
    private final JavaMailSender mailSender;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                AppUserRepository appUserRepository,
                                JavaMailSender mailSender) {
        this.tokenRepository = tokenRepository;
        this.appUserRepository = appUserRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void sendResetEmail(String email) {
        // Không báo lỗi nếu email không tồn tại để tránh lộ thông tin
        AppUser user = appUserRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        // Xóa token cũ nếu có
        tokenRepository.deleteByUser(user);

        // Tạo token mới, hết hạn sau 15 phút
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(resetToken);

        // Gửi email
        String link = "http://localhost:8080/forgot-password/reset?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Đặt lại mật khẩu - The Casual Wear");
        message.setText(
                "Xin chào " + user.getUsername() + ",\n\n" +
                        "Nhấn vào link sau để đặt lại mật khẩu (hết hạn sau 15 phút):\n" +
                        link + "\n\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này."
        );
        mailSender.send(message);
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false; // Token không hợp lệ hoặc đã hết hạn
        }

        AppUser user = resetToken.getUser();
        user.setPassword("{noop}" + newPassword);
        appUserRepository.save(user);

        tokenRepository.delete(resetToken);
        return true;
    }
}