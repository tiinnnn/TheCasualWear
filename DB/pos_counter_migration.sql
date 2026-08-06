-- ============================================================
--  ClothingShop – Migration: Quầy POS (PosCounter)
-- ============================================================

USE ClothingShop;
GO

CREATE TABLE pos_counter (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    code      NVARCHAR(20)  NOT NULL UNIQUE,   -- mã quầy, VD: Q1, Q2
    name      NVARCHAR(150) NULL,              -- tên/vị trí, VD: "Quầy 1 - gần cửa ra vào"
    is_active BIT           NOT NULL DEFAULT 1
);
GO
