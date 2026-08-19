package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
@EqualsAndHashCode(of = "id")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── MỚI: tính năng "Cashier tạo tài khoản trước cho khách" ─────────────
    // Tài khoản do cashier tạo sẽ có enabled=false + activationToken khác
    // null cho tới khi khách tự bấm link trong email và đặt mật khẩu thật
    // (xem CashierAccountService.activateAccount). Sau khi kích hoạt xong,
    // cả 2 field này được set về null lại.
    // Cũng chính 2 field này là "dấu hiệu" để AccountCleanupScheduler nhận
    // biết đây là tài khoản đang chờ kích hoạt (không cần thêm cột riêng để
    // đánh dấu nguồn gốc tài khoản).
    @Column(name = "activation_token", unique = true, length = 100)
    private String activationToken;

    @Column(name = "activation_expires_at")
    private LocalDateTime activationExpiresAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private List<Address> addresses = new ArrayList<>();

    @OneToOne(mappedBy = "customer")
    private Cart cart;
}