package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
}
