package com.datn.TheCasualWear.enums;

public enum StockMovementType {
    IMPORT,   // Nhập kho từ nhà cung cấp
    SALE,     // Bán hàng (trừ kho) — cả ONLINE và COUNTER
    CANCEL,   // Hủy đơn / hủy đơn quầy → hoàn kho
    RETURN,   // Khách trả hàng (returnOrder, restock = true) → hoàn kho
    ADJUST    // Admin/Owner sửa tay số tồn (kiểm kho, điều chỉnh thủ công)
}
