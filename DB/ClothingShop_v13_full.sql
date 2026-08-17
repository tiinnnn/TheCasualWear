-- ============================================================
--  ClothingShop  –  Schema sạch + Seed Data (bản TỔNG HỢP HOÀN CHỈNH)
--  Gộp: v10 (baseline) + xóa Shift/PosCounter (giai đoạn 3.1)
--       + v11 (thêm app_order.shipping_fee, data mẫu theo khu vực)
--       + v12 (order_code + guest_email cho khách vãng lai)
--       + v13 (bảng sale_batch cho migration_sale_batch_v2.sql)
--
--  Lưu ý: employee_migration.sql KHÔNG được gộp vào đây vì bảng
--  employee đã có sẵn trong baseline v10 (seed sẵn owner/admin/cashier1).
--
--  v13 – Gộp migration_sale_batch_v2.sql:
--       + Bảng sale_batch (đợt sale gộp nhiều sản phẩm, VD: Sale hè,
--         Flash sale cuối tuần), discount_percent 1-90%, có ngày bắt đầu/kết thúc
--       + product_sale: + sale_batch_id (nullable, FK -> sale_batch) —
--         1 sale đơn lẻ (không thuộc batch nào) vẫn hợp lệ nếu để NULL
--       + Index IX_product_sale_batch trên product_sale(sale_batch_id)
--
--  v12 – Thêm order_code (mã tra cứu công khai, thay ID tự tăng) và
--        guest_email cho luồng khách vãng lai (task 4.1, 4.2, 6.6)
--       + app_order.order_code NVARCHAR(12) NOT NULL UNIQUE
--       + app_order.guest_email NVARCHAR(100) NULL (chỉ có khi
--         customer_id NULL và order_type='ONLINE' — khách vãng lai
--         đặt online, phân biệt với đơn COUNTER bán tại quầy)
--
--  v10 – Gộp migration sau v9:
--       + Bảng employee (tách quản lý nhân viên khỏi app_user,
--         quan hệ 1-1 qua user_id) — mọi tài khoản có vai trò
--         ADMIN / OWNER / CASHIER đều có bản ghi employee tương ứng
--
--  v9 – Gộp migration sau v8:
--       + shift: thêm counter_id (REFERENCES pos_counter) + unique index
--         chỉ 1 ca OPEN / quầy tại 1 thời điểm
--
--  v8 – Gộp các migration sau v7 thành 1 baseline duy nhất:
--       + shift: thêm xác nhận bàn giao ca (items_sold_count,
--         handover_confirmed_by, handover_confirmed_at, handover_note)
--       + Bảng pos_counter (quầy thu ngân vật lý)
--       + Bảng product_sale (sale/giảm giá theo lịch trình)
--       + order_detail: thêm original_price (giá gốc tại thời điểm
--         đặt hàng, phân biệt với price = giá thực trả sau sale)
--
--  v7 – Gộp các migration sau v6 thành 1 baseline duy nhất:
--       + Module Quản lý kho (goods_receipt, goods_receipt_item,
--         stock_movement_log)
--       + Module Giao ca (shift) + app_order.shift_id
--       + Xóa cột product_variant.price_adjustment (không dùng)
--       + app_order: + cancel_reason, cancel_note, cancelled_by,
--         cancelled_at (lý do hủy/hoàn đơn)
--       + status DELIVERED đã bị loại khỏi enum OrderStatus,
--         luồng mới SHIPPING -> COMPLETED đi thẳng
--
--  Giai đoạn 3.1 – Bỏ hoàn toàn Shift / PosCounter khỏi cấu trúc DB
--       + Xóa bảng shift, pos_counter
--       + Xóa app_order.shift_id (và index IX_app_order_shift)
--       + Đơn bán tại quầy (order_type='COUNTER') không còn gắn ca nữa
--
--  v11 – Thêm app_order.shipping_fee (phí ship cố định, fallback
--        trước khi tích hợp GHN API tính phí thật). Data mẫu tính
--        theo khu vực giao hàng: Hà Nội 20k | Đà Nẵng 32k | TP.HCM 38k
--        | đơn COUNTER = 0 (nhận hàng tại quầy, không ship)
-- ============================================================

DROP DATABASE IF EXISTS ClothingShop;
GO
CREATE DATABASE ClothingShop;
GO
USE ClothingShop;
GO

-- ============================================================
--  TABLES
-- ============================================================

-- Roles
-- id: 1=ROLE_ADMIN | 2=ROLE_CUSTOMER | 3=ROLE_OWNER | 4=ROLE_CASHIER
CREATE TABLE role (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Users
-- id: 1=owner | 2=admin | 3-7=customer | 8=cashier1
CREATE TABLE app_user (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    username   NVARCHAR(50)  NOT NULL UNIQUE,
    password   NVARCHAR(255) NOT NULL,
    email      NVARCHAR(100) UNIQUE,
    phone      NVARCHAR(20),
    enabled    BIT           NOT NULL DEFAULT 1,
    created_at DATETIME      NOT NULL DEFAULT GETDATE()
);

CREATE TABLE user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
);

-- Nhân viên (1-1 với app_user) — áp dụng cho mọi tài khoản có
-- vai trò ADMIN / OWNER / CASHIER (không áp dụng cho CUSTOMER)
CREATE TABLE employee (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    user_id       INT           NOT NULL UNIQUE REFERENCES app_user(id),
    employee_code NVARCHAR(20)  NOT NULL UNIQUE,   -- VD: NV001
    hire_date     DATE          NULL,               -- ngày vào làm
    is_active     BIT           NOT NULL DEFAULT 1, -- đang làm việc / đã nghỉ
    note          NVARCHAR(500) NULL
);

