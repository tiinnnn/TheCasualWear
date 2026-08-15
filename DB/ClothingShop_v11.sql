-- ClothingShop_v11.sql
-- Thêm cột shipping_fee vào app_order (phí ship cố định, fallback trước khi tích hợp GHN API tính phí)

ALTER TABLE app_order
    ADD shipping_fee DECIMAL(18,2) NULL;

-- Set giá trị mặc định cho các đơn đã tồn tại (không hồi tố giá trị chính xác, chỉ để không NULL)
UPDATE app_order
SET shipping_fee = 30000
WHERE shipping_fee IS NULL;
