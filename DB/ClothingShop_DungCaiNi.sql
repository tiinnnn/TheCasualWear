DROP DATABASE IF EXISTS ClothingShop;
GO
CREATE DATABASE ClothingShop;
GO
USE ClothingShop;
GO

-- ========================================
-- TABLES
-- ========================================

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

CREATE TABLE color (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE product (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100)  NOT NULL,
    description NVARCHAR(MAX),
    sku         NVARCHAR(50)   UNIQUE,
    price       DECIMAL(18,2)  NOT NULL,
    stock       INT            NOT NULL DEFAULT 0,
    cost_price  DECIMAL(18,2)  NOT NULL DEFAULT 0,
    is_deleted  BIT            DEFAULT 0,
    category_id INT FOREIGN KEY REFERENCES category(id),
    size_id     INT FOREIGN KEY REFERENCES size(id),
    color_id    INT FOREIGN KEY REFERENCES color(id),
    created_at  DATETIME       DEFAULT GETDATE()
);

CREATE TABLE product_image (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    image_url  NVARCHAR(500) NOT NULL,
    product_id INT FOREIGN KEY REFERENCES product(id)
);

CREATE TABLE cart (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT FOREIGN KEY REFERENCES app_user(id),
    created_at  DATETIME DEFAULT GETDATE()
);

CREATE TABLE cart_item (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    cart_id    INT FOREIGN KEY REFERENCES cart(id),
    product_id INT FOREIGN KEY REFERENCES product(id),
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
    customer_id         INT FOREIGN KEY REFERENCES app_user(id),
    order_date          DATETIME      DEFAULT GETDATE(),
    status              NVARCHAR(20)  DEFAULT 'PENDING',
    total_price         DECIMAL(18,2),
    payment_method      NVARCHAR(20)  DEFAULT 'COD',
    is_paid             BIT           DEFAULT 0,
    delivered_at        DATETIME      NULL,
    shipping_address_id INT FOREIGN KEY REFERENCES address(id),
    billing_address_id  INT FOREIGN KEY REFERENCES address(id)
);

CREATE TABLE order_detail (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    order_id   INT FOREIGN KEY REFERENCES app_order(id),
    product_id INT FOREIGN KEY REFERENCES product(id),
    quantity   INT           NOT NULL,
    price      DECIMAL(18,2) NOT NULL
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
    order_id    INT FOREIGN KEY REFERENCES app_order(id),
    voucher_id  INT FOREIGN KEY REFERENCES voucher(id),
    customer_id INT FOREIGN KEY REFERENCES app_user(id)
);

