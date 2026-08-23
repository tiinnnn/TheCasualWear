package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.DuplicateVariantException;
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

    private final ProductVariantRepository   variantRepository;
    private final CartItemRepository         cartItemRepository;
    private final OrderDetailRepository      orderDetailRepository;
    private final VariantImageService        variantImageService;
    private final StockMovementLogRepository stockMovementLogRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private static final OrderStatus CANCELLED = OrderStatus.CANCELLED;

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

        // 1. Check trùng size + màu TRƯỚC — nếu trùng thì ném DuplicateVariantException
        // ngay, để controller gom vào danh sách chuyển sang trang nhập kho.
        variantRepository.findByProductAndSizeAndColor(
                product.getId(),
                variant.getSize()  != null ? variant.getSize().getId()  : null,
                variant.getColor() != null ? variant.getColor().getId() : null
        ).ifPresent(existing -> {
            throw new DuplicateVariantException(existing);
        });

        // 2. Chỉ check trùng SKU khi chắc chắn đây là variant mới (size+màu chưa tồn tại)
        if (variant.getSku() != null && variantRepository.existsBySku(variant.getSku())) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + variant.getSku());
        }

        // Luôn ép tồn kho + giá vốn ban đầu = 0, bất kể client gửi lên giá trị
        // gì — cả 2 giờ chỉ được thiết lập qua GoodsReceiptService (module
        // Quản lý kho): tồn kho cộng dồn, giá vốn tính bình quân gia quyền
        // theo từng lần nhập, đảm bảo audit trail đầy đủ trong stock_movement_log.
        variant.setStock(0);
        variant.setCostPrice(java.math.BigDecimal.ZERO);
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

        // BUG FIX: thiếu dọn các bảng con còn tham chiếu variant_id nhưng
        // không có ON DELETE CASCADE ở DB — cùng logic đã áp dụng ở
        // ProductService.hardDeleteProduct(), chỉ khác là ở đây chỉ có 1
        // variant nên xóa trực tiếp theo variantId, không cần loop.
        //   - order_detail: variant đã pass check hasActiveOrder ở trên,
        //     nghĩa là nếu còn order_detail nào trỏ tới nó thì chắc chắn
        //     thuộc đơn CANCELLED, nên xóa thẳng không sợ mất dữ liệu
        //     đơn đang hoạt động.
        //   - stock_movement_log: audit trail nhập/xuất kho.
        //   - goods_receipt_item: dòng chi tiết trong phiếu nhập kho
        //     (nguyên nhân gây lỗi FK__goods_rec__varia__690797E6 trước đó).
        orderDetailRepository.deleteByVariantId(id);
        stockMovementLogRepository.deleteByVariantId(id);
        goodsReceiptItemRepository.deleteByVariantId(id);

        variantRepository.delete(variant);
    }

    //THỐNG KÊ

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