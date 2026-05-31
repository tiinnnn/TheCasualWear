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

    // Active assignment (ASSIGNED) cua 1 don
    @Query("SELECT a FROM OrderAssignment a WHERE a.order.id = :orderId " +
            "AND a.status = com.datn.TheCasualWear.enums.AssignmentStatus.ASSIGNED " +
            "ORDER BY a.assignedAt DESC")
    Optional<OrderAssignment> findActiveByOrderId(@Param("orderId") Integer orderId);

    // Tat ca lich su assignment cua 1 don
    List<OrderAssignment> findByOrderIdOrderByAssignedAtDesc(Integer orderId);

    // Dem so lan that bai cua 1 don
    @Query("SELECT COUNT(a) FROM OrderAssignment a WHERE a.order.id = :orderId " +
            "AND a.status = com.datn.TheCasualWear.enums.AssignmentStatus.FAILED")
    int countFailedByOrderId(@Param("orderId") Integer orderId);

    // Kiem tra don co assignment active khong
    @Query("SELECT COUNT(a) > 0 FROM OrderAssignment a WHERE a.order.id = :orderId " +
            "AND a.status = com.datn.TheCasualWear.enums.AssignmentStatus.ASSIGNED")
    boolean existsActiveByOrderId(@Param("orderId") Integer orderId);

    // Danh sach assignment cua 1 delivery voi filter
    @Query("SELECT a FROM OrderAssignment a " +
            "WHERE a.deliveryStaff.id = :deliveryId " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND a.assignedAt >= :fromDate " +
            "AND a.assignedAt <= :toDate " +
            "ORDER BY a.assignedAt DESC")
    List<OrderAssignment> findByDeliveryStaffFiltered(
            @Param("deliveryId") Integer deliveryId,
            @Param("status")     AssignmentStatus status,
            @Param("fromDate")   LocalDateTime fromDate,
            @Param("toDate")     LocalDateTime toDate);

    List<OrderAssignment> findByDeliveryStaffId(Integer deliveryId);
}