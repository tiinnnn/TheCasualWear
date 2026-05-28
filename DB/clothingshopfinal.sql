-- ============================================================
--  ClothingShop  –  Script hoàn chỉnh (schema + seed data)
--  Phiên bản: có product_variant, color_id FK
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

CREATE TABLE app_user (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    username    NVARCHAR(50)  NOT NULL UNIQUE,
    password    NVARCHAR(255) NOT NULL,
    email       NVARCHAR(100) UNIQUE,
    phone       NVARCHAR(20),
    enabled     BIT           DEFAULT 1,
    created_at  DATETIME      DEFAULT GETDATE()
);

CREATE TABLE role (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE category (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(255)
);

CREATE TABLE size (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(20) NOT NULL UNIQUE
);

-- id: 1=Trắng | 2=Đen | 3=Xanh navy | 4=Xám | 5=Xanh nhạt
CREATE TABLE color (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Mỗi bản ghi = 1 sản phẩm gốc (không còn size_id / color_id / stock / sku)
CREATE TABLE product (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100)  NOT NULL,
    description NVARCHAR(MAX),
    price       DECIMAL(18,2)  NOT NULL,   -- giá tham chiếu
    is_deleted  BIT            DEFAULT 0,
    category_id INT            FOREIGN KEY REFERENCES category(id),
    created_at  DATETIME       DEFAULT GETDATE()
);

CREATE TABLE product_image (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    image_url  NVARCHAR(500) NOT NULL,
    product_id INT NOT NULL   FOREIGN KEY REFERENCES product(id)
);

-- Mỗi bản ghi = 1 biến thể (size + màu) của sản phẩm
CREATE TABLE product_variant (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    product_id       INT           NOT NULL FOREIGN KEY REFERENCES product(id),
    size_id          INT                    FOREIGN KEY REFERENCES size(id),
    color_id         INT                    FOREIGN KEY REFERENCES color(id),
    sku              NVARCHAR(50)  UNIQUE,
    stock            INT           NOT NULL DEFAULT 0,
    cost_price       DECIMAL(18,2) NOT NULL DEFAULT 0,
    price_adjustment DECIMAL(18,2)          DEFAULT 0,  -- chênh lệch so với product.price
    created_at       DATETIME               DEFAULT GETDATE()
);

CREATE TABLE cart (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT FOREIGN KEY REFERENCES app_user(id),
    created_at  DATETIME DEFAULT GETDATE()
);

CREATE TABLE cart_item (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    cart_id    INT NOT NULL FOREIGN KEY REFERENCES cart(id),
    product_id INT NOT NULL FOREIGN KEY REFERENCES product(id),
    variant_id INT NOT NULL FOREIGN KEY REFERENCES product_variant(id),
    quantity   INT NOT NULL
);

CREATE TABLE address (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT FOREIGN KEY REFERENCES app_user(id),
    full_name  NVARCHAR(100) NOT NULL,
    phone      NVARCHAR(20)  NOT NULL,
    street     NVARCHAR(255) NOT NULL,
    city       NVARCHAR(100) NOT NULL,
    district   NVARCHAR(100),
    country    NVARCHAR(100) DEFAULT N'Vietnam',
    is_default BIT           DEFAULT 0
);

CREATE TABLE app_order (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    customer_id         INT           FOREIGN KEY REFERENCES app_user(id),
    order_date          DATETIME      DEFAULT GETDATE(),
    status              NVARCHAR(20)  DEFAULT 'PENDING',
    total_price         DECIMAL(18,2),
    payment_method      NVARCHAR(20)  DEFAULT 'COD',
    is_paid             BIT           DEFAULT 0,
    delivered_at        DATETIME      NULL,
    shipping_address_id INT           FOREIGN KEY REFERENCES address(id),
    billing_address_id  INT           FOREIGN KEY REFERENCES address(id)
);

CREATE TABLE order_detail (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    order_id   INT           NOT NULL FOREIGN KEY REFERENCES app_order(id),
    product_id INT           NOT NULL FOREIGN KEY REFERENCES product(id),
    variant_id INT           NOT NULL FOREIGN KEY REFERENCES product_variant(id),
    quantity   INT           NOT NULL,
    price      DECIMAL(18,2) NOT NULL   -- snapshot giá tại thời điểm mua
);

CREATE TABLE voucher (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    code             NVARCHAR(50)  NOT NULL UNIQUE,
    description      NVARCHAR(255),
    discount_percent DECIMAL(5,2),
    max_discount     DECIMAL(18,2) NULL,
    min_order_value  DECIMAL(18,2),
    start_date       DATETIME,
    end_date         DATETIME,
    is_active        BIT DEFAULT 1
);

CREATE TABLE order_voucher (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    order_id    INT NOT NULL FOREIGN KEY REFERENCES app_order(id),
    voucher_id  INT NOT NULL FOREIGN KEY REFERENCES voucher(id),
    customer_id INT NOT NULL FOREIGN KEY REFERENCES app_user(id)
);

CREATE TABLE notification (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT FOREIGN KEY REFERENCES app_user(id),
    message    NVARCHAR(500) NOT NULL,
    link       NVARCHAR(255),
    is_read    BIT      DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE password_reset_token (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    INT          NOT NULL UNIQUE,
    expires_at DATETIME2    NOT NULL,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

-- Mỗi đơn chỉ dùng 1 voucher; mỗi user chỉ dùng 1 lần mỗi voucher
ALTER TABLE order_voucher ADD CONSTRAINT UQ_order_voucher UNIQUE (order_id);
ALTER TABLE order_voucher ADD CONSTRAINT UQ_user_voucher  UNIQUE (customer_id, voucher_id);
GO

-- ============================================================
--  SEED DATA
-- ============================================================

-- ── ROLES ──────────────────────────────────────────────────
-- id: 1=ADMIN | 2=CUSTOMER | 3=DELIVERY | 4=OWNER
INSERT INTO role (name) VALUES
(N'ROLE_ADMIN'),
(N'ROLE_CUSTOMER'),
(N'ROLE_DELIVERY'),
(N'ROLE_OWNER');
GO

-- ── USERS ──────────────────────────────────────────────────
-- id: 1=owner | 2=admin | 3=delivery | 4-8=customer
INSERT INTO app_user (username, password, email, phone, enabled) VALUES
(N'owner',      N'{noop}owner123',    N'owner@casualwear.vn',    N'0900000001', 1),
(N'admin',      N'{noop}admin123',    N'admin@casualwear.vn',    N'0900000002', 1),
(N'delivery',   N'{noop}delivery123', N'delivery@casualwear.vn', N'0900000003', 1),
(N'nguyenvana', N'{noop}pass1234',    N'nguyenvana@gmail.com',   N'0901234567', 1),
(N'tranthib',   N'{noop}pass1234',    N'tranthib@gmail.com',     N'0912345678', 1),
(N'levanc',     N'{noop}pass1234',    N'levanc@gmail.com',       N'0923456789', 1),
(N'phamthid',   N'{noop}pass1234',    N'phamthid@gmail.com',     N'0934567890', 1),
(N'hoangvane',  N'{noop}pass1234',    N'hoangvane@gmail.com',    N'0945678901', 1);
GO

INSERT INTO user_role (user_id, role_id) VALUES
(1,1),(1,4),   -- owner  → ADMIN + OWNER
(2,1),         -- admin  → ADMIN
(3,3),         -- delivery → DELIVERY
(4,2),(5,2),(6,2),(7,2),(8,2);  -- customers
GO

-- ── CATEGORIES ─────────────────────────────────────────────
-- id: 1=Áo thun | 2=Áo sơ mi | 3=Quần | 4=Áo hoodie
INSERT INTO category (name, description) VALUES
(N'Áo thun',   N'Áo thun nam các loại chất liệu cotton cao cấp'),
(N'Áo sơ mi',  N'Áo sơ mi nam công sở và dạo phố phong cách'),
(N'Quần',      N'Quần nam các loại từ jean đến sweater'),
(N'Áo hoodie', N'Áo hoodie và sweatshirt nam giữ nhiệt mùa lạnh');
GO

-- ── SIZES ──────────────────────────────────────────────────
-- id: 1=S | 2=M | 3=L | 4=XL | 5=XXL
INSERT INTO size (name) VALUES
(N'S'),(N'M'),(N'L'),(N'XL'),(N'XXL');
GO

-- ── COLORS ─────────────────────────────────────────────────
-- id: 1=Trắng | 2=Đen | 3=Xanh navy | 4=Xám | 5=Xanh nhạt
INSERT INTO color (name) VALUES
(N'Trắng'),
(N'Đen'),
(N'Xanh navy'),
(N'Xám'),
(N'Xanh nhạt');
GO

-- ── PRODUCTS (10 sản phẩm gốc) ─────────────────────────────
-- id: 1=AT Basic Trắng | 2=AT Basic Đen | 3=AT In Mèo Trắng
--     4=AT In Mèo Đen  | 5=SM Trắng     | 6=SM Xanh Nhạt
--     7=SM Xanh Navy   | 8=Quần Jean     | 9=Quần Sweater
--    10=Hoodie Đen
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

-- ── PRODUCT_VARIANTS ───────────────────────────────────────
-- Quy tắc id:
--   p=1  AT Basic Trắng   → v1-4   (S,M,L,XL  / màu Trắng)
--   p=2  AT Basic Đen     → v5-8   (S,M,L,XL  / màu Đen)
--   p=3  AT In Mèo Trắng  → v9-11  (S,M,L     / màu Trắng)
--   p=4  AT In Mèo Đen    → v12-14 (S,M,L     / màu Đen)
--   p=5  SM Trắng         → v15-18 (S,M,L,XL  / màu Trắng)
--   p=6  SM Xanh Nhạt     → v19-21 (S,M,L     / màu Xanh nhạt)
--   p=7  SM Xanh Navy     → v22-24 (S,M,L     / màu Xanh navy)
--   p=8  Quần Jean        → v25-28 (S,M,L,XL  / màu Đen)
--   p=9  Quần Sweater     → v29-31 (S,M,L     / màu Đen)
--   p=10 Hoodie Đen       → v32-35 (S,M,L,XL  / màu Đen)
INSERT INTO product_variant (product_id, size_id, color_id, sku, stock, cost_price, price_adjustment) VALUES
-- Áo thun Basic Trắng (p=1, color=1/Trắng)
(1,1,1,N'AT-WHT-S',  20,95000,0),
(1,2,1,N'AT-WHT-M',  35,95000,0),
(1,3,1,N'AT-WHT-L',  28,95000,0),
(1,4,1,N'AT-WHT-XL', 15,95000,0),
-- Áo thun Basic Đen (p=2, color=2/Đen)
(2,1,2,N'AT-BLK-S',  18,95000,0),
(2,2,2,N'AT-BLK-M',  30,95000,0),
(2,3,2,N'AT-BLK-L',  22,95000,0),
(2,4,2,N'AT-BLK-XL',  3,95000,0),
-- Áo thun In Mèo Trắng (p=3, color=1/Trắng)
(3,1,1,N'AT-CAT-WHT-S', 12,120000,0),
(3,2,1,N'AT-CAT-WHT-M', 20,120000,0),
(3,3,1,N'AT-CAT-WHT-L',  8,120000,0),
-- Áo thun In Mèo Đen (p=4, color=2/Đen)
(4,1,2,N'AT-CAT-BLK-S', 10,120000,0),
(4,2,2,N'AT-CAT-BLK-M', 18,120000,0),
(4,3,2,N'AT-CAT-BLK-L',  4,120000,0),
-- Áo sơ mi Công sở Trắng (p=5, color=1/Trắng)
(5,1,1,N'SM-WHT-S',  12,165000,0),
(5,2,1,N'SM-WHT-M',  25,165000,0),
(5,3,1,N'SM-WHT-L',  18,165000,0),
(5,4,1,N'SM-WHT-XL',  2,165000,0),
-- Áo sơ mi Xanh Nhạt (p=6, color=5/Xanh nhạt)
(6,1,5,N'SM-LBL-S',  10,180000,0),
(6,2,5,N'SM-LBL-M',  20,180000,0),
(6,3,5,N'SM-LBL-L',   8,180000,0),
-- Áo sơ mi Xanh Navy (p=7, color=3/Xanh navy)
(7,1,3,N'SM-NVY-S',   8,190000,0),
(7,2,3,N'SM-NVY-M',  15,190000,0),
(7,3,3,N'SM-NVY-L',  10,190000,0),
-- Quần Jean Layer (p=8, color=2/Đen)
(8,1,2,N'QJ-LAY-S',  12,260000,0),
(8,2,2,N'QJ-LAY-M',  20,260000,0),
(8,3,2,N'QJ-LAY-L',  15,260000,0),
(8,4,2,N'QJ-LAY-XL',  3,260000,0),
-- Quần Sweater Đen (p=9, color=2/Đen)
(9,1,2,N'QSW-BLK-S', 10,215000,0),
(9,2,2,N'QSW-BLK-M', 18,215000,0),
(9,3,2,N'QSW-BLK-L',  0,215000,0),
-- Áo Hoodie Đen (p=10, color=2/Đen)
(10,1,2,N'HD-BLK-S',  12,285000,0),
(10,2,2,N'HD-BLK-M',  20,285000,0),
(10,3,2,N'HD-BLK-L',  15,285000,0),
(10,4,2,N'HD-BLK-XL',  2,285000,0);
GO

-- ── PRODUCT IMAGES ─────────────────────────────────────────
-- Mỗi product 2 ảnh (ảnh dùng chung cho mọi variant của cùng product)
INSERT INTO product_image (image_url, product_id) VALUES
-- p=1 Áo thun Basic Trắng
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg',  1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg',  1),
-- p=2 Áo thun Basic Đen
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg',  2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg',  2),
-- p=3 Áo thun In Mèo Trắng
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg',  3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg',  3),
-- p=4 Áo thun In Mèo Đen
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg',  4),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg',  4),
-- p=5 Áo sơ mi Công sở Trắng
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg',   5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg',   5),
-- p=6 Áo sơ mi Xanh Nhạt
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg',  6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg',  6),
-- p=7 Áo sơ mi Xanh Navy
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg',  7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg',  7),
-- p=8 Quần Jean Layer
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg',  8),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg',  8),
-- p=9 Quần Sweater Đen
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg',  9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg',  9),
-- p=10 Áo Hoodie Đen
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 10);
GO