CREATE TABLE notification (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT FOREIGN KEY REFERENCES app_user(id),
    message    NVARCHAR(500) NOT NULL,
    link       NVARCHAR(255),
    is_read    BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE password_reset_token (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    INT NOT NULL UNIQUE,
    expires_at DATETIME2 NOT NULL,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

ALTER TABLE order_voucher ADD CONSTRAINT UQ_order_voucher UNIQUE (order_id);
ALTER TABLE order_voucher ADD CONSTRAINT UQ_user_voucher  UNIQUE (customer_id, voucher_id);
GO

-- ========================================
-- ROLES
-- id: 1=ADMIN | 2=CUSTOMER | 3=DELIVERY | 4=OWNER
-- ========================================
INSERT INTO role (name) VALUES
(N'ROLE_ADMIN'),
(N'ROLE_CUSTOMER'),
(N'ROLE_DELIVERY'),
(N'ROLE_OWNER');
GO

-- ========================================
-- USERS
-- id=1 owner | id=2 admin | id=3 delivery | id=4-8 customer
-- ========================================
INSERT INTO app_user (username, password, email, phone, enabled) VALUES
(N'owner',    N'{noop}owner123',    N'owner@casualwear.vn',    N'0900000001', 1),
(N'admin',    N'{noop}admin123',    N'admin@casualwear.vn',    N'0900000002', 1),
(N'delivery', N'{noop}delivery123', N'delivery@casualwear.vn', N'0900000003', 1),
(N'nguyenvana', N'{noop}pass1234', N'nguyenvana@gmail.com',   N'0901234567', 1),
(N'tranthib',   N'{noop}pass1234', N'tranthib@gmail.com',     N'0912345678', 1),
(N'levanc',     N'{noop}pass1234', N'levanc@gmail.com',       N'0923456789', 1),
(N'phamthid',   N'{noop}pass1234', N'phamthid@gmail.com',     N'0934567890', 1),
(N'hoangvane',  N'{noop}pass1234', N'hoangvane@gmail.com',    N'0945678901', 1);
GO

INSERT INTO user_role (user_id, role_id) VALUES
(1, 1), (1, 4), -- owner → ADMIN + OWNER
(2, 1),         -- admin → ADMIN
(3, 3),         -- delivery → DELIVERY
(4, 2),         -- nguyenvana → CUSTOMER
(5, 2),         -- tranthib → CUSTOMER
(6, 2),         -- levanc → CUSTOMER
(7, 2),         -- phamthid → CUSTOMER
(8, 2);         -- hoangvane → CUSTOMER
GO

-- ========================================
-- CATEGORIES
-- id: 1=Áo thun | 2=Áo sơ mi | 3=Quần | 4=Áo hoodie
-- ========================================
INSERT INTO category (name, description) VALUES
(N'Áo thun',   N'Áo thun nam các loại chất liệu cotton cao cấp'),
(N'Áo sơ mi',  N'Áo sơ mi nam công sở và dạo phố phong cách'),
(N'Quần',      N'Quần nam các loại từ jean đến sweater'),
(N'Áo hoodie', N'Áo hoodie và sweatshirt nam giữ nhiệt mùa lạnh');
GO

-- ========================================
-- SIZES
-- id: 1=S | 2=M | 3=L | 4=XL | 5=XXL
-- ========================================
INSERT INTO size (name) VALUES
(N'S'), (N'M'), (N'L'), (N'XL'), (N'XXL');
GO

-- ========================================
-- COLORS
-- id: 1=Trắng | 2=Đen | 3=Xanh navy | 4=Xám | 5=Xanh nhạt
-- ========================================
INSERT INTO color (name) VALUES
(N'Trắng'),
(N'Đen'),
(N'Xanh navy'),
(N'Xám'),
(N'Xanh nhạt');
GO

-- ========================================
-- PRODUCTS (35 variants, 10 nhóm)
-- Nhóm 1: Áo thun Basic Trắng   (cat=1, color=1) S,M,L,XL  → id 1-4
-- Nhóm 2: Áo thun Basic Đen     (cat=1, color=2) S,M,L,XL  → id 5-8
-- Nhóm 3: Áo thun In Mèo Trắng  (cat=1, color=1) S,M,L     → id 9-11
-- Nhóm 4: Áo thun In Mèo Đen    (cat=1, color=2) S,M,L     → id 12-14
-- Nhóm 5: Áo sơ mi Trắng        (cat=2, color=1) S,M,L,XL  → id 15-18
-- Nhóm 6: Áo sơ mi Xanh Nhạt    (cat=2, color=5) S,M,L     → id 19-21
-- Nhóm 7: Áo sơ mi Xanh Navy    (cat=2, color=3) S,M,L     → id 22-24
-- Nhóm 8: Quần Jean Layer        (cat=3, color=2) S,M,L,XL  → id 25-28
-- Nhóm 9: Quần Sweater Đen       (cat=3, color=2) S,M,L     → id 29-31
-- Nhóm 10: Áo Hoodie Đen         (cat=4, color=2) S,M,L,XL  → id 32-35
-- ========================================

INSERT INTO product (name, description, sku, price, stock, cost_price, category_id, size_id, color_id) VALUES
-- Nhóm 1: Áo thun Basic Trắng
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Chất liệu mềm mại, thấm hút mồ hôi tốt, phù hợp mặc hàng ngày.', N'AT-WHT-S',  199000, 20, 95000, 1, 1, 1),
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Chất liệu mềm mại, thấm hút mồ hôi tốt, phù hợp mặc hàng ngày.', N'AT-WHT-M',  199000, 35, 95000, 1, 2, 1),
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Chất liệu mềm mại, thấm hút mồ hôi tốt, phù hợp mặc hàng ngày.', N'AT-WHT-L',  199000, 28, 95000, 1, 3, 1),
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Chất liệu mềm mại, thấm hút mồ hôi tốt, phù hợp mặc hàng ngày.', N'AT-WHT-XL', 199000, 15, 95000, 1, 4, 1),

-- Nhóm 2: Áo thun Basic Đen
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Màu đen cổ điển dễ phối đồ, bền màu sau nhiều lần giặt.', N'AT-BLK-S',  199000, 18, 95000, 1, 1, 2),
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Màu đen cổ điển dễ phối đồ, bền màu sau nhiều lần giặt.', N'AT-BLK-M',  199000, 30, 95000, 1, 2, 2),
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Màu đen cổ điển dễ phối đồ, bền màu sau nhiều lần giặt.', N'AT-BLK-L',  199000, 22, 95000, 1, 3, 2),
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Màu đen cổ điển dễ phối đồ, bền màu sau nhiều lần giặt.', N'AT-BLK-XL', 199000,  3, 95000, 1, 4, 2),

-- Nhóm 3: Áo thun In Mèo Trắng
(N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Họa tiết sắc nét không phai màu, phong cách trẻ trung năng động.', N'AT-CAT-WHT-S', 249000, 12, 120000, 1, 1, 1),
(N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Họa tiết sắc nét không phai màu, phong cách trẻ trung năng động.', N'AT-CAT-WHT-M', 249000, 20, 120000, 1, 2, 1),
(N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Họa tiết sắc nét không phai màu, phong cách trẻ trung năng động.', N'AT-CAT-WHT-L', 249000,  8, 120000, 1, 3, 1),

-- Nhóm 4: Áo thun In Mèo Đen
(N'Áo thun In Mèo Đen',   N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Nền đen nổi bật họa tiết, phong cách streetwear hiện đại.', N'AT-CAT-BLK-S', 249000, 10, 120000, 1, 1, 2),
(N'Áo thun In Mèo Đen',   N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Nền đen nổi bật họa tiết, phong cách streetwear hiện đại.', N'AT-CAT-BLK-M', 249000, 18, 120000, 1, 2, 2),
(N'Áo thun In Mèo Đen',   N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Nền đen nổi bật họa tiết, phong cách streetwear hiện đại.', N'AT-CAT-BLK-L', 249000,  4, 120000, 1, 3, 2),

-- Nhóm 5: Áo sơ mi Công sở Trắng
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát, form slim fit. Thiết kế cổ đứng thanh lịch, phù hợp đi làm và các dịp trang trọng.', N'SM-WHT-S',  350000, 12, 165000, 2, 1, 1),
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát, form slim fit. Thiết kế cổ đứng thanh lịch, phù hợp đi làm và các dịp trang trọng.', N'SM-WHT-M',  350000, 25, 165000, 2, 2, 1),
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát, form slim fit. Thiết kế cổ đứng thanh lịch, phù hợp đi làm và các dịp trang trọng.', N'SM-WHT-L',  350000, 18, 165000, 2, 3, 1),
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát, form slim fit. Thiết kế cổ đứng thanh lịch, phù hợp đi làm và các dịp trang trọng.', N'SM-WHT-XL', 350000,  2, 165000, 2, 4, 1),

-- Nhóm 6: Áo sơ mi Xanh Nhạt
(N'Áo sơ mi Xanh Nhạt',   N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc, chất cotton pha. Màu sắc nhẹ nhàng dễ phối đồ, thích hợp đi chơi và dạo phố.', N'SM-LBL-S',  379000, 10, 180000, 2, 1, 5),
(N'Áo sơ mi Xanh Nhạt',   N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc, chất cotton pha. Màu sắc nhẹ nhàng dễ phối đồ, thích hợp đi chơi và dạo phố.', N'SM-LBL-M',  379000, 20, 180000, 2, 2, 5),
(N'Áo sơ mi Xanh Nhạt',   N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc, chất cotton pha. Màu sắc nhẹ nhàng dễ phối đồ, thích hợp đi chơi và dạo phố.', N'SM-LBL-L',  379000,  8, 180000, 2, 3, 5),

-- Nhóm 7: Áo sơ mi Xanh Navy
(N'Áo sơ mi Xanh Navy',   N'Áo sơ mi xanh navy lịch sự sang trọng. Chất vải cao cấp ít nhăn, dễ ủi, phù hợp công sở và các buổi gặp gỡ quan trọng.', N'SM-NVY-S',  399000,  8, 190000, 2, 1, 3),
(N'Áo sơ mi Xanh Navy',   N'Áo sơ mi xanh navy lịch sự sang trọng. Chất vải cao cấp ít nhăn, dễ ủi, phù hợp công sở và các buổi gặp gỡ quan trọng.', N'SM-NVY-M',  399000, 15, 190000, 2, 2, 3),
(N'Áo sơ mi Xanh Navy',   N'Áo sơ mi xanh navy lịch sự sang trọng. Chất vải cao cấp ít nhăn, dễ ủi, phù hợp công sở và các buổi gặp gỡ quan trọng.', N'SM-NVY-L',  399000, 10, 190000, 2, 3, 3),

-- Nhóm 8: Quần Jean Layer
(N'Quần Jean Layer',       N'Quần jean layer thiết kế độc đáo phong cách streetwear. Chất jean co giãn thoải mái, form slim fit tôn dáng, phù hợp mọi hoạt động.', N'QJ-LAY-S',  549000, 12, 260000, 3, 1, 2),
(N'Quần Jean Layer',       N'Quần jean layer thiết kế độc đáo phong cách streetwear. Chất jean co giãn thoải mái, form slim fit tôn dáng, phù hợp mọi hoạt động.', N'QJ-LAY-M',  549000, 20, 260000, 3, 2, 2),
(N'Quần Jean Layer',       N'Quần jean layer thiết kế độc đáo phong cách streetwear. Chất jean co giãn thoải mái, form slim fit tôn dáng, phù hợp mọi hoạt động.', N'QJ-LAY-L',  549000, 15, 260000, 3, 3, 2),
(N'Quần Jean Layer',       N'Quần jean layer thiết kế độc đáo phong cách streetwear. Chất jean co giãn thoải mái, form slim fit tôn dáng, phù hợp mọi hoạt động.', N'QJ-LAY-XL', 549000,  3, 260000, 3, 4, 2),

-- Nhóm 9: Quần Sweater Đen
(N'Quần Sweater Đen',      N'Quần sweater chất nỉ bông dày dặn ấm áp mùa đông. Thiết kế đơn giản dễ phối, có túi hai bên tiện dụng.', N'QSW-BLK-S',  449000, 10, 215000, 3, 1, 2),
(N'Quần Sweater Đen',      N'Quần sweater chất nỉ bông dày dặn ấm áp mùa đông. Thiết kế đơn giản dễ phối, có túi hai bên tiện dụng.', N'QSW-BLK-M',  449000, 18, 215000, 3, 2, 2),
(N'Quần Sweater Đen',      N'Quần sweater chất nỉ bông dày dặn ấm áp mùa đông. Thiết kế đơn giản dễ phối, có túi hai bên tiện dụng.', N'QSW-BLK-L',  449000,  0, 215000, 3, 3, 2),

-- Nhóm 10: Áo Hoodie Đen
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt, form oversize thoải mái. Có mũ điều chỉnh được, túi kangaroo rộng rãi, dây rút chắc chắn.', N'HD-BLK-S',  599000, 12, 285000, 4, 1, 2),
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt, form oversize thoải mái. Có mũ điều chỉnh được, túi kangaroo rộng rãi, dây rút chắc chắn.', N'HD-BLK-M',  599000, 20, 285000, 4, 2, 2),
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt, form oversize thoải mái. Có mũ điều chỉnh được, túi kangaroo rộng rãi, dây rút chắc chắn.', N'HD-BLK-L',  599000, 15, 285000, 4, 3, 2),
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt, form oversize thoải mái. Có mũ điều chỉnh được, túi kangaroo rộng rãi, dây rút chắc chắn.', N'HD-BLK-XL', 599000,  2, 285000, 4, 4, 2);
GO

-- ========================================
-- PRODUCT IMAGES
-- ========================================

-- Nhóm 1: Áo thun Basic Trắng (id 1-4)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 4),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 4);

-- Nhóm 2: Áo thun Basic Đen (id 5-8)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 8),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 8);

-- Nhóm 3: Áo thun In Mèo Trắng (id 9-11)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 11),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 11);

-- Nhóm 4: Áo thun In Mèo Đen (id 12-14)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 12),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 12),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 13),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 13),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 14),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 14);

