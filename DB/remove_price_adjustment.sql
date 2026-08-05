-- ============================================================
--  ClothingShop – Fix: xóa price_adjustment (bản có xử lý
--  default constraint tự sinh - lỗi Msg 5074/4922 khi drop thẳng)
-- ============================================================

USE ClothingShop;
GO

-- SQL Server tự sinh 1 DEFAULT CONSTRAINT ẩn cho cột có "DEFAULT 0"
-- (tên dạng DF__product_v__price__xxxxxxxx, ngẫu nhiên theo từng máy).
-- Không thể DROP COLUMN khi constraint này còn tồn tại, nên phải tìm
-- đúng tên constraint hiện tại rồi xóa nó trước.
DECLARE @constraintName NVARCHAR(200);

SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c
    ON dc.parent_object_id = c.object_id
    AND dc.parent_column_id = c.column_id
WHERE dc.parent_object_id = OBJECT_ID('product_variant')
  AND c.name = 'price_adjustment';

IF @constraintName IS NOT NULL
BEGIN
    EXEC('ALTER TABLE product_variant DROP CONSTRAINT ' + @constraintName);
    PRINT 'Đã xóa default constraint: ' + @constraintName;
END
ELSE
BEGIN
    PRINT 'Không tìm thấy default constraint nào trên price_adjustment (có thể đã xóa trước đó).';
END
GO

-- Giờ mới drop được cột
ALTER TABLE product_variant DROP COLUMN price_adjustment;
GO