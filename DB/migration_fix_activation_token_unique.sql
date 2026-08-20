-- Fix: UQ_app_user_activation_token đang là UNIQUE constraint thường trên
-- cột NULL-able activation_token — SQL Server chỉ cho phép TỐI ĐA 1 dòng
-- NULL trong 1 cột UNIQUE (khác Postgres/MySQL), nên chỉ cần user thứ 2
-- không dùng flow cashier-tạo-tài-khoản (activation_token = NULL) là vỡ
-- constraint, gây lỗi khi insert/update app_user ở code.
--
-- Fix: chuyển sang filtered unique index — chỉ ép unique với các dòng CÓ
-- activation_token thật, bỏ qua NULL hoàn toàn.
--
-- Script này an toàn chạy nhiều lần (idempotent) và chạy được dù máy bạn
-- đang ở tình trạng nào (constraint cũ đã tạo thành công / tạo lỗi giữa
-- chừng / chưa tạo).

-- 1. Drop constraint UNIQUE kiểu cũ nếu đã tồn tại
IF EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = 'UQ_app_user_activation_token'
      AND parent_object_id = OBJECT_ID('app_user')
)
    ALTER TABLE app_user DROP CONSTRAINT UQ_app_user_activation_token;
GO

-- 2. Drop index cùng tên nếu đã tồn tại dưới dạng index (đề phòng đã có
--    người thử tạo unique index thường trước đó)
IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_app_user_activation_token'
      AND object_id = OBJECT_ID('app_user')
)
    DROP INDEX UQ_app_user_activation_token ON app_user;
GO

-- 3. Đảm bảo 2 cột đã tồn tại (phòng trường hợp migration_cho_v14.sql
--    chạy lỗi giữa chừng và cột chưa được ADD)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('app_user') AND name = 'activation_token'
)
    ALTER TABLE app_user ADD activation_token VARCHAR(100) NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('app_user') AND name = 'activation_expires_at'
)
    ALTER TABLE app_user ADD activation_expires_at DATETIME2 NULL;
GO

-- 4. Tạo lại đúng kiểu: filtered unique index, cho phép nhiều NULL
CREATE UNIQUE INDEX UQ_app_user_activation_token
    ON app_user(activation_token)
    WHERE activation_token IS NOT NULL;
GO
