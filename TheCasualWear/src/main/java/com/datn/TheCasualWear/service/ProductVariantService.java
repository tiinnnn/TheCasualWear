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
    private final CartItemRepository       cartItemRepository;
    private final OrderDetailRepository    orderDetailRepository;
    private final ProductRepository        productRepository;
    private final VariantImageService      variantImageService;

    private static final OrderStatus CANCELLED = OrderStatus.CANCELLED;

    //DÙNG CHUNG

    public ProductVariant getVariantById(Integer id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy variant với id: " + id));
    }

    public List<ProductVariant> getVariantsByProduct(Integer productId) {
        return variantRepository.findByProductId(productId);
    }

    public List<ProductVariant> getAvailableVariants(Integer productId) {
        return variantRepository.findByProductIdAndStockGreaterThan(productId, 0);
    }

    public ProductVariant findVariant(Integer productId, Integer sizeId, Integer colorId) {
        return variantRepository.findByProductAndSizeAndColor(productId, sizeId, colorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy variant phù hợp!"));
    }

    //PHÍA ADMIN

    public ProductVariant createVariant(Product product, ProductVariant variant) {
        if (variant.getSku() != null && variantRepository.existsBySku(variant.getSku())) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + variant.getSku());
        }

        if (variant.getCostPrice() != null && product.getPrice() != null
                && variant.getCostPrice().compareTo(product.getPrice()) > 0) {
            throw new IllegalArgumentException("Giá vốn không được lớn hơn giá bán!");
        }

        variantRepository.findByProductAndSizeAndColor(
                product.getId(),
                variant.getSize()  != null ? variant.getSize().getId()  : null,
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

        boolean hasActiveOrder = orderDetailRepository.existsByVariantIdAndOrderStatusNot(
                id, CANCELLED);
        if (hasActiveOrder) {
            throw new IllegalStateException(
                    "Không thể xóa! Variant đang có trong đơn hàng.");
        }

        // Xóa cart items chứa variant này
        cartItemRepository.deleteByVariantId(id);

        try {
            variantImageService.deleteAllByVariant(id);
        } catch (Exception e) {
            // Không để lỗi Cloudinary chặn việc xóa variant
        }

        variantRepository.delete(variant);
    }

    // ==================== THỐNG KÊ ====================

    public List<ProductVariant> getLowStockVariants() {
        return variantRepository.findByStockGreaterThanAndStockLessThan(0, 5);
    }

    public List<ProductVariant> getOutOfStockVariants() {
        return variantRepository.findByStock(0);
    }

    public int getTotalStock(Integer productId) {
        return variantRepository.findByProductId(productId)
                .stream()
                .mapToInt(ProductVariant::getStock)
                .sum();
    }
}