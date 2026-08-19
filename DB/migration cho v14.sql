-- Migration: thêm cột activation_token / activation_expires_at vào app_user
-- phục vụ tính năng "Cashier tạo tài khoản trước cho khách"
-- Chạy 1 lần trên SQL Server trước khi deploy code mới.

ALTER TABLE app_user ADD activation_token VARCHAR(100) NULL;
ALTER TABLE app_user ADD activation_expires_at DATETIME2 NULL;

ALTER TABLE app_user
    ADD CONSTRAINT UQ_app_user_activation_token UNIQUE (activation_token);