-- Nhóm 5: Áo sơ mi Trắng (id 15-18)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 15),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 15),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 16),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 16),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 17),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 17),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 18),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 18);

-- Nhóm 6: Áo sơ mi Xanh Nhạt (id 19-21)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 19),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 20),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 21);

-- Nhóm 7: Áo sơ mi Xanh Navy (id 22-24)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 22),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 23),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 24);

-- Nhóm 8: Quần Jean Layer (id 25-28)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 25),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 25),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 26),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 26),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 27),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 27),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 28),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 28);

-- Nhóm 9: Quần Sweater Đen (id 29-31)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg', 29),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 29),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/d7b16901da465b18025722_jjg0ey.jpg', 29),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg', 30),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 30),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/d7b16901da465b18025722_jjg0ey.jpg', 30),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg', 31),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 31),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/d7b16901da465b18025722_jjg0ey.jpg', 31);

-- Nhóm 10: Áo Hoodie Đen (id 32-35)
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 32),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 32),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/08f0ec795f3ede60872f31_dxv2t5.jpg', 32),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 33),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 33),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/08f0ec795f3ede60872f31_dxv2t5.jpg', 33),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 34),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 34),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/08f0ec795f3ede60872f31_dxv2t5.jpg', 34),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 35),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 35),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/08f0ec795f3ede60872f31_dxv2t5.jpg', 35);
GO

