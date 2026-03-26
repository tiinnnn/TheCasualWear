package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.CartItem;
import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);

    // Tìm item cụ thể trong giỏ (để cộng số lượng nếu đã có)
    Optional<CartItem> findByCartIdAndProductId(Integer cartId, Integer productId);

    void deleteByProduct(Product product);
    void deleteByCartId(Integer cartId); // xóa toàn bộ giỏ sau khi đặt hàng
}
