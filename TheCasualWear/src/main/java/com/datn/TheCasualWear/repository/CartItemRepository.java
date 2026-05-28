package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);
    void deleteByCartId(Integer cartId);

    // ✅ Tìm item trùng theo variantId (không cần productId nữa)
    Optional<CartItem> findByCartIdAndVariantId(Integer cartId, Integer variantId);

    // ✅ Xóa cart item khi variant bị xóa
    void deleteByVariantId(Integer variantId);
}