# 🔍 Debug Guide - Gym Smartcard App

## 📋 Debug Commands

### 1. Log Chi Tiết Thông Tin Thẻ

Để xem toàn bộ thông tin thẻ đang lưu, thêm vào code:

```kotlin
// Trong AdminView hoặc CustomerView
val cardService = JCardSimService() // hoặc từ AppState

// Log toàn bộ thông tin thẻ
cardService.logCardInfo("ID12345") // Thay ID12345 bằng member ID
```

**Output:**
```
=== 🔍 CARD INFO DEBUG ===
Member ID: ID12345
Card exists in registry: true
Salt: e896dd4c1675ad129bf4382350db171d
Encrypted data exists: true
Verified PIN exists: true

📋 Member Info (unencrypted registry):
  Full Name: Nguyễn Văn A
  Balance: 50000 đ
  Birth Date: 1990-01-01
  CCCD: 001234567890
  Photo Path: photos/ID12345_1234567890.png
  Photo Data: 12345 bytes
  Package: 1 Tháng
  Start: 2024-12-02
  Expire: 2025-01-02

🔓 Decrypted Data (from card):
  Full Name: Nguyễn Văn A
  Balance: 50000 đ
  Birth Date: 1990-01-01
  CCCD: 001234567890
  Photo Path: photos/ID12345_1234567890.png
  Photo Data: 12345 bytes
  Package: 1 Tháng
  Start: 2024-12-02
  Expire: 2025-01-02

🔐 RSA Identity:
  Keypair exists: true
  Public Key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4CYa...

💳 Inserted Card:
  Inserted: true
  Inserted Member ID: ID12345
=== 🔍 END DEBUG ===
```

---

## 🐛 Common Issues & Solutions

### Issue 1: Giao Diện Không Cập Nhật Số Tiền

**Triệu chứng:**
- Update balance thành công
- Database đã cập nhật
- Giao diện khách hàng vẫn hiện số tiền cũ

**Nguyên nhân:**
- UI không reactive sau khi updateBalance()

**Solution:**
AppState.kt đã có code đọc lại data:
```kotlin
val ok = cardService.updateBalance(member.memberId, newBalance, pin)
if (ok) {
    scannedMember = cardService.readCardData() // ✅ Đã có
    toast = "Nạp tiền thành công"
}
```

**Debug:**
1. Check console logs:
   ```
   ✅ Balance updated: 50000 đ
     ✓ memberInfoRegistry updated
     ✓ encryptedDataRegistry updated
   ```

2. Nếu không thấy log → updateBalance failed
3. Nếu thấy log nhưng UI không update → readCardData() failed

**Force Refresh:**
```kotlin
// Sau khi update balance
scannedMember = null
scannedMember = cardService.readCardData()
```

---

### Issue 2: Firebase Timeout

**Triệu chứng:**
```
WARNING: Failed to resolve host firestore.googleapis.com
❌ [Firebase] Failed to store public key: Waited 10 seconds
```

**Nguyên nhân:**
- Không có kết nối internet
- Firewall block firestore.googleapis.com
- Firebase credentials không đúng

**Solution:**
App vẫn chạy bình thường ở **local mode** (không cần Firebase)

**Fix Firebase connection:**
1. Check internet connection
2. Check firewall settings
3. Verify `firebase-credentials.json` is valid
4. Try restart app

---

### Issue 3: Rút Thẻ Rồi Cắm Lại Không Đọc Được

**Triệu chứng:**
- Lần 1: Cắm thẻ → Verify PIN → Đọc được data ✅
- Rút thẻ → Cắm lại → Quét thẻ → "Không có thẻ nào được cắm" ❌

**Nguyên nhân:**
- ~~Cũ: ejectCard() xóa verified PIN~~ ✅ FIXED
- Mới: PIN được giữ trong session, cắm lại không cần verify lại

**Solution (Đã Fix):**
PIN giờ **không bị xóa** khi eject card → Cắm lại vẫn dùng được!

**Console Logs:**
```
✓ Card inserted: ID12345 (PIN verified: true)  ← Cắm lại, PIN còn
✓ PIN verified in session                      ← Đọc được ngay
✓ AES key derived from verified PIN
✓ Member data decrypted successfully
```

**Nếu vẫn lỗi:**
```
✓ Card inserted: ID12345 (PIN verified: false) ← PIN đã bị clear
❌ No verified PIN for this session
💡 Hint: You need to verify PIN first
```
→ Cần verify PIN lại

**Clear PIN Manual (Security):**
```kotlin
// Clear PIN for one card
cardService.clearVerifiedPin(memberId)

// Clear all PINs (logout)
cardService.clearAllVerifiedPins()
```

