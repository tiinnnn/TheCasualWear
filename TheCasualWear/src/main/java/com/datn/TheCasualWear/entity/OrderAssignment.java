package com.datn.TheCasualWear.entity;

import com.datn.TheCasualWear.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_assignment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private AppOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private AppUser deliveryStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private AppUser assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AssignmentStatus status;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Column(name = "note", length = 255)
    private String note;

    @PrePersist
    public void prePersist() {
        this.assignedAt = LocalDateTime.now();
        if (this.status == null) this.status = AssignmentStatus.ASSIGNED;
    }
}