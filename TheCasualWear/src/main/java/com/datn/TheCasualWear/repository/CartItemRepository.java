package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
}
