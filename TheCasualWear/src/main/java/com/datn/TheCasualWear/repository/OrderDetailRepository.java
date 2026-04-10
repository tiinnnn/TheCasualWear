package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.OrderDetail;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    List<OrderDetail> findByOrderId(Integer orderId);
    void deleteByProduct(Product product);
    void deleteByOrderId(Integer orderId);
    // Kiểm tra có order active không (trừ CANCELLED)
    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrderDetail od WHERE od.product.id = :productId " +
            "AND od.order.status != :status")
    boolean existsByProductIdAndOrderStatusNot(@Param("productId") Integer productId,
                                               @Param("status") OrderStatus status);

    // Xóa order_detail của đơn CANCELLED
    @Modifying
    @Transactional
    @Query("DELETE FROM OrderDetail od WHERE od.product.id = :productId " +
            "AND od.order.status = com.datn.TheCasualWear.enums.OrderStatus.CANCELLED")
    void deleteByProductId(@Param("productId") Integer productId);
}
