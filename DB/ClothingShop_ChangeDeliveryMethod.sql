-- ============================================================
--  ClothingShop  –  Schema sạch + Seed Data
--  v5 – Bỏ ROLE_DELIVERY / order_assignment / delivery_profile
--       Thêm tracking_code + shipped_at vào app_order (tích hợp GHN)
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
-- id: 1=ROLE_ADMIN | 2=ROLE_CUSTOMER | 3=ROLE_OWNER
CREATE TABLE role (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Users
-- id: 1=owner | 2=admin | 3-7=customer
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
    price_adjustment DECIMAL(18,2)          DEFAULT 0,
    created_at       DATETIME      NOT NULL DEFAULT GETDATE()
);

-- Ảnh riêng cho từng variant
CREATE TABLE variant_image (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    variant_id INT           NOT NULL REFERENCES product_variant(id),
    image_url  NVARCHAR(500) NOT NULL,
    sort_order INT           NOT NULL DEFAULT 0
);

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

-- Đơn hàng
-- tracking_code : mã vận đơn GHN nhân viên nhập thủ công
-- shipped_at    : thời điểm admin xác nhận gửi hàng cho GHN
CREATE TABLE app_order (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    customer_id         INT           REFERENCES app_user(id),
    order_date          DATETIME      NOT NULL DEFAULT GETDATE(),
    status              NVARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_price         DECIMAL(18,2),
    payment_method      NVARCHAR(20)  NOT NULL DEFAULT 'COD',
    is_paid             BIT           NOT NULL DEFAULT 0,
    delivered_at        DATETIME      NULL,
    shipped_at          DATETIME      NULL,
    tracking_code       NVARCHAR(50)  NULL,
    shipping_address_id INT           REFERENCES address(id),
    billing_address_id  INT           REFERENCES address(id)
);

