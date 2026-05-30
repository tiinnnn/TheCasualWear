package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.OrderAssignment;
import com.datn.TheCasualWear.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Integer> {

    Optional<OrderAssignment> findByOrderId(Integer orderId);

    boolean existsByOrderId(Integer orderId);

    List<OrderAssignment> findByDeliveryStaffId(Integer deliveryId);

    List<OrderAssignment> findByDeliveryStaffIdAndStatus(
            Integer deliveryId, AssignmentStatus status);

    @Query("SELECT a FROM OrderAssignment a WHERE a.status NOT IN ('DELIVERED', 'FAILED')")
    List<OrderAssignment> findActiveAssignments();

    @Query("SELECT a FROM OrderAssignment a " +
            "WHERE a.deliveryStaff.id = :deliveryId " +
            "AND a.status NOT IN ('DELIVERED', 'FAILED')")
    List<OrderAssignment> findActiveByDeliveryStaff(
            @Param("deliveryId") Integer deliveryId);

    @Query("""
    SELECT a FROM OrderAssignment a
    WHERE a.deliveryStaff.id = :deliveryId
    AND (:status IS NULL OR a.status = :status)
    AND a.assignedAt >= :fromDate
    AND a.assignedAt <= :toDate
    ORDER BY a.assignedAt DESC
    """)
    List<OrderAssignment> findByDeliveryStaffFiltered(
            @Param("deliveryId") Integer deliveryId,
            @Param("status")     AssignmentStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate")     LocalDateTime toDate
    );
}