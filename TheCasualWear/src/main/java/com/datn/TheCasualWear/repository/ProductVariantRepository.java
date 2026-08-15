package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Color;
import com.datn.TheCasualWear.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    // Đã đổi sang phân trang để không load hết variants khớp keyword cùng lúc.
    @Query("SELECT v FROM ProductVariant v WHERE v.stock > 0 AND (" +
            "LOWER(v.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ProductVariant> searchAvailableByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ── MỚI: keyword rỗng ở POS → trả toàn bộ variant còn hàng (có phân trang)
    // thay vì list rỗng. Cùng điều kiện lọc (stock > 0) với searchAvailableByKeyword
    // để nhất quán giữa 2 nhánh tìm kiếm.
    @Query("SELECT v FROM ProductVariant v WHERE v.stock > 0")
    Page<ProductVariant> findAllAvailable(Pageable pageable);

    // ── MỚI: đọc variant có khóa PESSIMISTIC_WRITE (SELECT ... WITH UPDLOCK
    // trên SQL Server) — dùng khi giữ chỗ/hoàn chỗ kho cho giỏ POS, để 2
    // cashier thao tác cùng 1 variant sắp hết hàng không bị lệch số tồn.
    // Chỉ dùng bên trong 1 @Transactional ngắn (reserve/release 1 dòng),
    // không dùng cho các query đọc thông thường.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Integer id);
}