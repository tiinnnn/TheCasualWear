-- ============================================================
--  ClothingShop – Migration: Bảng Employee (tách quản lý nhân
--  viên khỏi quản lý user chung) — quan hệ 1-1 với app_user.
-- ============================================================

USE ClothingShop;
GO

CREATE TABLE employee (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    user_id       INT           NOT NULL UNIQUE REFERENCES app_user(id),
    employee_code NVARCHAR(20)  NOT NULL UNIQUE,   -- VD: NV001
    hire_date     DATE          NULL,               -- ngày vào làm
    is_active     BIT           NOT NULL DEFAULT 1, -- đang làm việc / đã nghỉ
    note          NVARCHAR(500) NULL
);
GO
