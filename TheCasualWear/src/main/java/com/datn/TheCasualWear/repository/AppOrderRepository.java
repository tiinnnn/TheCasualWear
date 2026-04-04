package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppOrderRepository extends JpaRepository<AppOrder, Integer> {
    // Lịch sử đơn hàng của user
    List<AppOrder> findByCustomerIdOrderByOrderDateDesc(Integer customerId);

    // Lọc theo trạng thái (admin quản lý)
    List<AppOrder> findByStatus(OrderStatus status);

    // Tìm đơn hàng của user theo trạng thái
    List<AppOrder> findByCustomerIdAndStatus(Integer customerId, String status);

    @Query("SELECT o FROM AppOrder o ORDER BY " +
            "CASE o.status " +
            "WHEN 'PENDING'   THEN 1 " +
            "WHEN 'CONFIRMED' THEN 2 " +
            "WHEN 'SHIPPING'  THEN 3 " +
            "WHEN 'DELIVERED' THEN 4 " +
            "WHEN 'COMPLETED' THEN 5 " +
            "WHEN 'CANCELLED' THEN 6 " +
            "END ASC, o.orderDate ASC")
    List<AppOrder> findAllOrderedByStatus();

    @Query("SELECT o FROM AppOrder o WHERE " +
            "(:keyword IS NULL OR LOWER(o.customer.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY CASE o.status " +
            "WHEN 'PENDING'   THEN 1 " +
            "WHEN 'CONFIRMED' THEN 2 " +
            "WHEN 'SHIPPING'  THEN 3 " +
            "WHEN 'DELIVERED' THEN 4 " +
            "WHEN 'COMPLETED' THEN 5 " +
            "WHEN 'CANCELLED' THEN 6 " +
            "END ASC, o.orderDate ASC")
    Page<AppOrder> searchOrders(@Param("keyword") String keyword,
                                @Param("status") String status,
                                Pageable pageable);
}
