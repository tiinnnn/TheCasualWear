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
    shipping_address_id INT FOREIGN KEY REFERENCES address(id),
    billing_address_id  INT FOREIGN KEY REFERENCES address(id)
);

CREATE TABLE order_detail (
    id       INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT FOREIGN KEY REFERENCES app_order(id),
    product_id INT FOREIGN KEY REFERENCES product(id),
    quantity INT           NOT NULL,
    price    DECIMAL(18,2) NOT NULL
);

CREATE TABLE voucher (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    code             NVARCHAR(50)  NOT NULL UNIQUE,
    description      NVARCHAR(255),
    discount_percent DECIMAL(5,2),
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

ALTER TABLE order_voucher ADD CONSTRAINT UQ_order_voucher UNIQUE (order_id);
ALTER TABLE order_voucher ADD CONSTRAINT UQ_user_voucher  UNIQUE (customer_id, voucher_id);
GO

-- ========================================
-- ROLES
-- ========================================

INSERT INTO role (name) VALUES
(N'ROLE_ADMIN'),
(N'ROLE_CUSTOMER'),
(N'ROLE_DELIVERY');
GO

-- ========================================
-- USERS
-- id=1 admin | id=2 delivery | id=3,4,5 customer
-- ========================================

INSERT INTO app_user (username, password, email, phone, enabled) VALUES
(N'admin',    N'{noop}admin123',    N'admin@casualwear.vn',    N'0901111111', 1),
(N'delivery', N'{noop}delivery123', N'delivery@casualwear.vn', N'0902222222', 1),
(N'john',     N'{noop}john123',     N'john@gmail.com',         N'0903333333', 1),
(N'jane',     N'{noop}jane123',     N'jane@gmail.com',         N'0904444444', 1),
(N'minh',     N'{noop}minh123',     N'minh@gmail.com',         N'0905555555', 1);
GO

INSERT INTO user_role (user_id, role_id) VALUES
(1, 1), -- admin     → ROLE_ADMIN
(2, 3), -- delivery  → ROLE_DELIVERY
(3, 2), -- john      → ROLE_CUSTOMER
(4, 2), -- jane      → ROLE_CUSTOMER
(5, 2); -- minh      → ROLE_CUSTOMER
GO

-- ========================================
-- CATEGORIES
-- ========================================

INSERT INTO category (name, description) VALUES
(N'Áo thun',   N'Áo thun nam các loại'),
(N'Áo sơ mi',  N'Áo sơ mi nam công sở và dạo phố'),
(N'Quần jean', N'Quần jean nam slim fit và regular'),
(N'Quần kaki', N'Quần kaki nam lịch sự'),
(N'Áo hoodie', N'Áo hoodie và sweatshirt nam');
GO

-- ========================================
-- SIZES
-- ========================================

INSERT INTO size (name) VALUES
(N'S'), (N'M'), (N'L'), (N'XL'), (N'XXL');
GO

-- ========================================
-- COLORS
-- ========================================

INSERT INTO color (name) VALUES
(N'Trắng'), (N'Đen'), (N'Xanh navy'),
(N'Xám'),   (N'Be'),  (N'Xanh rêu');
GO

-- ========================================
-- PRODUCTS (18 sản phẩm)
-- Ảnh mẫu xoay vòng 3 link cloudinary
-- ========================================

DECLARE @img1 NVARCHAR(500) = N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg'
DECLARE @img2 NVARCHAR(500) = N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg'
DECLARE @img3 NVARCHAR(500) = N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg'

-- Áo thun (category 1)
INSERT INTO product (name, description, sku, price, stock, cost_price, category_id, size_id, color_id) VALUES
(N'Áo thun Basic Trắng M',   N'Áo thun cotton 100% thoáng mát, form regular fit', N'AT-WHT-M',  199000, 50, 100000, 1, 2, 1),
(N'Áo thun Basic Đen L',     N'Áo thun cotton 100% thoáng mát, form regular fit', N'AT-BLK-L',  199000, 40, 100000, 1, 3, 2),
(N'Áo thun Basic Xám XL',    N'Áo thun cotton 100% thoáng mát, form regular fit', N'AT-GRY-XL', 199000, 30, 100000, 1, 4, 4),
(N'Áo thun Oversize Be L',   N'Áo thun oversize form rộng thời thượng',           N'AT-BE-L',   249000, 35, 120000, 1, 3, 5),
(N'Áo thun Polo Navy M',     N'Áo polo cổ bẻ chất cotton pique cao cấp',          N'AT-POL-M',  299000, 25, 150000, 1, 2, 3),

-- Áo sơ mi (category 2)
(N'Áo sơ mi Trắng M',        N'Áo sơ mi công sở vải lụa mềm mại',                N'SM-WHT-M',  350000, 20, 175000, 2, 2, 1),
(N'Áo sơ mi Xanh navy L',    N'Áo sơ mi kẻ sọc phong cách Hàn Quốc',             N'SM-NVY-L',  399000, 15, 200000, 2, 3, 3),
(N'Áo sơ mi Be XL',          N'Áo sơ mi oversize dáng suông trẻ trung',           N'SM-BE-XL',  379000, 18, 190000, 2, 4, 5),

-- Quần jean (category 3)
(N'Quần jean Slimfit Đen 30', N'Quần jean co giãn 4 chiều ôm vừa',                N'QJ-BLK-30', 499000, 20, 250000, 3, 2, 2),
(N'Quần jean Slimfit Xanh 32',N'Quần jean xanh indigo phối đá nhẹ',               N'QJ-BLU-32', 499000, 25, 250000, 3, 3, 3),
(N'Quần jean Regular Xám 34', N'Quần jean form rộng thoải mái năng động',          N'QJ-GRY-34', 549000, 15, 275000, 3, 4, 4),

-- Quần kaki (category 4)
(N'Quần kaki Be M',           N'Quần kaki cotton slim fit thanh lịch',             N'QK-BE-M',   399000, 30, 200000, 4, 2, 5),
(N'Quần kaki Đen L',          N'Quần kaki đen công sở dễ phối đồ',                N'QK-BLK-L',  399000, 25, 200000, 4, 3, 2),
(N'Quần kaki Xám XL',         N'Quần kaki xám form straight thoải mái',            N'QK-GRY-XL', 419000, 20, 210000, 4, 4, 4),

-- Áo hoodie (category 5)
(N'Áo hoodie Đen M',          N'Áo hoodie nỉ bông giữ nhiệt mùa đông',            N'HD-BLK-M',  599000, 20, 300000, 5, 2, 2),
(N'Áo hoodie Xám L',          N'Áo hoodie form rộng streetwear',                   N'HD-GRY-L',  599000, 15, 300000, 5, 3, 4),
(N'Áo hoodie Xanh rêu XL',    N'Áo hoodie màu earth tone thời thượng',             N'HD-GRN-XL', 649000, 12, 325000, 5, 4, 6),
(N'Áo hoodie Xanh navy S',    N'Áo hoodie zip dây kéo tiện lợi',                  N'HD-NVY-S',  679000, 10, 340000, 5, 1, 3);
GO

-- ========================================
-- PRODUCT IMAGES (mỗi sản phẩm 1 ảnh, xoay vòng 3 link)
-- ========================================

INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg', 4),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg', 5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg', 8),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg', 9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg', 11),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg', 12),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg', 13),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg', 14),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg', 15),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780410/4f16884e6c034f04fd8df26d27937189_io6eah.jpg', 16),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/1da64ed144cd440047208e378d34651b_csttse.jpg', 17),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774780411/bc7b6a867df9ae7a7860b1404a387e77_ca5wb2.jpg', 18);
GO