-- Danh mục sản phẩm
-- id: 1=Áo thun | 2=Áo sơ mi | 3=Quần | 4=Áo hoodie
CREATE TABLE category (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(255)
);

-- Sizes
-- id: 1=S | 2=M | 3=L | 4=XL | 5=XXL
CREATE TABLE size (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(20) NOT NULL UNIQUE
);

-- Colors
-- id: 1=Trắng | 2=Đen | 3=Xanh navy | 4=Xám | 5=Xanh nhạt
CREATE TABLE color (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Sản phẩm gốc
CREATE TABLE product (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX),
    price       DECIMAL(18,2) NOT NULL,
    is_deleted  BIT           NOT NULL DEFAULT 0,
    category_id INT           REFERENCES category(id),
    created_at  DATETIME      NOT NULL DEFAULT GETDATE()
);

-- Ảnh đại diện sản phẩm
CREATE TABLE product_image (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    image_url  NVARCHAR(500) NOT NULL,
    product_id INT           NOT NULL REFERENCES product(id)
);

-- Biến thể sản phẩm (size + màu)
CREATE TABLE product_variant (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    product_id       INT           NOT NULL REFERENCES product(id),
    size_id          INT                    REFERENCES size(id),
    color_id         INT                    REFERENCES color(id),
    sku              NVARCHAR(50)  UNIQUE,
    stock            INT           NOT NULL DEFAULT 0,
    cost_price       DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at       DATETIME      NOT NULL DEFAULT GETDATE()
);

-- Ảnh riêng cho từng variant
CREATE TABLE variant_image (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    variant_id INT           NOT NULL REFERENCES product_variant(id),
    image_url  NVARCHAR(500) NOT NULL,
    sort_order INT           NOT NULL DEFAULT 0
);

-- ============================================================
--  SALE / KHUYẾN MÃI (từ migration_product_sale.sql)
-- ============================================================

-- Đợt sale gộp nhiều sản phẩm (VD: Sale hè, Flash sale cuối tuần) —
-- từ migration_sale_batch_v2.sql
CREATE TABLE sale_batch (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    name             NVARCHAR(150)  NOT NULL,
    discount_percent DECIMAL(5,2)   NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 90),
    start_date       DATETIME       NOT NULL,
    end_date         DATETIME       NOT NULL,
    created_at       DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_sale_batch_dates CHECK (end_date > start_date)
);

-- Sale/giảm giá theo lịch trình cho từng sản phẩm
-- sale_batch_id: NULL nếu là sale đơn lẻ riêng cho 1 sản phẩm,
--                có giá trị nếu sản phẩm này thuộc 1 đợt sale gộp (sale_batch)
CREATE TABLE product_sale (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    product_id       INT           NOT NULL REFERENCES product(id),
    sale_batch_id    INT           NULL REFERENCES sale_batch(id),
    discount_percent DECIMAL(5,2)  NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 90),
    start_date       DATETIME      NOT NULL,
    end_date         DATETIME      NOT NULL,
    is_active        BIT           NOT NULL DEFAULT 1,
    created_at       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_product_sale_dates CHECK (end_date > start_date)
);

CREATE INDEX IX_product_sale_product ON product_sale(product_id);
CREATE INDEX IX_product_sale_dates   ON product_sale(start_date, end_date);
CREATE INDEX IX_product_sale_batch   ON product_sale(sale_batch_id);

-- Địa chỉ giao hàng
CREATE TABLE address (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT           REFERENCES app_user(id),
    full_name  NVARCHAR(100) NOT NULL,
    phone      NVARCHAR(20)  NOT NULL,
    street     NVARCHAR(255) NOT NULL,
    city       NVARCHAR(100) NOT NULL,
    district   NVARCHAR(100),
    country    NVARCHAR(100) NOT NULL DEFAULT N'Vietnam',
    is_default BIT           NOT NULL DEFAULT 0
);

-- ============================================================
--  QUẢN LÝ KHO (từ warehouse_migration.sql)
-- ============================================================

-- Phiếu nhập kho (header)
-- supplier_name: chỉ lưu tên NCC dạng text, không tách bảng riêng
-- created_by: nhân viên (ADMIN/OWNER) thực hiện nhập kho
CREATE TABLE goods_receipt (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    code          NVARCHAR(30)  NOT NULL UNIQUE,        -- vd: PN-20260803-001
    supplier_name NVARCHAR(150) NOT NULL,
    note          NVARCHAR(500) NULL,
    created_by    INT           NOT NULL REFERENCES app_user(id),
    created_at    DATETIME      NOT NULL DEFAULT GETDATE(),
    total_amount  DECIMAL(18,2) NOT NULL DEFAULT 0       -- tổng giá trị phiếu (tính từ item)
);

-- Chi tiết phiếu nhập kho (từng dòng variant)
CREATE TABLE goods_receipt_item (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    goods_receipt_id  INT           NOT NULL REFERENCES goods_receipt(id),
    variant_id        INT           NOT NULL REFERENCES product_variant(id),
    quantity          INT           NOT NULL CHECK (quantity > 0),
    unit_cost_price   DECIMAL(18,2) NOT NULL DEFAULT 0   -- giá nhập tại thời điểm nhập (snapshot)
);

