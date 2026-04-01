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
INSERT INTO role (name) VALUES ('ROLE_OWNER');

-- Gán ROLE_OWNER cho admin (user id=1)
INSERT INTO user_role (user_id, role_id) 
SELECT 1, id FROM role WHERE name = 'ROLE_OWNER';

-- ========================================
-- CATEGORIES
-- id: 1=Áo thun | 2=Áo sơ mi | 3=Quần | 4=Áo hoodie
-- ========================================
INSERT INTO category (name, description) VALUES
(N'Áo thun',   N'Áo thun nam các loại'),
(N'Áo sơ mi',  N'Áo sơ mi nam công sở và dạo phố'),
(N'Quần',      N'Quần nam các loại'),
(N'Áo hoodie', N'Áo hoodie và sweatshirt nam');
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
-- PRODUCTS
-- Nhóm 1: Áo thun trắng (cat=1, color=1) — S,M,L,XL → id 1-4
-- Nhóm 2: Áo thun đen (cat=1, color=2) — S,M,L,XL → id 5-8
-- Nhóm 3: Áo thun mèo trắng (cat=1, color=1) — S,M,L → id 9-11
-- Nhóm 4: Áo thun mèo đen (cat=1, color=2) — S,M,L → id 12-14
-- Nhóm 5: Áo sơ mi trắng (cat=2, color=1) — S,M,L,XL → id 15-18
-- Nhóm 6: Áo sơ mi xanh nhạt (cat=2, color=5) — S,M,L → id 19-21
-- Nhóm 7: Áo sơ mi xanh đậm (cat=2, color=3) — S,M,L → id 22-24
-- Nhóm 8: Quần jean layer (cat=3, color=2) — S,M,L,XL → id 25-28
-- Nhóm 9: Quần sweater đen (cat=3, color=2) — S,M,L → id 29-31
-- Nhóm 10: Áo hoodie đen (cat=4, color=2) — S,M,L,XL → id 32-35
-- ========================================