-- ========================================
-- ADDRESSES
-- ========================================

INSERT INTO address (user_id, full_name, phone, street, city, district, country, is_default) VALUES
(3, N'John Doe',   N'0903333333', N'123 Đường Lê Lợi',     N'Hà Nội',      N'Hoàn Kiếm', N'Vietnam', 1),
(3, N'John Doe',   N'0903333333', N'456 Đường Nguyễn Huệ', N'Hà Nội',      N'Đống Đa',   N'Vietnam', 0),
(4, N'Jane Smith', N'0904444444', N'789 Đường Trần Hưng Đạo', N'TP.HCM',   N'Quận 1',    N'Vietnam', 1),
(5, N'Minh Tran',  N'0905555555', N'321 Đường Hai Bà Trưng',  N'Đà Nẵng',  N'Hải Châu',  N'Vietnam', 1);
GO

-- ========================================
-- CARTS
-- ========================================

INSERT INTO cart (customer_id) VALUES (3), (4), (5);
GO

INSERT INTO cart_item (cart_id, product_id, quantity) VALUES
(1, 1, 2),
(1, 9, 1),
(2, 5, 1),
(2, 12, 2),
(3, 15, 1);
GO

-- ========================================
-- VOUCHERS
-- ========================================

INSERT INTO voucher (code, description, discount_percent, min_order_value, start_date, end_date, is_active) VALUES
(N'WELCOME10',   N'Giảm 10% cho khách hàng mới',       10, 200000, GETDATE(), DATEADD(DAY, 30,  GETDATE()), 1),
(N'SUMMER20',    N'Giảm 20% mùa hè',                   20, 500000, GETDATE(), DATEADD(DAY, 60,  GETDATE()), 1),
(N'FREESHIP',    N'Giảm 5% áp dụng mọi đơn hàng',       5,      0, GETDATE(), DATEADD(DAY, 90,  GETDATE()), 1),
(N'VIP30',       N'Giảm 30% dành cho khách VIP',        30, 1000000, GETDATE(), DATEADD(DAY, 15, GETDATE()), 1),
(N'EXPIRED',     N'Voucher đã hết hạn',                 15, 300000, DATEADD(DAY, -60, GETDATE()), DATEADD(DAY, -30, GETDATE()), 0);
GO

