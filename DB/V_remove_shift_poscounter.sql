-- =====================================================================
-- Migration: Bỏ hoàn toàn Shift / PosCounter khỏi cấu trúc DB (SQL Server)
-- Giai đoạn 3.1 - TheCasualWear
--
-- ⚠️ BẮT BUỘC BACKUP DATABASE TRƯỚC KHI CHẠY SCRIPT NÀY.
-- Thao tác DROP TABLE không thể hoàn tác. Nếu cần giữ lại dữ liệu ca cũ
-- để tra cứu/báo cáo sau này, chạy PHẦN 0 (export) trước khi chạy PHẦN 1-4.
--
-- Bản cập nhật: xử lý thêm index phụ thuộc (IX_app_order_shift) và
-- tự động drop MỌI foreign key đang tham chiếu tới bảng shift / pos_counter
-- (không chỉ hard-code app_order), để tránh lỗi:
--   Msg 4922: ALTER TABLE DROP COLUMN ... failed because one or more
--   objects access this column.
--   Msg 3726: Could not drop object because it is referenced by a
--   FOREIGN KEY constraint.
-- =====================================================================

-- =====================================================================
-- PHẦN 0 (TÙY CHỌN NHƯNG KHUYẾN NGHỊ): Export dữ liệu cũ trước khi xóa
-- Bỏ comment nếu muốn giữ lại 1 bản sao dữ liệu ca/quầy trong bảng archive.
-- =====================================================================

-- SELECT * INTO shift_archive FROM shift;
-- SELECT * INTO pos_counter_archive FROM pos_counter;

-- =====================================================================
-- PHẦN 1: Gỡ index + FK shift_id khỏi bảng app_order
-- =====================================================================

BEGIN TRANSACTION;

-- 1.1. Xóa mọi index liên quan tới cột app_order.shift_id
--      (ví dụ IX_app_order_shift) — bắt buộc phải xóa index trước khi
--      drop column, nếu không sẽ dính lỗi Msg 4922.
DECLARE @idxSql NVARCHAR(MAX) = N'';

SELECT @idxSql = @idxSql +
    'DROP INDEX ' + QUOTENAME(i.name) + ' ON app_order;' + CHAR(13)
FROM sys.indexes i
INNER JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
INNER JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE i.object_id = OBJECT_ID('app_order')
  AND c.name = 'shift_id'
  AND i.is_primary_key = 0
  AND i.type > 0; -- bỏ heap (type=0)

IF LEN(@idxSql) > 0
BEGIN
    PRINT 'Đang xóa các index phụ thuộc vào app_order.shift_id:';
    PRINT @idxSql;
    EXEC sp_executesql @idxSql;
END
ELSE
BEGIN
    PRINT 'Không có index nào phụ thuộc vào app_order.shift_id.';
END

-- 1.2. Tìm và xóa constraint FK app_order.shift_id -> shift.id
--      (tên constraint tự sinh có thể khác nhau tùy môi trường, script này
--       tự tra tên thật thay vì hard-code, để an toàn khi chạy trên các
--       DB đã tồn tại lâu với tên constraint không đồng nhất)
DECLARE @fkName NVARCHAR(200);

SELECT @fkName = fk.name
FROM sys.foreign_keys fk
INNER JOIN sys.tables t ON fk.parent_object_id = t.object_id
INNER JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
INNER JOIN sys.columns c ON fkc.parent_object_id = c.object_id AND fkc.parent_column_id = c.column_id
WHERE t.name = 'app_order' AND c.name = 'shift_id';

IF @fkName IS NOT NULL
BEGIN
    EXEC('ALTER TABLE app_order DROP CONSTRAINT ' + @fkName);
    PRINT 'Đã xóa FK constraint: ' + @fkName;
END
ELSE
BEGIN
    PRINT 'Không tìm thấy FK constraint trên app_order.shift_id (có thể đã xóa trước đó).';
END

-- 1.3. Xóa cột shift_id khỏi app_order
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('app_order') AND name = 'shift_id')
BEGIN
    ALTER TABLE app_order DROP COLUMN shift_id;
    PRINT 'Đã xóa cột app_order.shift_id';
