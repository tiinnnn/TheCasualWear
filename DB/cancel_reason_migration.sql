-- ============================================================
--  ClothingShop – Migration: Lý do hủy/hoàn đơn hàng
--  Chạy sau khi đã có ClothingShop_v6.sql + warehouse_migration.sql
--  + shift_migration.sql
--
--  status hiện tại đã có sẵn giá trị 'CANCELLED' được set cho cả 2
--  trường hợp hủy VÀ hoàn hàng (bug ở OrderService.returnOrder cũ).
--  Sau khi deploy code mới (dùng status 'RETURNED' riêng), các đơn
--  RETURNED phát sinh MỚI sẽ tự đúng. Đơn CANCELLED cũ đã lỡ tạo do
--  hoàn hàng thì không thể phân biệt lại bằng script (không có cách
--  nào biết đơn nào từng là hoàn hàng), nên không cố migrate dữ liệu
--  cũ — chỉ áp dụng cho đơn phát sinh từ nay về sau.
-- ============================================================

USE ClothingShop;
GO

ALTER TABLE app_order
    ADD cancel_reason NVARCHAR(30)  NULL,
        cancel_note   NVARCHAR(255) NULL,
        cancelled_by  INT           NULL REFERENCES app_user(id),
        cancelled_at  DATETIME      NULL;
GO

CREATE INDEX IX_app_order_cancelled_by ON app_order(cancelled_by);
GO
