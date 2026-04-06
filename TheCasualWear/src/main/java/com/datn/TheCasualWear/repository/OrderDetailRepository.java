package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.OrderDetail;
import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    List<OrderDetail> findByOrderId(Integer orderId);
    void deleteByProduct(Product product);
    void deleteByOrderId(Integer orderId);
}
