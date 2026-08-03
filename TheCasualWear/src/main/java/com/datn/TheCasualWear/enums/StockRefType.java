package com.datn.TheCasualWear.enums;

// Cho biết dòng log biến động kho xuất phát từ bảng nào,
// dùng chung với ref_id (id của goods_receipt hoặc app_order tương ứng).
public enum StockRefType {
    GOODS_RECEIPT,  // ref_id = goods_receipt.id
    ORDER,          // ref_id = app_order.id
    MANUAL          // admin sửa tay, không có bản ghi gốc tham chiếu
}
