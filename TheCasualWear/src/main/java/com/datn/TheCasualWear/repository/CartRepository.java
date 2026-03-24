package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Integer> {
}