-- Lịch sử biến động kho (audit trail)
-- Mỗi lần stock của 1 variant thay đổi (do nhập kho, bán hàng, hủy đơn,
-- hoặc admin sửa tay) đều ghi 1 dòng ở đây.
-- change_qty: dương = tăng tồn, âm = giảm tồn
-- balance_after: số tồn SAU khi áp dụng thay đổi này (để tra cứu nhanh, khỏi tính lại)
-- ref_type/ref_id: tham chiếu tới bản ghi gốc gây ra biến động (linh hoạt, không FK cứng
--                  vì có thể trỏ tới nhiều bảng khác nhau: goods_receipt, app_order...)
CREATE TABLE stock_movement_log (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    variant_id     INT           NOT NULL REFERENCES product_variant(id),
    change_type    NVARCHAR(20)  NOT NULL,   -- 'IMPORT' | 'SALE' | 'RETURN' | 'ADJUST' | 'CANCEL'
    change_qty     INT           NOT NULL,
    balance_after  INT           NOT NULL,
    ref_type       NVARCHAR(30)  NULL,       -- 'GOODS_RECEIPT' | 'ORDER' | 'MANUAL'
    ref_id         INT           NULL,       -- id của goods_receipt hoặc app_order tương ứng
    note           NVARCHAR(255) NULL,
    created_by     INT           NULL REFERENCES app_user(id),
    created_at     DATETIME      NOT NULL DEFAULT GETDATE()
);

CREATE INDEX IX_stock_movement_variant ON stock_movement_log(variant_id);
CREATE INDEX IX_stock_movement_created_at ON stock_movement_log(created_at);

-- Đơn hàng
-- tracking_code : mã vận đơn GHN nhân viên nhập thủ công
-- shipped_at    : thời điểm admin xác nhận gửi hàng cho GHN
-- order_type    : ONLINE (đơn web) | COUNTER (bán tại quầy)
-- cashier_id    : nhân viên thu ngân tạo đơn (chỉ có khi order_type = COUNTER)
-- customer_id   : NULL khi là khách vãng lai (mua tại quầy HOẶC guest checkout online)
-- shipping_fee  : phí ship (0 với đơn COUNTER, tính theo khu vực với đơn ONLINE)
-- cancel_reason/cancel_note/cancelled_by/cancelled_at : lý do hủy/hoàn đơn
--   status = 'CANCELLED' dùng cho cả hủy đơn VÀ hoàn hàng do bug cũ ở
--   OrderService – code mới dùng 'RETURNED' riêng.
-- order_code    : mã tra cứu công khai (khách vãng lai không có tài khoản để
--                 xem lịch sử đơn, nên dùng mã này để tra cứu thay vì id tự tăng)
-- guest_email   : email khách vãng lai đặt hàng online (customer_id NULL +
--                 order_type='ONLINE'); NULL với khách có tài khoản và đơn COUNTER
CREATE TABLE app_order (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    customer_id         INT           NULL REFERENCES app_user(id),
    order_date          DATETIME      NOT NULL DEFAULT GETDATE(),
    status              NVARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_price         DECIMAL(18,2),
    payment_method      NVARCHAR(20)  NOT NULL DEFAULT 'COD',
    is_paid             BIT           NOT NULL DEFAULT 0,
    delivered_at        DATETIME      NULL,
    shipped_at          DATETIME      NULL,
    tracking_code       NVARCHAR(50)  NULL,
    shipping_address_id INT           REFERENCES address(id),
    billing_address_id  INT           REFERENCES address(id),
    order_type          NVARCHAR(20)  NOT NULL DEFAULT 'ONLINE',
    cashier_id          INT           NULL REFERENCES app_user(id),
    shipping_fee        DECIMAL(18,2) NULL,
    cancel_reason       NVARCHAR(30)  NULL,
    cancel_note         NVARCHAR(255) NULL,
    cancelled_by        INT           NULL REFERENCES app_user(id),
    cancelled_at        DATETIME      NULL,
    order_code          VARCHAR(12)   NOT NULL,
    guest_email         VARCHAR(100)  NULL
);

CREATE INDEX IX_app_order_cancelled_by ON app_order(cancelled_by);
CREATE UNIQUE INDEX UQ_app_order_order_code ON app_order(order_code);

-- Chi tiết đơn hàng
-- original_price : giá gốc của sản phẩm tại thời điểm đặt hàng
-- price           : giá thực khách trả (đã áp sale nếu có)
-- original_price == price → mua giá thường; original_price > price → mua lúc đang sale
CREATE TABLE order_detail (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    order_id        INT           NOT NULL REFERENCES app_order(id),
    variant_id      INT           NOT NULL REFERENCES product_variant(id),
    quantity        INT           NOT NULL,
    price           DECIMAL(18,2) NOT NULL,
    original_price  DECIMAL(18,2) NOT NULL
);

-- Voucher giảm giá
CREATE TABLE voucher (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    code             NVARCHAR(50)  NOT NULL UNIQUE,
    description      NVARCHAR(255),
    discount_percent DECIMAL(5,2),
    max_discount     DECIMAL(18,2) NULL,
    min_order_value  DECIMAL(18,2),
    start_date       DATETIME,
    end_date         DATETIME,
    is_active        BIT           NOT NULL DEFAULT 1,
    usage_limit      INT           NULL,
    used_count       INT           NOT NULL DEFAULT 0
);

-- Voucher được áp dụng vào đơn hàng
CREATE TABLE order_voucher (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    order_id        INT           NOT NULL REFERENCES app_order(id),
    voucher_id      INT           NOT NULL REFERENCES voucher(id),
    customer_id     INT           NOT NULL REFERENCES app_user(id),
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    CONSTRAINT UQ_order_voucher UNIQUE (order_id),
    CONSTRAINT UQ_user_voucher  UNIQUE (customer_id, voucher_id)
);

-- Giỏ hàng
CREATE TABLE cart (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT      REFERENCES app_user(id),
    created_at  DATETIME NOT NULL DEFAULT GETDATE()
);

-- Sản phẩm trong giỏ hàng
CREATE TABLE cart_item (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    cart_id    INT NOT NULL REFERENCES cart(id),
    variant_id INT NOT NULL REFERENCES product_variant(id),
    quantity   INT NOT NULL
);

