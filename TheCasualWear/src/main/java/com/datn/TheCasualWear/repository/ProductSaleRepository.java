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

    // ── DASHBOARD: sale bắt đầu từ :since trở lại đây, mới nhất trước ────
    // Dùng cho ProductSaleService.getSaleEffectiveness() — tránh quét toàn
    // bộ lịch sử sale khi bảng đã tích lũy nhiều năm dữ liệu.
    @Query("""
        SELECT s FROM ProductSale s
        WHERE s.startDate >= :since
        ORDER BY s.startDate DESC
        """)
    List<ProductSale> findRecentSales(@Param("since") LocalDateTime since);

    // Sale đã qua end_date nhưng is_active vẫn true — dùng cho job dọn dẹp
    // định kỳ (SaleScheduler). Việc tính giá không phụ thuộc vào is_active
    // của các bản ghi này (đã tự động loại theo end_date rồi), job này chỉ
    // để cập nhật trạng thái hiển thị cho gọn trong admin/product/sales.html.
    @Query("""
        SELECT s FROM ProductSale s
        WHERE s.isActive = true
          AND s.endDate < :now
        """)
    List<ProductSale> findExpiredButStillActive(@Param("now") LocalDateTime now);

    // ── SALE THEO ĐỢT (sale_batch) ──────────────────────────────────────

    // Toàn bộ product_sale thuộc 1 đợt — dùng để huỷ sớm cả đợt
    // (SaleBatchService.deactivateBatch)
    List<ProductSale> findBySaleBatchId(Integer saleBatchId);

    // Đếm số sản phẩm trong 1 đợt — dùng cho trang danh sách đợt sale,
    // tránh load cả entity chỉ để đếm.
    long countBySaleBatchId(Integer saleBatchId);

    // Batch còn dòng product_sale nào đang active không — dùng để phân biệt
    // "Đã huỷ sớm" (deactivateBatch() đã set is_active=false hết) với
    // "Đang chạy" (endDate batch vẫn còn ở tương lai nhưng chưa bị huỷ tay).
    // Cần thiết vì sale_batch KHÔNG có cột trạng thái riêng — trạng thái
    // thật nằm ở is_active của từng dòng product_sale con.
    boolean existsBySaleBatchIdAndIsActiveTrue(Integer saleBatchId);
}