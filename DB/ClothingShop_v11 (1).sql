-- =====================================================================
-- ClothingShop_v11.sql (bản hoàn chỉnh)
-- Thêm cột shipping_fee vào app_order (phí ship cố định, fallback trước
-- khi tích hợp GHN API tính phí thật) + gán data mẫu thực tế.
--
-- Khác với bản gốc (chỉ set cứng 30000 cho mọi đơn), bản này:
--   1. Idempotent: chạy lại nhiều lần không lỗi nếu cột đã tồn tại.
--   2. Đơn mua tại quầy (order_type = 'COUNTER') → shipping_fee = 0,
--      vì khách nhận hàng trực tiếp, không phát sinh phí ship.
--   3. Đơn online (order_type = 'ONLINE') → phí ship tính theo khu vực
--      giao hàng (city của shipping_address_id), phản ánh đúng chi phí
--      thực tế: cùng khu vực kho (Hà Nội) rẻ hơn, các tỉnh xa hơn cao hơn.
-- =====================================================================

-- 1. Thêm cột shipping_fee (bỏ qua nếu đã tồn tại)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('app_order') AND name = 'shipping_fee'
)
BEGIN
    ALTER TABLE app_order ADD shipping_fee DECIMAL(18,2) NULL;
    PRINT 'Đã thêm cột app_order.shipping_fee';
END
ELSE
BEGIN
    PRINT 'Cột app_order.shipping_fee đã tồn tại, bỏ qua bước thêm cột.';
END
GO

-- 2. Đơn mua tại quầy (COUNTER) không phát sinh phí ship
UPDATE app_order
SET shipping_fee = 0
WHERE order_type = 'COUNTER'
  AND shipping_fee IS NULL;
GO

-- 3. Đơn online: phí ship theo khu vực giao hàng
--    (join qua shipping_address_id -> address.city)
--    Hà Nội   : 20,000đ  (gần kho)
--    Đà Nẵng  : 32,000đ  (miền Trung)
--    TP.HCM   : 38,000đ  (xa nhất)
--    Khác     : 30,000đ  (fallback)
UPDATE ao
SET ao.shipping_fee = CASE a.city
        WHEN N'Hà Nội'  THEN 20000
        WHEN N'Đà Nẵng' THEN 32000
        WHEN N'TP.HCM'  THEN 38000
        ELSE 30000
    END
FROM app_order ao
INNER JOIN address a ON a.id = ao.shipping_address_id
WHERE ao.shipping_fee IS NULL
  AND ao.order_type = 'ONLINE';
GO

-- 4. Phòng hờ: đơn nào vẫn còn NULL (thiếu shipping_address_id, dữ liệu lỗi...)
--    → gán fallback 30,000đ để không còn NULL sót lại.
UPDATE app_order
SET shipping_fee = 30000
WHERE shipping_fee IS NULL;
GO

-- 5. Kiểm tra lại kết quả
SELECT id, order_type, status, shipping_address_id, shipping_fee
FROM app_order
ORDER BY id;
GO
