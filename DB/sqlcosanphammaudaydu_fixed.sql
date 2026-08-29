USE [master]
GO
/****** Fixed for portability: drop old DB if exists, create using server's default data/log paths ******/
IF DB_ID(N'ClothingShop') IS NOT NULL
BEGIN
    ALTER DATABASE [ClothingShop] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [ClothingShop];
END
GO
/****** Object:  Database [ClothingShop]    Script Date: 8/29/2026 10:10:15 AM ******/
CREATE DATABASE [ClothingShop]
 CONTAINMENT = NONE
 WITH CATALOG_COLLATION = DATABASE_DEFAULT
GO
ALTER DATABASE [ClothingShop] SET COMPATIBILITY_LEVEL = 150
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [ClothingShop].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [ClothingShop] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [ClothingShop] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [ClothingShop] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [ClothingShop] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [ClothingShop] SET ARITHABORT OFF 
GO
ALTER DATABASE [ClothingShop] SET AUTO_CLOSE OFF 
GO
ALTER DATABASE [ClothingShop] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [ClothingShop] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [ClothingShop] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [ClothingShop] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [ClothingShop] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [ClothingShop] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [ClothingShop] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [ClothingShop] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [ClothingShop] SET  ENABLE_BROKER 
GO
ALTER DATABASE [ClothingShop] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [ClothingShop] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [ClothingShop] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [ClothingShop] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [ClothingShop] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [ClothingShop] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [ClothingShop] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [ClothingShop] SET RECOVERY FULL 
GO
ALTER DATABASE [ClothingShop] SET  MULTI_USER 
GO
ALTER DATABASE [ClothingShop] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [ClothingShop] SET DB_CHAINING OFF 
GO
ALTER DATABASE [ClothingShop] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [ClothingShop] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [ClothingShop] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [ClothingShop] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
EXEC sys.sp_db_vardecimal_storage_format N'ClothingShop', N'ON'
GO
ALTER DATABASE [ClothingShop] SET QUERY_STORE = OFF
GO
USE [ClothingShop]
GO
/****** Object:  Table [dbo].[address]    Script Date: 8/29/2026 10:10:15 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[address](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NULL,
	[full_name] [nvarchar](100) NOT NULL,
	[phone] [nvarchar](20) NOT NULL,
	[street] [nvarchar](255) NOT NULL,
	[city] [nvarchar](100) NOT NULL,
	[district] [nvarchar](100) NULL,
	[country] [nvarchar](100) NOT NULL,
	[is_default] [bit] NOT NULL,
	[ghn_province_id] [int] NULL,
	[ghn_district_id] [int] NULL,
	[ghn_ward_code] [varchar](20) NULL,
	[active] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[app_order]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[app_order](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[customer_id] [int] NULL,
	[order_date] [datetime] NOT NULL,
	[status] [nvarchar](20) NOT NULL,
	[total_price] [decimal](18, 2) NULL,
	[payment_method] [nvarchar](20) NOT NULL,
	[is_paid] [bit] NOT NULL,
	[delivered_at] [datetime] NULL,
	[shipped_at] [datetime] NULL,
	[tracking_code] [nvarchar](50) NULL,
	[shipping_address_id] [int] NULL,
	[billing_address_id] [int] NULL,
	[order_type] [nvarchar](20) NOT NULL,
	[cashier_id] [int] NULL,
	[shipping_fee] [decimal](18, 2) NULL,
	[cancel_reason] [nvarchar](30) NULL,
	[cancel_note] [nvarchar](255) NULL,
	[cancelled_by] [int] NULL,
	[cancelled_at] [datetime] NULL,
	[order_code] [varchar](12) NOT NULL,
	[guest_email] [varchar](100) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[app_user]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[app_user](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[username] [nvarchar](50) NOT NULL,
	[password] [nvarchar](255) NOT NULL,
	[email] [nvarchar](100) NULL,
	[phone] [nvarchar](20) NULL,
	[enabled] [bit] NOT NULL,
	[created_at] [datetime] NOT NULL,
	[activation_token] [varchar](100) NULL,
	[activation_expires_at] [datetime2](7) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[cart]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[cart](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[customer_id] [int] NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[cart_item]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[cart_item](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[cart_id] [int] NOT NULL,
	[variant_id] [int] NOT NULL,
	[quantity] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[category]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[category](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](100) NOT NULL,
	[description] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[collection]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[collection](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](100) NOT NULL,
	[description] [nvarchar](500) NULL,
	[cover_image] [nvarchar](500) NULL,
	[start_date] [date] NULL,
	[end_date] [date] NULL,
	[is_active] [bit] NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[color]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[color](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](50) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[employee]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[employee](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[employee_code] [nvarchar](20) NOT NULL,
	[hire_date] [date] NULL,
	[is_active] [bit] NOT NULL,
	[note] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[goods_receipt]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[goods_receipt](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[code] [nvarchar](30) NOT NULL,
	[supplier_name] [nvarchar](150) NOT NULL,
	[note] [nvarchar](500) NULL,
	[created_by] [int] NOT NULL,
	[created_at] [datetime] NOT NULL,
	[total_amount] [decimal](18, 2) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[goods_receipt_item]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[goods_receipt_item](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[goods_receipt_id] [int] NOT NULL,
	[variant_id] [int] NOT NULL,
	[quantity] [int] NOT NULL,
	[unit_cost_price] [decimal](18, 2) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[notification]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[notification](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NULL,
	[message] [nvarchar](500) NOT NULL,
	[link] [nvarchar](255) NULL,
	[is_read] [bit] NOT NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[order_detail]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[order_detail](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[order_id] [int] NOT NULL,
	[variant_id] [int] NOT NULL,
	[quantity] [int] NOT NULL,
	[price] [decimal](18, 2) NOT NULL,
	[original_price] [decimal](18, 2) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[order_voucher]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[order_voucher](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[order_id] [int] NOT NULL,
	[voucher_id] [int] NOT NULL,
	[customer_id] [int] NOT NULL,
	[discount_amount] [decimal](18, 2) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[password_reset_token]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[password_reset_token](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[token] [varchar](255) NOT NULL,
	[user_id] [int] NOT NULL,
	[expires_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[product]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[product](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](100) NOT NULL,
	[description] [nvarchar](max) NULL,
	[price] [decimal](18, 2) NOT NULL,
	[is_deleted] [bit] NOT NULL,
	[category_id] [int] NULL,
	[created_at] [datetime] NOT NULL,
	[weight] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[product_collection]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[product_collection](
	[product_id] [int] NOT NULL,
	[collection_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[product_id] ASC,
	[collection_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[product_image]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[product_image](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[image_url] [nvarchar](500) NOT NULL,
	[product_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[product_sale]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[product_sale](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[product_id] [int] NOT NULL,
	[sale_batch_id] [int] NULL,
	[discount_percent] [decimal](5, 2) NOT NULL,
	[start_date] [datetime] NOT NULL,
	[end_date] [datetime] NOT NULL,
	[is_active] [bit] NOT NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[product_variant]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[product_variant](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[product_id] [int] NOT NULL,
	[size_id] [int] NULL,
	[color_id] [int] NULL,
	[sku] [nvarchar](50) NULL,
	[stock] [int] NOT NULL,
	[cost_price] [decimal](18, 2) NOT NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[role]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[role](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](50) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[sale_batch]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[sale_batch](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](150) NOT NULL,
	[discount_percent] [decimal](5, 2) NOT NULL,
	[start_date] [datetime] NOT NULL,
	[end_date] [datetime] NOT NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[size]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[size](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](20) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[stock_movement_log]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[stock_movement_log](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[variant_id] [int] NOT NULL,
	[change_type] [nvarchar](20) NOT NULL,
	[change_qty] [int] NOT NULL,
	[balance_after] [int] NOT NULL,
	[ref_type] [nvarchar](30) NULL,
	[ref_id] [int] NULL,
	[note] [nvarchar](255) NULL,
	[created_by] [int] NULL,
	[created_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[user_role]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[user_role](
	[user_id] [int] NOT NULL,
	[role_id] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[user_id] ASC,
	[role_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[variant_image]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[variant_image](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[variant_id] [int] NOT NULL,
	[image_url] [nvarchar](500) NOT NULL,
	[sort_order] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[voucher]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[voucher](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[code] [nvarchar](50) NOT NULL,
	[description] [nvarchar](255) NULL,
	[discount_percent] [decimal](5, 2) NULL,
	[max_discount] [decimal](18, 2) NULL,
	[min_order_value] [decimal](18, 2) NULL,
	[start_date] [datetime] NULL,
	[end_date] [datetime] NULL,
	[is_active] [bit] NOT NULL,
	[usage_limit] [int] NULL,
	[used_count] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[wishlist]    Script Date: 8/29/2026 10:10:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[wishlist](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[user_id] [int] NOT NULL,
	[product_id] [int] NOT NULL,
	[added_at] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
SET IDENTITY_INSERT [dbo].[address] ON 

INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (1, 3, N'Nguyễn Văn A', N'0901234567', N'12 Nguyễn Trãi', N'Hà Nội', N'Thanh Xuân', N'Vietnam', 1, 201, 1542, N'1B2107', 1)
INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (2, 3, N'Nguyễn Văn A', N'0901234567', N'45 Láng Hạ', N'Hà Nội', N'Đống Đa', N'Vietnam', 0, 201, 1487, N'1A0113', 1)
INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (3, 4, N'Trần Thị B', N'0912345678', N'88 Lê Văn Việt', N'TP.HCM', N'Quận 9', N'Vietnam', 1, 202, 3695, N'90737', 1)
INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (4, 5, N'Lê Văn C', N'0923456789', N'56 Trần Phú', N'Đà Nẵng', N'Hải Châu', N'Vietnam', 1, 205, 1490, N'20308', 1)
INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (5, 6, N'Phạm Thị D', N'0934567890', N'23 Nguyễn Văn Cừ', N'TP.HCM', N'Quận 5', N'Vietnam', 1, 202, 3705, N'11109', 1)
INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (6, 7, N'Hoàng Văn E', N'0945678901', N'78 Đinh Tiên Hoàng', N'Hà Nội', N'Hoàn Kiếm', N'Vietnam', 1, 201, 1482, N'1A0107', 1)
INSERT [dbo].[address] ([id], [user_id], [full_name], [phone], [street], [city], [district], [country], [is_default], [ghn_province_id], [ghn_district_id], [ghn_ward_code], [active]) VALUES (7, NULL, N'Đỗ Văn Khách', N'0956789012', N'101 Cầu Giấy', N'Hà Nội', N'Cầu Giấy', N'Vietnam', 0, NULL, NULL, NULL, 1)
SET IDENTITY_INSERT [dbo].[address] OFF
GO
SET IDENTITY_INSERT [dbo].[app_order] ON 

INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (1, 3, CAST(N'2026-07-29T13:12:05.773' AS DateTime), N'COMPLETED', CAST(747000.00 AS Decimal(18, 2)), N'COD', 1, NULL, NULL, NULL, 1, 1, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000001', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (2, 3, CAST(N'2026-08-13T13:12:05.773' AS DateTime), N'COMPLETED', CAST(549000.00 AS Decimal(18, 2)), N'VNPAY', 1, NULL, NULL, NULL, 1, 1, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000002', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (3, 3, CAST(N'2026-08-23T13:12:05.773' AS DateTime), N'SHIPPING', CAST(398000.00 AS Decimal(18, 2)), N'COD', 0, NULL, CAST(N'2026-08-24T13:12:05.773' AS DateTime), N'GHN-20250001', 2, 2, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000003', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (4, 3, CAST(N'2026-08-27T13:12:05.773' AS DateTime), N'PENDING', CAST(199000.00 AS Decimal(18, 2)), N'COD', 0, NULL, NULL, NULL, 1, 1, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000004', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (5, 4, CAST(N'2026-08-08T13:12:05.773' AS DateTime), N'COMPLETED', CAST(799000.00 AS Decimal(18, 2)), N'VNPAY', 1, NULL, NULL, NULL, 3, 3, N'ONLINE', NULL, CAST(38000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000005', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (6, 4, CAST(N'2026-08-20T13:12:05.773' AS DateTime), N'CONFIRMED', CAST(598000.00 AS Decimal(18, 2)), N'COD', 0, NULL, NULL, NULL, 3, 3, N'ONLINE', NULL, CAST(38000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000006', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (7, 4, CAST(N'2026-08-26T13:12:05.773' AS DateTime), N'PENDING', CAST(249000.00 AS Decimal(18, 2)), N'VNPAY', 0, NULL, NULL, NULL, 3, 3, N'ONLINE', NULL, CAST(38000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000007', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (8, 5, CAST(N'2026-08-03T13:12:05.777' AS DateTime), N'COMPLETED', CAST(599000.00 AS Decimal(18, 2)), N'COD', 1, NULL, NULL, NULL, 4, 4, N'ONLINE', NULL, CAST(32000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000008', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (9, 5, CAST(N'2026-08-21T13:12:05.777' AS DateTime), N'COMPLETED', CAST(599000.00 AS Decimal(18, 2)), N'COD', 1, CAST(N'2026-08-27T13:12:05.790' AS DateTime), CAST(N'2026-08-22T13:12:05.777' AS DateTime), N'GHN-20250002', 4, 4, N'ONLINE', NULL, CAST(32000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000009', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (10, 5, CAST(N'2026-08-25T13:12:05.777' AS DateTime), N'CONFIRMED', CAST(349000.00 AS Decimal(18, 2)), N'VNPAY', 1, NULL, NULL, NULL, 4, 4, N'ONLINE', NULL, CAST(32000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000010', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (11, 6, CAST(N'2026-08-18T13:12:05.777' AS DateTime), N'COMPLETED', CAST(448000.00 AS Decimal(18, 2)), N'COD', 0, NULL, CAST(N'2026-08-19T13:12:05.777' AS DateTime), N'GHN-20250003', 5, 5, N'ONLINE', NULL, CAST(38000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000011', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (12, 6, CAST(N'2026-08-27T13:12:05.777' AS DateTime), N'PENDING', CAST(498000.00 AS Decimal(18, 2)), N'COD', 0, NULL, NULL, NULL, 5, 5, N'ONLINE', NULL, CAST(38000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000012', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (13, 7, CAST(N'2026-08-16T13:12:05.777' AS DateTime), N'CANCELLED', CAST(199000.00 AS Decimal(18, 2)), N'COD', 0, NULL, NULL, NULL, 6, 6, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), N'CUSTOMER_REQUEST', N'Khách đổi ý, không muốn mua nữa', 7, CAST(N'2026-08-16T13:12:05.797' AS DateTime), N'CW00000013', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (14, 7, CAST(N'2026-08-26T13:12:05.777' AS DateTime), N'PENDING', CAST(449000.00 AS Decimal(18, 2)), N'VNPAY', 0, NULL, NULL, NULL, 6, 6, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000014', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (15, NULL, CAST(N'2026-08-26T13:12:05.780' AS DateTime), N'COMPLETED', CAST(294000.00 AS Decimal(18, 2)), N'CASH', 1, NULL, NULL, NULL, NULL, NULL, N'COUNTER', 8, CAST(0.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000015', NULL)
INSERT [dbo].[app_order] ([id], [customer_id], [order_date], [status], [total_price], [payment_method], [is_paid], [delivered_at], [shipped_at], [tracking_code], [shipping_address_id], [billing_address_id], [order_type], [cashier_id], [shipping_fee], [cancel_reason], [cancel_note], [cancelled_by], [cancelled_at], [order_code], [guest_email]) VALUES (16, NULL, CAST(N'2026-08-27T13:12:05.790' AS DateTime), N'PENDING', CAST(219000.00 AS Decimal(18, 2)), N'VNPAY', 1, NULL, NULL, NULL, 7, 7, N'ONLINE', NULL, CAST(20000.00 AS Decimal(18, 2)), NULL, NULL, NULL, NULL, N'CW00000016', N'guest.khach@gmail.com')
SET IDENTITY_INSERT [dbo].[app_order] OFF
GO
SET IDENTITY_INSERT [dbo].[app_user] ON 

INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (1, N'owner', N'{noop}owner123', N'owner@casualwear.vn', N'0900000001', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (2, N'admin', N'{noop}admin123', N'admin@casualwear.vn', N'0900000002', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (3, N'nguyenvana', N'{noop}pass1234', N'nguyenvana@gmail.com', N'0901234567', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (4, N'tranthib', N'{noop}pass1234', N'tranthib@gmail.com', N'0912345678', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (5, N'levanc', N'{noop}pass1234', N'levanc@gmail.com', N'0923456789', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (6, N'phamthid', N'{noop}pass1234', N'phamthid@gmail.com', N'0934567890', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (7, N'hoangvane', N'{noop}pass1234', N'hoangvane@gmail.com', N'0945678901', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (8, N'cashier1', N'{noop}cashier123', N'cashier1@casualwear.vn', N'0900000003', 1, CAST(N'2026-08-28T13:12:05.633' AS DateTime), NULL, NULL)
INSERT [dbo].[app_user] ([id], [username], [password], [email], [phone], [enabled], [created_at], [activation_token], [activation_expires_at]) VALUES (9, N'vokhachf', N'{noop}TEMP-CHANGE-ME', N'vokhachf@gmail.com', N'0967890123', 0, CAST(N'2026-08-28T13:12:05.640' AS DateTime), N'a1b2c3d4-e5f6-47a8-9b01-c2d3e4f5a6b7', CAST(N'2026-09-04T06:12:05.6413359' AS DateTime2))
SET IDENTITY_INSERT [dbo].[app_user] OFF
GO
SET IDENTITY_INSERT [dbo].[cart] ON 

INSERT [dbo].[cart] ([id], [customer_id], [created_at]) VALUES (1, 3, CAST(N'2026-08-28T13:12:05.710' AS DateTime))
INSERT [dbo].[cart] ([id], [customer_id], [created_at]) VALUES (2, 4, CAST(N'2026-08-28T13:12:05.710' AS DateTime))
INSERT [dbo].[cart] ([id], [customer_id], [created_at]) VALUES (3, 5, CAST(N'2026-08-28T13:12:05.710' AS DateTime))
INSERT [dbo].[cart] ([id], [customer_id], [created_at]) VALUES (4, 6, CAST(N'2026-08-28T13:12:05.710' AS DateTime))
INSERT [dbo].[cart] ([id], [customer_id], [created_at]) VALUES (5, 7, CAST(N'2026-08-28T13:12:05.710' AS DateTime))
INSERT [dbo].[cart] ([id], [customer_id], [created_at]) VALUES (6, 2, CAST(N'2026-08-28T13:14:18.077' AS DateTime))
SET IDENTITY_INSERT [dbo].[cart] OFF
GO
SET IDENTITY_INSERT [dbo].[cart_item] ON 

INSERT [dbo].[cart_item] ([id], [cart_id], [variant_id], [quantity]) VALUES (1, 1, 2, 2)
INSERT [dbo].[cart_item] ([id], [cart_id], [variant_id], [quantity]) VALUES (2, 1, 26, 1)
INSERT [dbo].[cart_item] ([id], [cart_id], [variant_id], [quantity]) VALUES (3, 2, 16, 1)
INSERT [dbo].[cart_item] ([id], [cart_id], [variant_id], [quantity]) VALUES (4, 3, 33, 1)
INSERT [dbo].[cart_item] ([id], [cart_id], [variant_id], [quantity]) VALUES (5, 4, 10, 2)
INSERT [dbo].[cart_item] ([id], [cart_id], [variant_id], [quantity]) VALUES (6, 5, 30, 1)
SET IDENTITY_INSERT [dbo].[cart_item] OFF
GO
SET IDENTITY_INSERT [dbo].[category] ON 

INSERT [dbo].[category] ([id], [name], [description]) VALUES (1, N'Áo thun', N'Áo thun nam các loại chất liệu cotton cao cấp')
INSERT [dbo].[category] ([id], [name], [description]) VALUES (2, N'Áo sơ mi', N'Áo sơ mi nam công sở và dạo phố phong cách')
INSERT [dbo].[category] ([id], [name], [description]) VALUES (3, N'Quần', N'Quần nam các loại từ jean đến sweater')
INSERT [dbo].[category] ([id], [name], [description]) VALUES (4, N'Áo hoodie', N'Áo hoodie và sweatshirt nam giữ nhiệt mùa lạnh')
INSERT [dbo].[category] ([id], [name], [description]) VALUES (5, N'Áo  Sweater', NULL)
SET IDENTITY_INSERT [dbo].[category] OFF
GO
SET IDENTITY_INSERT [dbo].[collection] ON 

INSERT [dbo].[collection] ([id], [name], [description], [cover_image], [start_date], [end_date], [is_active], [created_at]) VALUES (1, N'Thu Đông 2026', N'Đón gió mùa với những thiết kế ấm áp, thoải mái nhưng không kém phần phong cách. Tổng hợp các mẫu Áo len, Sweater và Hoodie nỉ bông mới nhất.', N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787972071/collections/uroxqhugvphfoxetpmsh.jpg', CAST(N'2026-08-29' AS Date), CAST(N'2026-09-30' AS Date), 1, CAST(N'2026-08-28T22:35:49.117' AS DateTime))
INSERT [dbo].[collection] ([id], [name], [description], [cover_image], [start_date], [end_date], [is_active], [created_at]) VALUES (2, N'Thanh Lịch Công Sở', N'Tuyển tập các mẫu sơ mi và quần âu chuẩn form, chất liệu chống nhăn cao cấp. Giải pháp hoàn hảo giúp phái mạnh tự tin, lịch lãm nơi văn phòng.', N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787972165/collections/ncam2m6cxtrssblc5p08.jpg', NULL, NULL, 1, CAST(N'2026-08-29T09:55:58.080' AS DateTime))
INSERT [dbo].[collection] ([id], [name], [description], [cover_image], [start_date], [end_date], [is_active], [created_at]) VALUES (3, N'Năng Động Xuống Phố', N'Phong cách đường phố cá tính với các item rộng rãi, bụi bặm. Phù hợp cho những buổi dạo phố, đi chơi hay cà phê cuối tuần cùng bạn bè.', N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787972290/collections/wmygira2cxmmlturvrne.jpg', NULL, NULL, 1, CAST(N'2026-08-29T09:58:03.493' AS DateTime))
SET IDENTITY_INSERT [dbo].[collection] OFF
GO
SET IDENTITY_INSERT [dbo].[color] ON 

INSERT [dbo].[color] ([id], [name]) VALUES (2, N'Đen')
INSERT [dbo].[color] ([id], [name]) VALUES (6, N'Đỏ')
INSERT [dbo].[color] ([id], [name]) VALUES (8, N'Nâu sáng')
INSERT [dbo].[color] ([id], [name]) VALUES (1, N'Trắng')
INSERT [dbo].[color] ([id], [name]) VALUES (4, N'Xám')
INSERT [dbo].[color] ([id], [name]) VALUES (10, N'Xám chuột')
INSERT [dbo].[color] ([id], [name]) VALUES (9, N'Xanh đậm')
INSERT [dbo].[color] ([id], [name]) VALUES (3, N'Xanh navy')
INSERT [dbo].[color] ([id], [name]) VALUES (5, N'Xanh nhạt')
INSERT [dbo].[color] ([id], [name]) VALUES (7, N'Xanh rêu')
SET IDENTITY_INSERT [dbo].[color] OFF
GO
SET IDENTITY_INSERT [dbo].[employee] ON 

INSERT [dbo].[employee] ([id], [user_id], [employee_code], [hire_date], [is_active], [note]) VALUES (1, 1, N'NV0001', CAST(N'2023-01-01' AS Date), 1, N'Owner - chủ cửa hàng')
INSERT [dbo].[employee] ([id], [user_id], [employee_code], [hire_date], [is_active], [note]) VALUES (2, 2, N'NV0002', CAST(N'2023-02-01' AS Date), 1, N'Admin - quản trị hệ thống')
INSERT [dbo].[employee] ([id], [user_id], [employee_code], [hire_date], [is_active], [note]) VALUES (3, 8, N'NV0003', CAST(N'2024-06-01' AS Date), 1, N'Cashier - thu ngân quầy 1')
SET IDENTITY_INSERT [dbo].[employee] OFF
GO
SET IDENTITY_INSERT [dbo].[goods_receipt] ON 

INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (1, N'PN-20260801-001', N'Xưởng may Thành Công', N'Nhập bổ sung Áo thun Basic', 2, CAST(N'2026-08-23T13:12:05.737' AS DateTime), CAST(4750000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (2, N'PN-20260803-001', N'Xưởng may An Phát', N'Nhập hàng Hoodie mùa lạnh', 2, CAST(N'2026-08-26T13:12:05.737' AS DateTime), CAST(2850000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (3, N'PN-20260828-001', N'Xưởng May Mặc An Phú', N'Chuyên hàng thun cotton 100% và nỉ bông dày dặn. Giao hàng trong 3-5 ngày.', 2, CAST(N'2026-08-28T13:25:30.710' AS DateTime), CAST(650000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (4, N'PN-20260828-002', N'Xưởng May Mặc An Phú', N'Chuyên hàng thun cotton 100% và nỉ bông dày dặn. Giao hàng trong 3-5 ngày.', 2, CAST(N'2026-08-28T13:27:24.070' AS DateTime), CAST(150000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (5, N'PN-20260828-003', N'Xưởng May Mặc An Phú', N'Chuyên hàng thun cotton 100% và nỉ bông dày dặn. Giao hàng trong 3-5 ngày.', 2, CAST(N'2026-08-28T13:28:48.363' AS DateTime), CAST(700000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (6, N'PN-20260828-004', N'Công ty Dệt May Hà Nội (Hanoitex)', N'Nguồn sỉ các dòng áo sơ mi công sở, vải sơ mi lụa, vải Oxford khu vực phía Bắc. Năng lực cung ứng số lượng lớn, giao hàng trong ngày.', 2, CAST(N'2026-08-28T13:46:03.647' AS DateTime), CAST(840000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (7, N'PN-20260828-005', N'Công ty Dệt May Hà Nội (Hanoitex)', N'Chuyên hàng thun cotton 100% và nỉ bông dày dặn. Giao hàng trong 3-5 ngày.', 2, CAST(N'2026-08-28T14:00:16.833' AS DateTime), CAST(1280000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (8, N'PN-20260828-006', N'Xưởng May Gia Công Atino', N'Đối tác chuyên gia công áo thun, áo polo chuẩn form local brand, đường may kỹ, tỉ lệ lỗi hỏng thấp. Đạt tiêu chuẩn xuất khẩu.', 2, CAST(N'2026-08-28T18:41:26.553' AS DateTime), CAST(2280000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (9, N'PN-20260828-007', N'Xưởng Cắt May Nỉ Mùa Thu', N'Chuyên các dòng áo khoác nhẹ, sweater, hoodie mỏng. Chất liệu vải nỉ da cá chuyên dụng cho thời tiết giao mùa, không làm hàng nỉ bông dày.', 2, CAST(N'2026-08-28T21:38:48.680' AS DateTime), CAST(910000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (10, N'PN-20260828-008', N'Công ty Dệt May Hà Nội (Hanoitex)', N'Nguồn sỉ các dòng áo sơ mi công sở, vải sơ mi lụa, vải Oxford khu vực phía Bắc. Năng lực cung ứng số lượng lớn, giao hàng trong ngày.', 2, CAST(N'2026-08-28T21:42:40.060' AS DateTime), CAST(1120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (11, N'PN-20260828-009', N'Công ty Dệt May Hà Nội (Hanoitex)', N'Nguồn sỉ các dòng áo sơ mi công sở, vải sơ mi lụa, vải Oxford khu vực phía Bắc. Năng lực cung ứng số lượng lớn, giao hàng trong ngày.', 2, CAST(N'2026-08-28T21:45:35.853' AS DateTime), CAST(600000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (12, N'PN-20260828-010', N'Xưởng Jean VNXK Ninh Hiệp', N'Chuyên sỉ quần Jean ống suông, quần Kaki theo dây (ri) đủ size. Chất lượng denim ổn định, wash màu đều, nhãn mác "Made in VietNam" đầy đủ.', 2, CAST(N'2026-08-28T21:48:00.460' AS DateTime), CAST(2070000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (13, N'PN-20260828-011', N'Xưởng Cắt May Nỉ Mùa Thu', N'Chuyên gia công các dòng áo khoác nhẹ, sweater, thun dài tay, thun cổ lọ mùa thu. Chất liệu mềm mại, co giãn tốt. Không nhận làm hàng nỉ bông dày.', 2, CAST(N'2026-08-28T21:57:34.850' AS DateTime), CAST(550000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (14, N'PN-20260828-012', N'Xưởng Jean VNXK Ninh Hiệp', N'Chuyên sỉ quần Jean ống suông, quần Kaki theo dây (ri) đủ size. Chất lượng denim ổn định, wash màu đều, nhãn mác "Made in VietNam" đầy đủ.', 2, CAST(N'2026-08-28T22:01:54.157' AS DateTime), CAST(420000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (15, N'PN-20260828-013', N'Xưởng May Mặc An Phú', N'Chuyên gia công hàng thun cotton 100% và nỉ bông dày dặn mùa đông. Công nghệ in dập nổi tốt, ít bị bong tróc khi giặt máy.', 2, CAST(N'2026-08-28T22:06:10.373' AS DateTime), CAST(300000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (16, N'PN-20260828-014', N'Xưởng Cắt May Nỉ Mùa Thu', N'Chuyên các dòng áo khoác nhẹ, sweater mùa thu. Chất liệu nỉ da cá mềm mại, co giãn tốt. Không nhận làm hàng nỉ bông lót lông', 2, CAST(N'2026-08-28T22:08:13.987' AS DateTime), CAST(450000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (17, N'PN-20260828-015', N'Xưởng May Mặc An Phú', N'Lấy đồng bộ với các mã áo hoodie nỉ bông để tạo thành set đồ (tracksuit). Yêu cầu kiểm tra kỹ độ đàn hồi của chun gấu quần và chun bụng khi nhập lô mới.', 2, CAST(N'2026-08-28T22:11:08.263' AS DateTime), CAST(560000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (18, N'PN-20260828-016', N'Xưởng Dệt & Gia Công Atino', N'Đối tác có dây chuyền dệt kim/áo len chuẩn form local brand. Tỉ lệ hàng lỗi/thủng sợi cực thấp, viền sọc đan chắc chắn.', 2, CAST(N'2026-08-28T22:12:35.003' AS DateTime), CAST(560000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (19, N'PN-20260828-017', N'Xưởng May Mặc An Phú', N'Thế mạnh gia công hoodie nỉ bông form rộng. Yêu cầu kiểm tra kỹ phần dây rút và khoen kim loại ở mũ khi nhập hàng để tránh rỉ sét.', 2, CAST(N'2026-08-28T22:14:21.747' AS DateTime), CAST(910000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (20, N'PN-20260828-018', N'Công ty Dệt May Hà Nội (Hanoitex)', N'Hàng xưởng may kỹ, chuẩn form may đo âu phục. Lưu ý dặn xưởng vắt sổ viền cẩn thận phần gấu quần để khách hàng có thể tự cắt lên lai (lên gấu) tùy theo chiều cao cá nhân.', 2, CAST(N'2026-08-28T22:16:04.250' AS DateTime), CAST(560000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt] ([id], [code], [supplier_name], [note], [created_by], [created_at], [total_amount]) VALUES (21, N'PN-20260828-019', N'Xưởng May Mặc An Phú', N'Đơn giá nhập cao hơn do sử dụng phụ kiện khóa kéo YKK tĩnh điện và khuôn dập nổi 3D. Hàng đóng gói từng túi zip riêng biệt cẩn thận.', 2, CAST(N'2026-08-28T22:17:30.117' AS DateTime), CAST(720000.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[goods_receipt] OFF
GO
SET IDENTITY_INSERT [dbo].[goods_receipt_item] ON 

INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (1, 1, 2, 30, CAST(95000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (2, 1, 3, 20, CAST(95000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (3, 2, 33, 10, CAST(285000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (4, 3, 36, 4, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (5, 3, 37, 5, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (6, 3, 38, 4, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (7, 4, 39, 3, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (8, 5, 40, 3, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (9, 5, 41, 5, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (10, 5, 42, 6, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (11, 6, 43, 2, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (12, 6, 44, 3, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (13, 6, 45, 4, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (14, 6, 46, 5, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (15, 7, 47, 5, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (16, 7, 48, 6, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (17, 7, 49, 3, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (18, 7, 50, 2, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (19, 8, 51, 3, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (20, 8, 52, 4, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (21, 8, 53, 5, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (23, 8, 55, 3, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (24, 8, 56, 2, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (25, 9, 57, 3, CAST(130000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (26, 9, 58, 4, CAST(130000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (27, 10, 59, 2, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (28, 10, 60, 3, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (29, 10, 61, 5, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (30, 10, 62, 4, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (31, 11, 63, 3, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (32, 11, 64, 2, CAST(120000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (33, 12, 65, 4, CAST(230000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (34, 12, 66, 5, CAST(230000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (35, 13, 67, 3, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (36, 13, 68, 2, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (37, 13, 69, 3, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (38, 13, 70, 3, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (39, 14, 71, 3, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (40, 14, 72, 4, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (41, 15, 73, 3, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (42, 15, 74, 2, CAST(60000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (43, 16, 75, 3, CAST(90000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (44, 16, 76, 2, CAST(90000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (45, 17, 77, 4, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (46, 17, 78, 3, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (47, 18, 79, 3, CAST(70000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (48, 18, 80, 5, CAST(70000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (49, 19, 81, 4, CAST(130000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (50, 19, 82, 3, CAST(130000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (51, 20, 83, 5, CAST(70000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (52, 20, 84, 3, CAST(70000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (53, 21, 85, 5, CAST(80000.00 AS Decimal(18, 2)))
INSERT [dbo].[goods_receipt_item] ([id], [goods_receipt_id], [variant_id], [quantity], [unit_cost_price]) VALUES (54, 21, 86, 4, CAST(80000.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[goods_receipt_item] OFF
GO
SET IDENTITY_INSERT [dbo].[notification] ON 

INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (1, 3, N'Đơn hàng #1 của bạn đã hoàn thành.', N'/order/detail/1', 1, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (2, 3, N'Đơn hàng #3 đã được gửi đi! Mã vận đơn GHN: GHN-20250001. Tra cứu tại ghn.vn', N'/order/detail/3', 0, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (3, 4, N'Đơn hàng #5 của bạn đã hoàn thành.', N'/order/detail/5', 1, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (4, 4, N'Đơn hàng #6 đã được xác nhận! Chúng tôi đang chuẩn bị hàng.', N'/order/detail/6', 0, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (5, 5, N'Đơn hàng #9 đã được giao thành công! Vui lòng xác nhận :>', N'/order/detail/9', 0, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (6, 6, N'Đơn hàng #11 đã được gửi đi! Mã vận đơn GHN: GHN-20250003. Tra cứu tại ghn.vn', N'/order/detail/11', 0, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
INSERT [dbo].[notification] ([id], [user_id], [message], [link], [is_read], [created_at]) VALUES (7, 7, N'Đơn hàng #13 của bạn đã bị huỷ.', N'/order/detail/13', 1, CAST(N'2026-08-28T13:12:05.820' AS DateTime))
SET IDENTITY_INSERT [dbo].[notification] OFF
GO
SET IDENTITY_INSERT [dbo].[order_detail] ON 

INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (1, 1, 2, 2, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (2, 1, 26, 1, CAST(549000.00 AS Decimal(18, 2)), CAST(549000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (3, 2, 27, 1, CAST(549000.00 AS Decimal(18, 2)), CAST(549000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (4, 3, 16, 1, CAST(350000.00 AS Decimal(18, 2)), CAST(350000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (5, 3, 6, 1, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (6, 4, 1, 1, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (7, 5, 16, 1, CAST(350000.00 AS Decimal(18, 2)), CAST(350000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (8, 5, 30, 1, CAST(449000.00 AS Decimal(18, 2)), CAST(449000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (9, 6, 33, 1, CAST(599000.00 AS Decimal(18, 2)), CAST(599000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (10, 6, 10, 1, CAST(249000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (11, 7, 13, 1, CAST(249000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (12, 8, 33, 1, CAST(599000.00 AS Decimal(18, 2)), CAST(599000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (13, 9, 34, 1, CAST(599000.00 AS Decimal(18, 2)), CAST(599000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (14, 10, 20, 1, CAST(379000.00 AS Decimal(18, 2)), CAST(379000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (15, 11, 29, 1, CAST(449000.00 AS Decimal(18, 2)), CAST(449000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (16, 11, 9, 1, CAST(249000.00 AS Decimal(18, 2)), CAST(249000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (17, 12, 25, 1, CAST(549000.00 AS Decimal(18, 2)), CAST(549000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (18, 12, 5, 1, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (19, 13, 2, 1, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (20, 14, 30, 1, CAST(449000.00 AS Decimal(18, 2)), CAST(499000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (21, 15, 2, 1, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (22, 15, 9, 1, CAST(95000.00 AS Decimal(18, 2)), CAST(95000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_detail] ([id], [order_id], [variant_id], [quantity], [price], [original_price]) VALUES (23, 16, 1, 1, CAST(199000.00 AS Decimal(18, 2)), CAST(199000.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[order_detail] OFF
GO
SET IDENTITY_INSERT [dbo].[order_voucher] ON 

INSERT [dbo].[order_voucher] ([id], [order_id], [voucher_id], [customer_id], [discount_amount]) VALUES (1, 1, 1, 3, CAST(50000.00 AS Decimal(18, 2)))
INSERT [dbo].[order_voucher] ([id], [order_id], [voucher_id], [customer_id], [discount_amount]) VALUES (2, 5, 2, 4, CAST(100000.00 AS Decimal(18, 2)))
SET IDENTITY_INSERT [dbo].[order_voucher] OFF
GO
SET IDENTITY_INSERT [dbo].[product] ON 

INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (1, N'Áo thun Basic Trắng', N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Chất liệu mềm mại, thấm hút mồ hôi tốt, phù hợp mặc hàng ngày.', CAST(199000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 180)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (2, N'Áo thun Basic Đen', N'Áo thun cotton 100% thoáng mát, form regular fit unisex. Màu đen cổ điển dễ phối đồ, bền màu sau nhiều lần giặt.', CAST(199000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 180)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (3, N'Áo thun In Mèo Trắng', N'Áo thun cotton in hình mèo dễ thương độc đáo, unisex. Họa tiết sắc nét không phai màu, phong cách trẻ trung năng động.', CAST(249000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 190)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (4, N'Áo thun In Mèo Đen', N'Áo thun cotton in hình mèo dễ thương, nền đen nổi bật họa tiết, phong cách streetwear hiện đại.', CAST(249000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 190)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (5, N'Áo sơ mi Công sở Trắng', N'Áo sơ mi công sở vải lụa mềm mại thoáng mát, form slim fit. Thiết kế cổ đứng thanh lịch, phù hợp đi làm và các dịp trang trọng.', CAST(350000.00 AS Decimal(18, 2)), 0, 2, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 250)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (6, N'Áo sơ mi Xanh Nhạt', N'Áo sơ mi màu xanh nhạt phong cách Hàn Quốc, chất cotton pha. Màu sắc nhẹ nhàng dễ phối đồ, thích hợp đi chơi và dạo phố.', CAST(379000.00 AS Decimal(18, 2)), 0, 2, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 240)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (7, N'Áo sơ mi Xanh Navy', N'Áo sơ mi xanh navy lịch sự sang trọng. Chất vải cao cấp ít nhăn, dễ ủi, phù hợp công sở và các buổi gặp gỡ quan trọng.', CAST(399000.00 AS Decimal(18, 2)), 0, 2, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 260)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (8, N'Quần Jean Layer', N'Quần jean layer thiết kế độc đáo phong cách streetwear. Chất jean co giãn thoải mái, form slim fit tôn dáng.', CAST(549000.00 AS Decimal(18, 2)), 0, 3, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 600)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (9, N'Quần Sweater Đen', N'Quần sweater chất nỉ bông dày dặn ấm áp mùa đông. Thiết kế đơn giản dễ phối, có túi hai bên tiện dụng.', CAST(449000.00 AS Decimal(18, 2)), 0, 3, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 450)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (10, N'Áo Hoodie Đen', N'Áo hoodie nỉ bông dày dặn giữ nhiệt tốt, form oversize thoải mái. Có mũ điều chỉnh được, túi kangaroo rộng rãi.', CAST(599000.00 AS Decimal(18, 2)), 0, 4, CAST(N'2026-08-28T13:12:05.670' AS DateTime), 550)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (11, N'Áo thun Basic Oversize', N'Áo thun form rộng thoải mái, chất cotton 100% thấm hút mồ hôi. Thiết kế trơn cơ bản dễ dàng phối đồ mặc hàng ngày.', CAST(199000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:21:12.810' AS DateTime), 250)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (12, N'Áo thun Thể thao Phối lưới', N'Chất vải poly lạnh co giãn 4 chiều, thiết kế rãnh lưới thoát khí nhanh. Trọng lượng siêu nhẹ, tối ưu cho các hoạt động di chuyển liên tục trên sân cầu lông.', CAST(150000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:45:01.917' AS DateTime), 150)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (13, N'Áo Polo Nam Cổ Bẻ Thanh Lịch', N'Vải cá sấu (spandex) co giãn nhẹ, giữ form tốt. Cổ áo dệt kim cứng cáp, phù hợp đi học hoặc đi cà phê cuối tuần.', CAST(249000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T13:59:22.883' AS DateTime), 300)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (14, N'Áo Sweater Nỉ Dáng Suông', N'Chất nỉ da cá mỏng nhẹ vừa phải, bề mặt mềm mịn. Độ giữ ấm ở mức khá, cực kỳ thoải mái và dễ chịu cho thời tiết mát mẻ của mùa thu.', CAST(320000.00 AS Decimal(18, 2)), 1, 5, CAST(N'2026-08-28T18:39:44.813' AS DateTime), 400)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (15, N'Áo Sweater Nỉ Dáng Suông', N'Chất nỉ da cá mỏng nhẹ vừa phải, bề mặt mềm mịn. Độ giữ ấm ở mức khá, cực kỳ thoải mái và dễ chịu cho thời tiết mát mẻ của mùa thu.', CAST(320000.00 AS Decimal(18, 2)), 0, 5, CAST(N'2026-08-28T18:40:13.290' AS DateTime), 400)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (16, N'Áo Hoodie Zip Basic', N'o khoác hoodie có khóa kéo tiện lợi, form rộng rãi, lót bông nỉ mỏng bên trong. Thiết kế gọn nhẹ, không gây bí bách hay cộm cứng.', CAST(380000.00 AS Decimal(18, 2)), 0, 4, CAST(N'2026-08-28T21:37:44.447' AS DateTime), 300)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (17, N'Áo Sơ mi Công sở Chống nhăn', N'Chất vải lụa pha nilon chống nhăn hiệu quả, bề mặt trơn nhẵn. Cổ áo ép mếch phẳng phiu, phù hợp môi trường lịch sự.', CAST(280000.00 AS Decimal(18, 2)), 0, 2, CAST(N'2026-08-28T21:41:49.627' AS DateTime), 250)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (18, N'Áo Sơ mi Oxford Dài Tay', N'Vải Oxford dệt thoi nổi vân sọc chéo đặc trưng. Thấm hút tốt, mang lại vẻ ngoài năng động, trẻ trung và hiện đại.', CAST(310000.00 AS Decimal(18, 2)), 0, 2, CAST(N'2026-08-28T21:44:58.497' AS DateTime), 320)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (19, N'Quần Jean Ống Suông Hack Dáng', N'Denim 100% không co giãn giúp giữ form quần cố định. Ống quần rộng vừa phải, che khuyết điểm chân hiệu quả.', CAST(420000.00 AS Decimal(18, 2)), 0, 3, CAST(N'2026-08-28T21:46:50.077' AS DateTime), 550)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (20, N'Áo Thun Dài Tay Cổ Lọ Basic', N'Áo thun cotton tici dài tay, cổ lọ cao 3cm giữ ấm cổ. Phù hợp mặc lót bên trong áo khoác, áo blazer hoặc mặc đơn lẻ vào mùa thu.', CAST(220000.00 AS Decimal(18, 2)), 0, 1, CAST(N'2026-08-28T21:53:42.503' AS DateTime), 280)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (21, N'Quần Dài Kaki Nam', N'Phong cách streetwear với thiết kế dài. Cạp chun kèm dây rút chắc chắn, dễ dàng điều chỉnh độ rộng.', CAST(350000.00 AS Decimal(18, 2)), 1, 3, CAST(N'2026-08-28T21:59:56.303' AS DateTime), 450)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (22, N'Quần Dài Kaki Nam', N'Phong cách streetwear với thiết kế dài. Cạp chun kèm dây rút chắc chắn, dễ dàng điều chỉnh độ rộng.', CAST(350000.00 AS Decimal(18, 2)), 0, 3, CAST(N'2026-08-28T22:00:11.473' AS DateTime), 450)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (23, N'Áo Sweater Nỉ Bông In Logo Form Rộng', N'Chất nỉ bông dày dặn, ấm áp cho mùa đông. Mặt trước in logo thương hiệu dập nổi chắc chắn. Form oversize rộng rãi, tay áo bo thun ôm sát giữ nhiệt.', CAST(350000.00 AS Decimal(18, 2)), 0, 5, CAST(N'2026-08-28T22:05:20.433' AS DateTime), 450)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (24, N'Áo Sweater Nỉ Da Cá Cổ Tròn Basic', N'Áo nỉ da cá thoáng mát, nhẹ nhàng, phù hợp thời tiết se lạnh. Cổ tròn basic bo gân dễ phối sơ mi bên trong. Bề mặt vải mềm mịn, đã qua xử lý chống xù lông.', CAST(290000.00 AS Decimal(18, 2)), 0, 5, CAST(N'2026-08-28T22:07:25.630' AS DateTime), 350)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (25, N'Quần Nỉ Jogger Dáng Thể Thao', N'Quần nỉ jogger ống rộng phong cách sporty, bo chun gấu quần. Chất nỉ bông mềm mại giữ ấm tốt, lưng thun có dây rút bọc kim loại điều chỉnh. Phù hợp mặc đi tập thể thao', CAST(280000.00 AS Decimal(18, 2)), 0, 3, CAST(N'2026-08-28T22:10:35.967' AS DateTime), 400)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (26, N'Áo Sweater Dệt Kim Sọc Ngang', N'Áo len dệt kim mỏng nhẹ với họa tiết sọc ngang xen kẽ trẻ trung. Chất len lông cừu tổng hợp giữ ấm tốt, co giãn thoải mái không gây dão form phần cổ và tay áo.', CAST(420000.00 AS Decimal(18, 2)), 0, 5, CAST(N'2026-08-28T22:12:09.397' AS DateTime), 500)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (27, N'Áo Hoodie Phối Màu Raglan', N'Thiết kế tay áo raglan phối màu tương phản nổi bật phần vai và cánh tay. Lót nỉ bông siêu ấm. Form áo trùm qua mông, mũ hai lớp cứng cáp đứng form, dây rút bọc kim loại.', CAST(410000.00 AS Decimal(18, 2)), 0, 4, CAST(N'2026-08-28T22:13:41.157' AS DateTime), 549)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (28, N'Quần Tây Nam Xếp Ly Phom Suông', N'Quần tây nam dáng suông rộng có 2 ly xếp trước cạp quần mang hơi hướng classic menswear. Chất vải tuyết mưa cao cấp, mình vải dày dặn, rủ tự nhiên, chống nhăn nhàu tuyệt đối.', CAST(420000.00 AS Decimal(18, 2)), 0, 3, CAST(N'2026-08-28T22:15:34.270' AS DateTime), 450)
INSERT [dbo].[product] ([id], [name], [description], [price], [is_deleted], [category_id], [created_at], [weight]) VALUES (29, N'Áo Hoodie Zip Dập Nổi 3D', N'Phiên bản áo khoác hoodie zip cao cấp với họa tiết dập nổi 3D dọc cánh tay. Khóa kéo hợp kim trơn tru, túi ốp trước rộng rãi, chất nỉ cotton 100% dày dặn chống gió tốt.', CAST(460000.00 AS Decimal(18, 2)), 0, 4, CAST(N'2026-08-28T22:16:58.867' AS DateTime), 300)
SET IDENTITY_INSERT [dbo].[product] OFF
GO
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (1, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (2, 1)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (2, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (3, 1)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (3, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (4, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (5, 1)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (8, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (9, 1)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (10, 1)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (11, 1)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (19, 2)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (20, 2)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (22, 2)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (23, 2)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (23, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (24, 2)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (24, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (25, 2)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (26, 3)
INSERT [dbo].[product_collection] ([product_id], [collection_id]) VALUES (27, 2)
GO
SET IDENTITY_INSERT [dbo].[product_image] ON 

INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (1, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (2, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 1)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (3, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 2)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (4, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 2)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (5, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 3)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (6, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/5af9ca5c791bf845a10a15_n8b5fk.jpg', 3)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (7, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 4)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (8, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/394df5f646b1c7ef9ea017_wdzmbo.jpg', 4)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (9, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 5)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (10, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 5)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (11, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 6)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (12, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 6)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (13, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 7)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (14, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 7)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (15, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 8)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (16, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 8)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (17, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg', 9)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (18, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 9)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (19, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 10)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (20, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 10)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (21, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898074/products/l9ffxwzs2jnhhkzqxsaq.webp', 11)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (22, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787899503/products/czenv17xwnyfjbkoaqcz.webp', 12)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (23, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787900364/products/qrxpdglbopm2tvrzlabd.webp', 13)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (24, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787917214/products/k0ub7hijw5nshdokeqsh.webp', 15)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (25, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787927865/products/zhchrpc2hqemdcecivsf.webp', 16)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (26, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928111/products/msrouqw8aqjtbctw3vnl.jpg', 17)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (27, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928299/products/l02dy0pjbp3cpeqc60on.webp', 18)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (28, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928411/products/yyy8nd6n8onqtcs6t4wd.webp', 19)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (29, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928823/products/a8il0yxfvq56s7uiobfo.webp', 20)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (30, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929213/products/imh6wpxgwsumie7gtcn3.webp', 22)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (31, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929521/products/sqktc34u4lfemzyfbm7o.webp', 23)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (32, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929646/products/kh02e00iayn15552jnok.webp', 24)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (33, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929836/products/t6itr9tbobropeth3thk.webp', 25)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (34, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929930/products/pgqnzgi0yqlblfjavdup.webp', 26)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (35, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930022/products/ktk6zkhvgdvnvpfmh5ie.webp', 27)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (36, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930135/products/kadumzolry2eko7bi0ey.webp', 28)
INSERT [dbo].[product_image] ([id], [image_url], [product_id]) VALUES (37, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930219/products/k1fovgew2c3vrpj4ioua.webp', 29)
SET IDENTITY_INSERT [dbo].[product_image] OFF
GO
SET IDENTITY_INSERT [dbo].[product_sale] ON 

INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (1, 9, 1, CAST(10.00 AS Decimal(5, 2)), CAST(N'2026-08-23T13:12:05.730' AS DateTime), CAST(N'2026-08-30T13:12:05.730' AS DateTime), 1, CAST(N'2026-08-28T13:12:05.730' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (2, 1, 2, CAST(15.00 AS Decimal(5, 2)), CAST(N'2026-08-27T13:12:05.730' AS DateTime), CAST(N'2026-09-03T13:12:05.730' AS DateTime), 1, CAST(N'2026-08-28T13:12:05.730' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (3, 5, NULL, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-07-19T13:12:05.730' AS DateTime), CAST(N'2026-07-29T13:12:05.730' AS DateTime), 0, CAST(N'2026-08-28T13:12:05.730' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (4, 2, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.273' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (5, 3, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.280' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (6, 4, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.280' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (7, 5, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.283' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (8, 6, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.287' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (9, 7, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.290' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (10, 8, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.293' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (11, 10, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.297' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (12, 11, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.300' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (13, 12, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.300' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (14, 13, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.303' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (15, 15, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.307' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (16, 16, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.307' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (17, 17, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.310' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (18, 18, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.313' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (19, 19, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.313' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (20, 20, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.317' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (21, 22, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.320' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (22, 23, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.323' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (23, 24, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.327' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (24, 25, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.327' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (25, 26, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.330' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (26, 27, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.333' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (27, 28, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.333' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (28, 29, 3, CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), 0, CAST(N'2026-08-28T22:29:19.337' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (29, 9, 4, CAST(12.00 AS Decimal(5, 2)), CAST(N'2026-09-05T22:29:00.000' AS DateTime), CAST(N'2026-09-12T22:29:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:09.953' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (30, 1, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.130' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (31, 2, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.130' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (32, 3, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.133' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (33, 4, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.133' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (34, 5, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.133' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (35, 6, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.137' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (36, 7, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.140' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (37, 8, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.140' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (38, 9, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.143' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (39, 10, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.143' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (40, 11, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.147' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (41, 12, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.150' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (42, 13, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.153' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (43, 15, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.153' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (44, 16, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.157' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (45, 17, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.160' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (46, 18, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.160' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (47, 19, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.163' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (48, 20, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.167' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (49, 22, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.170' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (50, 23, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.170' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (51, 24, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.173' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (52, 25, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.177' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (53, 26, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.177' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (54, 27, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.180' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (55, 28, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.183' AS DateTime))
INSERT [dbo].[product_sale] ([id], [product_id], [sale_batch_id], [discount_percent], [start_date], [end_date], [is_active], [created_at]) VALUES (56, 29, 5, CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), 1, CAST(N'2026-08-28T22:30:53.187' AS DateTime))
SET IDENTITY_INSERT [dbo].[product_sale] OFF
GO
SET IDENTITY_INSERT [dbo].[product_variant] ON 

INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (1, 1, 1, 1, N'AT-WHT-S', 20, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (2, 1, 2, 1, N'AT-WHT-M', 35, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (3, 1, 3, 1, N'AT-WHT-L', 28, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (4, 1, 4, 1, N'AT-WHT-XL', 15, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (5, 2, 1, 2, N'AT-BLK-S', 18, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (6, 2, 2, 2, N'AT-BLK-M', 30, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (7, 2, 3, 2, N'AT-BLK-L', 22, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (8, 2, 4, 2, N'AT-BLK-XL', 3, CAST(95000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (9, 3, 1, 1, N'AT-CAT-WHT-S', 12, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (10, 3, 2, 1, N'AT-CAT-WHT-M', 20, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (11, 3, 3, 1, N'AT-CAT-WHT-L', 8, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (12, 4, 1, 2, N'AT-CAT-BLK-S', 10, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (13, 4, 2, 2, N'AT-CAT-BLK-M', 18, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (14, 4, 3, 2, N'AT-CAT-BLK-L', 4, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (15, 5, 1, 1, N'SM-WHT-S', 12, CAST(165000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (16, 5, 2, 1, N'SM-WHT-M', 25, CAST(165000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (17, 5, 3, 1, N'SM-WHT-L', 18, CAST(165000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (18, 5, 4, 1, N'SM-WHT-XL', 2, CAST(165000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (19, 6, 1, 5, N'SM-LBL-S', 10, CAST(180000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (20, 6, 2, 5, N'SM-LBL-M', 20, CAST(180000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (21, 6, 3, 5, N'SM-LBL-L', 8, CAST(180000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (22, 7, 1, 3, N'SM-NVY-S', 8, CAST(190000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (23, 7, 2, 3, N'SM-NVY-M', 15, CAST(190000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (24, 7, 3, 3, N'SM-NVY-L', 10, CAST(190000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (25, 8, 1, 2, N'QJ-LAY-S', 12, CAST(260000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (26, 8, 2, 2, N'QJ-LAY-M', 20, CAST(260000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (27, 8, 3, 2, N'QJ-LAY-L', 15, CAST(260000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (28, 8, 4, 2, N'QJ-LAY-XL', 3, CAST(260000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (29, 9, 1, 2, N'QSW-BLK-S', 10, CAST(215000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (30, 9, 2, 2, N'QSW-BLK-M', 18, CAST(215000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (31, 9, 3, 2, N'QSW-BLK-L', 0, CAST(215000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (32, 10, 1, 2, N'HD-BLK-S', 12, CAST(285000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (33, 10, 2, 2, N'HD-BLK-M', 20, CAST(285000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (34, 10, 3, 2, N'HD-BLK-L', 15, CAST(285000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (35, 10, 4, 2, N'HD-BLK-XL', 2, CAST(285000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.690' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (36, 11, 3, 2, N'SP11-C2-S3', 4, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:21:48.353' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (37, 11, 2, 2, N'SP11-C2-S2', 5, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:21:48.397' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (38, 11, 4, 2, N'SP11-C2-S4', 4, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:21:48.407' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (39, 11, 2, 1, N'SP11-C1-S2', 3, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:27:02.000' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (40, 11, 3, 6, N'SP11-C4-S3', 3, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:28:29.183' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (41, 11, 2, 6, N'SP11-C4-S2', 5, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:28:29.217' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (42, 11, 1, 6, N'SP11-C4-S1', 6, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:28:29.250' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (43, 12, 3, 1, N'SP12-C1-S3', 2, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:45:26.337' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (44, 12, 4, 1, N'SP12-C1-S4', 3, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:45:26.353' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (45, 12, 3, 3, N'SP12-C3-S3', 4, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:45:26.363' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (46, 12, 4, 3, N'SP12-C3-S4', 5, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:45:26.380' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (47, 13, 3, 2, N'SP13-C2-S3', 5, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:59:46.307' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (48, 13, 4, 2, N'SP13-C2-S4', 6, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:59:46.317' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (49, 13, 3, 3, N'SP13-C3-S3', 3, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:59:46.327' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (50, 13, 4, 3, N'SP13-C3-S4', 2, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:59:46.340' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (51, 15, 2, 2, N'SP15-C2-S2', 3, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T18:40:25.333' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (52, 15, 4, 2, N'SP15-C2-S4', 4, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T18:40:25.347' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (53, 15, 5, 2, N'SP15-C2-S5', 5, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T18:40:25.353' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (55, 15, 4, 1, N'SP15-C1-S4', 3, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T18:40:25.370' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (56, 15, 5, 1, N'SP15-C1-S5', 2, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T18:40:25.377' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (57, 16, 3, 1, N'SP16-C1-S3', 3, CAST(130000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:37:55.793' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (58, 16, 4, 1, N'SP16-C1-S4', 4, CAST(130000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:37:55.803' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (59, 17, 1, 1, N'SP17-C1-S1', 2, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:42:03.183' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (60, 17, 4, 1, N'SP17-C1-S4', 3, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:42:03.187' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (61, 17, 1, 3, N'SP17-C3-S1', 5, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:42:03.193' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (62, 17, 4, 3, N'SP17-C3-S4', 4, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:42:03.197' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (63, 18, 3, 1, N'SP18-C1-S3', 3, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:45:06.000' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (64, 18, 4, 1, N'SP18-C1-S4', 2, CAST(120000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:45:06.003' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (65, 19, 1, 5, N'SP19-C5-S1', 4, CAST(230000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:47:08.557' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (66, 19, 4, 5, N'SP19-C5-S4', 5, CAST(230000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:47:08.560' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (67, 20, 3, 2, N'SP20-C2-S3', 3, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:53:57.677' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (68, 20, 4, 2, N'SP20-C2-S4', 2, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:53:57.680' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (69, 20, 3, 7, N'SP20-C1-S3', 3, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:53:57.680' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (70, 20, 4, 7, N'SP20-C1-S4', 3, CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T21:53:57.683' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (71, 22, 4, 8, N'SP22-C8-S4', 3, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:01:19.010' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (72, 22, 5, 8, N'SP22-C8-S5', 4, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:01:19.013' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (73, 23, 4, 9, N'SP23-C9-S4', 3, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:05:48.060' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (74, 23, 5, 9, N'SP23-C9-S5', 2, CAST(60000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:05:48.063' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (75, 24, 4, 1, N'SP24-C1-S4', 3, CAST(90000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:07:32.910' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (76, 24, 5, 1, N'SP24-C1-S5', 2, CAST(90000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:07:32.913' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (77, 25, 4, 10, N'SP25-C10-S4', 4, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:10:43.407' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (78, 25, 5, 10, N'SP25-C10-S5', 3, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:10:43.410' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (79, 26, 4, 10, N'SP26-C10-S4', 3, CAST(70000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:12:15.467' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (80, 26, 5, 10, N'SP26-C10-S5', 5, CAST(70000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:12:15.467' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (81, 27, 4, 1, N'SP27-C1-S4', 4, CAST(130000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:13:58.383' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (82, 27, 5, 1, N'SP27-C1-S5', 3, CAST(130000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:13:58.387' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (83, 28, 3, 4, N'SP28-C4-S3', 5, CAST(70000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:15:45.807' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (84, 28, 1, 4, N'SP28-C4-S1', 3, CAST(70000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:15:45.807' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (85, 29, 4, 2, N'SP29-C2-S4', 5, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:17:09.960' AS DateTime))
INSERT [dbo].[product_variant] ([id], [product_id], [size_id], [color_id], [sku], [stock], [cost_price], [created_at]) VALUES (86, 29, 5, 2, N'SP29-C2-S5', 4, CAST(80000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T22:17:09.963' AS DateTime))
SET IDENTITY_INSERT [dbo].[product_variant] OFF
GO
SET IDENTITY_INSERT [dbo].[role] ON 

INSERT [dbo].[role] ([id], [name]) VALUES (1, N'ROLE_ADMIN')
INSERT [dbo].[role] ([id], [name]) VALUES (4, N'ROLE_CASHIER')
INSERT [dbo].[role] ([id], [name]) VALUES (2, N'ROLE_CUSTOMER')
INSERT [dbo].[role] ([id], [name]) VALUES (3, N'ROLE_OWNER')
SET IDENTITY_INSERT [dbo].[role] OFF
GO
SET IDENTITY_INSERT [dbo].[sale_batch] ON 

INSERT [dbo].[sale_batch] ([id], [name], [discount_percent], [start_date], [end_date], [created_at]) VALUES (1, N'Flash Sale Cuối Tuần', CAST(10.00 AS Decimal(5, 2)), CAST(N'2026-08-23T13:12:05.723' AS DateTime), CAST(N'2026-08-30T13:12:05.723' AS DateTime), CAST(N'2026-08-28T13:12:05.723' AS DateTime))
INSERT [dbo].[sale_batch] ([id], [name], [discount_percent], [start_date], [end_date], [created_at]) VALUES (2, N'Sale Hè 2026', CAST(15.00 AS Decimal(5, 2)), CAST(N'2026-08-27T13:12:05.723' AS DateTime), CAST(N'2026-09-03T13:12:05.723' AS DateTime), CAST(N'2026-08-28T13:12:05.723' AS DateTime))
INSERT [dbo].[sale_batch] ([id], [name], [discount_percent], [start_date], [end_date], [created_at]) VALUES (3, N'Đón Thu Sang', CAST(20.00 AS Decimal(5, 2)), CAST(N'2026-08-29T22:28:00.000' AS DateTime), CAST(N'2026-09-10T22:29:00.000' AS DateTime), CAST(N'2026-08-28T22:29:19.257' AS DateTime))
INSERT [dbo].[sale_batch] ([id], [name], [discount_percent], [start_date], [end_date], [created_at]) VALUES (4, N'Tuần Lễ Vàng Công Sở', CAST(12.00 AS Decimal(5, 2)), CAST(N'2026-09-05T22:29:00.000' AS DateTime), CAST(N'2026-09-12T22:29:00.000' AS DateTime), CAST(N'2026-08-28T22:30:09.950' AS DateTime))
INSERT [dbo].[sale_batch] ([id], [name], [discount_percent], [start_date], [end_date], [created_at]) VALUES (5, N'Clear Kho Thu Đông', CAST(25.00 AS Decimal(5, 2)), CAST(N'2026-09-15T22:30:00.000' AS DateTime), CAST(N'2026-09-30T22:30:00.000' AS DateTime), CAST(N'2026-08-28T22:30:53.127' AS DateTime))
SET IDENTITY_INSERT [dbo].[sale_batch] OFF
GO
SET IDENTITY_INSERT [dbo].[size] ON 

INSERT [dbo].[size] ([id], [name]) VALUES (3, N'L')
INSERT [dbo].[size] ([id], [name]) VALUES (2, N'M')
INSERT [dbo].[size] ([id], [name]) VALUES (1, N'S')
INSERT [dbo].[size] ([id], [name]) VALUES (4, N'XL')
INSERT [dbo].[size] ([id], [name]) VALUES (5, N'XXL')
SET IDENTITY_INSERT [dbo].[size] OFF
GO
SET IDENTITY_INSERT [dbo].[stock_movement_log] ON 

INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (1, 2, N'IMPORT', 30, 35, N'GOODS_RECEIPT', 1, N'Nhập từ PN-20260801-001', 2, CAST(N'2026-08-23T13:12:05.747' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (2, 3, N'IMPORT', 20, 28, N'GOODS_RECEIPT', 1, N'Nhập từ PN-20260801-001', 2, CAST(N'2026-08-23T13:12:05.747' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (3, 33, N'IMPORT', 10, 20, N'GOODS_RECEIPT', 2, N'Nhập từ PN-20260803-001', 2, CAST(N'2026-08-26T13:12:05.747' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (4, 36, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 3, N'Nhập kho từ phiếu PN-20260828-001', 2, CAST(N'2026-08-28T13:25:30.770' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (5, 37, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 3, N'Nhập kho từ phiếu PN-20260828-001', 2, CAST(N'2026-08-28T13:25:30.787' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (6, 38, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 3, N'Nhập kho từ phiếu PN-20260828-001', 2, CAST(N'2026-08-28T13:25:30.793' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (7, 39, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 4, N'Nhập kho từ phiếu PN-20260828-002', 2, CAST(N'2026-08-28T13:27:24.117' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (8, 40, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 5, N'Nhập kho từ phiếu PN-20260828-003', 2, CAST(N'2026-08-28T13:28:48.390' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (9, 41, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 5, N'Nhập kho từ phiếu PN-20260828-003', 2, CAST(N'2026-08-28T13:28:48.400' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (10, 42, N'IMPORT', 6, 6, N'GOODS_RECEIPT', 5, N'Nhập kho từ phiếu PN-20260828-003', 2, CAST(N'2026-08-28T13:28:48.407' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (11, 43, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 6, N'Nhập kho từ phiếu PN-20260828-004', 2, CAST(N'2026-08-28T13:46:03.690' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (12, 44, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 6, N'Nhập kho từ phiếu PN-20260828-004', 2, CAST(N'2026-08-28T13:46:03.733' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (13, 45, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 6, N'Nhập kho từ phiếu PN-20260828-004', 2, CAST(N'2026-08-28T13:46:03.747' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (14, 46, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 6, N'Nhập kho từ phiếu PN-20260828-004', 2, CAST(N'2026-08-28T13:46:03.753' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (15, 47, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 7, N'Nhập kho từ phiếu PN-20260828-005', 2, CAST(N'2026-08-28T14:00:16.847' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (16, 48, N'IMPORT', 6, 6, N'GOODS_RECEIPT', 7, N'Nhập kho từ phiếu PN-20260828-005', 2, CAST(N'2026-08-28T14:00:16.860' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (17, 49, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 7, N'Nhập kho từ phiếu PN-20260828-005', 2, CAST(N'2026-08-28T14:00:16.867' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (18, 50, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 7, N'Nhập kho từ phiếu PN-20260828-005', 2, CAST(N'2026-08-28T14:00:16.877' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (19, 51, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 8, N'Nhập kho từ phiếu PN-20260828-006', 2, CAST(N'2026-08-28T18:41:26.570' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (20, 52, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 8, N'Nhập kho từ phiếu PN-20260828-006', 2, CAST(N'2026-08-28T18:41:26.587' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (21, 53, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 8, N'Nhập kho từ phiếu PN-20260828-006', 2, CAST(N'2026-08-28T18:41:26.590' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (23, 55, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 8, N'Nhập kho từ phiếu PN-20260828-006', 2, CAST(N'2026-08-28T18:41:26.600' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (24, 56, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 8, N'Nhập kho từ phiếu PN-20260828-006', 2, CAST(N'2026-08-28T18:41:26.607' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (25, 57, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 9, N'Nhập kho từ phiếu PN-20260828-007', 2, CAST(N'2026-08-28T21:38:48.690' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (26, 58, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 9, N'Nhập kho từ phiếu PN-20260828-007', 2, CAST(N'2026-08-28T21:38:48.697' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (27, 59, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 10, N'Nhập kho từ phiếu PN-20260828-008', 2, CAST(N'2026-08-28T21:42:40.063' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (28, 60, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 10, N'Nhập kho từ phiếu PN-20260828-008', 2, CAST(N'2026-08-28T21:42:40.067' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (29, 61, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 10, N'Nhập kho từ phiếu PN-20260828-008', 2, CAST(N'2026-08-28T21:42:40.070' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (30, 62, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 10, N'Nhập kho từ phiếu PN-20260828-008', 2, CAST(N'2026-08-28T21:42:40.070' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (31, 63, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 11, N'Nhập kho từ phiếu PN-20260828-009', 2, CAST(N'2026-08-28T21:45:35.857' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (32, 64, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 11, N'Nhập kho từ phiếu PN-20260828-009', 2, CAST(N'2026-08-28T21:45:35.860' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (33, 65, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 12, N'Nhập kho từ phiếu PN-20260828-010', 2, CAST(N'2026-08-28T21:48:00.463' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (34, 66, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 12, N'Nhập kho từ phiếu PN-20260828-010', 2, CAST(N'2026-08-28T21:48:00.467' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (35, 67, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 13, N'Nhập kho từ phiếu PN-20260828-011', 2, CAST(N'2026-08-28T21:57:34.857' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (36, 68, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 13, N'Nhập kho từ phiếu PN-20260828-011', 2, CAST(N'2026-08-28T21:57:34.860' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (37, 69, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 13, N'Nhập kho từ phiếu PN-20260828-011', 2, CAST(N'2026-08-28T21:57:34.863' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (38, 70, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 13, N'Nhập kho từ phiếu PN-20260828-011', 2, CAST(N'2026-08-28T21:57:34.867' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (39, 71, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 14, N'Nhập kho từ phiếu PN-20260828-012', 2, CAST(N'2026-08-28T22:01:54.163' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (40, 72, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 14, N'Nhập kho từ phiếu PN-20260828-012', 2, CAST(N'2026-08-28T22:01:54.163' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (41, 73, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 15, N'Nhập kho từ phiếu PN-20260828-013', 2, CAST(N'2026-08-28T22:06:10.377' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (42, 74, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 15, N'Nhập kho từ phiếu PN-20260828-013', 2, CAST(N'2026-08-28T22:06:10.380' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (43, 75, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 16, N'Nhập kho từ phiếu PN-20260828-014', 2, CAST(N'2026-08-28T22:08:13.993' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (44, 76, N'IMPORT', 2, 2, N'GOODS_RECEIPT', 16, N'Nhập kho từ phiếu PN-20260828-014', 2, CAST(N'2026-08-28T22:08:13.997' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (45, 77, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 17, N'Nhập kho từ phiếu PN-20260828-015', 2, CAST(N'2026-08-28T22:11:08.267' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (46, 78, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 17, N'Nhập kho từ phiếu PN-20260828-015', 2, CAST(N'2026-08-28T22:11:08.270' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (47, 79, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 18, N'Nhập kho từ phiếu PN-20260828-016', 2, CAST(N'2026-08-28T22:12:35.007' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (48, 80, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 18, N'Nhập kho từ phiếu PN-20260828-016', 2, CAST(N'2026-08-28T22:12:35.010' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (49, 81, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 19, N'Nhập kho từ phiếu PN-20260828-017', 2, CAST(N'2026-08-28T22:14:21.750' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (50, 82, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 19, N'Nhập kho từ phiếu PN-20260828-017', 2, CAST(N'2026-08-28T22:14:21.753' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (51, 83, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 20, N'Nhập kho từ phiếu PN-20260828-018', 2, CAST(N'2026-08-28T22:16:04.253' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (52, 84, N'IMPORT', 3, 3, N'GOODS_RECEIPT', 20, N'Nhập kho từ phiếu PN-20260828-018', 2, CAST(N'2026-08-28T22:16:04.253' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (53, 85, N'IMPORT', 5, 5, N'GOODS_RECEIPT', 21, N'Nhập kho từ phiếu PN-20260828-019', 2, CAST(N'2026-08-28T22:17:30.123' AS DateTime))
INSERT [dbo].[stock_movement_log] ([id], [variant_id], [change_type], [change_qty], [balance_after], [ref_type], [ref_id], [note], [created_by], [created_at]) VALUES (54, 86, N'IMPORT', 4, 4, N'GOODS_RECEIPT', 21, N'Nhập kho từ phiếu PN-20260828-019', 2, CAST(N'2026-08-28T22:17:30.123' AS DateTime))
SET IDENTITY_INSERT [dbo].[stock_movement_log] OFF
GO
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (1, 1)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (1, 3)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (2, 1)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (3, 2)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (4, 2)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (5, 2)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (6, 2)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (7, 2)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (8, 4)
INSERT [dbo].[user_role] ([user_id], [role_id]) VALUES (9, 2)
GO
SET IDENTITY_INSERT [dbo].[variant_image] ON 

INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (1, 1, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (2, 2, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (3, 3, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/c39a86253562b43ced7321_wm3mqm.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (4, 4, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/a626f69845dfc4819dce20_s7ielq.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (5, 5, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (6, 6, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (7, 7, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/0ba987153452b50cec4318_edprgs.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (8, 8, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/c665cfd87c9ffdc1a48e19_mcx5ck.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (9, 9, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (10, 10, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (11, 11, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/70865b23e864693a307514_vnfwoy.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (12, 12, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (13, 13, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (14, 14, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935621/4b68a8d21b959acbc38416_aveocs.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (15, 15, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (16, 16, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (17, 17, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/a9e3df376c70ed2eb4617_nr2kxl.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (18, 18, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/9456348287c5069b5fd48_hixdiz.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (19, 19, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (20, 20, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (21, 21, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/dbc2e66c552bd4758d3a12_oe0pp5.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (22, 22, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (23, 23, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (24, 24, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/476ce5c25685d7db8e9413_jrvply.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (25, 25, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (26, 26, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/af81b9360a718b2fd26028_kcdrpk.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (27, 26, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (28, 27, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (29, 28, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935623/2c4ceafa59bdd8e381ac26_askdix.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (30, 29, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (31, 30, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e3b17102c245431b1a5425_xiupwp.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (32, 31, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935622/8180f9334a74cb2a926524_x83he3.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (33, 32, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (34, 33, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (35, 34, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/e21cfd974ed0cf8e96c133_krr24i.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (36, 35, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1774935624/aceb8b633824b97ae03529_wspm1v.jpg', 2)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (37, 36, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898376/products/koh3azxjqueecjzq8npx.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (38, 37, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898387/products/pmyzqdeu06wyrjltafgx.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (39, 38, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898395/products/rte1xjjhsrmxv3cnnqni.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (40, 39, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898467/products/rfim9zlcqvkphsxjxbof.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (41, 40, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898625/products/ymrkyh8wyy7z8tsxuxew.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (42, 41, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898634/products/zh4exw2ewcajijdu1d3n.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (43, 42, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787898643/products/zxr6ctzhjjm0lcuucaut.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (44, 43, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787899604/products/n7azahzyiozvrojjmgeb.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (45, 44, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787899616/products/agjajs5rckstnwhbxwsx.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (46, 45, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787899626/products/ygufbzxsdyfjzcnmenim.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (47, 46, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787899637/products/cnbczyrt4gu1rapefyty.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (48, 47, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787900454/products/g2yxnbchrlpsigidkniy.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (49, 48, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787900464/products/jtnhovibf2szfqdcrfhc.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (50, 49, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787900475/products/wzjsxa8uw43njggmowqz.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (51, 50, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787900490/products/vqvhjeysxtsbyanovfyb.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (54, 55, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787917364/products/k6byii3oqfxuejx9lp9s.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (55, 56, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787917372/products/a0t5yswmpcbxgzi9ac7m.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (56, 51, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787917385/products/ehvk3ui9q5kd3u4ykkbc.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (57, 52, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787917393/products/o2wrfglcpwdws04u3wbk.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (58, 53, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787917402/products/wbmjfvkqjt5kb9ukvhsv.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (59, 57, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787927939/products/gn9dkoxicuds6pjavgwh.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (60, 58, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787927945/products/igr7bq21fkien5owitb5.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (61, 59, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928172/products/dhobaltvemkrzfuhponb.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (62, 60, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928178/products/nah33ktuqqcamuwe8chj.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (63, 61, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928186/products/cvhd5jcxnawoo0bzgity.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (64, 62, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928193/products/wuxaotvp4wep11ftxfzb.jpg', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (65, 63, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928345/products/zwi5kboy6yksmpobwwxb.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (66, 64, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928350/products/znbr8umhstyrgevnrvxr.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (67, 65, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928495/products/hs5iavgs9pjr5smon0bp.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (68, 66, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787928503/products/du9zrhylnxnembqqqhb5.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (69, 67, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929106/products/ah343lgmisetfoltmdmr.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (70, 68, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929111/products/awe2to04pohzgtuuuhpw.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (71, 69, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929116/products/qxzxo9xzvlhjt4njw05y.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (72, 70, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929123/products/hhze06qowrimcmvlfocv.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (73, 71, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929325/products/te1dfb16har7ywhxun20.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (74, 72, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929334/products/tknow1jej0yf91vcpwjm.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (75, 73, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929578/products/sw7angxnefwrvnwbsi9a.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (76, 74, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929583/products/libaae62wo6cqcnatlol.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (77, 75, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929703/products/qac68mr1swoyopul47ir.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (78, 76, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929708/products/lpq9hyubbjlpqj1ddbpc.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (79, 77, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929876/products/vkqpgvjfqtgjacwapb6n.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (80, 78, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929880/products/qtvt752vydk3dxsywryb.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (81, 79, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929963/products/ytbivcnecomrvwv9itrq.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (82, 80, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787929967/products/doajcm8pcoqpr5ytvias.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (83, 81, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930072/products/f7lhnqbbqgg9868nd09v.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (84, 82, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930077/products/cbomows7xgykiy68lb5u.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (85, 83, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930170/products/r6pgcftgvdxxiltrssfh.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (86, 84, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930175/products/mbukjdlaq7oertlrzn6f.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (87, 85, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930256/products/g5kowwg9yc8iwui1iief.webp', 1)
INSERT [dbo].[variant_image] ([id], [variant_id], [image_url], [sort_order]) VALUES (88, 86, N'https://res.cloudinary.com/dozzwbiww/image/upload/v1787930260/products/zg7khw4bszchjwbyzofl.webp', 1)
SET IDENTITY_INSERT [dbo].[variant_image] OFF
GO
SET IDENTITY_INSERT [dbo].[voucher] ON 

INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (1, N'WELCOME10', N'Giảm 10% cho khách hàng mới', CAST(10.00 AS Decimal(5, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(200000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.720' AS DateTime), CAST(N'2026-09-27T13:12:05.720' AS DateTime), 1, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (2, N'SUMMER20', N'Giảm 20% mùa hè – tối đa 100k', CAST(20.00 AS Decimal(5, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(500000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.720' AS DateTime), CAST(N'2026-10-27T13:12:05.720' AS DateTime), 1, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (3, N'FREESHIP5', N'Giảm 5% không giới hạn đơn tối thiểu', CAST(5.00 AS Decimal(5, 2)), NULL, CAST(0.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.720' AS DateTime), CAST(N'2026-11-26T13:12:05.720' AS DateTime), 1, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (4, N'VIP30', N'Giảm 30% dành cho khách VIP – tối đa 200k', CAST(30.00 AS Decimal(5, 2)), CAST(200000.00 AS Decimal(18, 2)), CAST(1000000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.720' AS DateTime), CAST(N'2026-09-12T13:12:05.720' AS DateTime), 1, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (5, N'SALE15', N'Giảm 15% cuối tuần', CAST(15.00 AS Decimal(5, 2)), CAST(75000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(N'2026-08-28T13:12:05.720' AS DateTime), CAST(N'2026-09-04T13:12:05.720' AS DateTime), 1, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (6, N'EXPIRED', N'Voucher đã hết hạn (test)', CAST(10.00 AS Decimal(5, 2)), NULL, CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-06-29T13:12:05.720' AS DateTime), CAST(N'2026-07-29T13:12:05.720' AS DateTime), 0, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (7, N'FREESHIP', N'Giảm 10% để hỗ trợ phí vận chuyển cho đơn từ 300k.', CAST(10.00 AS Decimal(5, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(N'2026-08-29T22:24:00.000' AS DateTime), CAST(N'2026-09-30T22:24:00.000' AS DateTime), 1, NULL, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (8, N'BIGSALE20', N'Giảm siêu khủng 20% cho đơn hàng từ 1 triệu đồng', CAST(20.00 AS Decimal(5, 2)), CAST(250000.00 AS Decimal(18, 2)), CAST(1000000.00 AS Decimal(18, 2)), CAST(N'2026-09-01T22:25:00.000' AS DateTime), CAST(N'2026-09-15T22:25:00.000' AS DateTime), 1, 100, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (9, N'THU2026', N'Đón thu sang giảm ngay 15% không giới hạn mức giảm.', CAST(15.00 AS Decimal(5, 2)), NULL, CAST(500000.00 AS Decimal(18, 2)), CAST(N'2026-08-29T22:26:00.000' AS DateTime), CAST(N'2026-09-01T22:26:00.000' AS DateTime), 1, 200, 0)
INSERT [dbo].[voucher] ([id], [code], [description], [discount_percent], [max_discount], [min_order_value], [start_date], [end_date], [is_active], [usage_limit], [used_count]) VALUES (10, N'QUOCKHANH50', N'Giảm sốc 50% chớp nhoáng mừng lễ Quốc Khánh.', CAST(50.00 AS Decimal(5, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(N'2026-08-29T22:26:00.000' AS DateTime), CAST(N'2026-09-03T22:26:00.000' AS DateTime), 1, 49, 0)
SET IDENTITY_INSERT [dbo].[voucher] OFF
GO
SET IDENTITY_INSERT [dbo].[wishlist] ON 

INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (1, 3, 5, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (2, 3, 10, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (3, 4, 8, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (4, 4, 3, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (5, 5, 10, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (6, 6, 7, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
INSERT [dbo].[wishlist] ([id], [user_id], [product_id], [added_at]) VALUES (7, 7, 9, CAST(N'2026-08-28T13:12:05.813' AS DateTime))
SET IDENTITY_INSERT [dbo].[wishlist] OFF
GO
/****** Object:  Index [IX_app_order_cancelled_by]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE NONCLUSTERED INDEX [IX_app_order_cancelled_by] ON [dbo].[app_order]
(
	[cancelled_by] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_app_order_order_code]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_app_order_order_code] ON [dbo].[app_order]
(
	[order_code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__app_user__AB6E6164FB59BF93]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[app_user] ADD UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__app_user__F3DBC5720560F578]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[app_user] ADD UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_app_user_activation_token]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_app_user_activation_token] ON [dbo].[app_user]
(
	[activation_token] ASC
)
WHERE ([activation_token] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_app_user_phone]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_app_user_phone] ON [dbo].[app_user]
(
	[phone] ASC
)
WHERE ([phone] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__color__72E12F1BF86B4061]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[color] ADD UNIQUE NONCLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__employee__B0AA7345E96DBE8A]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[employee] ADD UNIQUE NONCLUSTERED 
(
	[employee_code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ__employee__B9BE370E4EB5BE89]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[employee] ADD UNIQUE NONCLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__goods_re__357D4CF99EC6503C]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[goods_receipt] ADD UNIQUE NONCLUSTERED 
(
	[code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_order_voucher]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[order_voucher] ADD  CONSTRAINT [UQ_order_voucher] UNIQUE NONCLUSTERED 
(
	[order_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_user_voucher]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[order_voucher] ADD  CONSTRAINT [UQ_user_voucher] UNIQUE NONCLUSTERED 
(
	[customer_id] ASC,
	[voucher_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ__password__B9BE370EF8529E83]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[password_reset_token] ADD UNIQUE NONCLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__password__CA90DA7A8188AAF1]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[password_reset_token] ADD UNIQUE NONCLUSTERED 
(
	[token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IX_product_sale_batch]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE NONCLUSTERED INDEX [IX_product_sale_batch] ON [dbo].[product_sale]
(
	[sale_batch_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IX_product_sale_dates]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE NONCLUSTERED INDEX [IX_product_sale_dates] ON [dbo].[product_sale]
(
	[start_date] ASC,
	[end_date] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IX_product_sale_product]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE NONCLUSTERED INDEX [IX_product_sale_product] ON [dbo].[product_sale]
(
	[product_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__product___DDDF4BE77A619544]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[product_variant] ADD UNIQUE NONCLUSTERED 
(
	[sku] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__role__72E12F1B5FB7F0DC]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[role] ADD UNIQUE NONCLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__size__72E12F1B51BFCD7D]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[size] ADD UNIQUE NONCLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IX_stock_movement_created_at]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE NONCLUSTERED INDEX [IX_stock_movement_created_at] ON [dbo].[stock_movement_log]
(
	[created_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IX_stock_movement_variant]    Script Date: 8/29/2026 10:10:16 AM ******/
