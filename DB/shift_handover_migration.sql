-- ============================================================
--  ClothingShop – Migration: Xác nhận bàn giao ca (handover)
--  Chạy sau khi đã có shift_migration.sql
-- ============================================================

USE ClothingShop;
GO

ALTER TABLE shift ADD items_sold_count      INT           NULL;
ALTER TABLE shift ADD handover_confirmed_by INT           NULL REFERENCES app_user(id);
ALTER TABLE shift ADD handover_confirmed_at DATETIME      NULL;
ALTER TABLE shift ADD handover_note         NVARCHAR(500) NULL;
GO