-- ── ADDRESSES ──────────────────────────────────────────────
INSERT INTO address (user_id, full_name, phone, street, city, district, country, is_default) VALUES
(4, N'Nguyễn Văn A', N'0901234567', N'12 Nguyễn Trãi',       N'Hà Nội',   N'Thanh Xuân', N'Vietnam', 1),
(4, N'Nguyễn Văn A', N'0901234567', N'45 Láng Hạ',            N'Hà Nội',   N'Đống Đa',    N'Vietnam', 0),
(5, N'Trần Thị B',   N'0912345678', N'88 Lê Văn Việt',        N'TP.HCM',   N'Quận 9',     N'Vietnam', 1),
(6, N'Lê Văn C',     N'0923456789', N'56 Trần Phú',           N'Đà Nẵng',  N'Hải Châu',   N'Vietnam', 1),
(7, N'Phạm Thị D',   N'0934567890', N'23 Nguyễn Văn Cừ',     N'TP.HCM',   N'Quận 5',     N'Vietnam', 1),
(8, N'Hoàng Văn E',  N'0945678901', N'78 Đinh Tiên Hoàng',    N'Hà Nội',   N'Hoàn Kiếm',  N'Vietnam', 1);
GO

-- ── CARTS (mỗi customer 1 cart) ────────────────────────────
-- id: 1=nguyenvana | 2=tranthib | 3=levanc | 4=phamthid | 5=hoangvane
INSERT INTO cart (customer_id) VALUES (4),(5),(6),(7),(8);
GO