CREATE NONCLUSTERED INDEX [IX_stock_movement_variant] ON [dbo].[stock_movement_log]
(
	[variant_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__voucher__357D4CF94A19D7A7]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[voucher] ADD UNIQUE NONCLUSTERED 
(
	[code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_wishlist]    Script Date: 8/29/2026 10:10:16 AM ******/
ALTER TABLE [dbo].[wishlist] ADD  CONSTRAINT [UQ_wishlist] UNIQUE NONCLUSTERED 
(
	[user_id] ASC,
	[product_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[address] ADD  DEFAULT (N'Vietnam') FOR [country]
GO
ALTER TABLE [dbo].[address] ADD  DEFAULT ((0)) FOR [is_default]
GO
ALTER TABLE [dbo].[address] ADD  DEFAULT ((1)) FOR [active]
GO
ALTER TABLE [dbo].[app_order] ADD  DEFAULT (getdate()) FOR [order_date]
GO
ALTER TABLE [dbo].[app_order] ADD  DEFAULT ('PENDING') FOR [status]
GO
ALTER TABLE [dbo].[app_order] ADD  DEFAULT ('COD') FOR [payment_method]
GO
ALTER TABLE [dbo].[app_order] ADD  DEFAULT ((0)) FOR [is_paid]
GO
ALTER TABLE [dbo].[app_order] ADD  DEFAULT ('ONLINE') FOR [order_type]
GO
ALTER TABLE [dbo].[app_user] ADD  DEFAULT ((1)) FOR [enabled]
GO
ALTER TABLE [dbo].[app_user] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[cart] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[collection] ADD  DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[collection] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[employee] ADD  DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[goods_receipt] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[goods_receipt] ADD  DEFAULT ((0)) FOR [total_amount]
GO
ALTER TABLE [dbo].[goods_receipt_item] ADD  DEFAULT ((0)) FOR [unit_cost_price]
GO
ALTER TABLE [dbo].[notification] ADD  DEFAULT ((0)) FOR [is_read]
GO
ALTER TABLE [dbo].[notification] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[order_voucher] ADD  DEFAULT ((0)) FOR [discount_amount]
GO
ALTER TABLE [dbo].[product] ADD  DEFAULT ((0)) FOR [is_deleted]
GO
ALTER TABLE [dbo].[product] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[product] ADD  DEFAULT ((300)) FOR [weight]
GO
ALTER TABLE [dbo].[product_sale] ADD  DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[product_sale] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[product_variant] ADD  DEFAULT ((0)) FOR [stock]
GO
ALTER TABLE [dbo].[product_variant] ADD  DEFAULT ((0)) FOR [cost_price]
GO
ALTER TABLE [dbo].[product_variant] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[sale_batch] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[stock_movement_log] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[variant_image] ADD  DEFAULT ((0)) FOR [sort_order]
GO
ALTER TABLE [dbo].[voucher] ADD  DEFAULT ((1)) FOR [is_active]
GO
ALTER TABLE [dbo].[voucher] ADD  DEFAULT ((0)) FOR [used_count]
GO
ALTER TABLE [dbo].[wishlist] ADD  DEFAULT (getdate()) FOR [added_at]
GO
ALTER TABLE [dbo].[address]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[app_order]  WITH CHECK ADD FOREIGN KEY([billing_address_id])
REFERENCES [dbo].[address] ([id])
GO
ALTER TABLE [dbo].[app_order]  WITH CHECK ADD FOREIGN KEY([cancelled_by])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[app_order]  WITH CHECK ADD FOREIGN KEY([cashier_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[app_order]  WITH CHECK ADD FOREIGN KEY([customer_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[app_order]  WITH CHECK ADD FOREIGN KEY([shipping_address_id])
REFERENCES [dbo].[address] ([id])
GO
ALTER TABLE [dbo].[cart]  WITH CHECK ADD FOREIGN KEY([customer_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[cart_item]  WITH CHECK ADD FOREIGN KEY([cart_id])
REFERENCES [dbo].[cart] ([id])
GO
ALTER TABLE [dbo].[cart_item]  WITH CHECK ADD FOREIGN KEY([variant_id])
REFERENCES [dbo].[product_variant] ([id])
GO
ALTER TABLE [dbo].[employee]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[goods_receipt]  WITH CHECK ADD FOREIGN KEY([created_by])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[goods_receipt_item]  WITH CHECK ADD FOREIGN KEY([goods_receipt_id])
REFERENCES [dbo].[goods_receipt] ([id])
GO
ALTER TABLE [dbo].[goods_receipt_item]  WITH CHECK ADD FOREIGN KEY([variant_id])
REFERENCES [dbo].[product_variant] ([id])
GO
ALTER TABLE [dbo].[notification]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[order_detail]  WITH CHECK ADD FOREIGN KEY([order_id])
REFERENCES [dbo].[app_order] ([id])
GO
ALTER TABLE [dbo].[order_detail]  WITH CHECK ADD FOREIGN KEY([variant_id])
REFERENCES [dbo].[product_variant] ([id])
GO
ALTER TABLE [dbo].[order_voucher]  WITH CHECK ADD FOREIGN KEY([customer_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[order_voucher]  WITH CHECK ADD FOREIGN KEY([order_id])
REFERENCES [dbo].[app_order] ([id])
GO
ALTER TABLE [dbo].[order_voucher]  WITH CHECK ADD FOREIGN KEY([voucher_id])
REFERENCES [dbo].[voucher] ([id])
GO
ALTER TABLE [dbo].[password_reset_token]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[product]  WITH CHECK ADD FOREIGN KEY([category_id])
REFERENCES [dbo].[category] ([id])
GO
ALTER TABLE [dbo].[product_collection]  WITH CHECK ADD FOREIGN KEY([collection_id])
REFERENCES [dbo].[collection] ([id])
GO
ALTER TABLE [dbo].[product_collection]  WITH CHECK ADD FOREIGN KEY([product_id])
REFERENCES [dbo].[product] ([id])
GO
ALTER TABLE [dbo].[product_image]  WITH CHECK ADD FOREIGN KEY([product_id])
REFERENCES [dbo].[product] ([id])
GO
ALTER TABLE [dbo].[product_sale]  WITH CHECK ADD FOREIGN KEY([product_id])
REFERENCES [dbo].[product] ([id])
GO
ALTER TABLE [dbo].[product_sale]  WITH CHECK ADD FOREIGN KEY([sale_batch_id])
REFERENCES [dbo].[sale_batch] ([id])
GO
ALTER TABLE [dbo].[product_variant]  WITH CHECK ADD FOREIGN KEY([color_id])
REFERENCES [dbo].[color] ([id])
GO
ALTER TABLE [dbo].[product_variant]  WITH CHECK ADD FOREIGN KEY([product_id])
REFERENCES [dbo].[product] ([id])
GO
ALTER TABLE [dbo].[product_variant]  WITH CHECK ADD FOREIGN KEY([size_id])
REFERENCES [dbo].[size] ([id])
GO
ALTER TABLE [dbo].[stock_movement_log]  WITH CHECK ADD FOREIGN KEY([created_by])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[stock_movement_log]  WITH CHECK ADD FOREIGN KEY([variant_id])
REFERENCES [dbo].[product_variant] ([id])
GO
ALTER TABLE [dbo].[user_role]  WITH CHECK ADD FOREIGN KEY([role_id])
REFERENCES [dbo].[role] ([id])
GO
ALTER TABLE [dbo].[user_role]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[variant_image]  WITH CHECK ADD FOREIGN KEY([variant_id])
REFERENCES [dbo].[product_variant] ([id])
GO
ALTER TABLE [dbo].[wishlist]  WITH CHECK ADD FOREIGN KEY([product_id])
REFERENCES [dbo].[product] ([id])
GO
ALTER TABLE [dbo].[wishlist]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[app_user] ([id])
GO
ALTER TABLE [dbo].[goods_receipt_item]  WITH CHECK ADD CHECK  (([quantity]>(0)))
GO
ALTER TABLE [dbo].[product_sale]  WITH CHECK ADD CHECK  (([discount_percent]>(0) AND [discount_percent]<=(90)))
GO
ALTER TABLE [dbo].[product_sale]  WITH CHECK ADD  CONSTRAINT [CK_product_sale_dates] CHECK  (([end_date]>[start_date]))
GO
ALTER TABLE [dbo].[product_sale] CHECK CONSTRAINT [CK_product_sale_dates]
GO
ALTER TABLE [dbo].[sale_batch]  WITH CHECK ADD CHECK  (([discount_percent]>(0) AND [discount_percent]<=(90)))
GO
ALTER TABLE [dbo].[sale_batch]  WITH CHECK ADD  CONSTRAINT [CK_sale_batch_dates] CHECK  (([end_date]>[start_date]))
GO
ALTER TABLE [dbo].[sale_batch] CHECK CONSTRAINT [CK_sale_batch_dates]
GO
USE [master]
GO
ALTER DATABASE [ClothingShop] SET  READ_WRITE 
GO
