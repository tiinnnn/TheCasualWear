# 🔐 HƯỚNG DẪN: Setup Credentials cho Team

## 📋 Tóm Tắt

Dự án sử dụng `.env.local` file để lưu credentials locally:
- ✅ **File `.env.local`** được gitignored - chứa actual credentials (KHÔNG commit)
- ✅ **File `.env.local.example`** commit được - template cho team
- ✅ Ứng dụng tự động load từ `.env.local` khi startup

---

## 🚀 Cách Setup Lần Đầu (New Developer)

### Bước 1: Clone project
```bash
git clone <repo>
cd TheCasualWear
```

### Bước 2: Tạo `.env.local` file từ template
```bash
# Copy template
cp .env.local.example .env.local
```

### Bước 3: Điền credentials vào `.env.local`
```bash
# Mở file .env.local và update values
nano .env.local  (hoặc dùng editor)

# Các credentials cần:
DB_PASSWORD=123
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
MAIL_PASSWORD=...
VNPAY_HASH_SECRET=...
```

### Bước 4: Verify gitignore
```bash
# Confirm .env.local is ignored
git check-ignore .env.local
# Output: .env.local (nếu ignored)
```

### Bước 5: Chạy ứng dụng
```bash
mvn spring-boot:run
```

✅ Ứng dụng sẽ tự động load từ `.env.local`

---

## 📁 File Structure

```
TheCasualWear/
├── .env.local                  ❌ KHÔNG COMMIT (gitignored)
│   └── Chứa: actual credentials
│
├── .env.local.example          ✅ COMMIT (template)
│   └── Chứa: placeholder values (non-sensitive)
│
├── src/main/resources/
│   └── application.properties  ✅ COMMIT (uses env vars)
│       └── Format: ${VAR_NAME:default_value}
│
└── .gitignore                  ✅ COMMIT
    └── Có: .env.local, .env.*.local
```

---

## 🛡️ Bảo Mật

### ✅ DO:
- ✓ Giữ `.env.local` local, không share
- ✓ Add `.env.local` vào `.gitignore`
- ✓ Commit `.env.local.example` (template)
- ✓ Use environment variables trong config

### ❌ DON'T:
- ✗ Commit `.env.local` với actual credentials
- ✗ Share `.env.local` file trên chat/email
- ✗ Hardcode secrets trong code
- ✗ Push `.env.local` vào Git

---

## 📝 Các Credentials Cần

```env
# Database (local)
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=ClothingShop;...
DB_USERNAME=sa
DB_PASSWORD=123

# Cloudinary (image upload)
CLOUDINARY_CLOUD_NAME=dozzwbiww
CLOUDINARY_API_KEY=355158441565431
CLOUDINARY_API_SECRET=QvY_X8aX8KPRpN85KluizdfTK88

# Gmail (password reset)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=anhnhgth06559@gmail.com
MAIL_PASSWORD=uzwl ekjz lwbu engy

# VNPay (payment gateway)
VNPAY_TMN_CODE=SUBIH7ZN
VNPAY_HASH_SECRET=LKP2ZVFBAW4L7HFM1H3NALYNWQZ8QBVJ
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/order/vnpay-return
```

---

## 🔍 Verify Setup Works

### Test 1: Check credentials loaded
```bash
# Khi startup, sẽ thấy logs:
# "INFO ... Inferring JDBC URL is enabled."
# "INFO ... Connected to database successfully"
```

### Test 2: Test database connection
```bash
# Trong ứng dụng, gọi endpoint bất kỳ sẽ connect DB
curl http://localhost:8080/api/products
# Response: 200 OK (nếu DB connection thành công)
```

### Test 3: Check env vars
```bash
# Xem biến được load
mvn spring-boot:run -Ddebug 2>&1 | grep "DB_URL\|DB_PASSWORD"
```

---

## 🆘 Troubleshooting

### Lỗi: "Could not resolve placeholder"
```
Error: Could not resolve placeholder 'DB_PASSWORD'
```

**Fix:**
- ✓ Verify `.env.local` file tồn tại
- ✓ Verify biến được set trong `.env.local`
- ✓ Verify tên biến đúng (case-sensitive)
- ✓ Restart IDE/terminal

### Lỗi: "Connection refused"
```
Error: java.sql.SQLException: The TCP/IP connection to the host has failed
```

**Fix:**
- ✓ Verify SQL Server running: `sqlcmd -S localhost -U sa -P "password"`
- ✓ Verify DB_URL đúng
- ✓ Verify DB_USERNAME và DB_PASSWORD đúng

### Lỗi: "File not found"
```
Error: .env.local file not found
```

**Fix:**
- ✓ Copy từ template: `cp .env.local.example .env.local`
- ✓ Verify file tạo trong đúng folder: `TheCasualWear/` root

---

## 📋 Checklist cho New Developer

- [ ] Clone project
- [ ] Copy `.env.local.example` → `.env.local`
- [ ] Điền credentials vào `.env.local`
- [ ] Verify `.env.local` không tracked: `git status`
- [ ] Run: `mvn spring-boot:run`
- [ ] Test: `curl http://localhost:8080/api/products`
- [ ] ✅ Ready to code!

---

## 📞 Contact

Nếu gặp vấn đề:
1. Check `.env.local` file tồn tại
2. Check biến được set đúng
3. Xem troubleshooting section trên
4. Contact team lead nếu vẫn không được

**QUAN TRỌNG:** Không share `.env.local` file! Chỉ share template `.env.local.example`.
