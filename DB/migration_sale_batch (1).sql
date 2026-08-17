-- ============================================================
--  migration_sale_batch_v2.sql
--  Chạy TAY file này trên đúng database ClothingShop (không có
--  cơ chế tự sinh/tự đồng bộ — Hibernate KHÔNG tự tạo cột này,
--  entity chỉ sinh câu SELECT/INSERT dùng cột, DB phải có sẵn cột
--  trước thì Hibernate mới chạy được).
--
--  Cách chạy:
--    SSMS / Azure Data Studio: mở file này, chọn đúng database
--    ClothingShop ở dropdown, F5 để chạy toàn bộ.
--
--  Idempotent: chạy lại nhiều lần không lỗi nếu đã tồn tại.
-- ============================================================

USE ClothingShop;
GO

-- 1. Bảng sale_batch
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'sale_batch')
BEGIN
    CREATE TABLE sale_batch (
        id               INT IDENTITY(1,1) PRIMARY KEY,
        name             NVARCHAR(150)  NOT NULL,
        discount_percent DECIMAL(5,2)   NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 90),
        start_date       DATETIME       NOT NULL,
        end_date         DATETIME       NOT NULL,
        created_at       DATETIME       NOT NULL DEFAULT GETDATE(),
        CONSTRAINT CK_sale_batch_dates CHECK (end_date > start_date)
    );
    PRINT 'Đã tạo bảng sale_batch';
END
ELSE
BEGIN
    PRINT 'Bảng sale_batch đã tồn tại, bỏ qua.';
END
GO

-- 2. Cột sale_batch_id trên product_sale (nullable, FK -> sale_batch)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('product_sale') AND name = 'sale_batch_id'
)
BEGIN
    ALTER TABLE product_sale ADD sale_batch_id INT NULL;
    PRINT 'Đã thêm cột product_sale.sale_batch_id';
END
ELSE
BEGIN
    PRINT 'Cột product_sale.sale_batch_id đã tồn tại, bỏ qua.';
END
GO

-- 3. FK constraint riêng (tách khỏi ALTER ADD ở trên để không lỗi nếu
--    cột đã có nhưng constraint chưa có, hoặc ngược lại)
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_product_sale_batch'
)
BEGIN
    ALTER TABLE product_sale
        ADD CONSTRAINT FK_product_sale_batch
        FOREIGN KEY (sale_batch_id) REFERENCES sale_batch(id);
    PRINT 'Đã thêm FK_product_sale_batch';
END
ELSE
BEGIN
    PRINT 'FK_product_sale_batch đã tồn tại, bỏ qua.';
END
GO

-- 4. Index phục vụ findBySaleBatchId() / countBySaleBatchId()
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_product_sale_batch' AND object_id = OBJECT_ID('product_sale')
)
BEGIN
    CREATE INDEX IX_product_sale_batch ON product_sale(sale_batch_id);
    PRINT 'Đã tạo index IX_product_sale_batch';
END
ELSE
BEGIN
    PRINT 'Index IX_product_sale_batch đã tồn tại, bỏ qua.';
END
GO

-- 5. XÁC NHẬN — chạy xong phải thấy sale_batch_id trong danh sách cột
SELECT name AS column_name
FROM sys.columns
WHERE object_id = OBJECT_ID('product_sale')
ORDER BY column_id;
GO

-- 6. Xác nhận DB/schema đang chạy đúng chỗ (đối chiếu với JDBC URL
--    trong log app: databaseName=ClothingShop)
SELECT DB_NAME() AS current_database;
GO