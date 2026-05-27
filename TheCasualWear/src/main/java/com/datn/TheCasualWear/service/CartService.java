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

    private final CartRepository         cartRepository;
    private final CartItemRepository     cartItemRepository;
    private final ProductRepository      productRepository;
    private final ProductVariantRepository variantRepository; // ✅ thêm mới

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
     * Cần truyền variantId để xác định đúng biến thể (size + màu).
     */
    @Transactional
    public void addToCart(AppUser user, Integer productId,
                          Integer variantId, Integer quantity) {
        // ✅ Lấy product để hiển thị tên trong thông báo lỗi
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm!"));

        // ✅ Lấy variant để check stock
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy biến thể sản phẩm!"));

        // ✅ Đảm bảo variant thuộc product
        if (!variant.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Biến thể không thuộc sản phẩm này!");
        }

        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                    "Biến thể này chỉ còn " + variant.getStock() + " trong kho!");
        }

        Cart cart = getOrCreateCart(user);

        // ✅ Check trùng theo cả productId + variantId
        cartItemRepository.findByCartIdAndProductIdAndVariantId(
                        cart.getId(), productId, variantId)
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
                            item.setProduct(product);
                            item.setVariant(variant); // ✅ set variant
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

        // ✅ Check stock từ variant
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
    // Giá = product.price + variant.priceAdjustment
    public long getTotalPrice(AppUser user) {
        return getCartItems(user).stream()
                .mapToLong(item -> {
                    java.math.BigDecimal base = item.getProduct().getPrice();
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