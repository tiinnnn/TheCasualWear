package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.dto.GuestCartItem;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductSale;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Giỏ hàng cho khách vãng lai (chưa đăng nhập) — lưu trong HttpSession,
 * KHÔNG đụng tới CartItem/CartService hiện có (vốn gắn với AppUser đã login).
 * Song song, không thay thế.
 *
 * Giá lấy qua ProductSaleService.getEffectivePrice(Product) — vì
 * ProductVariant không tự có giá riêng, mọi variant của 1 sản phẩm dùng
 * chung product.getPrice() (xem ProductVariant.getActualPrice()).
 *
 * ⚠️ Còn 1 giả định chưa xác nhận: ProductVariantRepository.findById(Integer)
 * trả Optional<ProductVariant> — chuẩn Spring Data JPA nên khả năng đúng cao,
 * nhưng gửi ProductVariantRepository.java nếu muốn chắc chắn 100%.
 */
@Service
public class GuestCartService {

    private static final String SESSION_KEY = "guestCart";

    private final ProductVariantRepository productVariantRepository;
    private final ProductSaleService productSaleService;

    public GuestCartService(ProductVariantRepository productVariantRepository,
                            ProductSaleService productSaleService) {
        this.productVariantRepository = productVariantRepository;
        this.productSaleService = productSaleService;
    }

    @SuppressWarnings("unchecked")
    public List<GuestCartItem> getCart(HttpSession session) {
        List<GuestCartItem> cart = (List<GuestCartItem>) session.getAttribute(SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(SESSION_KEY, cart);
        }
        return cart;
    }

    public void addItem(HttpSession session, Integer variantId, Integer quantity) {
        List<GuestCartItem> cart = getCart(session);

        Optional<GuestCartItem> existing = cart.stream()
                .filter(i -> i.getVariantId().equals(variantId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
            return;
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
        Product product = variant.getProduct();

        BigDecimal unitPrice = productSaleService.getEffectivePrice(product);
        Optional<ProductSale> activeSale = productSaleService.getActiveSale(product);

        String imageUrl = (variant.getImages() != null && !variant.getImages().isEmpty())
                ? variant.getImages().get(0).getImageUrl()
                : null;

        GuestCartItem item = new GuestCartItem();
        item.setVariantId(variant.getId());
        item.setProductName(product.getName());
        item.setSizeName(variant.getSize() != null ? variant.getSize().getName() : null);
        item.setColorName(variant.getColor() != null ? variant.getColor().getName() : null);
        item.setImageUrl(imageUrl);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setOriginalPrice(activeSale.isPresent() ? product.getPrice() : null);
        item.setDiscountPercent(activeSale.map(ProductSale::getDiscountPercent).orElse(null));

        cart.add(item);
    }

    public void updateQuantity(HttpSession session, Integer variantId, Integer quantity) {
        getCart(session).stream()
                .filter(i -> i.getVariantId().equals(variantId))
                .findFirst()
                .ifPresent(i -> i.setQuantity(quantity));
    }

    public void removeItem(HttpSession session, Integer variantId) {
        getCart(session).removeIf(i -> i.getVariantId().equals(variantId));
    }

    public void clearCart(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    public BigDecimal getTotalPrice(HttpSession session) {
        return getCart(session).stream()
                .map(GuestCartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}