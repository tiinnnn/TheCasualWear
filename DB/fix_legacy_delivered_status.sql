-- ============================================================
--  ClothingShop – Migration: Dọn dữ liệu cũ còn status = 'DELIVERED'
--  Chạy 1 lần sau khi đã bỏ DELIVERED khỏi enum OrderStatus.
--
--  Lý do: OrderStatus.DELIVERED đã bị xóa (SHIPPING -> COMPLETED
--  giờ đi thẳng, admin tự đánh dấu sau khi kiểm tra GHN). Các đơn
--  tạo trước thời điểm đổi mà đang dừng ở DELIVERED sẽ không map
--  được vào enum mới -> Hibernate ném lỗi
--  "No enum constant ...OrderStatus.DELIVERED" khi query.
--
--  Hướng xử lý: DELIVERED nghĩa là hàng đã tới tay khách, gần với
--  COMPLETED nhất trong luồng mới -> chuyển thẳng sang COMPLETED.
--  deliveredAt của các đơn này thường đã được set sẵn từ lần admin
--  bấm "xác nhận giao thành công" trước đây nên không cần set lại.
-- ============================================================

USE ClothingShop;
GO

UPDATE app_order
SET status = 'COMPLETED'
WHERE status = 'DELIVERED';
GO
