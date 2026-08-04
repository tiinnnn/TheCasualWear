-- ============================================================
--  ClothingShop – Migration: Module Giao ca (Shift handover)
--  Chạy sau khi đã có ClothingShop_v6.sql + warehouse_migration.sql
-- ============================================================

USE ClothingShop;
GO

-- ============================================================
--  CA LÀM VIỆC CỦA CASHIER
-- ============================================================

CREATE TABLE shift (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    cashier_id      INT           NOT NULL REFERENCES app_user(id),

    opened_at       DATETIME      NOT NULL DEFAULT GETDATE(),
    closed_at       DATETIME      NULL,

    opening_cash    DECIMAL(18,2) NOT NULL DEFAULT 0,  -- tiền quỹ đầu ca (cashier tự đếm nhập)
    expected_cash   DECIMAL(18,2) NULL,                -- hệ thống tự tính lúc đóng ca
    actual_cash     DECIMAL(18,2) NULL,                -- cashier đếm thực tế lúc đóng ca
    cash_difference DECIMAL(18,2) NULL,                -- actual_cash - expected_cash

    status          NVARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- 'OPEN' | 'CLOSED'
    note            NVARCHAR(500) NULL
);

-- Mỗi cashier chỉ được có tối đa 1 ca đang OPEN tại một thời điểm.
-- Filtered unique index: chỉ áp dụng ràng buộc khi status = 'OPEN'.
CREATE UNIQUE INDEX UX_shift_cashier_open
    ON shift(cashier_id)
    WHERE status = 'OPEN';

CREATE INDEX IX_shift_cashier ON shift(cashier_id);
GO

-- ============================================================
--  GẮN ĐƠN HÀNG VÀO CA TƯƠNG ỨNG
-- ============================================================

ALTER TABLE app_order
    ADD shift_id INT NULL REFERENCES shift(id);

CREATE INDEX IX_app_order_shift ON app_order(shift_id);
GO
