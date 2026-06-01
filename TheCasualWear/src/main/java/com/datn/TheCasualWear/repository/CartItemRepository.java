package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);
    void deleteByCartId(Integer cartId);

    Optional<CartItem> findByCartIdAndVariantId(Integer cartId, Integer variantId);

    void deleteByVariantId(Integer variantId);
}