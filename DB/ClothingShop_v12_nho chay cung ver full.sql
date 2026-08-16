-- ClothingShop_v12.sql
-- Thêm order_code (mã tra cứu công khai, thay ID tự tăng) và guest_email
-- cho luồng khách vãng lai (4.1, 4.2, 6.6)

ALTER TABLE app_order ADD order_code VARCHAR(12) NULL;
ALTER TABLE app_order ADD guest_email VARCHAR(100) NULL;
GO

-- Backfill mã đơn cho các đơn đã tồn tại (dạng: 8 ký tự hex viết hoa từ NEWID())
UPDATE app_order
SET order_code = UPPER(LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 8))
WHERE order_code IS NULL;
GO

-- Đảm bảo không trùng mã sau backfill (xác suất trùng cực thấp nhưng vẫn nên check)
-- Nếu câu SELECT dưới đây trả về dòng nào, chạy lại UPDATE ở trên cho các dòng đó
-- SELECT order_code, COUNT(*) FROM app_order GROUP BY order_code HAVING COUNT(*) > 1;

ALTER TABLE app_order ALTER COLUMN order_code VARCHAR(12) NOT NULL;
GO

CREATE UNIQUE INDEX UQ_app_order_order_code ON app_order(order_code);
GO