INSERT INTO product (name, description, sku, price, stock, cost_price, category_id, size_id, color_id) VALUES
-- Áo thun trắng
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-WHT-S',  199000, 20, 100000, 1, 1, 1),
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-WHT-M',  199000, 30, 100000, 1, 2, 1),
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-WHT-L',  199000, 25, 100000, 1, 3, 1),
(N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-WHT-XL', 199000, 15, 100000, 1, 4, 1),

-- Áo thun đen
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-BLK-S',  199000, 20, 100000, 1, 1, 2),
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-BLK-M',  199000, 25, 100000, 1, 2, 2),
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-BLK-L',  199000, 18, 100000, 1, 3, 2),
(N'Áo thun Basic Đen',   N'Áo thun cotton 100% thoáng mát, form regular fit',    N'AT-BLK-XL', 199000, 12, 100000, 1, 4, 2),

-- Áo thun mèo trắng
(N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương, unisex',       N'AT-CAT-WHT-S',  249000, 15, 120000, 1, 1, 1),
(N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương, unisex',       N'AT-CAT-WHT-M',  249000, 20, 120000, 1, 2, 1),
(N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương, unisex',       N'AT-CAT-WHT-L',  249000, 10, 120000, 1, 3, 1),

-- Áo thun mèo đen
(N'Áo thun In Mèo Đen',   N'Áo thun cotton in hình mèo dễ thương, unisex',       N'AT-CAT-BLK-S',  249000, 15, 120000, 1, 1, 2),
(N'Áo thun In Mèo Đen',   N'Áo thun cotton in hình mèo dễ thương, unisex',       N'AT-CAT-BLK-M',  249000, 20, 120000, 1, 2, 2),
(N'Áo thun In Mèo Đen',   N'Áo thun cotton in hình mèo dễ thương, unisex',       N'AT-CAT-BLK-L',  249000, 10, 120000, 1, 3, 2),

-- Áo sơ mi trắng
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát',     N'SM-WHT-S',  350000, 15, 175000, 2, 1, 1),
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát',     N'SM-WHT-M',  350000, 20, 175000, 2, 2, 1),
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát',     N'SM-WHT-L',  350000, 18, 175000, 2, 3, 1),
(N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát',     N'SM-WHT-XL', 350000, 10, 175000, 2, 4, 1),

-- Áo sơ mi xanh nhạt
(N'Áo sơ mi Xanh Nhạt',   N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc',        N'SM-LBL-S',  379000, 12, 190000, 2, 1, 5),
(N'Áo sơ mi Xanh Nhạt',   N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc',        N'SM-LBL-M',  379000, 18, 190000, 2, 2, 5),
(N'Áo sơ mi Xanh Nhạt',   N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc',        N'SM-LBL-L',  379000, 10, 190000, 2, 3, 5),

-- Áo sơ mi xanh đậm
(N'Áo sơ mi Xanh Navy',   N'Áo sơ mi xanh navy lịch sự dễ phối đồ',              N'SM-NVY-S',  399000, 10, 200000, 2, 1, 3),
(N'Áo sơ mi Xanh Navy',   N'Áo sơ mi xanh navy lịch sự dễ phối đồ',              N'SM-NVY-M',  399000, 15, 200000, 2, 2, 3),
(N'Áo sơ mi Xanh Navy',   N'Áo sơ mi xanh navy lịch sự dễ phối đồ',              N'SM-NVY-L',  399000, 12, 200000, 2, 3, 3),

-- Quần jean layer
(N'Quần Jean Layer',       N'Quần jean layer phong cách streetwear độc đáo',       N'QJ-LAY-S',  549000, 15, 275000, 3, 1, 2),
(N'Quần Jean Layer',       N'Quần jean layer phong cách streetwear độc đáo',       N'QJ-LAY-M',  549000, 20, 275000, 3, 2, 2),
(N'Quần Jean Layer',       N'Quần jean layer phong cách streetwear độc đáo',       N'QJ-LAY-L',  549000, 18, 275000, 3, 3, 2),
(N'Quần Jean Layer',       N'Quần jean layer phong cách streetwear độc đáo',       N'QJ-LAY-XL', 549000, 10, 275000, 3, 4, 2),

-- Quần sweater đen
(N'Quần Sweater Đen',      N'Quần sweater chất nỉ bông ấm áp mùa đông',           N'QSW-BLK-S',  449000, 12, 225000, 3, 1, 2),
(N'Quần Sweater Đen',      N'Quần sweater chất nỉ bông ấm áp mùa đông',           N'QSW-BLK-M',  449000, 18, 225000, 3, 2, 2),
(N'Quần Sweater Đen',      N'Quần sweater chất nỉ bông ấm áp mùa đông',           N'QSW-BLK-L',  449000, 15, 225000, 3, 3, 2),

-- Áo hoodie đen
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt',            N'HD-BLK-S',  599000, 15, 300000, 4, 1, 2),
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt',            N'HD-BLK-M',  599000, 20, 300000, 4, 2, 2),
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt',            N'HD-BLK-L',  599000, 18, 300000, 4, 3, 2),
(N'Áo Hoodie Đen',         N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt',            N'HD-BLK-XL', 599000, 10, 300000, 4, 4, 2);
GO

-- ========================================
-- PRODUCT IMAGES
-- Mỗi nhóm sản phẩm dùng chung ảnh (tất cả variant cùng nhóm có ảnh giống nhau)
-- ========================================

-- Áo thun trắng (id 1-4) — 2 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 1),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 2),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 3),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 4),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 4);

-- Áo thun đen (id 5-8) — 2 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 5),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 6),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 7),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 8),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 8);

-- Áo thun mèo trắng (id 9-11) — 2 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 9),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 10),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 11),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 11);

-- Áo thun mèo đen (id 12-14) — 2 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 12),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 12),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 13),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 13),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 14),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 14);

-- Áo sơ mi trắng (id 15-18) — 2 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 15),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 15),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 16),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 16),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 17),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 17),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 18),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 18);

-- Áo sơ mi xanh nhạt (id 19-21) — 1 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 19),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 20),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 21);

-- Áo sơ mi xanh navy (id 22-24) — 1 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 22),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 23),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 24);

-- Quần jean layer (id 25-28) — 2 ảnh
INSERT INTO product_image (image_url, product_id) VALUES
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 25),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 25),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 26),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 26),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 27),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 27),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 28),
(N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 28);

-- Quần sweater đen (id 29-31) — 3 ảnh
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

-- Áo hoodie đen (id 32-35) — 3 ảnh
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
(3, N'John Doe',   N'0903333333', N'123 Đường Lê Lợi',        N'Hà Nội',   N'Hoàn Kiếm', N'Vietnam', 1),
(3, N'John Doe',   N'0903333333', N'456 Đường Nguyễn Huệ',    N'Hà Nội',   N'Đống Đa',   N'Vietnam', 0),
(4, N'Jane Smith', N'0904444444', N'789 Đường Trần Hưng Đạo', N'TP.HCM',   N'Quận 1',    N'Vietnam', 1),
(5, N'Minh Tran',  N'0905555555', N'321 Đường Hai Bà Trưng',  N'Đà Nẵng',  N'Hải Châu',  N'Vietnam', 1);
GO

