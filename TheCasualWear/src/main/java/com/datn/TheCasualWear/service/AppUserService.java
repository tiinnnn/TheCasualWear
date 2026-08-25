package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Role;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository    roleRepository;
    private static final int ADMIN_PAGE_SIZE = 10;

    public AppUserService(AppUserRepository appUserRepository,
                          RoleRepository roleRepository) {
        this.appUserRepository = appUserRepository;
        this.roleRepository    = roleRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION HELPERS
    // ─────────────────────────────────────────────────────────────

    private boolean isValidEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@gmail.com");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{10}$");
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;
        return password.chars().anyMatch(Character::isDigit);
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY
    // ─────────────────────────────────────────────────────────────

    public Page<AppUser> getAllUsers(String keyword, String roleName, int page) {
        String kw   = (keyword == null  || keyword.isBlank())  ? null : keyword.trim();
        String role = (roleName == null || roleName.isBlank()) ? null : roleName;
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE,
                Sort.by("id").ascending());
        return appUserRepository.searchUsers(kw, role, pageable);
    }

    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    public AppUser getUserById(Integer id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy user với id: " + id));
    }

    public AppUser getUserByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy user: " + username));
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────
    // ĐĂNG KÝ
    // ─────────────────────────────────────────────────────────────

    // Khách tự đăng ký ở /auth/register. Có 2 tình huống:
    //   1) Email chưa tồn tại (hoặc đăng ký không kèm email, chỉ SĐT) -> tạo
    //      user mới hoàn toàn như trước giờ.
    //   2) Email TRÙNG với 1 tài khoản do cashier tạo sẵn (xem
    //      CashierAccountService.getOrCreateAccountForCustomer) mà khách CHƯA
    //      kích hoạt qua link email -> đây KHÔNG phải trùng thật, khách đang
    //      tự bổ sung username/password cho chính tài khoản của họ. Nhận biết
    //      qua activationToken != null (dấu hiệu "đang chờ kích hoạt", xem
    //      comment trên field này ở AppUser).
    // Nếu email trùng nhưng tài khoản ĐÃ kích hoạt rồi (activationToken ==
    // null) thì vẫn là trùng thật -> rơi xuống createUserWithRole() và bị
    // chặn bằng lỗi "Email đã được sử dụng!" như bình thường.
    @Transactional
    public void register(AppUser user) {
        String email = (user.getEmail() == null || user.getEmail().isBlank())
                ? null : user.getEmail().trim();

        if (email != null) {
            Optional<AppUser> existing = appUserRepository.findByEmail(email);
            if (existing.isPresent() && existing.get().getActivationToken() != null) {
                fillPendingCashierAccount(existing.get(), user);
                return;
            }
        }

        createUserWithRole(user, "ROLE_CUSTOMER");
    }

    // Bổ sung username/password (+ phone nếu có) vào tài khoản pending do
    // cashier tạo sẵn, rồi kích hoạt luôn (enabled=true) — cùng mức tin cậy
    // với đăng ký tự thân bình thường (register() vốn cũng không bắt xác
    // thực email), chỉ khác là user đã sở hữu email này từ trước nên không
    // cần link kích hoạt riêng nữa.
    private void fillPendingCashierAccount(AppUser pending, AppUser formInput) {
        if (formInput.getUsername() == null || formInput.getUsername().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập!");
        }
        // existsByUsername không đủ vì cần LOẠI TRỪ chính bản ghi pending
        // (trường hợp form gửi lại đúng username cũ do resubmit).
        Optional<AppUser> usernameOwner = appUserRepository.findByUsername(formInput.getUsername());
        if (usernameOwner.isPresent() && !usernameOwner.get().getId().equals(pending.getId())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (!isValidPassword(formInput.getPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có 6 ký tự và có ít nhất 1 chữ số!");
        }

        String phone = (formInput.getPhone() == null || formInput.getPhone().isBlank())
                ? null : formInput.getPhone().trim();
        if (phone != null) {
            if (!isValidPhone(phone)) {
                throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
            }
            if (!phone.equals(pending.getPhone()) && appUserRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
            }
            pending.setPhone(phone);
        }

        pending.setUsername(formInput.getUsername());
        pending.setPassword("{noop}" + formInput.getPassword());
        pending.setEnabled(true);
        pending.setActivationToken(null);
        pending.setActivationExpiresAt(null);
        // Role ROLE_CUSTOMER đã được gán sẵn lúc CashierAccountService tạo
        // tài khoản này -> không cần add lại.

        appUserRepository.save(pending);
    }

    // Dùng chung cho cả đăng ký khách hàng (role CUSTOMER) lẫn tạo nhân viên
    // (role CASHIER/ADMIN/OWNER) từ trang Admin — cùng 1 bộ validate, tránh
    // trùng lặp logic email/phone/password ở 2 nơi.
    public AppUser createUserWithRole(AppUser user, String roleName) {
        if (user.getEmail() != null && user.getEmail().isBlank()) {
            user.setEmail(null);
        }
        if (user.getPhone() != null && user.getPhone().isBlank()) {
            user.setPhone(null);
        }

        if (appUserRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (!isValidPassword(user.getPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có 6 ký tự và có ít nhất 1 chữ số!");
        }
        if ((user.getEmail() == null || user.getEmail().isBlank())
                && (user.getPhone() == null || user.getPhone().isBlank())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập ít nhất 1 Email hoặc Số điện thoại!");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && appUserRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()
                && appUserRepository.existsByPhone(user.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && !isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email phải có đuôi @gmail.com!");
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()
                && !isValidPhone(user.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }

        user.setPassword("{noop}" + user.getPassword());

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy role: " + roleName));
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN — QUẢN LÝ USER
    // ─────────────────────────────────────────────────────────────

    public void lockUser(Integer id) {
        AppUser user = getUserById(id);
        user.setEnabled(false);
        appUserRepository.save(user);
    }

    public void unlockUser(Integer id) {
        AppUser user = getUserById(id);
        user.setEnabled(true);
        appUserRepository.save(user);
    }

    public void addRole(Integer id, String roleName) {
        AppUser user = getUserById(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy role: " + roleName));

        if (user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User đã có role: " + roleName);
        }

        user.getRoles().add(role);
        appUserRepository.save(user);
    }

    public void removeRole(Integer id, String roleName) {
        AppUser user = getUserById(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy role: " + roleName));

        if (!user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User không có role: " + roleName);
        }
        if (user.getRoles().size() == 1) {
            throw new IllegalStateException("User phải có ít nhất 1 role!");
        }
        // Không cho xóa ROLE_OWNER nếu đây là Owner cuối cùng của hệ thống —
        // tránh trường hợp hệ thống mất sạch owner (kể cả khi chính owner đó
        // tự thao tác lên chính mình và vẫn còn role khác như ROLE_ADMIN).
        if (roleName.equals("ROLE_OWNER") && appUserRepository.countByRoles_Name("ROLE_OWNER") <= 1) {
            throw new IllegalStateException("Hệ thống phải có ít nhất 1 Owner!");
        }

        user.getRoles().remove(role);
        appUserRepository.save(user);
    }

    // ─────────────────────────────────────────────────────────────
    // CUSTOMER — TỰ QUẢN LÝ PROFILE
    // ─────────────────────────────────────────────────────────────

    public void updateProfile(String username, AppUser details) {
        AppUser user = getUserByUsername(username);

        String newEmail = details.getEmail();
        String newPhone = details.getPhone();

        if ((newEmail == null || newEmail.isBlank())
                && (newPhone == null || newPhone.isBlank())) {
            throw new IllegalArgumentException(
                    "Vui lòng giữ ít nhất Email hoặc Số điện thoại!");
        }
        if (newEmail != null && newEmail.isBlank()) newEmail = null;
        if (newPhone != null && newPhone.isBlank()) newPhone = null;

        if (newEmail != null && !newEmail.equals(user.getEmail())
                && appUserRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        if (newPhone != null && !newPhone.equals(user.getPhone())
                && appUserRepository.existsByPhone(newPhone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
        }
        if (details.getEmail() != null && !details.getEmail().isBlank()
                && !isValidEmail(details.getEmail())) {
            throw new IllegalArgumentException("Email phải có đuôi @gmail.com!");
        }
        if (details.getPhone() != null && !details.getPhone().isBlank()
                && !isValidPhone(details.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại phải đúng 10 chữ số!");
        }

        user.setEmail(newEmail);
        user.setPhone(newPhone);
        appUserRepository.save(user);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        AppUser user = getUserByUsername(username);

        String stored = user.getPassword().replace("{noop}", "");
        if (!oldPassword.equals(stored)) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng!");
        }
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu mới phải trên 6 ký tự và có ít nhất 1 chữ số!");
        }

        user.setPassword("{noop}" + newPassword);
        appUserRepository.save(user);
    }
}