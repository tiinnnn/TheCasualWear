package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.CartItem;
import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);
    void deleteByCartId(Integer cartId);

    Optional<CartItem> findByCartIdAndVariantId(Integer cartId, Integer variantId);

    // dùng khi soft-delete product
    void deleteByProductId(Integer productId);

    // dùng khi xóa variant
    void deleteByVariantId(Integer variantId);

    // Dùng bởi CartService.addToCart() — tìm item trùng variant
    Optional<CartItem> findByCartIdAndProductIdAndVariantId(
            Integer cartId, Integer productId, Integer variantId);

}