-- Chi tiết đơn hàng
CREATE TABLE order_detail (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    order_id   INT           NOT NULL REFERENCES app_order(id),
    variant_id INT           NOT NULL REFERENCES product_variant(id),
    quantity   INT           NOT NULL,
    price      DECIMAL(18,2) NOT NULL
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
    is_active        BIT           NOT NULL DEFAULT 1
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
-- Bỏ ROLE_DELIVERY: chỉ còn ADMIN, CUSTOMER, OWNER
INSERT INTO role (name) VALUES
(N'ROLE_ADMIN'),
(N'ROLE_CUSTOMER'),
(N'ROLE_OWNER');
GO

-- ── USERS ──────────────────────────────────────────────────
-- Bỏ user 'delivery' (user_id=3 cũ), customer bắt đầu từ id=3
INSERT INTO app_user (username, password, email, phone, enabled) VALUES
(N'owner',      N'{noop}owner123', N'owner@casualwear.vn',  N'0900000001', 1),
(N'admin',      N'{noop}admin123', N'admin@casualwear.vn',  N'0900000002', 1),
(N'nguyenvana', N'{noop}pass1234', N'nguyenvana@gmail.com', N'0901234567', 1),
(N'tranthib',   N'{noop}pass1234', N'tranthib@gmail.com',   N'0912345678', 1),
(N'levanc',     N'{noop}pass1234', N'levanc@gmail.com',     N'0923456789', 1),
(N'phamthid',   N'{noop}pass1234', N'phamthid@gmail.com',   N'0934567890', 1),
(N'hoangvane',  N'{noop}pass1234', N'hoangvane@gmail.com',  N'0945678901', 1);
GO

-- ── USER ROLES ──────────────────────────────────────────────
-- role: 1=ROLE_ADMIN | 2=ROLE_CUSTOMER | 3=ROLE_OWNER
INSERT INTO user_role (user_id, role_id) VALUES
(1, 1), (1, 3),                          -- owner  → ROLE_ADMIN + ROLE_OWNER
(2, 1),                                  -- admin  → ROLE_ADMIN
(3, 2), (4, 2), (5, 2), (6, 2), (7, 2); -- customers → ROLE_CUSTOMER
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
INSERT INTO product_variant (product_id, size_id, color_id, sku, stock, cost_price, price_adjustment) VALUES
-- AT Basic Trắng (p=1) → v1-4
(1, 1, 1, N'AT-WHT-S',     20, 95000,  0),
(1, 2, 1, N'AT-WHT-M',     35, 95000,  0),
(1, 3, 1, N'AT-WHT-L',     28, 95000,  0),
(1, 4, 1, N'AT-WHT-XL',    15, 95000,  0),
-- AT Basic Đen (p=2) → v5-8
(2, 1, 2, N'AT-BLK-S',     18, 95000,  0),
(2, 2, 2, N'AT-BLK-M',     30, 95000,  0),
(2, 3, 2, N'AT-BLK-L',     22, 95000,  0),
(2, 4, 2, N'AT-BLK-XL',     3, 95000,  0),
-- AT In Mèo Trắng (p=3) → v9-11
(3, 1, 1, N'AT-CAT-WHT-S', 12, 120000, 0),
(3, 2, 1, N'AT-CAT-WHT-M', 20, 120000, 0),
(3, 3, 1, N'AT-CAT-WHT-L',  8, 120000, 0),
-- AT In Mèo Đen (p=4) → v12-14
(4, 1, 2, N'AT-CAT-BLK-S', 10, 120000, 0),
(4, 2, 2, N'AT-CAT-BLK-M', 18, 120000, 0),
(4, 3, 2, N'AT-CAT-BLK-L',  4, 120000, 0),
-- SM Công sở Trắng (p=5) → v15-18
(5, 1, 1, N'SM-WHT-S',     12, 165000, 0),
(5, 2, 1, N'SM-WHT-M',     25, 165000, 0),
(5, 3, 1, N'SM-WHT-L',     18, 165000, 0),
(5, 4, 1, N'SM-WHT-XL',     2, 165000, 0),
-- SM Xanh Nhạt (p=6) → v19-21
(6, 1, 5, N'SM-LBL-S',     10, 180000, 0),
(6, 2, 5, N'SM-LBL-M',     20, 180000, 0),
(6, 3, 5, N'SM-LBL-L',      8, 180000, 0),
-- SM Xanh Navy (p=7) → v22-24
(7, 1, 3, N'SM-NVY-S',      8, 190000, 0),
(7, 2, 3, N'SM-NVY-M',     15, 190000, 0),
(7, 3, 3, N'SM-NVY-L',     10, 190000, 0),
-- Quần Jean (p=8) → v25-28
(8, 1, 2, N'QJ-LAY-S',     12, 260000, 0),
(8, 2, 2, N'QJ-LAY-M',     20, 260000, 0),
(8, 3, 2, N'QJ-LAY-L',     15, 260000, 0),
(8, 4, 2, N'QJ-LAY-XL',     3, 260000, 0),
-- Quần Sweater (p=9) → v29-31
(9, 1, 2, N'QSW-BLK-S',    10, 215000, 0),
(9, 2, 2, N'QSW-BLK-M',    18, 215000, 0),
(9, 3, 2, N'QSW-BLK-L',     0, 215000, 0),
-- Hoodie Đen (p=10) → v32-35
(10, 1, 2, N'HD-BLK-S',    12, 285000, 0),
(10, 2, 2, N'HD-BLK-M',    20, 285000, 0),
(10, 3, 2, N'HD-BLK-L',    15, 285000, 0),
(10, 4, 2, N'HD-BLK-XL',    2, 285000, 0);
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

-- ── ORDERS ─────────────────────────────────────────────────
-- nguyenvana (user 3) – 4 đơn: id 1-4
-- Đơn 3 (SHIPPING): có tracking_code và shipped_at — demo flow GHN
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id) VALUES
(3, DATEADD(DAY,-30,GETDATE()), N'COMPLETED', 747000, N'COD',   1, NULL,                          NULL,            1, 1),
(3, DATEADD(DAY,-15,GETDATE()), N'COMPLETED', 549000, N'VNPAY', 1, NULL,                          NULL,            1, 1),
(3, DATEADD(DAY, -5,GETDATE()), N'SHIPPING',  398000, N'COD',   0, DATEADD(DAY,-4,GETDATE()),     N'GHN-20250001', 2, 2),
(3, DATEADD(DAY, -1,GETDATE()), N'PENDING',   199000, N'COD',   0, NULL,                          NULL,            1, 1);