-- ========================================
-- ORDERS (nhiều đơn hàng mẫu)
-- ========================================

-- john (user 3) - 3 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, shipping_address_id, billing_address_id) VALUES
(3, DATEADD(DAY, -20, GETDATE()), N'COMPLETED', 597000,  1, 1),
(3, DATEADD(DAY, -10, GETDATE()), N'SHIPPING',  499000,  1, 1),
(3, DATEADD(DAY, -1,  GETDATE()), N'PENDING',   199000,  1, 1);

-- jane (user 4) - 2 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, shipping_address_id, billing_address_id) VALUES
(4, DATEADD(DAY, -15, GETDATE()), N'COMPLETED', 748000, 3, 3),
(4, DATEADD(DAY, -3,  GETDATE()), N'CONFIRMED', 399000, 3, 3);

-- minh (user 5) - 2 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, shipping_address_id, billing_address_id) VALUES
(5, DATEADD(DAY, -7,  GETDATE()), N'DELIVERED', 599000, 4, 4),
(5, DATEADD(DAY, -2,  GETDATE()), N'PENDING',   649000, 4, 4);
GO

-- ========================================
-- ORDER DETAILS
-- ========================================

-- Đơn 1 (john - COMPLETED)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(1, 1, 2, 199000),
(1, 9, 1, 499000);

-- Đơn 2 (john - SHIPPING)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(2, 9, 1, 499000);

-- Đơn 3 (john - PENDING)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(3, 1, 1, 199000);

-- Đơn 4 (jane - COMPLETED)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(4, 5,  1, 299000),
(4, 12, 1, 399000);

-- Đơn 5 (jane - CONFIRMED)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(5, 13, 1, 399000);

-- Đơn 6 (minh - DELIVERED)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(6, 15, 1, 599000);

-- Đơn 7 (minh - PENDING)
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(7, 17, 1, 649000);
GO

-- ========================================
-- ORDER VOUCHERS
-- ========================================

INSERT INTO order_voucher (order_id, voucher_id, customer_id) VALUES
(1, 1, 3),  -- john dùng WELCOME10
(4, 2, 4);  -- jane dùng SUMMER20
GO
