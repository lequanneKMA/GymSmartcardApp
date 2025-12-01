# 🏋️ GymSmartcardApp - Ứng dụng quản lý thẻ thông minh phòng gym

Ứng dụng desktop quản lý thẻ thành viên gym với smartcard, hỗ trợ thanh toán, gia hạn gói tập, nạp tiền. Sử dụng **Compose Multiplatform**, **JCardSim**, và mã hóa **AES-256-GCM + PBKDF2-HMAC-SHA256**.

---

## 📋 Mục lục

- [🚀 Quick Start](#-quick-start)
- [📁 Cấu trúc Project](#-cấu-trúc-project)
- [🔐 Security Features](#-security-features)
- [✨ Features](#-features)
- [🛠️ Tech Stack](#️-tech-stack)
- [📚 Documentation](#-documentation)

---

## 🚀 Quick Start

### Prerequisites
- **JDK 17** hoặc cao hơn
- **Gradle 8.14** (hoặc dùng wrapper `./gradlew`)
- **IntelliJ IDEA** (recommended)

### Cách chạy

**Option 1: IntelliJ IDEA**
1. Open folder: `File → Open...` → chọn thư mục project
2. Đợi IntelliJ tải Gradle dependencies
3. Set Project SDK: `File → Project Structure → Project → SDK: JDK 17`
4. Run: `Gradle → Tasks → application → run`

**Option 2: Terminal**
```bash
# Windows
.\gradlew run

# Linux/Mac
./gradlew run
```

**Option 3: Build JAR**
```bash
.\gradlew build
java -jar build/libs/GymSmartcardApp.jar
```

---

## 📁 Cấu trúc Project

```
src/main/kotlin/app/
├── 🎨 ui/                      # User Interface (Compose)
│   ├── admin/                  # Admin views (tạo thẻ, unlock)
│   ├── customer/               # Customer views (thanh toán, gia hạn)
│   ├── staff/                  # Staff views (quét thẻ)
│   ├── shared/                 # Shared components
│   └── dialog/                 # Reusable dialogs
│
├── 🏗️ core/                    # Core business logic
│   ├── smartcard/              # Smartcard service (JCardSim + APDU)
│   └── state/                  # AppState management
│
├── 🎯 manager/                 # Business logic managers
│   └── pin/                    # PIN verification manager
│
├── 🔐 security/                # Encryption & hashing
│   ├── AESEncryptionManager
│   └── CardDataEncryptionManager
│
└── 📊 model/                   # Data models
    ├── Member, CartItem, Transaction, Role
```

👉 **Chi tiết:** Xem [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) và [QUICK_REFERENCE.md](QUICK_REFERENCE.md)

---

## 🔐 Security Features

### 🛡️ Multi-layer Security

1. **PBKDF2-HMAC-SHA256 Key Derivation**
   - 10,000 iterations (NIST SP 800-132)
   - Random 16-byte salt per card
   - 256-bit AES key output

2. **AES-256-GCM Encryption**
   - Authenticated encryption (integrity + confidentiality)
   - Random IV per operation
   - Encrypts: fullName, birthDate, CCCD, photo path, balance, dates, package

3. **PIN Security**
   - Stored hashed on card applet
   - Session-based verified PIN (cleared on eject)
   - Auto-lock after 3 failed attempts
   - Admin unlock capability

4. **Data Isolation**
   - Separate admin/customer views
   - Encrypted data on card, plain data in UI only after PIN verification

👉 **Chi tiết:** Xem [ARCHITECTURE.md](ARCHITECTURE.md)

---

## ✨ Features

### 👨‍💼 Admin
- ✅ Tạo thẻ mới cho thành viên (với mã hóa AES)
- ✅ Quét thẻ (bypass PIN requirement)
- ✅ Mở khóa thẻ bị lock
- ✅ Xem dữ liệu đã mã hóa

### 👨‍💻 Staff
- ✅ Quét thẻ (yêu cầu PIN khách hàng)
- ✅ Xem lịch sử giao dịch

### 👤 Customer
- ✅ Xem thông tin cá nhân (sau khi verify PIN)
- ✅ Thanh toán tại cửa hàng (giỏ hàng)
- ✅ Gia hạn gói tập
- ✅ Nạp tiền vào thẻ
- ✅ Đổi mã PIN

### 🔒 Security
- ✅ PBKDF2 key derivation từ PIN
- ✅ AES-256-GCM encryption cho dữ liệu nhạy cảm
- ✅ PIN verification với tracking attempts
- ✅ Auto-lock card sau 3 lần nhập sai
- ✅ Admin unlock với password (ADMIN123)

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **UI Framework** | Compose Multiplatform 1.4.0 |
| **Language** | Kotlin 1.8.20 |
| **Smartcard** | JCardSim 3.0.5 (Java Card 3.0.5 Classic) |
| **Encryption** | AES-256-GCM, PBKDF2-HMAC-SHA256 |
| **Build Tool** | Gradle 8.14 |
| **JDK** | Java 17 |

---

## 📚 Documentation

| File | Description |
|------|-------------|
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | Chi tiết cấu trúc folder, package, files |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | Quick reference - tìm file nhanh, import paths |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture diagram, dependency flow, security flow |

---

## 🎯 Common Tasks

### Sửa UI Admin
```kotlin
// File: ui/admin/AdminView.kt
```

### Thêm field vào Member
```kotlin
// 1. Update model
// File: model/Member.kt
data class Member(..., val newField: String)

// 2. Update encryption
// File: security/CardDataEncryptionManager.kt
// Thêm field vào encryptMemberData() và decryptMemberData()

// 3. Update UI
// File: ui/admin/AdminView.kt
// Thêm input field cho newField
```

### Thay đổi logic verify PIN
```kotlin
// File: manager/pin/PinVerificationManager.kt
```

---

## 🧪 Testing

```bash
# Build project
.\gradlew build

# Run tests (nếu có)
.\gradlew test

# Clean build
.\gradlew clean build
```

---

## 🐛 Known Issues

- [ ] Card dropdown empty after creation → **FIXED** (dùng `memberInfoRegistry`)
- [ ] PIN blocked on applet after 3 failures → Requires card eject/re-insert
- [ ] Photo upload chưa implement (pending)

---

## 📝 TODOs

- [ ] Thêm `ui/admin/CreateCardDialog.kt` (tách logic tạo thẻ)
- [ ] Thêm `manager/transaction/TransactionManager.kt` (centralize transaction logic)
- [ ] Thêm `core/storage/` package (persistence layer)
- [ ] Thêm photo capture/upload feature
- [ ] Thêm unit tests

---

## 📄 License

Educational project - Đồ án môn học Smartcard

---

## 👥 Contributors

- Developer: [Your Name]
- Instructor: [Professor Name]

---

## 🙏 Acknowledgments

- **JCardSim**: Java Card simulator library
- **Compose Multiplatform**: Modern UI framework
- **NIST**: PBKDF2 standard guidelines (SP 800-132)


