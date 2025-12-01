# 📁 Project Structure - GymSmartcardApp

Cấu trúc project được tổ chức theo kiến trúc **Clean Architecture** với phân chia rõ ràng giữa các tầng.

## 🏗️ Folder Structure

```
src/main/kotlin/app/
├── 📦 core/                    # Core Business Logic & Infrastructure
│   ├── smartcard/              # Smartcard Service Layer
│   │   ├── SmartcardService.kt         # Interface định nghĩa các operation
│   │   └── JCardSimService.kt          # Implementation với JCardSim & AES encryption
│   └── state/                  # Application State Management
│       └── AppState.kt                 # Central state với business logic
│
├── 🎯 manager/                 # Business Logic Managers
│   └── pin/                    # PIN Management
│       └── PinVerificationManager.kt   # Quản lý xác thực PIN, tracking attempts
│
├── 📊 model/                   # Data Models
│   ├── Member.kt               # Thành viên gym
│   ├── CartItem.kt             # Item trong giỏ hàng
│   ├── Transaction.kt          # Giao dịch
│   └── Role.kt                 # Phân quyền (Admin/Staff/Customer)
│
├── 🔐 security/                # Security & Encryption
│   ├── AESEncryptionManager.kt         # AES-256-GCM encryption utilities
│   └── CardDataEncryptionManager.kt    # High-level API để encrypt/decrypt Member data
│
└── 🎨 ui/                      # User Interface Components
    ├── admin/                  # Admin-specific UI
    │   ├── AdminView.kt                # Admin dashboard (tạo thẻ, unlock, xem dữ liệu)
    │   └── AdminPasswordDialog.kt      # Dialog xác thực mật khẩu admin
    │
    ├── customer/               # Customer-specific UI
    │   └── CustomerView.kt             # Customer view (thanh toán, gia hạn gói tập)
    │
    ├── staff/                  # Staff-specific UI
    │   └── StaffView.kt                # Staff view (quét thẻ, giao dịch)
    │
    ├── shared/                 # Shared UI Components
    │   ├── MemberInfoCard.kt           # Component hiển thị thông tin thành viên
    │   └── RoleSwitcher.kt             # Component chuyển đổi role
    │
    └── dialog/                 # Reusable Dialogs
        └── PinVerificationDialog.kt    # Dialog xác thực PIN (dùng chung cho nhiều màn)
```

---

## 📋 Chi tiết từng package

### 🔷 `core/` - Core Business Logic

**Mục đích:** Chứa các service cốt lõi và state management

#### `core/smartcard/`
- **SmartcardService.kt**: Interface định nghĩa contract cho smartcard operations
  - `createCard()`, `insertCard()`, `ejectCard()`
  - `verifyPin()`, `changePin()`, `unlockCard()`
  - `readCardData()`, `updateBalance()`
  
- **JCardSimService.kt**: Implementation với JCardSim simulator
  - APDU commands (INS_VERIFY_PIN, INS_READ_DATA, INS_UPDATE_BALANCE...)
  - PBKDF2-HMAC-SHA256 key derivation (10,000 iterations)
  - AES-256-GCM encryption cho sensitive data
  - Card/Salt/EncryptedData/VerifiedPIN registries

#### `core/state/`
- **AppState.kt**: Central application state
  - Current role, scanned members (admin vs customer)
  - Cart, transactions, locked cards
  - PIN verification integration
  - Business logic methods: `scan()`, `adminScan()`, `verifyCardPin()`

---

### 🔷 `manager/` - Business Logic Managers

**Mục đích:** Các manager xử lý business logic phức tạp

#### `manager/pin/`
- **PinVerificationManager.kt**: Centralized PIN verification
  - `startVerification()`: Bắt đầu flow xác thực
  - `verifyPin()`: Kiểm tra PIN + tracking attempts
  - `resetAttempts()`: Admin unlock card
  - Per-card attempt tracking với `attemptsMap`
  - Auto-lock card sau 3 lần sai

---

### 🔷 `model/` - Data Models

**Mục đích:** Pure data classes, không chứa business logic

- **Member.kt**: `memberId`, `fullName`, `birthDate`, `cccdNumber`, `photoPath`, `balance`, `packageType`...
- **CartItem.kt**: `name`, `price`, `quantity`
- **Transaction.kt**: `type`, `amount`, `oldBalance`, `newBalance`, `timestamp`
- **Role.kt**: `ADMIN`, `STAFF`, `CUSTOMER`

---

### 🔷 `security/` - Security Layer

**Mục đích:** Encryption/Hashing utilities

