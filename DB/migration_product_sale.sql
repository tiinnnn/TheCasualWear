-- Migration: thêm bảng product_sale (sale/giảm giá theo lịch trình cho từng sản phẩm)
-- Chạy sau ClothingShop_v7.sql

CREATE TABLE product_sale (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    product_id       INT           NOT NULL REFERENCES product(id),
    discount_percent DECIMAL(5,2)  NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 90),
    start_date       DATETIME      NOT NULL,
    end_date         DATETIME      NOT NULL,
    is_active        BIT           NOT NULL DEFAULT 1,
    created_at       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_product_sale_dates CHECK (end_date > start_date)
);

CREATE INDEX IX_product_sale_product ON product_sale(product_id);
CREATE INDEX IX_product_sale_dates   ON product_sale(start_date, end_date);
