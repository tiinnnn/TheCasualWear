/*
  DỌN RÁC TRONG DATABASE [master]
  ---------------------------------
  Do lần chạy trước bị lỗi ngay từ bước CREATE DATABASE (vì đường dẫn file
  .mdf/.ldf cứng trong script không tồn tại trên máy bạn), lệnh USE [ClothingShop]
  sau đó cũng lỗi theo, nên toàn bộ các lệnh CREATE TABLE / INSERT phía sau
  đã vô tình chạy vào database [master] (context mặc định) thay vì [ClothingShop].
  Vì vậy hiện tại [master] có thể đang chứa rác: các bảng address, product,
  product_variant, v.v. và cả 1 FK "product_c...produ..." trỏ tới bảng
  "dbo.Product" (viết hoa) không liên quan tới project của bạn.

  Chạy đoạn dưới đây (trên [master]) để liệt kê các bảng "rác" nghi ngờ,
  rồi tự kiểm tra trước khi xoá. KHÔNG chạy DROP tự động hàng loạt vì
  [master] là database hệ thống, xoá nhầm object hệ thống sẽ ảnh hưởng
  toàn bộ SQL Server instance.
*/

USE [master];
GO

SELECT t.name AS table_name, t.create_date
FROM sys.tables t
WHERE t.name IN (
    'address','app_order','app_user','cart','cart_item','category','collection',
    'color','employee','goods_receipt','goods_receipt_item','notification',
    'order_detail','order_voucher','password_reset_token','product',
    'product_collection','product_image','product_sale','product_variant',
    'role','sale_batch','size','stock_movement_log','user_role',
    'variant_image','voucher','wishlist'
)
ORDER BY t.name;
GO

/*
  Sau khi xem danh sách trên và xác nhận đây đúng là rác của lần chạy lỗi
  (không phải bảng bạn đang dùng cho việc khác trong master), bỏ comment
  đoạn DROP bên dưới rồi chạy để dọn sạch trước khi chạy lại
  sqlcosanphammaudaydu_fixed.sql.

  -- Xoá theo đúng thứ tự để tránh lỗi FOREIGN KEY (con trước, cha sau):
  DROP TABLE IF EXISTS [dbo].[wishlist];
  DROP TABLE IF EXISTS [dbo].[variant_image];
  DROP TABLE IF EXISTS [dbo].[user_role];
  DROP TABLE IF EXISTS [dbo].[stock_movement_log];
  DROP TABLE IF EXISTS [dbo].[order_voucher];
  DROP TABLE IF EXISTS [dbo].[order_detail];
  DROP TABLE IF EXISTS [dbo].[product_sale];
  DROP TABLE IF EXISTS [dbo].[product_variant];
  DROP TABLE IF EXISTS [dbo].[product_image];
  DROP TABLE IF EXISTS [dbo].[product_collection];
  DROP TABLE IF EXISTS [dbo].[goods_receipt_item];
  DROP TABLE IF EXISTS [dbo].[goods_receipt];
  DROP TABLE IF EXISTS [dbo].[cart_item];
  DROP TABLE IF EXISTS [dbo].[cart];
  DROP TABLE IF EXISTS [dbo].[notification];
  DROP TABLE IF EXISTS [dbo].[password_reset_token];
  DROP TABLE IF EXISTS [dbo].[app_order];
  DROP TABLE IF EXISTS [dbo].[product];
  DROP TABLE IF EXISTS [dbo].[sale_batch];
  DROP TABLE IF EXISTS [dbo].[category];
  DROP TABLE IF EXISTS [dbo].[collection];
  DROP TABLE IF EXISTS [dbo].[color];
  DROP TABLE IF EXISTS [dbo].[size];
  DROP TABLE IF EXISTS [dbo].[voucher];
  DROP TABLE IF EXISTS [dbo].[employee];
  DROP TABLE IF EXISTS [dbo].[role];
  DROP TABLE IF EXISTS [dbo].[app_user];
  DROP TABLE IF EXISTS [dbo].[address];
*/
