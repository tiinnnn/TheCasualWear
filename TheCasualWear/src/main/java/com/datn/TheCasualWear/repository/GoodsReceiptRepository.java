package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {

    List<GoodsReceipt> findAllByOrderByCreatedAtDesc();

    // Dùng để sinh số thứ tự trong mã phiếu (PN-yyyyMMdd-xxx)
    long countByCodeStartingWith(String prefix);

    // Tìm kiếm theo mã phiếu, nhà cung cấp và khoảng ngày tạo.
    // Mỗi tham số truyền null nghĩa là không lọc theo tiêu chí đó.
    @Query("""
            SELECT r FROM GoodsReceipt r
            WHERE (:code IS NULL OR LOWER(r.code) LIKE LOWER(CONCAT('%', :code, '%')))
              AND (:supplierName IS NULL OR LOWER(r.supplierName) LIKE LOWER(CONCAT('%', :supplierName, '%')))
              AND (:from IS NULL OR r.createdAt >= :from)
              AND (:to IS NULL OR r.createdAt <= :to)
            ORDER BY r.createdAt DESC
            """)
    List<GoodsReceipt> search(@Param("code") String code,
                              @Param("supplierName") String supplierName,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}