---

### Issue 4: Decrypt Failed

**Triệu chứng:**
```
❌ Failed to decrypt: AEADBadTagException
```

**Nguyên nhân:**
- PIN sai
- Salt không đúng
- Data bị corrupt

**Solution:**
```kotlin
// 1. Verify PIN trước
val verified = cardService.verifyPin(memberId, pin)
if (!verified) {
    println("❌ PIN incorrect")
    return
}

// 2. Log card info
cardService.logCardInfo(memberId)

// 3. Check salt exists
val salt = saltRegistry[memberId]
if (salt == null) {
    println("❌ Salt not found - card may be corrupted")
}
```

---

## 📊 Console Logs Explained

### Card Creation
```
=== Creating Card with AES-256-GCM + RSA-2048 Signature ===
Member ID: ID12345
Generated Salt: e896dd4c1675ad129bf4382350db171d  ← Unique per card
AES-256 key derived from PIN                      ← From PBKDF2
Member data encrypted with AES-256-GCM            ← All data encrypted
RSA-2048 keypair generated (anti-cloning)         ← For verification
  Public Key: MIIBIjANBgkqhkiG9w0BAQEF...        ← Stored in Firebase
✅ [Firebase] Public key stored for ID12345       ← Cloud backup
PIN changed successfully                          ← Applet PIN set
Encrypted data written to card                    ← Data on card
Card created successfully                         ← Done!
```

### Balance Update
```
✅ Balance updated: 50000 đ                       ← New balance
  ✓ memberInfoRegistry updated                    ← Local cache updated
  ✓ encryptedDataRegistry updated                 ← Encrypted data updated
✅ [Firebase] Transaction logged: TXN_xxx         ← Cloud log
```

### Card Read
```
=== Reading Card Data with AES Decryption ===
AES key derived from verified PIN                 ← Using verified PIN
Member data decrypted successfully                ← Decrypt OK
Member: Nguyễn Văn A                             ← Data retrieved
Balance: 50000 đ                                  ← Current balance
=== Read Complete ===
```

---

## 🧪 Testing Checklist

### Test 1: Create Card
1. ✅ Console shows "Card created successfully"
2. ✅ Salt generated (16 bytes hex)
3. ✅ RSA keypair generated
4. ✅ Firebase public key stored (if online)

### Test 2: Insert & Read Card
1. ✅ Insert card → Console shows member info
2. ✅ Verify PIN works
3. ✅ Read data shows correct balance

### Test 3: Update Balance
1. ✅ Update balance → Console shows "✅ Balance updated"
2. ✅ memberInfoRegistry updated
3. ✅ encryptedDataRegistry updated
4. ✅ Firebase transaction logged (if online)
5. ✅ Read card → Shows new balance
6. ✅ **UI shows new balance** ← CRITICAL

### Test 4: UI Refresh
1. Create card with 0đ
2. Top up 50,000đ
3. Check console: `✅ Balance updated: 50000 đ`
4. Check UI: Should show 50,000đ
5. If not → Use `logCardInfo()` to debug

---

## 💡 Tips

### Tip 1: Use Debug Function
```kotlin
// In your UI code
Button(onClick = {
    cardService.logCardInfo(memberId)
}) {
    Text("🔍 Debug Card Info")
}
```

### Tip 2: Monitor Console
Keep console open during testing:
- ✅ = Success
- ❌ = Error
- ⚠️ = Warning (Firebase offline OK)

### Tip 3: Verify Data Flow
```
User Action → updateBalance() → 
  ✓ Update registry → 
  ✓ Update encrypted data → 
  ✓ Log to Firebase → 
readCardData() → 
  ✓ Decrypt data → 
UI Update
```

---

## 🔧 Quick Fixes

### Fix: UI Not Updating
```kotlin
// Force state change
scannedMember = scannedMember?.copy() // Trigger recomposition
```

### Fix: Firebase Offline
```kotlin
// Disable Firebase (local mode only)
// Comment out in Main.kt:
// FirebaseService.getInstance().initialize(...)
```

### Fix: Card Not Found
```kotlin
// Check registry
val exists = cardRegistry.containsKey(memberId)
if (!exists) {
    println("❌ Card not in registry - need to create first")
}
```

---

## 📞 Debug Workflow

1. **Problem occurs** → Check console logs
2. **See error** → Find error message in this guide
3. **No error but wrong behavior** → Use `logCardInfo()`
4. **Still stuck** → Compare console output with examples above

---

**Need more help?** Check console output carefully - it tells you exactly what's happening! 🔍