-- cart_item tham chiếu cả product_id lẫn variant_id
INSERT INTO cart_item (cart_id, product_id, variant_id, quantity) VALUES
(1, 1,  2,  2),   -- Nguyễn Văn A: AT Trắng / M  x2
(1, 8,  26, 1),   -- Nguyễn Văn A: Quần Jean / M  x1
(2, 5,  16, 1),   -- Trần Thị B  : SM Trắng / M   x1
(3, 10, 33, 1),   -- Lê Văn C    : Hoodie Đen / M  x1
(4, 3,  10, 2),   -- Phạm Thị D  : AT In Mèo Trắng / M  x2
(5, 9,  30, 1);   -- Hoàng Văn E : Quần Sweater / M  x1
GO

-- ── VOUCHERS ───────────────────────────────────────────────
INSERT INTO voucher (code, description, discount_percent, max_discount, min_order_value, start_date, end_date, is_active) VALUES
(N'WELCOME10', N'Giảm 10% cho khách hàng mới',               10,  50000,  200000, GETDATE(), DATEADD(DAY, 30, GETDATE()), 1),
(N'SUMMER20',  N'Giảm 20% mùa hè - tối đa 100k',            20, 100000,  500000, GETDATE(), DATEADD(DAY, 60, GETDATE()), 1),
(N'FREESHIP5', N'Giảm 5% không giới hạn đơn tối thiểu',      5,   NULL,       0, GETDATE(), DATEADD(DAY, 90, GETDATE()), 1),
(N'VIP30',     N'Giảm 30% dành cho khách VIP - tối đa 200k', 30, 200000, 1000000, GETDATE(), DATEADD(DAY, 15, GETDATE()), 1),
(N'SALE15',    N'Giảm 15% cuối tuần',                        15,  75000,  300000, GETDATE(), DATEADD(DAY,  7, GETDATE()), 1),
(N'EXPIRED',   N'Voucher đã hết hạn (test)',                  10,   NULL,  100000, DATEADD(DAY,-60,GETDATE()), DATEADD(DAY,-30,GETDATE()), 0);
GO

