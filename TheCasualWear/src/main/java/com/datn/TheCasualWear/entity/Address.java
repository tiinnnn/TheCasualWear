package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    // MỚI (4.5): mã định danh riêng của GHN — khác hệ mã của
    // provinces.open-api.vn đang dùng cho city/district ở trên. Bắt buộc
    // phải có đủ 3 mã này mới gọi được GHN Calculate Fee; NULL nếu địa chỉ
    // được tạo trước khi có tính năng này hoặc GHN master-data lỗi lúc lưu
    // — GhnService sẽ fallback về phí region-based khi thiếu.
    @Column(name = "ghn_province_id")
    private Integer ghnProvinceId;

    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Column(name = "ghn_ward_code", length = 20)
    private String ghnWardCode;

    @Column(name = "country", length = 100)
    private String country = "Vietnam";

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}