package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Role;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;

    public AppUserService(AppUserRepository appUserRepository,
                          RoleRepository roleRepository) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
    }


    public AppUser getUserById(Integer id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + id));
    }

    public AppUser getUserByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));
    }

    //  ĐĂNG KÝ

    // Helper validate password
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;
        return password.chars().anyMatch(Character::isDigit);
    }

    public void register(AppUser user) {
        if (appUserRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        // Validate password
        if (!isValidPassword(user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu phải có 6 ký tự và có ít nhất 1 chữ số!");
        }

        // Validate email hoặc phone phải có ít nhất 1
        if ((user.getEmail() == null || user.getEmail().isBlank())
                && (user.getPhone() == null || user.getPhone().isBlank())) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất 1 Email hoặc Số điện thoại!");
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()
                && appUserRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        user.setPassword("{noop}" + user.getPassword());

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role CUSTOMER"));
        user.getRoles().add(customerRole);
        appUserRepository.save(user);
    }

    // PHÍA ADMIN

    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role: " + roleName));

        if (user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User đã có role: " + roleName);
        }

        user.getRoles().add(role);
        appUserRepository.save(user);
    }

    public void removeRole(Integer id, String roleName) {
        AppUser user = getUserById(id);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role: " + roleName));

        if (!user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User không có role: " + roleName);
        }

        if (user.getRoles().size() == 1) {
            throw new IllegalStateException("User phải có ít nhất 1 role!");
        }

        user.getRoles().remove(role);
        appUserRepository.save(user);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll(); // dùng để hiển thị dropdown chọn role trong admin
    }

    //  PHÍA CUSTOMER


    public void updateProfile(String username, AppUser details) {
        AppUser user = getUserByUsername(username);

        String newEmail = details.getEmail();
        String newPhone = details.getPhone();

        if ((newEmail == null || newEmail.isBlank())
                && (newPhone == null || newPhone.isBlank())) {
            throw new IllegalArgumentException("Vui lòng giữ ít nhất Email hoặc Số điện thoại!");
        }
        if (newEmail != null && newEmail.isBlank()) {
            newEmail = null;
        }
        if (newPhone != null && newPhone.isBlank()) {
            newPhone = null;
        }
        if (newEmail != null && !newEmail.isBlank()
                && !newEmail.equals(user.getEmail())
                && appUserRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        user.setEmail(newEmail);
        user.setPhone(newPhone);
        appUserRepository.save(user);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        AppUser user = getUserByUsername(username);

        String stored = user.getPassword().replace("{noop}", "");
        if (!oldPassword.equals(stored)) {throw new IllegalArgumentException("Mật khẩu cũ không đúng!");}
        if (!isValidPassword(newPassword)) {throw new IllegalArgumentException("Mật khẩu mới phải trên 6 ký tự và có ít nhất 1 chữ số!");}
        user.setPassword("{noop}" + newPassword);
        appUserRepository.save(user);
    }

}