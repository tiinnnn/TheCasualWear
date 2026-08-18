-- MỚI: thêm cột active vào bảng address (soft-delete/archive) — xem
-- Address.java + AddressService.updateAddress()/deleteAddress().
--
-- Các row đã có sẵn trong bảng sẽ tự động được set active = 1 nhờ DEFAULT
-- constraint (SQL Server tự backfill NOT NULL + DEFAULT cho row cũ khi
-- ALTER TABLE ADD COLUMN, không cần WITH VALUES vì đây không phải CHECK
-- constraint).

ALTER TABLE address ADD active BIT NOT NULL DEFAULT 1;
