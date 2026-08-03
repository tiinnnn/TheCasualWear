-- ============================================================
--  ClothingShop – Migration: Module Quản lý kho
--  Thêm: goods_receipt, goods_receipt_item, stock_movement_log
--  Chạy sau khi đã có ClothingShop_v6.sql
-- ============================================================

USE ClothingShop;
GO

-- ============================================================
--  PHIẾU NHẬP KHO
-- ============================================================

-- Phiếu nhập kho (header)
-- supplier_name: chỉ lưu tên NCC dạng text, không tách bảng riêng
-- created_by: nhân viên (ADMIN/OWNER) thực hiện nhập kho
CREATE TABLE goods_receipt (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    code          NVARCHAR(30)  NOT NULL UNIQUE,        -- vd: PN-20260803-001
    supplier_name NVARCHAR(150) NOT NULL,
    note          NVARCHAR(500) NULL,
    created_by    INT           NOT NULL REFERENCES app_user(id),
    created_at    DATETIME      NOT NULL DEFAULT GETDATE(),
    total_amount  DECIMAL(18,2) NOT NULL DEFAULT 0       -- tổng giá trị phiếu (tính từ item)
);

-- Chi tiết phiếu nhập kho (từng dòng variant)
CREATE TABLE goods_receipt_item (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    goods_receipt_id  INT           NOT NULL REFERENCES goods_receipt(id),
    variant_id        INT           NOT NULL REFERENCES product_variant(id),
    quantity          INT           NOT NULL CHECK (quantity > 0),
    unit_cost_price   DECIMAL(18,2) NOT NULL DEFAULT 0   -- giá nhập tại thời điểm nhập (snapshot)
);

-- ============================================================
--  LỊCH SỬ BIẾN ĐỘNG KHO (audit trail)
-- ============================================================

-- Mỗi lần stock của 1 variant thay đổi (do nhập kho, bán hàng, hủy đơn,
-- hoặc admin sửa tay) đều ghi 1 dòng ở đây.
-- change_qty: dương = tăng tồn, âm = giảm tồn
-- balance_after: số tồn SAU khi áp dụng thay đổi này (để tra cứu nhanh, khỏi tính lại)
-- ref_type/ref_id: tham chiếu tới bản ghi gốc gây ra biến động (linh hoạt, không FK cứng
--                  vì có thể trỏ tới nhiều bảng khác nhau: goods_receipt, app_order...)
CREATE TABLE stock_movement_log (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    variant_id     INT           NOT NULL REFERENCES product_variant(id),
    change_type    NVARCHAR(20)  NOT NULL,   -- 'IMPORT' | 'SALE' | 'RETURN' | 'ADJUST' | 'CANCEL'
    change_qty     INT           NOT NULL,
    balance_after  INT           NOT NULL,
    ref_type       NVARCHAR(30)  NULL,       -- 'GOODS_RECEIPT' | 'ORDER' | 'MANUAL'
    ref_id         INT           NULL,       -- id của goods_receipt hoặc app_order tương ứng
    note           NVARCHAR(255) NULL,
    created_by     INT           NULL REFERENCES app_user(id),
    created_at     DATETIME      NOT NULL DEFAULT GETDATE()
);

-- Index phục vụ tra cứu lịch sử theo variant hoặc theo ngày
CREATE INDEX IX_stock_movement_variant ON stock_movement_log(variant_id);
CREATE INDEX IX_stock_movement_created_at ON stock_movement_log(created_at);
GO
