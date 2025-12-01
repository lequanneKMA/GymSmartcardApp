# 🗂️ Quick Reference - File Locations

## 📍 Bạn đang tìm file gì?

### 🔴 **ADMIN** - Quản lý admin
```
ui/admin/
├── AdminView.kt              → Giao diện admin (tạo thẻ, unlock)
└── AdminPasswordDialog.kt    → Dialog nhập password admin
```

### 🟢 **CUSTOMER** - Màn hình khách hàng
```
ui/customer/
└── CustomerView.kt           → Thanh toán, gia hạn, nạp tiền
```

### 🔵 **STAFF** - Màn hình nhân viên
```
ui/staff/
└── StaffView.kt              → Quét thẻ, xem giao dịch
```

### 🟡 **SMARTCARD** - Logic thẻ thông minh
```
core/smartcard/
├── SmartcardService.kt       → Interface (contract)
└── JCardSimService.kt        → Implementation với mã hóa AES
```

### 🟣 **PIN** - Xác thực PIN
```
manager/pin/
└── PinVerificationManager.kt → Logic verify PIN, đếm attempts

ui/dialog/
└── PinVerificationDialog.kt  → UI dialog nhập PIN
```

### 🟠 **ENCRYPTION** - Mã hóa
```
security/
├── AESEncryptionManager.kt           → AES-256-GCM operations
└── CardDataEncryptionManager.kt      → Encrypt/decrypt Member data
```

### ⚫ **STATE** - Quản lý state
```
core/state/
└── AppState.kt               → Central state, business logic
```

### ⚪ **SHARED UI** - Components dùng chung
```
ui/shared/
├── MemberInfoCard.kt         → Card hiển thị thông tin member
└── RoleSwitcher.kt           → Button switch role

ui/dialog/
└── PinVerificationDialog.kt  → Dialog xác thực PIN (reusable)
```

### 🔘 **MODELS** - Data models
```
model/
├── Member.kt                 → Thông tin thành viên
├── CartItem.kt               → Item trong giỏ hàng
├── Transaction.kt            → Giao dịch
└── Role.kt                   → Enum phân quyền
```

---

## 🎯 Common Tasks

### ✏️ Sửa giao diện Admin
👉 `ui/admin/AdminView.kt`

### ✏️ Sửa giao diện thanh toán Customer
👉 `ui/customer/CustomerView.kt`

### 🔧 Thay đổi cách mã hóa
👉 `security/AESEncryptionManager.kt`
👉 `security/CardDataEncryptionManager.kt`

### 🔧 Thay đổi logic verify PIN
👉 `manager/pin/PinVerificationManager.kt`

### 🔧 Thêm/sửa APDU commands
👉 `core/smartcard/JCardSimService.kt`

### 🔧 Thêm field vào Member
👉 `model/Member.kt`
👉 `security/CardDataEncryptionManager.kt` (update encrypt/decrypt)
👉 `ui/admin/AdminView.kt` (add input field)

### 🔧 Thay đổi business logic (scan, payment, unlock...)
👉 `core/state/AppState.kt`

---

## 📦 Import Paths Reference

```kotlin
// Models
import app.model.Member
import app.model.CartItem
import app.model.Transaction
import app.model.Role

// Core Services
import app.core.smartcard.SmartcardService
import app.core.smartcard.JCardSimService
import app.core.state.AppState

// Managers
import app.manager.pin.PinVerificationManager

// Security
import app.security.AESEncryptionManager
import app.security.CardDataEncryptionManager
import app.security.EncryptedCardData

// UI - Admin
import app.ui.admin.AdminView
import app.ui.admin.AdminPasswordDialog

// UI - Customer
import app.ui.customer.CustomerView

// UI - Staff
import app.ui.staff.StaffView

// UI - Shared
import app.ui.shared.MemberInfoCard
import app.ui.shared.RoleSwitcher

// UI - Dialogs
import app.ui.dialog.PinVerificationDialog
```

---

## 🔍 Dependency Graph

```
┌─────────────────────────────────────────────┐
│                   Main.kt                   │ ← Entry point
└────────────────┬────────────────────────────┘
                 │
         ┌───────▼────────┐
         │  AppState      │ ← Central state
         │  (core/state)  │
         └───┬────────┬───┘
             │        │
    ┌────────▼──┐  ┌─▼──────────────────┐
    │ UI Layer  │  │ PinVerification    │
    │ (ui/*)    │  │ Manager            │
    └───────────┘  └─┬──────────────────┘
                     │
              ┌──────▼────────────┐
              │ SmartcardService  │ ← Interface
              │ (core/smartcard)  │
              └──────┬────────────┘
                     │
         ┌───────────▼──────────────┐
         │   JCardSimService        │ ← Implementation
         │   + AES Encryption       │
         └───────────┬──────────────┘
                     │
         ┌───────────▼──────────────┐
         │  Security Layer          │
         │  (AES/CardData Managers) │
         └───────────┬──────────────┘
                     │
              ┌──────▼─────┐
              │   Models   │ ← Pure data
              └────────────┘
```

**Flow hướng xuống (dependency inversion):**
- UI chỉ biết `SmartcardService` interface, không biết `JCardSimService`
- Manager gọi service qua interface
- Security layer độc lập, có thể swap implementation