-- ── ORDERS ─────────────────────────────────────────────────
-- Nguyễn Văn A (user 4) – 4 đơn: id 1-4
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(4, DATEADD(DAY,-30,GETDATE()), N'COMPLETED', 747000, N'COD',   1, 1, 1),
(4, DATEADD(DAY,-15,GETDATE()), N'COMPLETED', 549000, N'VNPAY', 1, 1, 1),
(4, DATEADD(DAY, -5,GETDATE()), N'SHIPPING',  398000, N'COD',   0, 2, 2),
(4, DATEADD(DAY, -1,GETDATE()), N'PENDING',   199000, N'COD',   0, 1, 1);

-- Trần Thị B (user 5) – 3 đơn: id 5-7
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(5, DATEADD(DAY,-20,GETDATE()), N'COMPLETED', 799000, N'VNPAY', 1, 3, 3),
(5, DATEADD(DAY, -8,GETDATE()), N'CONFIRMED', 598000, N'COD',   0, 3, 3),
(5, DATEADD(DAY, -2,GETDATE()), N'PENDING',   249000, N'VNPAY', 0, 3, 3);

-- Lê Văn C (user 6) – 3 đơn: id 8-10
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(6, DATEADD(DAY,-25,GETDATE()), N'COMPLETED', 599000, N'COD',   1, 4, 4),
(6, DATEADD(DAY, -7,GETDATE()), N'DELIVERED', 599000, N'COD',   0, 4, 4),
(6, DATEADD(DAY, -3,GETDATE()), N'CONFIRMED', 349000, N'VNPAY', 1, 4, 4);