-- Thông báo
CREATE TABLE notification (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT           REFERENCES app_user(id),
    message    NVARCHAR(500) NOT NULL,
    link       NVARCHAR(255),
    is_read    BIT           NOT NULL DEFAULT 0,
    created_at DATETIME      NOT NULL DEFAULT GETDATE()
);

-- Token reset mật khẩu
CREATE TABLE password_reset_token (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    INT          NOT NULL UNIQUE REFERENCES app_user(id),
    expires_at DATETIME2    NOT NULL
);

-- Danh sách yêu thích
CREATE TABLE wishlist (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT      NOT NULL REFERENCES app_user(id),
    product_id INT      NOT NULL REFERENCES product(id),
    added_at   DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT UQ_wishlist UNIQUE (user_id, product_id)
);

-- Bộ sưu tập
CREATE TABLE collection (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    cover_image NVARCHAR(500),
    start_date  DATE,
    end_date    DATE,
    is_active   BIT      DEFAULT 1,
    created_at  DATETIME DEFAULT GETDATE()
);

CREATE TABLE product_collection (
    product_id    INT NOT NULL FOREIGN KEY REFERENCES product(id),
    collection_id INT NOT NULL FOREIGN KEY REFERENCES collection(id),
    PRIMARY KEY (product_id, collection_id)
);
GO

-- ============================================================
--  SEED DATA
-- ============================================================

-- ── ROLES ──────────────────────────────────────────────────
-- id: 1=ROLE_ADMIN | 2=ROLE_CUSTOMER | 3=ROLE_OWNER | 4=ROLE_CASHIER
INSERT INTO role (name) VALUES
(N'ROLE_ADMIN'),
(N'ROLE_CUSTOMER'),
(N'ROLE_OWNER'),
(N'ROLE_CASHIER');
GO

-- ── USERS ──────────────────────────────────────────────────
-- customer bắt đầu từ id=3, cashier1 thêm cuối cùng id=8
INSERT INTO app_user (username, password, email, phone, enabled) VALUES
(N'owner',      N'{noop}owner123', N'owner@casualwear.vn',  N'0900000001', 1),
(N'admin',      N'{noop}admin123', N'admin@casualwear.vn',  N'0900000002', 1),
(N'nguyenvana', N'{noop}pass1234', N'nguyenvana@gmail.com', N'0901234567', 1),
(N'tranthib',   N'{noop}pass1234', N'tranthib@gmail.com',   N'0912345678', 1),
(N'levanc',     N'{noop}pass1234', N'levanc@gmail.com',     N'0923456789', 1),
(N'phamthid',   N'{noop}pass1234', N'phamthid@gmail.com',   N'0934567890', 1),
(N'hoangvane',  N'{noop}pass1234', N'hoangvane@gmail.com',  N'0945678901', 1),
(N'cashier1',   N'{noop}cashier123', N'cashier1@casualwear.vn', N'0900000003', 1);
GO

-- ── USER ROLES ──────────────────────────────────────────────
-- role: 1=ROLE_ADMIN | 2=ROLE_CUSTOMER | 3=ROLE_OWNER | 4=ROLE_CASHIER
INSERT INTO user_role (user_id, role_id) VALUES
(1, 1), (1, 3),                          -- owner    → ROLE_ADMIN + ROLE_OWNER
(2, 1),                                  -- admin    → ROLE_ADMIN
(3, 2), (4, 2), (5, 2), (6, 2), (7, 2),  -- customers → ROLE_CUSTOMER
(8, 4);                                  -- cashier1 → ROLE_CASHIER
GO

-- ── EMPLOYEES ──────────────────────────────────────────────
-- Mỗi tài khoản ADMIN/OWNER/CASHIER đều có 1 bản ghi employee tương ứng
INSERT INTO employee (user_id, employee_code, hire_date, is_active, note) VALUES
(1, N'NV0001', '2023-01-01', 1, N'Owner - chủ cửa hàng'),
(2, N'NV0002', '2023-02-01', 1, N'Admin - quản trị hệ thống'),
(8, N'NV0003', '2024-06-01', 1, N'Cashier - thu ngân quầy 1');
GO

-- ── CATEGORIES ─────────────────────────────────────────────
INSERT INTO category (name, description) VALUES
(N'Áo thun',   N'Áo thun nam các loại chất liệu cotton cao cấp'),
(N'Áo sơ mi',  N'Áo sơ mi nam công sở và dạo phố phong cách'),
(N'Quần',      N'Quần nam các loại từ jean đến sweater'),
(N'Áo hoodie', N'Áo hoodie và sweatshirt nam giữ nhiệt mùa lạnh');
GO

-- ── SIZES ──────────────────────────────────────────────────
INSERT INTO size (name) VALUES (N'S'), (N'M'), (N'L'), (N'XL'), (N'XXL');
GO

-- ── COLORS ─────────────────────────────────────────────────
INSERT INTO color (name) VALUES
(N'Trắng'), (N'Đen'), (N'Xanh navy'), (N'Xám'), (N'Xanh nhạt');
GO

-- ── PRODUCTS ───────────────────────────────────────────────
INSERT INTO product (name, description, price, category_id) VALUES
(N'Áo thun Basic Trắng',
 N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Chất liệu mềm mại, thấm hút mồ hôi tốt, phù hợp mặc hàng ngày.',
 199000, 1),
(N'Áo thun Basic Đen',
 N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Màu đen cổ điển dễ phối đồ, bền màu sau nhiều lần giặt.',
 199000, 1),
(N'Áo thun In Mèo Trắng',
 N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Họa tiết sắc nét không phai màu, phong cách trẻ trung năng động.',
 249000, 1),
(N'Áo thun In Mèo Đen',
 N'Áo thun cotton in hình mèo dễ thương, nền đen nổi bật họa tiết, phong cách streetwear hiện đại.',
 249000, 1),
