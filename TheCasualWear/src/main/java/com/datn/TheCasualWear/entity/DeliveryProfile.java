package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "area", length = 100)
    private String area;  // Khu vuc hoat dong, vd: "Hoan Kiem, Dong Da"

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}