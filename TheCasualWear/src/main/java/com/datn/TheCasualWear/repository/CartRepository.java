package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByCustomerId(Integer customerId);
    Optional<Cart> findByCustomer(AppUser customer);
}