(N'Áo sơ mi Công sở Trắng',
 N'Áo sơ mi công sở vải lụa mềm mại thoáng mát, form slim fit. Thiết kế cổ đứng thanh lịch, phù hợp đi làm và các dịp trang trọng.',
 350000, 2),
(N'Áo sơ mi Xanh Nhạt',
 N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc, chất cotton pha. Màu sắc nhẹ nhàng dễ phối đồ, thích hợp đi chơi và dạo phố.',
 379000, 2),
(N'Áo sơ mi Xanh Navy',
 N'Áo sơ mi xanh navy lịch sự sang trọng. Chất vải cao cấp ít nhăn, dễ ủi, phù hợp công sở và các buổi gặp gỡ quan trọng.',
 399000, 2),
(N'Quần Jean Layer',
 N'Quần jean layer thiết kế độc đáo phong cách streetwear. Chất jean co giãn thoải mái, form slim fit tôn dáng.',
 549000, 3),
(N'Quần Sweater Đen',
 N'Quần sweater chất nỉ bông dày dặn ấm áp mùa đông. Thiết kế đơn giản dễ phối, có túi hai bên tiện dụng.',
 449000, 3),
(N'Áo Hoodie Đen',
 N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt, form oversize thoải mái. Có mũ điều chỉnh được, túi kangaroo rộng rãi.',
 599000, 4);
GO

-- ── PRODUCT IMAGES ─────────────────────────────────────────
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 4),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 4),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg',  5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg',  5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 8),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 8),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg',  9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg',  9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 10);
GO

-- ── PRODUCT VARIANTS ───────────────────────────────────────
INSERT INTO product_variant (product_id, size_id, color_id, sku, stock, cost_price) VALUES
-- AT Basic Trắng (p=1) → v1-4
(1, 1, 1, N'AT-WHT-S',     20, 95000),
(1, 2, 1, N'AT-WHT-M',     35, 95000),
(1, 3, 1, N'AT-WHT-L',     28, 95000),
(1, 4, 1, N'AT-WHT-XL',    15, 95000),
-- AT Basic Đen (p=2) → v5-8
(2, 1, 2, N'AT-BLK-S',     18, 95000),
(2, 2, 2, N'AT-BLK-M',     30, 95000),
(2, 3, 2, N'AT-BLK-L',     22, 95000),
(2, 4, 2, N'AT-BLK-XL',     3, 95000),
-- AT In Mèo Trắng (p=3) → v9-11
(3, 1, 1, N'AT-CAT-WHT-S', 12, 120000),
(3, 2, 1, N'AT-CAT-WHT-M', 20, 120000),
(3, 3, 1, N'AT-CAT-WHT-L',  8, 120000),
-- AT In Mèo Đen (p=4) → v12-14
(4, 1, 2, N'AT-CAT-BLK-S', 10, 120000),
(4, 2, 2, N'AT-CAT-BLK-M', 18, 120000),
(4, 3, 2, N'AT-CAT-BLK-L',  4, 120000),
-- SM Công sở Trắng (p=5) → v15-18
(5, 1, 1, N'SM-WHT-S',     12, 165000),
(5, 2, 1, N'SM-WHT-M',     25, 165000),
(5, 3, 1, N'SM-WHT-L',     18, 165000),
(5, 4, 1, N'SM-WHT-XL',     2, 165000),
-- SM Xanh Nhạt (p=6) → v19-21
(6, 1, 5, N'SM-LBL-S',     10, 180000),
(6, 2, 5, N'SM-LBL-M',     20, 180000),
(6, 3, 5, N'SM-LBL-L',      8, 180000),
-- SM Xanh Navy (p=7) → v22-24
(7, 1, 3, N'SM-NVY-S',      8, 190000),
(7, 2, 3, N'SM-NVY-M',     15, 190000),
(7, 3, 3, N'SM-NVY-L',     10, 190000),
-- Quần Jean (p=8) → v25-28
(8, 1, 2, N'QJ-LAY-S',     12, 260000),
(8, 2, 2, N'QJ-LAY-M',     20, 260000),
(8, 3, 2, N'QJ-LAY-L',     15, 260000),
(8, 4, 2, N'QJ-LAY-XL',     3, 260000),
-- Quần Sweater (p=9) → v29-31
(9, 1, 2, N'QSW-BLK-S',    10, 215000),
(9, 2, 2, N'QSW-BLK-M',    18, 215000),
(9, 3, 2, N'QSW-BLK-L',     0, 215000),
-- Hoodie Đen (p=10) → v32-35
(10, 1, 2, N'HD-BLK-S',    12, 285000),
(10, 2, 2, N'HD-BLK-M',    20, 285000),
(10, 3, 2, N'HD-BLK-L',    15, 285000),
(10, 4, 2, N'HD-BLK-XL',    2, 285000);
GO

