package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Integer> {

    List<GoodsReceiptItem> findByGoodsReceiptId(Integer goodsReceiptId);
}