-- ========================================
-- ADDRESSES
-- ========================================
INSERT INTO address (user_id, full_name, phone, street, city, district, country, is_default) VALUES
-- Nguyễn Văn A (user 4) - 2 địa chỉ
(4, N'Nguyễn Văn A', N'0901234567', N'12 Nguyễn Trãi',         N'Hà Nội',  N'Thanh Xuân', N'Vietnam', 1),
(4, N'Nguyễn Văn A', N'0901234567', N'45 Láng Hạ',             N'Hà Nội',  N'Đống Đa',    N'Vietnam', 0),
-- Trần Thị B (user 5)
(5, N'Trần Thị B',   N'0912345678', N'88 Lê Văn Việt',         N'TP.HCM',  N'Quận 9',     N'Vietnam', 1),
-- Lê Văn C (user 6)
(6, N'Lê Văn C',     N'0923456789', N'56 Trần Phú',            N'Đà Nẵng', N'Hải Châu',   N'Vietnam', 1),
-- Phạm Thị D (user 7)
(7, N'Phạm Thị D',   N'0934567890', N'23 Nguyễn Văn Cừ',      N'TP.HCM',  N'Quận 5',     N'Vietnam', 1),
-- Hoàng Văn E (user 8)
(8, N'Hoàng Văn E',  N'0945678901', N'78 Đinh Tiên Hoàng',     N'Hà Nội',  N'Hoàn Kiếm',  N'Vietnam', 1);
GO

