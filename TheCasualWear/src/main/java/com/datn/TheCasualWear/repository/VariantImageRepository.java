package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.VariantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface VariantImageRepository extends JpaRepository<VariantImage, Integer> {

    List<VariantImage> findByVariantIdOrderBySortOrderAsc(Integer variantId);

    @Transactional
    void deleteByVariantId(Integer variantId);
}