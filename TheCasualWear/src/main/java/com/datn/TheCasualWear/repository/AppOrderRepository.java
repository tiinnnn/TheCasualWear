package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppOrderRepository extends JpaRepository<AppOrder, Integer> {

    List<AppOrder> findByCustomerIdOrderByOrderDateDesc(Integer customerId);

    List<AppOrder> findByStatus(OrderStatus status);

    List<AppOrder> findByCustomerIdAndStatus(Integer customerId, String status);

    @Query("SELECT o FROM AppOrder o ORDER BY " +
            "CASE o.status " +
            "WHEN 'PENDING'   THEN 1 " +
            "WHEN 'CONFIRMED' THEN 2 " +
            "WHEN 'SHIPPING'  THEN 3 " +
            "WHEN 'DELIVERED' THEN 4 " +
            "WHEN 'COMPLETED' THEN 5 " +
            "WHEN 'CANCELLED' THEN 6 " +
            "END ASC, o.orderDate DESC")
    List<AppOrder> findAllOrderedByStatus();

    @Query("SELECT o FROM AppOrder o LEFT JOIN o.customer c WHERE " +
            "(:keyword  IS NULL OR LOWER(c.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status   IS NULL OR o.status     = :status) " +
            "AND (:fromDate IS NULL OR o.orderDate >= :fromDate) " +
            "AND (:toDate   IS NULL OR o.orderDate <= :toDate) " +
            "ORDER BY CASE o.status " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.PENDING   THEN 1 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.CONFIRMED THEN 2 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.SHIPPING  THEN 3 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.DELIVERED THEN 4 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.COMPLETED THEN 5 " +
            "WHEN com.datn.TheCasualWear.enums.OrderStatus.CANCELLED THEN 6 " +
            "END ASC, o.orderDate DESC")
    Page<AppOrder> searchOrders(@Param("keyword")  String        keyword,
                                @Param("status")   OrderStatus   status,
                                @Param("fromDate") LocalDateTime fromDate,
                                @Param("toDate")   LocalDateTime toDate,
                                Pageable pageable);

    // ── MỚI: đơn bán tại quầy của 1 cashier trong khoảng thời gian gần đây ──
    @Query("SELECT o FROM AppOrder o WHERE o.orderType = com.datn.TheCasualWear.enums.OrderType.COUNTER " +
            "AND o.cashier.id = :cashierId AND o.orderDate >= :fromDate " +
            "ORDER BY o.orderDate DESC")
    List<AppOrder> findRecentCounterOrdersByCashier(@Param("cashierId") Integer cashierId,
                                                    @Param("fromDate") LocalDateTime fromDate);
}