-- ========================================
-- CARTS (mỗi customer 1 cart)
-- ========================================
INSERT INTO cart (customer_id) VALUES (4), (5), (6), (7), (8);
GO

INSERT INTO cart_item (cart_id, product_id, quantity) VALUES
(1, 2,  2),  -- Nguyễn Văn A: Áo thun Basic Trắng M x2
(1, 26, 1),  -- Nguyễn Văn A: Quần Jean Layer M x1
(2, 16, 1),  -- Trần Thị B: Áo sơ mi Trắng M x1
(3, 33, 1),  -- Lê Văn C: Áo Hoodie Đen M x1
(4, 10, 2),  -- Phạm Thị D: Áo thun In Mèo Trắng M x2
(5, 30, 1);  -- Hoàng Văn E: Quần Sweater Đen M x1
GO

-- ========================================
-- VOUCHERS
-- ========================================
INSERT INTO voucher (code, description, discount_percent, max_discount, min_order_value, start_date, end_date, is_active) VALUES
(N'WELCOME10',  N'Giảm 10% cho khách hàng mới',          10, 50000,   200000,  GETDATE(), DATEADD(DAY, 30,  GETDATE()), 1),
(N'SUMMER20',   N'Giảm 20% mùa hè - tối đa 100k',        20, 100000,  500000,  GETDATE(), DATEADD(DAY, 60,  GETDATE()), 1),
(N'FREESHIP5',  N'Giảm 5% không giới hạn đơn tối thiểu',  5, NULL,         0,  GETDATE(), DATEADD(DAY, 90,  GETDATE()), 1),
(N'VIP30',      N'Giảm 30% dành cho khách VIP - tối đa 200k', 30, 200000, 1000000, GETDATE(), DATEADD(DAY, 15, GETDATE()), 1),
(N'SALE15',     N'Giảm 15% cuối tuần',                   15, 75000,   300000,  GETDATE(), DATEADD(DAY, 7,   GETDATE()), 1),
(N'EXPIRED',    N'Voucher đã hết hạn (test)',             10, NULL,    100000,  DATEADD(DAY, -60, GETDATE()), DATEADD(DAY, -30, GETDATE()), 0);
GO

