# Gym Smartcard App - Hướng dẫn sử dụng Thẻ ảo

## Tổng quan

Ứng dụng quản lý thành viên phòng gym với 3 vai trò:
- **Admin**: Quản lý toàn bộ hệ thống, tạo/sửa/xóa thành viên, đặt mã PIN
- **Nhân viên (Staff)**: Quét thẻ, checkin/checkout, gia hạn gói, xem lịch sử
- **Khách hàng (Customer)**: Nạp tiền, mua đồ, xem thông tin cá nhân

## Hệ thống Thẻ ảo

### Cách hoạt động

Thẻ ảo được lưu trong thư mục `virtual_cards/` dưới dạng file JSON:

```
virtual_cards/
├── ID12345.json  (Thẻ demo)
├── ID67890.json
└── ...
```

Mỗi file thẻ chứa:
- Thông tin thành viên (ID, họ tên, gói tập, số dư, ngày hết hạn)
- Mã PIN (đã mã hóa)

### Cấu trúc file thẻ (JSON)

```json
{
  "memberId": "ID12345",
  "fullName": "Nguyễn Văn A",
  "startDate": "01/01/2024",
  "expireDate": "05/12/2025",
  "packageType": "1 Tháng",
  "balance": 250000.0,
  "pin": "4321X"
}
```

## Phân quyền

### Admin
**Quyền hạn:**
- ✅ Tạo thành viên mới
- ✅ Sửa thông tin thành viên (tên, gói tập, số dư)
- ✅ Đặt/Thay đổi mã PIN cho bất kỳ thẻ nào
- ✅ Xóa thành viên
- ✅ Xem báo cáo thống kê
- ✅ Quét thẻ

**Cách sử dụng:**
1. Chọn vai trò "Admin" ở sidebar
2. Click "Quét thẻ" để load thẻ
3. Chỉnh sửa thông tin trực tiếp trên form
4. Click "Lưu thay đổi"

**Đặt mã PIN mới:**
1. Bật switch "Quản lý mã PIN"
2. Nhập 4 số mới
3. Click "Đặt mã PIN mới"

### Nhân viên (Staff)
**Quyền hạn:**
- ✅ Quét thẻ thành viên
- ✅ Check-in / Check-out
- ✅ Xác nhận giao dịch từ khách hàng (nạp tiền, mua đồ, gia hạn gói)
- ✅ Xem lịch sử giao dịch
- ❌ KHÔNG được tạo/sửa/xóa thành viên
- ❌ KHÔNG được thay đổi mã PIN
- ❌ KHÔNG được truy cập các chức năng khác

**Cách sử dụng:**
1. Click "Quét thẻ" để đọc thông tin thành viên
2. Khi khách hàng yêu cầu giao dịch, màn hình sẽ hiện thông báo
3. Kiểm tra thông tin và click "Xác nhận" hoặc "Từ chối"

### Khách hàng (Customer)
**Quyền hạn:**
- ✅ Xem thông tin cá nhân (số dư, gói tập, ngày hết hạn)
- ✅ Nạp tiền (QR Code hoặc Tiền mặt) - cần nhân viên xác nhận
- ✅ Mua đồ tại cửa hàng - cần nhân viên xác nhận
- ✅ Gia hạn gói tập - cần nhân viên xác nhận
- ✅ Đổi mã PIN (cần biết mã PIN cũ)

**Cách sử dụng:**

**Nạp tiền:**
1. Click nút "💰 Nạp"
2. Chọn số tiền và phương thức (QR Code/Tiền mặt)
3. Nếu chọn QR: Quét mã và click "Đã thanh toán"
4. Đợi nhân viên xác nhận

**Mua đồ:**
1. Click nút "🛒 Mua đồ"
2. Chọn sản phẩm từ danh sách
3. Xem giỏ hàng bên phải
4. Click "Thanh toán"
5. Đợi nhân viên xác nhận

**Gia hạn gói:**
1. Click nút "📅 Gói tập"
2. Chọn gói muốn gia hạn
3. Đợi nhân viên xác nhận

**Đổi mã PIN:**
1. Click nút "🔐 Đổi mã PIN"
2. Nhập mã PIN hiện tại (mặc định: 1234)
3. Nhập mã PIN mới (4 số)
4. Xác nhận mã PIN mới

## Tích hợp với Thẻ thật

Hiện tại app dùng **Virtual Smartcard** (thẻ ảo) để phát triển và test.

### Để tích hợp với thẻ RFID/NFC thật:

1. **Thêm thư viện đọc thẻ:**
   ```kotlin
   // build.gradle.kts
   dependencies {
       implementation("javax.smartcardio:...")
       // hoặc thư viện RFID/NFC khác
   }
   ```

2. **Tạo RealSmartcardService:**
   ```kotlin
   class RealSmartcardService : SmartcardService {
       override fun scanCard(): Member? {
           // Thay thế file I/O bằng API đọc thẻ
           // val cardReader = CardTerminal.waitForCard()
           // return readMemberFromCard(cardReader)
       }

       override fun topUp(memberId: String, amount: Double): Boolean {
           // Ghi dữ liệu lên thẻ thật
           // return writeToCard(memberId, newBalance)
       }

       // ... các method khác
   }
   ```

3. **Thay đổi trong AppState.kt:**
   ```kotlin
   // Thay VirtualSmartcardService bằng RealSmartcardService
   class AppState(
       private val cardService: SmartcardService = RealSmartcardService()
   )
   ```

**KHÔNG cần thay đổi gì khác!** Interface `SmartcardService` được thiết kế để dễ dàng chuyển đổi giữa thẻ ảo và thẻ thật.

## Database đồng bộ

### Tại sao cần Database?

Thẻ thông minh chỉ lưu:
- Thông tin cơ bản (ID, tên, số dư, gói tập)
- Mã PIN

**Database cần lưu:**
- ✅ Lịch sử giao dịch chi tiết
- ✅ Log check-in/check-out
- ✅ Thông tin thanh toán
- ✅ Backup dữ liệu khi thẻ bị mất
- ✅ Báo cáo thống kê

### Luồng đồng bộ đề xuất:

```
┌─────────┐      ┌──────────┐      ┌──────────┐
│  Thẻ    │ ←───→│   App    │ ←───→│ Database │
│(Source  │      │ (Bridge) │      │ (Backup) │
│of Truth)│      │          │      │          │
└─────────┘      └──────────┘      └──────────┘
```

**Khi quét thẻ:**
1. Đọc dữ liệu từ thẻ (source of truth)
2. Sync với database nếu có thay đổi
3. Hiển thị lên app

**Khi có giao dịch:**
1. Ghi vào thẻ (cập nhật số dư)
2. Lưu log giao dịch vào database
3. Update UI

**Khi thẻ bị mất:**
- Dùng dữ liệu từ database để khôi phục lên thẻ mới

### Code mẫu đồng bộ:

```kotlin
class SyncService(
    private val cardService: SmartcardService,
    private val database: Database
) {
    suspend fun syncCardWithDatabase(memberId: String) {
        val cardData = cardService.scanCard()
        val dbData = database.getMember(memberId)

        // Thẻ là source of truth cho số dư
        if (cardData != null && dbData != null) {
            if (cardData.balance != dbData.balance) {
                database.updateBalance(memberId, cardData.balance)
            }
        }
    }

    suspend fun recordTransaction(transaction: Transaction) {
        // 1. Ghi vào thẻ
        cardService.topUp(transaction.memberId, transaction.amount)

        // 2. Lưu log vào database
        database.insertTransaction(transaction)
    }
}
```

## Chạy ứng dụng

```bash
# Clone repository
git clone <repo-url>

# Chạy ứng dụng
./gradlew run

# Hoặc trong IntelliJ
# Click vào Main.kt -> Run
```

Thư mục `virtual_cards/` sẽ tự động được tạo khi chạy lần đầu với 1 thẻ demo.

## Troubleshooting

**Q: Không quét được thẻ?**
- Kiểm tra thư mục `virtual_cards/` có file `.json` không
- Thử xóa folder `virtual_cards/` và chạy lại (sẽ tạo thẻ demo mới)

**Q: Muốn tạo thêm thẻ mới?**
- Chuyển sang role "Admin"
- Tạo file mới trong `virtual_cards/` theo format mẫu
- Hoặc dùng chức năng "Tạo thành viên mới" (đang phát triển)

**Q: Quên mã PIN?**
- Chuyển sang role "Admin"
- Quét thẻ cần reset
- Bật "Quản lý mã PIN" và đặt mã mới

## Tính năng sắp có

- [ ] Tạo thành viên mới từ UI
- [ ] Tích hợp Database (PostgreSQL/SQLite)
- [ ] Báo cáo thống kê chi tiết
- [ ] Export dữ liệu Excel
- [ ] Backup/Restore thẻ
- [ ] Tích hợp thẻ RFID/NFC thật

## Liên hệ

Nếu cần hỗ trợ, vui lòng tạo issue trên GitHub hoặc liên hệ qua email.
