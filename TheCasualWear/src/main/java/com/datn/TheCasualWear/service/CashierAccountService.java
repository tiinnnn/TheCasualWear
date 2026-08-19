package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Role;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Logic cho tính năng "Cashier tạo tài khoản trước cho khách":
 * cashier chỉ nhập email (+ SĐT tùy chọn) tại quầy, hệ thống tạo tài khoản
 * ở trạng thái enabled=false + activationToken, gửi email chứa link để
 * khách tự đặt mật khẩu và kích hoạt.
 *
 * ⚠️ KHỚP với convention hiện có của project (xem AppUserService):
 * project KHÔNG dùng PasswordEncoder/BCrypt — password lưu dạng
 * "{noop}" + mật khẩu thô, Spring Security tự nhận diện prefix "{noop}"
 * để so khớp plaintext lúc login. Class này làm y hệt, không tạo thêm
 * bean PasswordEncoder nào.
 *
 * ⚠️ GIẢ ĐỊNH còn lại: RoleRepository có method findByName(String) và
 * role khách hàng trong DB tên là "ROLE_CUSTOMER" (đúng như AppUserService
 * đang dùng ở method register()).
 */
@Service
@RequiredArgsConstructor
public class CashierAccountService {

    private static final int ACTIVATION_VALID_HOURS = 48;
    private static final String CUSTOMER_ROLE_NAME = "ROLE_CUSTOMER";

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final AccountEmailService accountEmailService;

    public record CashierAccountResult(AppUser user, boolean newlyCreated) {}

    /**
     * Trả về tài khoản khách theo email — nếu đã tồn tại thì dùng luôn
     * (không tạo trùng), nếu chưa có thì tạo mới + gửi email kích hoạt.
     */
    @Transactional
    public CashierAccountResult getOrCreateAccountForCustomer(String rawEmail, String rawPhone) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email của khách.");
        }
        String email = rawEmail.trim().toLowerCase();
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Email phải có đuôi @gmail.com!");
        }

        String phone = (rawPhone == null || rawPhone.isBlank()) ? null : rawPhone.trim();
        if (phone != null && !isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }

        Optional<AppUser> existing = appUserRepository.findByEmail(email);
        if (existing.isPresent()) {
            return new CashierAccountResult(existing.get(), false);
        }

        Role customerRole = roleRepository.findByName(CUSTOMER_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Thiếu role " + CUSTOMER_ROLE_NAME + " trong bảng role."));

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(generateUniqueUsername(email));
        // Mật khẩu placeholder ngẫu nhiên (dạng "{noop}" như AppUserService) —
        // không ai biết, và cũng không dùng được vì enabled=false chặn login
        // ở UserDetailsService (SecurityConfig.userDetailsService()).
        user.setPassword("{noop}" + UUID.randomUUID());
        user.setEnabled(false);
        user.setRoles(Set.of(customerRole));

        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);
        user.setActivationExpiresAt(LocalDateTime.now().plusHours(ACTIVATION_VALID_HOURS));

        appUserRepository.save(user);

        accountEmailService.sendAccountActivationEmailAsync(user, token);

        return new CashierAccountResult(user, true);
    }

    /**
     * Khách bấm link trong email, tự đặt mật khẩu -> kích hoạt tài khoản.
     */
    @Transactional
    public void activateAccount(String token, String newPassword) {
        AppUser user = appUserRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link kích hoạt không hợp lệ."));

        if (user.getActivationExpiresAt() == null
                || user.getActivationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Link kích hoạt đã hết hạn, vui lòng liên hệ cửa hàng.");
        }
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự và chứa ít nhất 1 chữ số.");
        }

        user.setPassword("{noop}" + newPassword);
        user.setEnabled(true);
        user.setActivationToken(null);
        user.setActivationExpiresAt(null);
        appUserRepository.save(user);
    }

    // Cùng rule với AppUserService.isValidEmail() / isValidPhone() — giữ
    // nhất quán validate trên toàn hệ thống, vì email/phone của tài khoản
    // này rồi cũng có thể được khách tự đăng ký lấp đầy qua /auth/register
    // (xem AppUserService.fillPendingCashierAccount()).
    private boolean isValidEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@gmail.com");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{10}$");
    }

    // Cùng rule với AppUserService.isValidPassword() — giữ nhất quán validate
    // mật khẩu trong toàn bộ hệ thống.
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;
        return password.chars().anyMatch(Character::isDigit);
    }

    // Sinh username không trùng, lấy phần trước "@" của email làm gốc.
    // Login hỗ trợ cả username/email/phone (findByUsernameOrEmailOrPhone)
    // nên chỉ cần đảm bảo username không trùng bất kỳ giá trị nào trong 3
    // trường đó của người khác.
    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9]", "");
        if (base.isBlank()) {
            base = "khach";
        }
        String candidate = base;
        int suffix = 0;
        while (appUserRepository.findByUsernameOrEmailOrPhone(candidate).isPresent()) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}