-- ========================================
-- ORDERS (đa dạng trạng thái, nhiều đơn)
-- ========================================

-- Nguyễn Văn A (user 4) - 4 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(4, DATEADD(DAY, -30, GETDATE()), N'COMPLETED', 747000,  N'COD',   1, 1, 1),
(4, DATEADD(DAY, -15, GETDATE()), N'COMPLETED', 549000,  N'VNPAY', 1, 1, 1),
(4, DATEADD(DAY, -5,  GETDATE()), N'SHIPPING',  398000,  N'COD',   0, 2, 2),
(4, DATEADD(DAY, -1,  GETDATE()), N'PENDING',   199000,  N'COD',   0, 1, 1);

-- Trần Thị B (user 5) - 3 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(5, DATEADD(DAY, -20, GETDATE()), N'COMPLETED', 799000,  N'VNPAY', 1, 3, 3),
(5, DATEADD(DAY, -8,  GETDATE()), N'CONFIRMED', 598000,  N'COD',   0, 3, 3),
(5, DATEADD(DAY, -2,  GETDATE()), N'PENDING',   249000,  N'VNPAY', 0, 3, 3);

-- Lê Văn C (user 6) - 3 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(6, DATEADD(DAY, -25, GETDATE()), N'COMPLETED', 599000,  N'COD',   1, 4, 4),
(6, DATEADD(DAY, -7,  GETDATE()), N'DELIVERED', 599000,  N'COD',   0, 4, 4),
(6, DATEADD(DAY, -3,  GETDATE()), N'CONFIRMED', 349000,  N'VNPAY', 1, 4, 4);