-- ── VARIANT IMAGES ─────────────────────────────────────────
INSERT INTO variant_image (variant_id, image_url, sort_order) VALUES
-- AT Basic Trắng (v1-v4)
(1,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1),
(2,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1),
(3,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 2),
(4,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1),
-- AT Basic Đen (v5-v8)
(5,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 2),
(6,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 1),
(7,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 2),
(8,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 1),
-- AT In Mèo Trắng (v9-v11)
(9,  N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 1),
(10, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 1),
(11, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 1),
-- AT In Mèo Đen (v12-v14)
(12, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 1),
(13, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 1),
(14, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 1),
-- SM Trắng (v15-v18)
(15, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg',  1),
(16, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg',  1),
(17, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg',  2),
(18, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg',  1),
-- SM Xanh Nhạt (v19-v21)
(19, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 2),
(20, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 1),
(21, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 1),
-- SM Xanh Navy (v22-v24)
(22, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 1),
(23, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 1),
(24, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 1),
-- Quần Jean (v25-v28)
(25, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 1),
(26, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 1),
(26, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 2),
(27, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 2),
(28, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 2),
-- Quần Sweater (v29-v31)
(29, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 2),
(30, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg', 1),
(31, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 2),
-- Hoodie Đen (v32-v35)
(32, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 1),
(33, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 1),
(34, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 1),
(35, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 2);
GO

-- ── ADDRESSES ──────────────────────────────────────────────
-- customer: user 3=nguyenvana | 4=tranthib | 5=levanc | 6=phamthid | 7=hoangvane
INSERT INTO address (user_id, full_name, phone, street, city, district, country, is_default) VALUES
(3, N'Nguyễn Văn A', N'0901234567', N'12 Nguyễn Trãi',    N'Hà Nội',  N'Thanh Xuân', N'Vietnam', 1),
(3, N'Nguyễn Văn A', N'0901234567', N'45 Láng Hạ',         N'Hà Nội',  N'Đống Đa',    N'Vietnam', 0),
(4, N'Trần Thị B',   N'0912345678', N'88 Lê Văn Việt',     N'TP.HCM',  N'Quận 9',     N'Vietnam', 1),
(5, N'Lê Văn C',     N'0923456789', N'56 Trần Phú',        N'Đà Nẵng', N'Hải Châu',   N'Vietnam', 1),
(6, N'Phạm Thị D',   N'0934567890', N'23 Nguyễn Văn Cừ',  N'TP.HCM',  N'Quận 5',     N'Vietnam', 1),
(7, N'Hoàng Văn E',  N'0945678901', N'78 Đinh Tiên Hoàng', N'Hà Nội',  N'Hoàn Kiếm',  N'Vietnam', 1);
GO

-- ── CARTS ──────────────────────────────────────────────────
INSERT INTO cart (customer_id) VALUES (3), (4), (5), (6), (7);
GO

-- ── CART ITEMS ─────────────────────────────────────────────
INSERT INTO cart_item (cart_id, variant_id, quantity) VALUES
(1,  2,  2),   -- nguyenvana: AT Trắng M  x2
(1, 26,  1),   -- nguyenvana: Quần Jean M  x1
(2, 16,  1),   -- tranthib  : SM Trắng M   x1
(3, 33,  1),   -- levanc    : Hoodie Đen M  x1
(4, 10,  2),   -- phamthid  : AT In Mèo Trắng M  x2
(5, 30,  1);   -- hoangvane : Quần Sweater M  x1
GO

-- ── VOUCHERS ───────────────────────────────────────────────
INSERT INTO voucher (code, description, discount_percent, max_discount, min_order_value, start_date, end_date, is_active) VALUES
(N'WELCOME10', N'Giảm 10% cho khách hàng mới',               10,  50000,   200000, GETDATE(), DATEADD(DAY, 30, GETDATE()), 1),
(N'SUMMER20',  N'Giảm 20% mùa hè – tối đa 100k',            20, 100000,   500000, GETDATE(), DATEADD(DAY, 60, GETDATE()), 1),
(N'FREESHIP5', N'Giảm 5% không giới hạn đơn tối thiểu',      5,   NULL,        0, GETDATE(), DATEADD(DAY, 90, GETDATE()), 1),
(N'VIP30',     N'Giảm 30% dành cho khách VIP – tối đa 200k', 30, 200000,  1000000, GETDATE(), DATEADD(DAY, 15, GETDATE()), 1),
(N'SALE15',    N'Giảm 15% cuối tuần',                        15,  75000,   300000, GETDATE(), DATEADD(DAY,  7, GETDATE()), 1),
(N'EXPIRED',   N'Voucher đã hết hạn (test)',                  10,   NULL,   100000, DATEADD(DAY,-60,GETDATE()), DATEADD(DAY,-30,GETDATE()), 0);
GO

-- ── SALE BATCHES (từ migration_sale_batch_v2.sql) ────────────
-- Đợt sale gộp nhiều sản phẩm cùng lúc
INSERT INTO sale_batch (name, discount_percent, start_date, end_date) VALUES
(N'Flash Sale Cuối Tuần',   10.00, DATEADD(DAY,-5,GETDATE()), DATEADD(DAY, 2,GETDATE())),
(N'Sale Hè 2026',           15.00, DATEADD(DAY,-1,GETDATE()), DATEADD(DAY, 6,GETDATE()));
GO

-- ── PRODUCT SALES (từ migration_product_sale.sql + sale_batch) ────
-- product 9 (Quần short) đang sale ~10%, bao trùm thời điểm đơn 14 đặt hàng
-- → thuộc batch "Flash Sale Cuối Tuần" (id=1)
-- product 1 (Áo thun Basic Trắng) → thuộc batch "Sale Hè 2026" (id=2)
-- product 5 (Áo sơ mi Công sở Trắng) → sale đơn lẻ, không thuộc batch nào (NULL)
INSERT INTO product_sale (product_id, sale_batch_id, discount_percent, start_date, end_date, is_active) VALUES
(9, 1,    10.00, DATEADD(DAY,-5,GETDATE()), DATEADD(DAY, 2,GETDATE()), 1),
(1, 2,    15.00, DATEADD(DAY,-1,GETDATE()), DATEADD(DAY, 6,GETDATE()), 1),
(5, NULL, 20.00, DATEADD(DAY,-40,GETDATE()), DATEADD(DAY,-30,GETDATE()), 0);
GO

-- ── GOODS RECEIPTS (từ warehouse_migration.sql) ─────────────
-- created_by: admin = user 2
INSERT INTO goods_receipt (code, supplier_name, note, created_by, created_at, total_amount) VALUES
(N'PN-20260801-001', N'Xưởng may Thành Công', N'Nhập bổ sung Áo thun Basic', 2, DATEADD(DAY,-5,GETDATE()), 4750000),
(N'PN-20260803-001', N'Xưởng may An Phát',    N'Nhập hàng Hoodie mùa lạnh', 2, DATEADD(DAY,-2,GETDATE()), 2850000);
GO

INSERT INTO goods_receipt_item (goods_receipt_id, variant_id, quantity, unit_cost_price) VALUES
-- PN-20260801-001: AT Basic Trắng size M + L
(1,  2, 30, 95000),
(1,  3, 20, 95000),
-- PN-20260803-001: Hoodie Đen size M
(2, 33, 10, 285000);
GO

-- ── STOCK MOVEMENT LOG (từ warehouse_migration.sql) ─────────
-- ref_id trỏ tới id tương ứng của goods_receipt
INSERT INTO stock_movement_log (variant_id, change_type, change_qty, balance_after, ref_type, ref_id, note, created_by, created_at) VALUES
(2,  N'IMPORT', 30, 35, N'GOODS_RECEIPT', 1, N'Nhập từ PN-20260801-001', 2, DATEADD(DAY,-5,GETDATE())),
(3,  N'IMPORT', 20, 28, N'GOODS_RECEIPT', 1, N'Nhập từ PN-20260801-001', 2, DATEADD(DAY,-5,GETDATE())),
(33, N'IMPORT', 10, 20, N'GOODS_RECEIPT', 2, N'Nhập từ PN-20260803-001', 2, DATEADD(DAY,-2,GETDATE()));
GO

-- ── ORDERS ─────────────────────────────────────────────────
-- shipping_fee tính theo khu vực giao hàng: Hà Nội 20k | Đà Nẵng 32k | TP.HCM 38k
-- nguyenvana (user 3) – 4 đơn: id 1-4 (giao Hà Nội)
-- Đơn 3 (SHIPPING): có tracking_code và shipped_at — demo flow GHN
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id, shipping_fee, order_code) VALUES
(3, DATEADD(DAY,-30,GETDATE()), N'COMPLETED', 747000, N'COD',   1, NULL,                          NULL,            1, 1, 20000, N'CW00000001'),
(3, DATEADD(DAY,-15,GETDATE()), N'COMPLETED', 549000, N'VNPAY', 1, NULL,                          NULL,            1, 1, 20000, N'CW00000002'),
(3, DATEADD(DAY, -5,GETDATE()), N'SHIPPING',  398000, N'COD',   0, DATEADD(DAY,-4,GETDATE()),     N'GHN-20250001', 2, 2, 20000, N'CW00000003'),
(3, DATEADD(DAY, -1,GETDATE()), N'PENDING',   199000, N'COD',   0, NULL,                          NULL,            1, 1, 20000, N'CW00000004');

-- tranthib (user 4) – 3 đơn: id 5-7 (giao TP.HCM)
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id, shipping_fee, order_code) VALUES
(4, DATEADD(DAY,-20,GETDATE()), N'COMPLETED', 799000, N'VNPAY', 1, NULL, NULL, 3, 3, 38000, N'CW00000005'),
(4, DATEADD(DAY, -8,GETDATE()), N'CONFIRMED', 598000, N'COD',   0, NULL, NULL, 3, 3, 38000, N'CW00000006'),
(4, DATEADD(DAY, -2,GETDATE()), N'PENDING',   249000, N'VNPAY', 0, NULL, NULL, 3, 3, 38000, N'CW00000007');

-- levanc (user 5) – 3 đơn: id 8-10 (giao Đà Nẵng)
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id, shipping_fee, order_code) VALUES
(5, DATEADD(DAY,-25,GETDATE()), N'COMPLETED', 599000, N'COD',   1, NULL,                      NULL,            4, 4, 32000, N'CW00000008'),
(5, DATEADD(DAY, -7,GETDATE()), N'COMPLETED', 599000, N'COD',   1, DATEADD(DAY,-6,GETDATE()), N'GHN-20250002', 4, 4, 32000, N'CW00000009'),
(5, DATEADD(DAY, -3,GETDATE()), N'CONFIRMED', 349000, N'VNPAY', 1, NULL,                      NULL,            4, 4, 32000, N'CW00000010');

-- phamthid (user 6) – 2 đơn: id 11-12 (giao TP.HCM)
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id, shipping_fee, order_code) VALUES
(6, DATEADD(DAY,-10,GETDATE()), N'COMPLETED',  448000, N'COD',   0, DATEADD(DAY,-9,GETDATE()), N'GHN-20250003', 5, 5, 38000, N'CW00000011'),
(6, DATEADD(DAY, -1,GETDATE()), N'PENDING',   498000, N'COD',   0, NULL,                      NULL,            5, 5, 38000, N'CW00000012');

-- hoangvane (user 7) – 2 đơn: id 13-14 (giao Hà Nội)
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id, shipping_fee, order_code) VALUES
(7, DATEADD(DAY,-12,GETDATE()), N'CANCELLED', 199000, N'COD',   0, NULL, NULL, 6, 6, 20000, N'CW00000013'),
(7, DATEADD(DAY, -2,GETDATE()), N'PENDING',   449000, N'VNPAY', 0, NULL, NULL, 6, 6, 20000, N'CW00000014');
GO

