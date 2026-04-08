package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

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

    // Thêm sản phẩm vào giỏ
    @Transactional
    public void addToCart(AppUser user, Integer productId, Integer quantity) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm!"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Sản phẩm chỉ còn " + product.getStock() + " trong kho!");
        }

        Cart cart = getOrCreateCart(user);

        // Kiểm tra sản phẩm đã có trong giỏ chưa
        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresentOrElse(
                        existingItem -> {
                            // Đã có → cộng thêm số lượng
                            int newQty = existingItem.getQuantity() + quantity;
                            if (newQty > product.getStock()) {
                                throw new IllegalStateException("Sản phẩm chỉ còn " + product.getStock() + " trong kho!");
                            }
                            existingItem.setQuantity(newQty);
                            cartItemRepository.save(existingItem);
                        },
                        () -> {
                            // Chưa có → thêm mới
                            CartItem item = new CartItem();
                            item.setCart(cart);
                            item.setProduct(product);
                            item.setQuantity(quantity);
                            cartItemRepository.save(item);
                        }
                );
    }

    // Cập nhật số lượng
    @Transactional
    public void updateQuantity(AppUser user, Integer cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy item!"));

        // Kiểm tra item có thuộc cart của user không
        if (!item.getCart().getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền thay đổi giỏ hàng này!");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }

        if (quantity > item.getProduct().getStock()) {
            throw new IllegalStateException("Sản phẩm chỉ còn " + item.getProduct().getStock() + " trong kho!");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    // Xóa 1 item khỏi giỏ
    public void removeItem(AppUser user, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy item!"));

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
    public long getTotalPrice(AppUser user) {
        return getCartItems(user).stream()
                .mapToLong(item -> item.getProduct().getPrice()
                        .multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
                        .longValue())
                .sum();
    }

    public int getCartItemCount(AppUser user) {
        return getCartItems(user).stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}