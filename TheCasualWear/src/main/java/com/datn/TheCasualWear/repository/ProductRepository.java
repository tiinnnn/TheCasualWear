package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    //  PHÍA USER

    // Trang shop: search + sort (stock > 0, chưa xóa)
    // Lấy 1 đại diện mỗi nhóm (tên + màu) cho trang shop
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.stock > 0 " +
            "AND p.id IN (" +
            "  SELECT MIN(p2.id) FROM Product p2 " +
            "  WHERE p2.isDeleted = false AND p2.stock > 0 " +
            "  GROUP BY p2.name, p2.color" +
            ") " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("categoryId") Integer categoryId,
                                 Pageable pageable);

    // Lấy tất cả size của 1 sản phẩm (cùng tên + màu)
    @Query("SELECT p FROM Product p WHERE p.name = :name " +
            "AND (:colorId IS NULL OR p.color.id = :colorId) " +
            "AND p.isDeleted = false " +
            "ORDER BY p.size.id ASC")
    List<Product> findVariantsByNameAndColor(@Param("name") String name,
                                             @Param("colorId") Integer colorId);
    // Trang chủ: 8 sản phẩm mới nhất
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.stock > 0 " +
            "AND p.id IN (" +
            "  SELECT MIN(p2.id) FROM Product p2 " +
            "  WHERE p2.isDeleted = false AND p2.stock > 0 " +
            "  GROUP BY p2.name, p2.color" +
            ") " +
            "ORDER BY p.createdAt DESC")
    List<Product> findTop8Newest(Pageable pageable);

    //  PHÍA ADMIN

    // Stock thấp
    List<Product> findByIsDeletedFalseAndStockLessThanAndStockGreaterThan(
            Integer maxStock, Integer minStock);

    List<Product> findByIsDeletedFalseAndStock(Integer stock);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProductsForAdmin(@Param("keyword") String keyword, Pageable pageable);


    List<Product> findByIsDeletedFalse();   // lấy tất cả không cần search
    List<Product> findByIsDeletedTrue();    // danh sách đã xóa mềm

    // DÙNG CHUNG

    Optional<Product> findByIdAndIsDeletedFalse(Integer id);

    // Validate SKU
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Integer id);

    boolean existsBySizeIdAndIsDeletedFalse(Integer sizeId);
    boolean existsByColorIdAndIsDeletedFalse(Integer colorId);
}