-- Đơn bán tại quầy (COUNTER) – id 15, khách nhận hàng trực tiếp nên shipping_fee = 0
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, order_type, cashier_id, shipping_fee, order_code) VALUES
(NULL, DATEADD(DAY,-2,GETDATE()), N'COMPLETED', 294000, N'CASH', 1, N'COUNTER', 8, 0, N'CW00000015');
GO

-- Đơn khách vãng lai đặt online (guest checkout, task 4.1/4.2/6.6) – id 16
-- customer_id NULL + order_type='ONLINE' + guest_email có giá trị → phân biệt với đơn COUNTER
-- Địa chỉ giao hàng của khách vãng lai (address.user_id NULL)
INSERT INTO address (user_id, full_name, phone, street, city, district, country, is_default) VALUES
(NULL, N'Đỗ Văn Khách', N'0956789012', N'101 Cầu Giấy', N'Hà Nội', N'Cầu Giấy', N'Vietnam', 0);
GO

INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id, order_type, shipping_fee, order_code, guest_email) VALUES
(NULL, DATEADD(DAY,-1,GETDATE()), N'PENDING', 219000, N'VNPAY', 1, 7, 7, N'ONLINE', 20000, N'CW00000016', N'guest.khach@gmail.com');
GO

-- Cập nhật delivered_at cho đơn DELIVERED (id=9, levanc)
UPDATE app_order SET delivered_at = DATEADD(DAY,-1,GETDATE()) WHERE id = 9;
GO