-- Phạm Thị D (user 7) – 2 đơn: id 11-12
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(7, DATEADD(DAY,-10,GETDATE()), N'SHIPPING',  448000, N'COD',   0, 5, 5),
(7, DATEADD(DAY, -1,GETDATE()), N'PENDING',   498000, N'COD',   0, 5, 5);

-- Hoàng Văn E (user 8) – 2 đơn: id 13-14
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(8, DATEADD(DAY,-12,GETDATE()), N'CANCELLED', 199000, N'COD',   0, 6, 6),
(8, DATEADD(DAY, -2,GETDATE()), N'PENDING',   449000, N'VNPAY', 0, 6, 6);
GO

-- Cập nhật ngày giao cho đơn DELIVERED (id=9)
UPDATE app_order SET delivered_at = DATEADD(DAY,-1,GETDATE()) WHERE id = 9;
GO

-- ── ORDER DETAILS ───────────────────────────────────────────
-- Cột price lưu snapshot giá tại thời điểm mua
INSERT INTO order_detail (order_id, product_id, variant_id, quantity, price) VALUES
-- Đơn 1 (Nguyễn Văn A – COMPLETED)
(1,  1,  2,  2, 199000),   -- AT Trắng M  x2
(1,  8,  26, 1, 549000),   -- Quần Jean M  x1
-- Đơn 2 (Nguyễn Văn A – COMPLETED)
(2,  8,  27, 1, 549000),   -- Quần Jean L  x1
-- Đơn 3 (Nguyễn Văn A – SHIPPING)
(3,  5,  16, 1, 350000),   -- SM Trắng M  x1
(3,  2,   6, 1, 199000),   -- AT Đen M  x1
-- Đơn 4 (Nguyễn Văn A – PENDING)
(4,  1,   1, 1, 199000),   -- AT Trắng S  x1
-- Đơn 5 (Trần Thị B – COMPLETED)
(5,  5,  16, 1, 350000),   -- SM Trắng M  x1
(5,  9,  30, 1, 449000),   -- Quần Sweater M  x1
-- Đơn 6 (Trần Thị B – CONFIRMED)
(6,  10, 33, 1, 599000),   -- Hoodie M  x1
(6,  3,  10, 1, 249000),   -- AT In Mèo Trắng M  x1
-- Đơn 7 (Trần Thị B – PENDING)
(7,  4,  13, 1, 249000),   -- AT In Mèo Đen M  x1
-- Đơn 8 (Lê Văn C – COMPLETED)
(8,  10, 33, 1, 599000),   -- Hoodie M  x1
-- Đơn 9 (Lê Văn C – DELIVERED)
(9,  10, 34, 1, 599000),   -- Hoodie L  x1
-- Đơn 10 (Lê Văn C – CONFIRMED)
(10, 6,  20, 1, 379000),   -- SM Xanh Nhạt M  x1
-- Đơn 11 (Phạm Thị D – SHIPPING)
(11, 9,  29, 1, 449000),   -- Quần Sweater S  x1
(11, 3,   9, 1, 249000),   -- AT In Mèo Trắng S  x1
-- Đơn 12 (Phạm Thị D – PENDING)
(12, 8,  25, 1, 549000),   -- Quần Jean S  x1
(12, 2,   5, 1, 199000),   -- AT Đen S  x1
-- Đơn 13 (Hoàng Văn E – CANCELLED)
(13, 1,   2, 1, 199000),   -- AT Trắng M  x1
-- Đơn 14 (Hoàng Văn E – PENDING)
(14, 9,  30, 1, 449000);   -- Quần Sweater M  x1
GO

-- ── ORDER VOUCHERS ──────────────────────────────────────────
INSERT INTO order_voucher (order_id, voucher_id, customer_id) VALUES
(1, 1, 4),   -- Nguyễn Văn A đơn 1 dùng WELCOME10
(5, 2, 5);   -- Trần Thị B đơn 5 dùng SUMMER20
GO