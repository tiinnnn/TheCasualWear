-- Migration: thêm cột activation_token / activation_expires_at vào app_user
-- phục vụ tính năng "Cashier tạo tài khoản trước cho khách"
-- Chạy 1 lần trên SQL Server trước khi deploy code mới.

ALTER TABLE app_user ADD activation_token VARCHAR(100) NULL;
ALTER TABLE app_user ADD activation_expires_at DATETIME2 NULL;

-- FIX: dùng filtered unique index thay vì UNIQUE constraint thường —
-- SQL Server chỉ cho phép 1 dòng NULL/bảng với UNIQUE constraint chuẩn,
-- trong khi hầu hết user (không dùng flow cashier) sẽ có activation_token
-- = NULL. Filtered index chỉ ép unique với các dòng có token thật, cho
-- phép nhiều NULL cùng lúc.
CREATE UNIQUE INDEX UQ_app_user_activation_token
    ON app_user(activation_token)
    WHERE activation_token IS NOT NULL;