-- Phạm Thị D (user 7) - 2 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(7, DATEADD(DAY, -10, GETDATE()), N'SHIPPING',  448000,  N'COD',   0, 5, 5),
(7, DATEADD(DAY, -1,  GETDATE()), N'PENDING',   498000,  N'COD',   0, 5, 5);

-- Hoàng Văn E (user 8) - 2 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, payment_method, is_paid, shipping_address_id, billing_address_id) VALUES
(8, DATEADD(DAY, -12, GETDATE()), N'CANCELLED', 199000,  N'COD',   0, 6, 6),
(8, DATEADD(DAY, -2,  GETDATE()), N'PENDING',   449000,  N'VNPAY', 0, 6, 6);
GO

-- Update delivered_at cho đơn DELIVERED
UPDATE app_order SET delivered_at = DATEADD(DAY, -1, GETDATE())
WHERE id = 10; -- Lê Văn C DELIVERED
GO

-- ========================================
-- ORDER DETAILS
-- ========================================

-- Đơn 1 (Nguyễn Văn A - COMPLETED): Áo thun trắng M x2 + Quần jean layer M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(1, 2,  2, 199000),
(1, 26, 1, 549000);

-- Đơn 2 (Nguyễn Văn A - COMPLETED): Quần jean layer L x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(2, 27, 1, 549000);

-- Đơn 3 (Nguyễn Văn A - SHIPPING): Áo sơ mi trắng M x1 + Áo thun đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(3, 16, 1, 350000),
(3,  6, 1, 199000);

-- Đơn 4 (Nguyễn Văn A - PENDING): Áo thun trắng S x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(4, 1, 1, 199000);

-- Đơn 5 (Trần Thị B - COMPLETED): Áo sơ mi trắng M x1 + Quần sweater đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(5, 16, 1, 350000),
(5, 30, 1, 449000);

-- Đơn 6 (Trần Thị B - CONFIRMED): Áo hoodie đen M x1 + Áo thun mèo trắng M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(6, 33, 1, 599000),
(6, 10, 1, 249000);

-- Đơn 7 (Trần Thị B - PENDING): Áo thun mèo đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(7, 13, 1, 249000);

-- Đơn 8 (Lê Văn C - COMPLETED): Áo hoodie đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(8, 33, 1, 599000);

-- Đơn 9 (Lê Văn C - DELIVERED): Áo hoodie đen L x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(9, 34, 1, 599000);

-- Đơn 10 (Lê Văn C - CONFIRMED): Áo sơ mi xanh nhạt M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(10, 20, 1, 379000);

-- Đơn 11 (Phạm Thị D - SHIPPING): Quần sweater đen S x1 + Áo thun mèo trắng S x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(11, 29, 1, 449000),
(11,  9, 1, 249000);

-- Đơn 12 (Phạm Thị D - PENDING): Quần jean layer S x1 + Áo thun đen S x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(12, 25, 1, 549000),
(12,  5, 1, 199000);

-- Đơn 13 (Hoàng Văn E - CANCELLED): Áo thun trắng M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(13, 2, 1, 199000);

-- Đơn 14 (Hoàng Văn E - PENDING): Quần sweater đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(14, 30, 1, 449000);
GO

-- ========================================
-- ORDER VOUCHERS
-- ========================================
INSERT INTO order_voucher (order_id, voucher_id, customer_id) VALUES
(1, 1, 4),  -- Nguyễn Văn A đơn 1 dùng WELCOME10
(5, 2, 5);  -- Trần Thị B đơn 5 dùng SUMMER20
GO