END

COMMIT TRANSACTION;
GO

-- =====================================================================
-- PHẦN 2: Xóa bảng shift
-- Tự động drop MỌI FK từ bảng khác trỏ tới shift (đề phòng còn bảng nào
-- khác ngoài app_order cũng tham chiếu, ví dụ stock_movement_log...),
-- rồi mới DROP TABLE shift.
-- =====================================================================

BEGIN TRANSACTION;

DECLARE @shiftFkSql NVARCHAR(MAX) = N'';

SELECT @shiftFkSql = @shiftFkSql +
    'ALTER TABLE ' + QUOTENAME(t_child.name) + ' DROP CONSTRAINT ' + QUOTENAME(fk.name) + ';' + CHAR(13)
FROM sys.foreign_keys fk
INNER JOIN sys.tables t_child ON fk.parent_object_id = t_child.object_id
INNER JOIN sys.tables t_parent ON fk.referenced_object_id = t_parent.object_id
WHERE t_parent.name = 'shift';

IF LEN(@shiftFkSql) > 0
BEGIN
    PRINT 'Đang xóa các FK còn lại trỏ tới bảng shift:';
    PRINT @shiftFkSql;
    EXEC sp_executesql @shiftFkSql;
END

IF OBJECT_ID('shift', 'U') IS NOT NULL
BEGIN
    DROP TABLE shift;
    PRINT 'Đã xóa bảng shift';
END
ELSE
BEGIN
    PRINT 'Bảng shift không tồn tại (có thể đã xóa trước đó).';
END

COMMIT TRANSACTION;
GO

-- =====================================================================
-- PHẦN 3: Xóa bảng pos_counter
-- Tương tự, tự động drop MỌI FK từ bảng khác trỏ tới pos_counter trước
-- khi DROP TABLE (ví dụ FK từ shift đã bị xóa ở PHẦN 2, nhưng đề phòng
-- còn bảng nào khác nữa).
-- =====================================================================

BEGIN TRANSACTION;

DECLARE @counterFkSql NVARCHAR(MAX) = N'';

SELECT @counterFkSql = @counterFkSql +
    'ALTER TABLE ' + QUOTENAME(t_child.name) + ' DROP CONSTRAINT ' + QUOTENAME(fk.name) + ';' + CHAR(13)
FROM sys.foreign_keys fk
INNER JOIN sys.tables t_child ON fk.parent_object_id = t_child.object_id
INNER JOIN sys.tables t_parent ON fk.referenced_object_id = t_parent.object_id
WHERE t_parent.name = 'pos_counter';

IF LEN(@counterFkSql) > 0
BEGIN
    PRINT 'Đang xóa các FK còn lại trỏ tới bảng pos_counter:';
    PRINT @counterFkSql;
    EXEC sp_executesql @counterFkSql;
END

IF OBJECT_ID('pos_counter', 'U') IS NOT NULL
BEGIN
    DROP TABLE pos_counter;
    PRINT 'Đã xóa bảng pos_counter';
END
ELSE
BEGIN
    PRINT 'Bảng pos_counter không tồn tại (có thể đã xóa trước đó).';
END

COMMIT TRANSACTION;
GO

-- =====================================================================
-- PHẦN 4: Kiểm tra lại sau khi chạy xong
-- =====================================================================

SELECT 'app_order.shift_id còn tồn tại?' AS check_item,
       CASE WHEN EXISTS (
           SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('app_order') AND name = 'shift_id'
       ) THEN 'CÓ - CẦN KIỂM TRA LẠI' ELSE 'OK - đã xóa' END AS result
UNION ALL
SELECT 'bảng shift còn tồn tại?',
       CASE WHEN OBJECT_ID('shift', 'U') IS NOT NULL
            THEN 'CÓ - CẦN KIỂM TRA LẠI' ELSE 'OK - đã xóa' END
UNION ALL
SELECT 'bảng pos_counter còn tồn tại?',
       CASE WHEN OBJECT_ID('pos_counter', 'U') IS NOT NULL
            THEN 'CÓ - CẦN KIỂM TRA LẠI' ELSE 'OK - đã xóa' END;
GO