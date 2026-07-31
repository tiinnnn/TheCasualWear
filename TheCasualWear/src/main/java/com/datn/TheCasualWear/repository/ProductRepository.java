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

    // Shop: tìm SP còn ít nhất 1 variant có stock > 0
    // Lưu ý: keyword truyền vào phải đã được bọc sẵn "%...%" ở tầng Service.
    // Không dùng CONCAT() trong JPQL vì Hibernate + SQL Server dialect sẽ sinh
    // ra CAST(? AS VARCHAR(MAX)) làm hỏng ký tự tiếng Việt có dấu khi so khớp LIKE.
    @Query("SELECT p FROM Product p " +
            "WHERE p.isDeleted = false " +
            "AND EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND v.stock > 0) " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(:keyword)) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchProducts(@Param("keyword")    String keyword,
                                 @Param("categoryId") Integer categoryId,
                                 Pageable pageable);

    // Trang chủ: 8 SP mới nhất còn hàng
    @Query("SELECT p FROM Product p " +
            "WHERE p.isDeleted = false " +
            "AND EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p AND v.stock > 0) " +
            "ORDER BY p.createdAt DESC")
    List<Product> findTop8Newest(Pageable pageable);

    // keyword đã được bọc sẵn "%...%" ở tầng Service
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(:keyword))")
    Page<Product> searchProductsForAdmin(@Param("keyword") String keyword, Pageable pageable);

    Optional<Product> findByIdAndIsDeletedFalse(Integer id);
    List<Product> findByIsDeletedFalse();
    List<Product> findByIsDeletedTrue();
}