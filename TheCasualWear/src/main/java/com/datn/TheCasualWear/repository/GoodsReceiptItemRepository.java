package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Integer> {

    List<GoodsReceiptItem> findByGoodsReceiptId(Integer goodsReceiptId);

    // MỚI: dọn dòng nhập kho trước khi hard-delete variant/product.
    // ⚠️ Giả định field trong entity tên là "variant" (ManyToOne
    // ProductVariant) — nếu tên khác (VD: "productVariant"), sửa lại tên
    // method này cho khớp (Spring Data derive theo tên field, không phải
    // tên cột DB).
    void deleteByVariantId(Integer variantId);
}