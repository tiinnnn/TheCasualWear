package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
