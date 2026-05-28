package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository           cartRepository;
    private final CartItemRepository       cartItemRepository;
    private final ProductVariantRepository variantRepository;

    // Lấy hoặc tạo mới cart cho user
    public Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByCustomer(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(user);
                    return cartRepository.save(cart);
                });
    }

    // Lấy danh sách item trong giỏ
    public List<CartItem> getCartItems(AppUser user) {
        Cart cart = getOrCreateCart(user);
        return cartItemRepository.findByCartId(cart.getId());
    }

    /**
     * Thêm sản phẩm vào giỏ.
     * Chỉ cần variantId — product được lấy qua variant.getProduct().
     */
    @Transactional
    public void addToCart(AppUser user, Integer variantId, Integer quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy biến thể sản phẩm!"));

        // Lấy product qua variant, kiểm tra còn active không
        Product product = variant.getProduct();
        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new ResourceNotFoundException("Sản phẩm không còn tồn tại!");
        }

        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                    "Biến thể này chỉ còn " + variant.getStock() + " trong kho!");
        }

        Cart cart = getOrCreateCart(user);

        // Check trùng chỉ cần theo variantId
        cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
                .ifPresentOrElse(
                        existingItem -> {
                            int newQty = existingItem.getQuantity() + quantity;
                            if (newQty > variant.getStock()) {
                                throw new IllegalStateException(
                                        "Biến thể này chỉ còn " + variant.getStock() + " trong kho!");
                            }
                            existingItem.setQuantity(newQty);
                            cartItemRepository.save(existingItem);
                        },
                        () -> {
                            CartItem item = new CartItem();
                            item.setCart(cart);
                            item.setVariant(variant);
                            item.setQuantity(quantity);
                            cartItemRepository.save(item);
                        }
                );
    }

    // Cập nhật số lượng
    @Transactional
    public void updateQuantity(AppUser user, Integer cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy item!"));

        if (!item.getCart().getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException(
                    "Bạn không có quyền thay đổi giỏ hàng này!");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }

        ProductVariant variant = item.getVariant();
        if (quantity > variant.getStock()) {
            throw new IllegalStateException(
                    "Biến thể này chỉ còn " + variant.getStock() + " trong kho!");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    // Xóa 1 item khỏi giỏ
    public void removeItem(AppUser user, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy item!"));

        if (!item.getCart().getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền xóa item này!");
        }

        cartItemRepository.delete(item);
    }

    // Xóa toàn bộ giỏ (sau khi đặt hàng xong)
    @Transactional
    public void clearCart(AppUser user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    // Tính tổng tiền giỏ hàng
    // Giá = variant.product.price + variant.priceAdjustment
    public long getTotalPrice(AppUser user) {
        return getCartItems(user).stream()
                .mapToLong(item -> {
                    java.math.BigDecimal base = item.getVariant().getProduct().getPrice();
                    java.math.BigDecimal adj  = item.getVariant().getPriceAdjustment() != null
                            ? item.getVariant().getPriceAdjustment()
                            : java.math.BigDecimal.ZERO;
                    return base.add(adj)
                            .multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
                            .longValue();
                })
                .sum();
    }

    public int getCartItemCount(AppUser user) {
        return getCartItems(user).stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}