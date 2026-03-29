
DROP DATABASE IF EXISTS ClothingShop;

CREATE DATABASE ClothingShop;
GO
USE ClothingShop;
GO

-- Bảng app_user (tránh keyword USER)
CREATE TABLE app_user (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    email NVARCHAR(100) UNIQUE,
    phone NVARCHAR(20),
    created_at DATETIME DEFAULT GETDATE()
);
ALTER TABLE app_user ADD enabled BIT DEFAULT 1;

-- Bảng role
CREATE TABLE role (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Bảng user_role (join table @ManyToMany)
CREATE TABLE user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
);

-- Bảng category
CREATE TABLE category (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(255)
);

-- Bảng size
CREATE TABLE size (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(20) NOT NULL UNIQUE
);

-- Bảng color
CREATE TABLE color (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

-- Bảng product
CREATE TABLE product (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX),
    sku NVARCHAR(50) UNIQUE,
    price DECIMAL(18,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    cost_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    category_id INT FOREIGN KEY REFERENCES category(id),
    size_id INT FOREIGN KEY REFERENCES size(id),
    color_id INT FOREIGN KEY REFERENCES color(id),
    created_at DATETIME DEFAULT GETDATE()
);

-- Bảng product_image
CREATE TABLE product_image (
    id INT IDENTITY(1,1) PRIMARY KEY,
    image_url NVARCHAR(500) NOT NULL,
    product_id INT FOREIGN KEY REFERENCES product(id)
);

-- Bảng cart
CREATE TABLE cart (
    id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT FOREIGN KEY REFERENCES app_user(id),
    created_at DATETIME DEFAULT GETDATE()
);

-- Bảng cart_item
CREATE TABLE cart_item (
    id INT IDENTITY(1,1) PRIMARY KEY,
    cart_id INT FOREIGN KEY REFERENCES cart(id),
    product_id INT FOREIGN KEY REFERENCES product(id),
    quantity INT NOT NULL
);

-- Bảng address
CREATE TABLE address (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT FOREIGN KEY REFERENCES app_user(id),
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20) NOT NULL,
    street NVARCHAR(255) NOT NULL,
    city NVARCHAR(100) NOT NULL,
    district NVARCHAR(100),
    country NVARCHAR(100) DEFAULT N'Vietnam',
    is_default BIT DEFAULT 0
);

-- Bảng app_order (tránh keyword ORDER)
CREATE TABLE app_order (
    id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT FOREIGN KEY REFERENCES app_user(id),
    order_date DATETIME DEFAULT GETDATE(),
    status NVARCHAR(20) DEFAULT 'PENDING',
    total_price DECIMAL(18,2),
    shipping_address_id INT FOREIGN KEY REFERENCES address(id),
    billing_address_id INT FOREIGN KEY REFERENCES address(id)
);

-- Bảng order_detail
CREATE TABLE order_detail (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT FOREIGN KEY REFERENCES app_order(id),
    product_id INT FOREIGN KEY REFERENCES product(id),
    quantity INT NOT NULL,
    price DECIMAL(18,2) NOT NULL
);

-- Bảng voucher
CREATE TABLE voucher (
    id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255),
    discount_percent DECIMAL(5,2),
    min_order_value DECIMAL(18,2),
    start_date DATETIME,
    end_date DATETIME,
    is_active BIT DEFAULT 1
);

-- Bảng order_voucher
CREATE TABLE order_voucher (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT FOREIGN KEY REFERENCES app_order(id),
    voucher_id INT FOREIGN KEY REFERENCES voucher(id),
    customer_id INT FOREIGN KEY REFERENCES app_user(id)
);
ALTER TABLE app_order ALTER COLUMN status NVARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE order_voucher ADD CONSTRAINT UQ_order_voucher UNIQUE (order_id);
ALTER TABLE order_voucher ADD CONSTRAINT UQ_user_voucher UNIQUE (customer_id, voucher_id);

ALTER TABLE product ADD is_deleted BIT DEFAULT 0;

-- ========================================
-- DATA MẪU
-- ========================================

INSERT INTO role (name) VALUES ('ROLE_DELIVERY');

INSERT INTO app_user (username, password, email, phone)
VALUES 
(N'john_doe', N'hashed_password1', N'john@example.com', N'0901234567'),
(N'jane_smith', N'hashed_password2', N'jane@example.com', N'0912345678');

UPDATE app_user SET password = '{noop}' + password;


INSERT INTO role (name)
VALUES (N'ROLE_ADMIN'), (N'ROLE_CUSTOMER');

INSERT INTO user_role (user_id, role_id)
VALUES (1, 1), (2, 2);

INSERT INTO category (name, description)
VALUES (N'T-Shirt', N'Áo thun nam nữ'),
       (N'Jeans', N'Quần jean các loại');

INSERT INTO size (name)
VALUES (N'S'), (N'M'), (N'L'), (N'XL');

INSERT INTO color (name)
VALUES (N'Trắng'), (N'Đen'), (N'Xanh'), (N'Đỏ');

INSERT INTO product (name, description, sku, price, stock, cost_price, category_id, size_id, color_id)
VALUES 
(N'Áo thun Basic', N'Áo thun cotton thoáng mát', N'TEE-WHT-M', 199000, 50, 100000, 1, 2, 1),
(N'Áo thun Basic', N'Áo thun cotton thoáng mát', N'TEE-BLK-L', 199000, 30, 100000, 1, 3, 2),
(N'Quần jean Slimfit', N'Quần jean co giãn', N'JEAN-BLU-32', 499000, 20, 250000, 2, NULL, 3);

INSERT INTO product_image (image_url, product_id)
VALUES 
(N'https://example.com/images/tee_white_m.jpg', 1),
(N'https://example.com/images/tee_black_l.jpg', 2),
(N'https://example.com/images/jean_blue_32.jpg', 3);

INSERT INTO cart (customer_id)
VALUES (2);

INSERT INTO cart_item (cart_id, product_id, quantity)
VALUES (1, 1, 2), (1, 3, 1);

INSERT INTO address (user_id, full_name, phone, street, city, district, country, is_default)
VALUES 
(2, N'Jane Smith', N'0912345678', N'123 Đường ABC', N'Hà Nội', N'Cầu Giấy', N'Vietnam', 1);

INSERT INTO app_order (customer_id, total_price, shipping_address_id, billing_address_id)
VALUES (2, 897000, 1, 1);

INSERT INTO order_detail (order_id, product_id, quantity, price)
VALUES 
(1, 1, 2, 199000),
(1, 3, 1, 499000);

INSERT INTO voucher (code, description, discount_percent, min_order_value, start_date, end_date)
VALUES 
(N'NEWCUSTOMER', N'Giảm giá cho khách hàng mới', 10, 300000, GETDATE(), DATEADD(DAY, 30, GETDATE()));

INSERT INTO order_voucher (order_id, voucher_id, customer_id)
VALUES (1, 1, 2);