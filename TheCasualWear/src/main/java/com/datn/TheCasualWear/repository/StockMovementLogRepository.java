package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.StockMovementLog;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementLogRepository extends JpaRepository<StockMovementLog, Integer> {

    List<StockMovementLog> findByVariantIdOrderByCreatedAtDesc(Integer variantId);

    List<StockMovementLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    // Tra cứu ngược: phiếu nhập / đơn hàng này đã tạo ra những dòng log nào
    List<StockMovementLog> findByRefTypeAndRefId(StockRefType refType, Integer refId);

    // Tìm kiếm nâng cao theo tên sản phẩm, mã SKU, loại biến động và khoảng ngày.
    // Mỗi tham số truyền null nghĩa là không lọc theo tiêu chí đó.
    @Query("""
            SELECT l FROM StockMovementLog l
            JOIN l.variant v
            JOIN v.product p
            WHERE (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
              AND (:sku IS NULL OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :sku, '%')))
              AND (:changeType IS NULL OR l.changeType = :changeType)
              AND (:from IS NULL OR l.createdAt >= :from)
              AND (:to IS NULL OR l.createdAt <= :to)
            ORDER BY l.createdAt DESC
            """)
    List<StockMovementLog> search(@Param("productName") String productName,
                                  @Param("sku") String sku,
                                  @Param("changeType") StockMovementType changeType,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}