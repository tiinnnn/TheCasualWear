package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.ProductSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductSaleRepository extends JpaRepository<ProductSale, Integer> {

    List<ProductSale> findByProductId(Integer productId);

    // Sale đang chạy tại thời điểm hiện tại cho 1 sản phẩm
    @Query("""
        SELECT s FROM ProductSale s
        WHERE s.product.id = :productId
          AND s.isActive = true
          AND :now BETWEEN s.startDate AND s.endDate
        """)
    Optional<ProductSale> findActiveSale(@Param("productId") Integer productId,
                                         @Param("now") LocalDateTime now);

    // Tất cả sale đang chạy ngay bây giờ, cho nhiều sản phẩm cùng lúc
    // (dùng để hiển thị badge "-X%" ở trang listing mà không phải
    // query từng sản phẩm 1 lần)
    @Query("""
        SELECT s FROM ProductSale s
        WHERE s.isActive = true
          AND :now BETWEEN s.startDate AND s.endDate
        """)
    List<ProductSale> findAllCurrentlyRunning(@Param("now") LocalDateTime now);

    // Kiểm tra trùng lịch khi tạo/sửa sale cho cùng sản phẩm.
    // excludeId = -1 khi tạo mới (không có gì để loại trừ).
    @Query("""
        SELECT COUNT(s) > 0 FROM ProductSale s
        WHERE s.product.id = :productId
          AND s.isActive = true
          AND s.id <> :excludeId
          AND s.startDate <= :endDate
          AND s.endDate >= :startDate
        """)
    boolean existsOverlapping(@Param("productId") Integer productId,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("excludeId") Integer excludeId);
}