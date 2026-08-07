package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductSale;
import com.datn.TheCasualWear.repository.ProductSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSaleService {

    private final ProductSaleRepository saleRepository;

    // ── TÍNH GIÁ (dùng ở CartService, CounterCart, trang shop...) ──────────

    // Giá sau sale (nếu đang có sale chạy), fallback về giá gốc
    public BigDecimal getEffectivePrice(Product product) {
        return getActiveSale(product)
                .map(sale -> applyDiscount(product.getPrice(), sale.getDiscountPercent()))
                .orElse(product.getPrice());
    }

    public Optional<ProductSale> getActiveSale(Product product) {
        return saleRepository.findActiveSale(product.getId(), LocalDateTime.now());
    }

    // Map<productId, ProductSale đang chạy> — dùng cho trang listing để
    // tránh N+1 query khi hiển thị nhiều sản phẩm cùng lúc.
    public Map<Integer, ProductSale> getActiveSalesByProductIds(List<Integer> productIds) {
        LocalDateTime now = LocalDateTime.now();
        return saleRepository.findAllCurrentlyRunning(now).stream()
                .filter(s -> productIds.contains(s.getProduct().getId()))
                .collect(Collectors.toMap(s -> s.getProduct().getId(), Function.identity()));
    }

    // Toàn bộ sản phẩm đang có sale chạy (dùng cho trang clearance + mục
    // "Đang sale" trên trang chủ). Bỏ sản phẩm đã bị ẩn/xóa mềm, và loại
    // trùng nếu 1 sản phẩm vô tình có nhiều bản ghi sale đang chạy cùng lúc
    // (không nên xảy ra do validate overlap, nhưng phòng hờ dữ liệu cũ).
    public List<Product> getProductsOnSale() {
        return saleRepository.findAllCurrentlyRunning(LocalDateTime.now()).stream()
                .map(ProductSale::getProduct)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .distinct()
                .toList();
    }

    private BigDecimal applyDiscount(BigDecimal price, BigDecimal percent) {
        BigDecimal discount = price.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.subtract(discount);
    }

    // ── ADMIN CRUD ───────────────────────────────────────────────────────

    public List<ProductSale> getSalesByProduct(Integer productId) {
        return saleRepository.findByProductId(productId);
    }

    public ProductSale getSaleById(Integer id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt sale!"));
    }

    @Transactional
    public ProductSale createSale(Product product, BigDecimal discountPercent,
                                  LocalDateTime start, LocalDateTime end) {
        validateSaleInput(discountPercent, start, end);
        if (saleRepository.existsOverlapping(product.getId(), start, end, -1)) {
            throw new IllegalArgumentException(
                    "Sản phẩm này đã có sale khác trùng thời gian!");
        }
        ProductSale sale = new ProductSale();
        sale.setProduct(product);
        sale.setDiscountPercent(discountPercent);
        sale.setStartDate(start);
        sale.setEndDate(end);
        return saleRepository.save(sale);
    }

    @Transactional
    public ProductSale updateSale(Integer saleId, BigDecimal discountPercent,
                                  LocalDateTime start, LocalDateTime end) {
        validateSaleInput(discountPercent, start, end);
        ProductSale sale = getSaleById(saleId);
        if (saleRepository.existsOverlapping(sale.getProduct().getId(), start, end, saleId)) {
            throw new IllegalArgumentException(
                    "Sản phẩm này đã có sale khác trùng thời gian!");
        }
        sale.setDiscountPercent(discountPercent);
        sale.setStartDate(start);
        sale.setEndDate(end);
        return saleRepository.save(sale);
    }

    private void validateSaleInput(BigDecimal discountPercent, LocalDateTime start, LocalDateTime end) {
        if (discountPercent == null
                || discountPercent.compareTo(BigDecimal.ZERO) <= 0
                || discountPercent.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm giá phải trong khoảng 0-90%!");
        }
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu!");
        }
    }

    @Transactional
    public void deactivateSale(Integer saleId) {
        ProductSale sale = getSaleById(saleId);
        sale.setIsActive(false);
        saleRepository.save(sale);
    }

    @Transactional
    public void deleteSale(Integer saleId) {
        saleRepository.delete(getSaleById(saleId));
    }

    // ── JOB DỌN DẸP (gọi từ SaleScheduler) ──────────────────────────────

    // Set is_active=false cho các sale đã qua end_date. Không ảnh hưởng đến
    // việc tính giá (getEffectivePrice/getActiveSale đã tự loại theo
    // end_date rồi) — chỉ để trạng thái hiển thị trong
    // admin/product/sales.html gọn hơn theo thời gian, tránh tồn đọng danh
    // sách sale "Chưa/hết hạn" mãi mãi.
    @Transactional
    public int deactivateExpiredSales() {
        List<ProductSale> expired = saleRepository.findExpiredButStillActive(LocalDateTime.now());
        expired.forEach(s -> s.setIsActive(false));
        saleRepository.saveAll(expired);
        return expired.size();
    }
}