-- ========================================
-- CARTS
-- ========================================
INSERT INTO cart (customer_id) VALUES (3), (4), (5);
GO

INSERT INTO cart_item (cart_id, product_id, quantity) VALUES
(1, 2,  2),  -- john: Áo thun Basic Trắng M x2
(1, 26, 1),  -- john: Quần Jean Layer M x1
(2, 16, 1),  -- jane: Áo sơ mi Trắng M x1
(2, 30, 1),  -- jane: Quần Sweater Đen M x1
(3, 33, 1);  -- minh: Áo Hoodie Đen M x1
GO

-- ========================================
-- VOUCHERS
-- ========================================
INSERT INTO voucher (code, description, discount_percent, min_order_value, start_date, end_date, is_active) VALUES
(N'WELCOME10', N'Giảm 10% cho khách hàng mới',   10, 200000,  GETDATE(), DATEADD(DAY, 30,  GETDATE()), 1),
(N'SUMMER20',  N'Giảm 20% mùa hè',               20, 500000,  GETDATE(), DATEADD(DAY, 60,  GETDATE()), 1),
(N'FREESHIP',  N'Giảm 5% áp dụng mọi đơn hàng',   5,      0,  GETDATE(), DATEADD(DAY, 90,  GETDATE()), 1),
(N'VIP30',     N'Giảm 30% dành cho khách VIP',    30, 1000000, GETDATE(), DATEADD(DAY, 15,  GETDATE()), 1),
(N'EXPIRED',   N'Voucher đã hết hạn',             15, 300000,  DATEADD(DAY, -60, GETDATE()), DATEADD(DAY, -30, GETDATE()), 0);
GO

-- ========================================
-- ORDERS
-- ========================================

-- john (user 3) - 3 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, shipping_address_id, billing_address_id) VALUES
(3, DATEADD(DAY, -20, GETDATE()), N'COMPLETED', 947000, 1, 1),
(3, DATEADD(DAY, -10, GETDATE()), N'SHIPPING',  549000, 1, 1),
(3, DATEADD(DAY, -1,  GETDATE()), N'PENDING',   199000, 1, 1);

-- jane (user 4) - 2 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, shipping_address_id, billing_address_id) VALUES
(4, DATEADD(DAY, -15, GETDATE()), N'COMPLETED', 799000, 3, 3),
(4, DATEADD(DAY, -3,  GETDATE()), N'CONFIRMED', 379000, 3, 3);

-- minh (user 5) - 2 đơn
INSERT INTO app_order (customer_id, order_date, status, total_price, shipping_address_id, billing_address_id) VALUES
(5, DATEADD(DAY, -7,  GETDATE()), N'DELIVERED', 599000, 4, 4),
(5, DATEADD(DAY, -2,  GETDATE()), N'PENDING',   449000, 4, 4);
GO

-- ========================================
-- ORDER DETAILS
-- ========================================

-- Đơn 1 (john - COMPLETED): Áo thun trắng M x2 + Quần jean layer M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(1, 2,  2, 199000),
(1, 26, 1, 549000);

-- Đơn 2 (john - SHIPPING): Quần jean layer L x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(2, 27, 1, 549000);

-- Đơn 3 (john - PENDING): Áo thun trắng S x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(3, 1, 1, 199000);

-- Đơn 4 (jane - COMPLETED): Áo sơ mi trắng M x1 + Quần sweater đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(4, 16, 1, 350000),
(4, 30, 1, 449000);

-- Đơn 5 (jane - CONFIRMED): Áo sơ mi xanh nhạt M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(5, 20, 1, 379000);

-- Đơn 6 (minh - DELIVERED): Áo hoodie đen M x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(6, 33, 1, 599000);

-- Đơn 7 (minh - PENDING): Quần sweater đen L x1
INSERT INTO order_detail (order_id, product_id, quantity, price) VALUES
(7, 31, 1, 449000);
GO

-- ========================================
-- ORDER VOUCHERS
-- ========================================
INSERT INTO order_voucher (order_id, voucher_id, customer_id) VALUES
(1, 1, 3),  -- john dùng WELCOME10
(4, 2, 4);  -- jane dùng SUMMER20
GO
