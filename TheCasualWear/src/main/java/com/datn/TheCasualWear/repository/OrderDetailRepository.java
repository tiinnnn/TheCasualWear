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

    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrderDetail od WHERE od.product.id = :productId " +
            "AND od.order.status != :status")
    boolean existsByProductIdAndOrderStatusNot(@Param("productId") Integer productId,
                                               @Param("status")    OrderStatus status);

    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrderDetail od WHERE od.variant.id = :variantId " +
            "AND od.order.status != :status")
    boolean existsByVariantIdAndOrderStatusNot(@Param("variantId") Integer variantId,
                                               @Param("status")    OrderStatus status);

    @Modifying @Transactional
    @Query("DELETE FROM OrderDetail od WHERE od.product.id = :productId " +
            "AND od.order.status = com.datn.TheCasualWear.enums.OrderStatus.CANCELLED")
    void deleteByProductId(@Param("productId") Integer productId);
}
