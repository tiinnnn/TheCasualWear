-- Migration: thêm cột original_price vào order_detail để admin biết đơn hàng
-- có được mua lúc đang sale hay không, mà không phụ thuộc vào việc bản ghi
-- product_sale gốc còn tồn tại hay không (không cần chặn xóa sale cũ).
--
-- original_price = giá gốc của sản phẩm tại thời điểm đặt hàng.
-- price (cột đã có sẵn)  = giá thực khách trả (đã áp sale nếu có).
-- original_price == price  → mua giá thường, không sale.
-- original_price >  price  → mua lúc đang có sale.


ALTER TABLE order_detail
    ADD original_price DECIMAL(18,2) NULL;

-- Dữ liệu cũ (trước khi có tính năng sale): coi như không có sale,
-- original_price = price luôn.
UPDATE order_detail
SET original_price = price
WHERE original_price IS NULL;

ALTER TABLE order_detail
    ALTER COLUMN original_price DECIMAL(18,2) NOT NULL;