-- tranthib (user 4) – 3 đơn: id 5-7
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id) VALUES
(4, DATEADD(DAY,-20,GETDATE()), N'COMPLETED', 799000, N'VNPAY', 1, NULL, NULL, 3, 3),
(4, DATEADD(DAY, -8,GETDATE()), N'CONFIRMED', 598000, N'COD',   0, NULL, NULL, 3, 3),
(4, DATEADD(DAY, -2,GETDATE()), N'PENDING',   249000, N'VNPAY', 0, NULL, NULL, 3, 3);

-- levanc (user 5) – 3 đơn: id 8-10
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id) VALUES
(5, DATEADD(DAY,-25,GETDATE()), N'COMPLETED', 599000, N'COD',   1, NULL,                      NULL,            4, 4),
(5, DATEADD(DAY, -7,GETDATE()), N'DELIVERED', 599000, N'COD',   1, DATEADD(DAY,-6,GETDATE()), N'GHN-20250002', 4, 4),
(5, DATEADD(DAY, -3,GETDATE()), N'CONFIRMED', 349000, N'VNPAY', 1, NULL,                      NULL,            4, 4);

-- phamthid (user 6) – 2 đơn: id 11-12
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id) VALUES
(6, DATEADD(DAY,-10,GETDATE()), N'SHIPPING',  448000, N'COD',   0, DATEADD(DAY,-9,GETDATE()), N'GHN-20250003', 5, 5),
(6, DATEADD(DAY, -1,GETDATE()), N'PENDING',   498000, N'COD',   0, NULL,                      NULL,            5, 5);

-- hoangvane (user 7) – 2 đơn: id 13-14
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipped_at, tracking_code, shipping_address_id, billing_address_id) VALUES
(7, DATEADD(DAY,-12,GETDATE()), N'CANCELLED', 199000, N'COD',   0, NULL, NULL, 6, 6),
(7, DATEADD(DAY, -2,GETDATE()), N'PENDING',   449000, N'VNPAY', 0, NULL, NULL, 6, 6);
GO

-- Cập nhật delivered_at cho đơn DELIVERED (id=9, levanc)
UPDATE app_order SET delivered_at = DATEADD(DAY,-1,GETDATE()) WHERE id = 9;
GO

-- ── ORDER DETAILS ───────────────────────────────────────────
INSERT INTO order_detail (order_id, variant_id, quantity, price) VALUES
-- Đơn 1 (nguyenvana – COMPLETED)
(1,   2, 2, 199000),
(1,  26, 1, 549000),
-- Đơn 2 (nguyenvana – COMPLETED)
(2,  27, 1, 549000),
-- Đơn 3 (nguyenvana – SHIPPING, có tracking GHN-20250001)
(3,  16, 1, 350000),
(3,   6, 1, 199000),
-- Đơn 4 (nguyenvana – PENDING)
(4,   1, 1, 199000),
-- Đơn 5 (tranthib – COMPLETED)
(5,  16, 1, 350000),
(5,  30, 1, 449000),
-- Đơn 6 (tranthib – CONFIRMED)
(6,  33, 1, 599000),
(6,  10, 1, 249000),
-- Đơn 7 (tranthib – PENDING)
(7,  13, 1, 249000),
-- Đơn 8 (levanc – COMPLETED)
(8,  33, 1, 599000),
-- Đơn 9 (levanc – DELIVERED, có tracking GHN-20250002)
(9,  34, 1, 599000),
-- Đơn 10 (levanc – CONFIRMED)
(10, 20, 1, 379000),
-- Đơn 11 (phamthid – SHIPPING, có tracking GHN-20250003)
(11, 29, 1, 449000),
(11,  9, 1, 249000),
-- Đơn 12 (phamthid – PENDING)
(12, 25, 1, 549000),
(12,  5, 1, 199000),
-- Đơn 13 (hoangvane – CANCELLED)
(13,  2, 1, 199000),
-- Đơn 14 (hoangvane – PENDING)
(14, 30, 1, 449000);
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