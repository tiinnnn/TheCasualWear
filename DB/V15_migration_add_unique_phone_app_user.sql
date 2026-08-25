-- ============================================================================
-- Migration: Thêm UNIQUE constraint cho app_user.phone
-- Bối cảnh: Đã phát hiện có thể tạo trùng SĐT đăng ký giữa các AppUser vì
-- cột phone trước đây không có ràng buộc UNIQUE (chỉ email mới có).
-- Áp dụng đúng pattern đã xử lý trước đó cho cột email.
--
-- Cách chạy: Thực thi lần lượt theo từng bước (STEP 1 -> 2 -> 3 -> 4).
-- KHÔNG chạy gộp toàn bộ khi chưa xem kết quả STEP 2, vì có thể cần xử lý
-- tay các dòng bị trùng trước khi ALTER TABLE ở STEP 4.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- STEP 1: Chuẩn hóa chuỗi rỗng '' -> NULL
-- Chuỗi rỗng '' được SQL Server coi là 1 giá trị cụ thể (khác NULL), nên
-- nhiều dòng cùng '' sẽ bị coi là trùng khi có UNIQUE index/constraint.
-- ----------------------------------------------------------------------------
use ClothingShop;
go
UPDATE app_user
SET phone = NULL
WHERE phone = '';


-- ----------------------------------------------------------------------------
-- STEP 2: Kiểm tra còn SĐT nào bị trùng thật sự không (khác NULL/'')
-- Nếu câu SELECT dưới đây trả về kết quả (có dòng), PHẢI xử lý tay các
-- dòng đó (đổi SĐT đúng, hoặc set về NULL nếu không xác định được) TRƯỚC
-- khi chạy STEP 4, nếu không tạo index sẽ báo lỗi vi phạm constraint.
-- ----------------------------------------------------------------------------
SELECT phone, COUNT(*) AS so_luong_trung
FROM app_user
WHERE phone IS NOT NULL AND phone <> ''
GROUP BY phone
HAVING COUNT(*) > 1;

-- Nếu STEP 2 có kết quả, dùng câu dưới đây để xem chi tiết từng user bị
-- trùng (id, username, phone) rồi tự quyết định giữ dòng nào / sửa dòng nào:
--
-- SELECT id, username, email, phone, created_at
-- FROM app_user
-- WHERE phone IN (
--     SELECT phone FROM app_user
--     WHERE phone IS NOT NULL AND phone <> ''
--     GROUP BY phone
--     HAVING COUNT(*) > 1
-- )
-- ORDER BY phone, created_at;
--
-- Sau khi xác định dòng cần sửa, dùng UPDATE tay từng id, ví dụ:
-- UPDATE app_user SET phone = NULL WHERE id = <id_can_sua>;


-- ----------------------------------------------------------------------------
-- STEP 3: Kiểm tra index đã tồn tại chưa (tránh lỗi khi chạy lại migration).
-- ----------------------------------------------------------------------------
IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_app_user_phone' AND object_id = OBJECT_ID('dbo.app_user')
)
    PRINT 'Index UQ_app_user_phone đã tồn tại, bỏ qua STEP 4.';
ELSE
    PRINT 'Chưa có index UQ_app_user_phone, có thể chạy STEP 4.';


-- ----------------------------------------------------------------------------
-- STEP 4: Tạo FILTERED UNIQUE INDEX trên cột phone (KHÔNG dùng
-- ALTER TABLE ... ADD CONSTRAINT UNIQUE).
--
-- LÝ DO: khác với PostgreSQL/MySQL, SQL Server coi NULL là 1 giá trị cụ
-- thể trong UNIQUE constraint/index thông thường -> chỉ cho phép TỐI ĐA
-- 1 dòng NULL. Vì có nhiều user chưa nhập SĐT (nhiều dòng NULL), câu
-- ALTER TABLE ADD CONSTRAINT UNIQUE thông thường sẽ luôn báo lỗi
-- "duplicate key value is (<NULL>)" dù dữ liệu thật không hề trùng.
--
-- "WHERE phone IS NOT NULL" loại NULL ra khỏi phạm vi kiểm tra unique,
-- nên nhiều dòng NULL vẫn thoải mái tồn tại, chỉ các giá trị SĐT THẬT
-- mới bị bắt buộc duy nhất.
--
-- CHỈ chạy khi STEP 2 không còn trả về dòng nào (đã dọn sạch trùng lặp
-- ở các giá trị SĐT thật, không tính NULL).
-- ----------------------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_app_user_phone' AND object_id = OBJECT_ID('dbo.app_user')
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_app_user_phone
    ON dbo.app_user(phone)
    WHERE phone IS NOT NULL;

    PRINT 'Đã tạo filtered unique index UQ_app_user_phone thành công.';
END