-- Cập nhật lý do hủy cho đơn CANCELLED (id=13, hoangvane) – từ cancel_reason_migration.sql
UPDATE app_order
SET cancel_reason = N'CUSTOMER_REQUEST',
    cancel_note   = N'Khách đổi ý, không muốn mua nữa',
    cancelled_by  = 7,
    cancelled_at  = DATEADD(DAY,-12,GETDATE())
WHERE id = 13;
GO

-- ── ORDER DETAILS ───────────────────────────────────────────
INSERT INTO order_detail (order_id, variant_id, quantity, price, original_price) VALUES
-- Đơn 1 (nguyenvana – COMPLETED)
(1,   2, 2, 199000, 199000),
(1,  26, 1, 549000, 549000),
-- Đơn 2 (nguyenvana – COMPLETED)
(2,  27, 1, 549000, 549000),
-- Đơn 3 (nguyenvana – SHIPPING, có tracking GHN-20250001)
(3,  16, 1, 350000, 350000),
(3,   6, 1, 199000, 199000),
-- Đơn 4 (nguyenvana – PENDING)
(4,   1, 1, 199000, 199000),
-- Đơn 5 (tranthib – COMPLETED)
(5,  16, 1, 350000, 350000),
(5,  30, 1, 449000, 449000),
-- Đơn 6 (tranthib – CONFIRMED)
(6,  33, 1, 599000, 599000),
(6,  10, 1, 249000, 249000),
-- Đơn 7 (tranthib – PENDING)
(7,  13, 1, 249000, 249000),
-- Đơn 8 (levanc – COMPLETED)
(8,  33, 1, 599000, 599000),
-- Đơn 9 (levanc – DELIVERED, có tracking GHN-20250002)
(9,  34, 1, 599000, 599000),
-- Đơn 10 (levanc – CONFIRMED)
(10, 20, 1, 379000, 379000),
-- Đơn 11 (phamthid – SHIPPING, có tracking GHN-20250003)
(11, 29, 1, 449000, 449000),
(11,  9, 1, 249000, 249000),
-- Đơn 12 (phamthid – PENDING)
(12, 25, 1, 549000, 549000),
(12,  5, 1, 199000, 199000),
-- Đơn 13 (hoangvane – CANCELLED)
(13,  2, 1, 199000, 199000),
-- Đơn 14 (hoangvane – PENDING)
(14, 30, 1, 449000, 499000),
-- Đơn 15 (COUNTER – bán tại quầy, cashier1)
(15,  2, 1, 199000, 199000),
(15,  9, 1,  95000, 95000),
-- Đơn 16 (guest checkout online, VNPAY, đã thanh toán)
(16,  1, 1, 199000, 199000);
GO

-- ── ORDER VOUCHERS ──────────────────────────────────────────
INSERT INTO order_voucher (order_id, voucher_id, customer_id, discount_amount) VALUES
(1, 1, 3, 50000),
(5, 2, 4, 100000);
GO

-- ── WISHLISTS ──────────────────────────────────────────────
INSERT INTO wishlist (user_id, product_id) VALUES
(3, 5),   -- nguyenvana yêu thích SM Trắng
(3, 10),  -- nguyenvana yêu thích Hoodie Đen
(4, 8),   -- tranthib yêu thích Quần Jean
(4, 3),   -- tranthib yêu thích AT In Mèo Trắng
(5, 10),  -- levanc yêu thích Hoodie Đen
(6, 7),   -- phamthid yêu thích SM Xanh Navy
(7, 9);   -- hoangvane yêu thích Quần Sweater
GO

-- ── NOTIFICATIONS ──────────────────────────────────────────
INSERT INTO notification (user_id, message, link, is_read) VALUES
(3, N'Đơn hàng #1 của bạn đã hoàn thành.',                                          N'/order/detail/1',  1),
(3, N'Đơn hàng #3 đã được gửi đi! Mã vận đơn GHN: GHN-20250001. Tra cứu tại ghn.vn', N'/order/detail/3',  0),
(4, N'Đơn hàng #5 của bạn đã hoàn thành.',                                          N'/order/detail/5',  1),
(4, N'Đơn hàng #6 đã được xác nhận! Chúng tôi đang chuẩn bị hàng.',                N'/order/detail/6',  0),
(5, N'Đơn hàng #9 đã được giao thành công! Vui lòng xác nhận :>',                  N'/order/detail/9',  0),
(6, N'Đơn hàng #11 đã được gửi đi! Mã vận đơn GHN: GHN-20250003. Tra cứu tại ghn.vn', N'/order/detail/11', 0),
(7, N'Đơn hàng #13 của bạn đã bị huỷ.',                                             N'/order/detail/13', 1);
GO
