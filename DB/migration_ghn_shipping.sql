-- Migration: tích hợp GHN Calculate Fee (feature 4.5)
-- Đặt tên file theo đúng version thực tế của bạn (v13/v14...) trước khi chạy —
-- ở đây đánh số trung tính vì mình không có bản v13 mới nhất bạn đang dùng.

-- ── ADDRESS: thêm mã định danh của GHN (khác hệ province/district/ward
-- của provinces.open-api.vn đang dùng cho dropdown hiện tại) ──────────────
-- Giữ nguyên city/district (String) cũ để không vỡ dữ liệu đã có; các cột
-- mới này NULL nếu địa chỉ tạo trước khi có tính năng GHN, code sẽ fallback
-- về region-based khi thiếu mã GHN (xem GhnService).
ALTER TABLE address ADD ghn_province_id INT NULL;
ALTER TABLE address ADD ghn_district_id INT NULL;
ALTER TABLE address ADD ghn_ward_code VARCHAR(20) NULL;
GO

-- ── PRODUCT: thêm weight (gram) — bắt buộc cho GHN Calculate Fee ─────────
-- Default 300g cho sản phẩm áo/quần thời trang thông thường, áp cho cả sản
-- phẩm cũ đã có sẵn trong DB (không NULL để tránh gọi GHN với weight=0).
ALTER TABLE product ADD weight INT NOT NULL DEFAULT 300;
GO
