package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Color;
import com.datn.TheCasualWear.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {


    List<ProductVariant> findByProductId(Integer productId);

    List<ProductVariant> findByProductIdAndStockGreaterThan(Integer productId, int stock);


    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Integer id);

    // Tìm đúng 1 variant (add to cart / validate trùng)

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId " +
            "AND (:sizeId IS NULL OR v.size.id = :sizeId) " +
            "AND (:colorId IS NULL OR v.color.id = :colorId)")
    Optional<ProductVariant> findByProductAndSizeAndColor(
            @Param("productId") Integer productId,
            @Param("sizeId")    Integer sizeId,
            @Param("colorId")   Integer colorId);

    @Query("SELECT DISTINCT v.color FROM ProductVariant v " +
            "WHERE v.product.id = :productId AND v.stock > 0")
    List<Color> findAvailableColorsByProduct(@Param("productId") Integer productId);


    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId " +
            "AND v.color.id = :colorId AND v.stock > 0")
    List<ProductVariant> findByProductAndColor(
            @Param("productId") Integer productId,
            @Param("colorId")   Integer colorId);

    List<ProductVariant> findByStock(int stock);

    List<ProductVariant> findByStockGreaterThanAndStockLessThan(int min, int max);

    boolean existsBySizeId(Integer sizeId);
    boolean existsByColorId(Integer colorId);

    // ── MỚI: tìm kiếm variant cho màn hình bán hàng tại quầy (Cashier) ────
    @Query("SELECT v FROM ProductVariant v WHERE v.stock > 0 AND (" +
            "LOWER(v.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ProductVariant> searchAvailableByKeyword(@Param("keyword") String keyword);
}