- **AESEncryptionManager.kt**: Low-level AES operations
  - `generateKeyFromPIN()`: PBKDF2 key derivation từ PIN
  - `generateSalt()`: Random 16-byte salt
  - `encrypt()/decrypt()`: AES-256-GCM với random IV
  - Helper methods: `encryptString()`, `encryptLong()`...

- **CardDataEncryptionManager.kt**: High-level Member encryption
  - `encryptMemberData()`: Member → EncryptedCardData
  - `decryptMemberData()`: EncryptedCardData → Member
  - Tự động handle tất cả fields (fullName, birthDate, balance...)

---

### 🔷 `ui/` - User Interface

**Mục đích:** Compose UI components phân chia theo role và responsibility

#### `ui/admin/` - Admin UI
- **AdminView.kt**: Tạo thẻ, unlock thẻ, quét thẻ (bypass PIN)
- **AdminPasswordDialog.kt**: Dialog nhập mật khẩu admin (ADMIN123)

#### `ui/customer/` - Customer UI
- **CustomerView.kt**: 
  - Hiển thị thông tin thành viên sau khi verify PIN
  - Thanh toán tại cửa hàng (cart)
  - Gia hạn gói tập
  - Nạp tiền

#### `ui/staff/` - Staff UI
- **StaffView.kt**: Quét thẻ (cần PIN), xem lịch sử giao dịch

#### `ui/shared/` - Shared Components
- **MemberInfoCard.kt**: Card component hiển thị thông tin Member (reusable)
- **RoleSwitcher.kt**: Button group để switch giữa Admin/Staff/Customer

#### `ui/dialog/` - Reusable Dialogs
- **PinVerificationDialog.kt**: 
  - Generic dialog xác thực PIN
  - Dùng cho: authentication, payment confirmation, package renewal
  - Tích hợp `PinVerificationManager`

---

## 🔄 Data Flow Examples

### 📌 Tạo thẻ mới (Admin)
```
AdminView 
  → AppState.cardService.createCard(member, pin)
    → JCardSimService
      → Generate Salt
      → PBKDF2: PIN → AES Key
      → CardDataEncryptionManager.encryptMemberData()
      → Store: encryptedDataRegistry + memberInfoRegistry
      → Write to card applet
```

### 📌 Xác thực PIN & thanh toán (Customer)
```
CustomerView (nhấn "Thanh Toán")
  → AppState.pinVerificationManager.startVerification()
    → PinVerificationDialog hiển thị
    → User nhập PIN
    → PinVerificationManager.verifyPin()
      → JCardSimService.verifyPin()
        → APDU VERIFY_PIN
        → Success: Store verified PIN
        → Failure: Decrement attempts → Lock nếu = 0
    → onSuccess: Process payment
      → AppState.processPayment()
        → JCardSimService.updateBalance()
          → Update memberInfoRegistry + encryptedDataRegistry
```

### 📌 Đọc thẻ đã mã hóa
```
CustomerView.scan()
  → AppState.scan()
    → PinVerificationManager.startVerification()
    → User verify PIN
    → JCardSimService.readCardData()
      → Get verified PIN from verifiedPINRegistry
      → Get salt from saltRegistry
      → PBKDF2: PIN + Salt → AES Key
      → Get encrypted data from encryptedDataRegistry
      → CardDataEncryptionManager.decryptMemberData()
      → Return Member object
```

---

## 🎯 Design Principles Applied

1. **Separation of Concerns**: Mỗi package có responsibility rõ ràng
2. **Single Responsibility**: Mỗi class chỉ làm 1 việc
3. **Dependency Inversion**: UI depends on interfaces (SmartcardService), không phụ thuộc implementation
4. **Reusability**: Shared components (MemberInfoCard, PinVerificationDialog) dùng chung
5. **Encapsulation**: Security logic tách biệt trong `security/`, business logic trong `manager/`

---

## 📝 Notes

- **Không có circular dependencies**: UI → Manager → Core → Model
- **Easy to test**: Mỗi layer có thể test độc lập
- **Easy to extend**: Thêm feature mới chỉ cần tạo package/class mới trong folder tương ứng
- **Team-friendly**: Mỗi developer có thể làm việc trên 1 package riêng mà không conflict

---

## 🚀 Next Steps (Pending Features)

- [ ] Thêm `ui/admin/CreateCardDialog.kt` (tách logic tạo thẻ ra khỏi AdminView)
- [ ] Thêm `manager/transaction/TransactionManager.kt` (centralize transaction logic)
- [ ] Thêm `core/storage/` package (persistence layer cho save/load data)
- [ ] Thêm `ui/customer/PhotoCaptureDialog.kt` (chụp ảnh thành viên)

