package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProductId(Integer productId);
    void deleteByProduct(Product product);
    // Kiểm tra URL đang dùng bởi SP khác không
    boolean existsByImageUrlAndProductIdNot(String imageUrl, Integer productId);
}
