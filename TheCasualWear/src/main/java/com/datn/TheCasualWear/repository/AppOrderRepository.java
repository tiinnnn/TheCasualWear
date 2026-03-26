package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppOrderRepository extends JpaRepository<AppOrder, Integer> {
    // Lịch sử đơn hàng của user
    List<AppOrder> findByCustomerIdOrderByOrderDateDesc(Integer customerId);

    // Lọc theo trạng thái (admin quản lý)
    List<AppOrder> findByStatus(String status);

    // Tìm đơn hàng của user theo trạng thái
    List<AppOrder> findByCustomerIdAndStatus(Integer customerId, String status);
}
