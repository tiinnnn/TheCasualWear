package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bảng "cộng thêm" (extension) cho AppUser — chỉ những user là nhân viên
// mới có 1 dòng ở đây. Việc phân quyền (ai được vào /admin, /cashier...)
// vẫn do Role đảm nhiệm như cũ; bảng này chỉ lưu THÊM dữ liệu nghiệp vụ
// riêng cho nhân viên (mã NV, ngày vào làm, chức vụ...) mà AppUser không có.
@Entity @Table(name = "employee")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Employee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode; // tự sinh NV0001..NV9999, không cho nhập tay

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(length = 500)
    private String note;
}