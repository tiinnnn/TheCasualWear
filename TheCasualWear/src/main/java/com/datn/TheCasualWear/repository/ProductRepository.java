package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // ==================== PHÍA USER ====================

    // Trang shop: search + sort (stock > 0, chưa xóa)
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.stock > 0 " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("keyword") String keyword, Sort sort);

    // Trang chủ: 8 sản phẩm mới nhất
    List<Product> findTop8ByIsDeletedFalseAndStockGreaterThanOrderByCreatedAtDesc(Integer stock);

    // ==================== PHÍA ADMIN ====================

    // Danh sách chưa xóa (có thể search theo tên)
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProductsForAdmin(@Param("keyword") String keyword);

    List<Product> findByIsDeletedFalse();   // lấy tất cả không cần search
    List<Product> findByIsDeletedTrue();    // danh sách đã xóa mềm

    // ==================== DÙNG CHUNG ====================

    Optional<Product> findByIdAndIsDeletedFalse(Integer id);

    // Validate SKU
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Integer id);

    boolean existsBySizeIdAndIsDeletedFalse(Integer sizeId);
    boolean existsByColorIdAndIsDeletedFalse(Integer colorId);
}