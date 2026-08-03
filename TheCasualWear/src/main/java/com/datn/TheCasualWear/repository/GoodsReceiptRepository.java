package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {

    List<GoodsReceipt> findAllByOrderByCreatedAtDesc();

    // Dùng để sinh số thứ tự trong mã phiếu (PN-yyyyMMdd-xxx)
    long countByCodeStartingWith(String prefix);
}
