package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    // Lấy toàn bộ wishlist của user (kèm product, tránh N+1)
    @Query("""
        SELECT w FROM Wishlist w
        JOIN FETCH w.product p
        WHERE w.user.id = :userId
          AND p.isDeleted = false
        ORDER BY w.addedAt DESC
    """)
    List<Wishlist> findByUserId(@Param("userId") Integer userId);

    // Kiểm tra sản phẩm đã được yêu thích chưa
    boolean existsByUserIdAndProductId(Integer userId, Integer productId);

    Optional<Wishlist> findByUserIdAndProductId(Integer userId, Integer productId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
    void deleteByUserIdAndProductId(
            @Param("userId") Integer userId,
            @Param("productId") Integer productId
    );

    long countByProductId(Integer productId);

    void deleteAllByUserId(Integer userId);
}