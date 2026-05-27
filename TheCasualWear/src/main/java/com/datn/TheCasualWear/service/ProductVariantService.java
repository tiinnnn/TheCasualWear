package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private static final OrderStatus CANCELLED = OrderStatus.CANCELLED;

    // ==================== DÙNG CHUNG ====================

    public ProductVariant getVariantById(Integer id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy variant với id: " + id));
    }

    // Lấy tất cả variant của 1 sản phẩm
    public List<ProductVariant> getVariantsByProduct(Integer productId) {
        return variantRepository.findByProductId(productId);
    }

    // Lấy variant còn hàng của 1 sản phẩm
    public List<ProductVariant> getAvailableVariants(Integer productId) {
        return variantRepository.findByProductIdAndStockGreaterThan(productId, 0);
    }

    // Tìm variant theo product + size + color (dùng khi add to cart)
    public ProductVariant findVariant(Integer productId, Integer sizeId, Integer colorId) {
        return variantRepository.findByProductAndSizeAndColor(productId, sizeId, colorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy variant phù hợp!"));
    }

    // ==================== PHÍA ADMIN ====================

    public ProductVariant createVariant(Product product, ProductVariant variant) {
        // Validate SKU
        if (variant.getSku() != null && variantRepository.existsBySku(variant.getSku())) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + variant.getSku());
        }

        // Validate giá vốn không vượt giá bán
        if (variant.getCostPrice() != null && product.getPrice() != null
                && variant.getCostPrice().compareTo(product.getPrice()) > 0) {
            throw new IllegalArgumentException("Giá vốn không được lớn hơn giá bán!");
        }

        // Kiểm tra không trùng size + color trong cùng product
        variantRepository.findByProductAndSizeAndColor(
                product.getId(),
                variant.getSize() != null ? variant.getSize().getId() : null,
                variant.getColor() != null ? variant.getColor().getId() : null
        ).ifPresent(v -> {
            throw new IllegalArgumentException(
                    "Variant với size và màu này đã tồn tại cho sản phẩm này!");
        });

        variant.setProduct(product);
        return variantRepository.save(variant);
    }

    public ProductVariant updateVariant(Integer id, ProductVariant details) {
        ProductVariant variant = getVariantById(id);

        // Validate SKU không trùng với variant khác
        if (details.getSku() != null
                && variantRepository.existsBySkuAndIdNot(details.getSku(), id)) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + details.getSku());
        }

        variant.setSku(details.getSku());
        variant.setSize(details.getSize());
        variant.setColor(details.getColor());
        variant.setStock(details.getStock());
        variant.setCostPrice(details.getCostPrice());
        variant.setPriceAdjustment(details.getPriceAdjustment());
        return variantRepository.save(variant);
    }

    // Cập nhật stock (dùng khi nhập hàng)
    public ProductVariant updateStock(Integer id, Integer stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Tồn kho không được âm!");
        }
        ProductVariant variant = getVariantById(id);
        variant.setStock(stock);
        return variantRepository.save(variant);
    }

    @Transactional
    public void deleteVariant(Integer id) {
        ProductVariant variant = getVariantById(id);

        // Kiểm tra có trong order active không
        boolean hasActiveOrder = orderDetailRepository.existsByVariantIdAndOrderStatusNot(
                id, CANCELLED);
        if (hasActiveOrder) {
            throw new IllegalStateException(
                    "Không thể xóa! Variant đang có trong đơn hàng.");
        }

        // Xóa khỏi cart
        cartItemRepository.deleteByVariantId(id);

        // Xóa variant
        variantRepository.delete(variant);
    }

    // ==================== THỐNG KÊ ====================

    // Variants sắp hết hàng (0 < stock < 5)
    public List<ProductVariant> getLowStockVariants() {
        return variantRepository.findByStockGreaterThanAndStockLessThan(0, 5);
    }

    // Variants hết hàng
    public List<ProductVariant> getOutOfStockVariants() {
        return variantRepository.findByStock(0);
    }

    // Tổng tồn kho của 1 sản phẩm
    public int getTotalStock(Integer productId) {
        return variantRepository.findByProductId(productId)
                .stream()
                .mapToInt(ProductVariant::getStock)
                .sum();
    }
}