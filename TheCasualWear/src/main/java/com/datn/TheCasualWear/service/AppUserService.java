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

    // ==================== DÙNG CHUNG ====================

    public AppUser getUserById(Integer id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + id));
    }

    public AppUser getUserByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));
    }

    // ==================== ĐĂNG KÝ ====================

    public void register(AppUser user) {
        if (appUserRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (appUserRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        user.setPassword("{noop}" + user.getPassword());
        // Gán role CUSTOMER mặc định
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role CUSTOMER"));
        user.getRoles().add(customerRole);

        appUserRepository.save(user);
    }

    // ==================== PHÍA ADMIN ====================

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

    // ==================== PHÍA CUSTOMER ====================

    public void updateProfile(String username, AppUser details) {
        AppUser user = getUserByUsername(username);

        if (!user.getEmail().equals(details.getEmail())
                && appUserRepository.existsByEmail(details.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        user.setEmail(details.getEmail());
        user.setPhone(details.getPhone());
        appUserRepository.save(user);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        AppUser user = getUserByUsername(username);

        // So sánh thẳng không encode
        if (!oldPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng!");
        }
        user.setPassword("{noop}" + newPassword);
        appUserRepository.save(user);
    }
}