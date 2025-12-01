# AES Encryption for Smartcard Data

## Tổng quan

Hệ thống mã hóa AES-256-GCM cho dữ liệu thẻ thông minh, sử dụng PIN để derive encryption key.

## Kiến trúc bảo mật

### 1. **Key Derivation (PBKDF2)**
```
PIN (4 digits) + Salt (16 bytes) 
    ↓ PBKDF2-HMAC-SHA256 (10,000 iterations)
    ↓
AES-256 Key (32 bytes)
```

### 2. **Encryption (AES-GCM)**
```
Plaintext + AES Key
    ↓ AES-256-GCM
    ↓
IV (12 bytes) + Ciphertext + Auth Tag (16 bytes)
```

### 3. **Data Flow**

#### **Tạo thẻ (Create Card):**
```
1. Admin nhập thông tin: Họ tên, ngày sinh, CCCD, ảnh
2. Admin đặt PIN (4 số)
3. Generate random salt (16 bytes)
4. Derive AES key: PBKDF2(PIN, salt) → AES-256 key
5. Encrypt dữ liệu:
   - Họ tên → AES(fullName, key)
   - Ngày sinh → AES(birthDate, key)
   - CCCD → AES(cccdNumber, key)
   - Ảnh path → AES(photoPath, key)
   - Gói tập → AES(packageType, key)
   - Số dư → AES(balance, key)
6. Lưu lên thẻ:
   - Member ID (plaintext)
   - Salt (plaintext)
   - Encrypted data
   - PIN hash (PBKDF2)
```

#### **Đọc thẻ (Read Card):**
```
1. Customer nhập PIN
2. Đọc salt từ thẻ
3. Derive AES key: PBKDF2(PIN, salt)
4. Decrypt dữ liệu:
   - Encrypted fullName → Họ tên
   - Encrypted birthDate → Ngày sinh
   - Encrypted CCCD → Số CCCD
   - ...
5. Hiển thị thông tin cho customer
```

## API Usage

### 1. **Generate AES Key from PIN**

```kotlin
import app.security.AESEncryptionManager

// Generate salt (chỉ làm 1 lần khi tạo thẻ)
val salt = AESEncryptionManager.generateSalt()

// Derive AES key from PIN
val pin = "1234"
val aesKey = AESEncryptionManager.generateKeyFromPIN(pin, salt)
```

### 2. **Encrypt Member Data**

```kotlin
import app.security.CardDataEncryptionManager
import app.model.Member
import java.time.LocalDate

// Tạo member data
val member = Member(
    memberId = "GYM001",
    fullName = "Nguyễn Văn A",
    birthDate = LocalDate.of(1990, 1, 15),
    cccdNumber = "001234567890",
    photoPath = "/photos/GYM001.jpg",
    startDate = LocalDate.now(),
    expireDate = LocalDate.now().plusMonths(1),
    packageType = "1 Tháng",
    balance = 500000L
)

// Encrypt với AES key
val encryptedData = CardDataEncryptionManager.encryptMemberData(member, aesKey)

// Lưu encryptedData + salt lên thẻ
```

### 3. **Decrypt Member Data**

```kotlin
// Đọc salt và encrypted data từ thẻ
val salt = readSaltFromCard()
val encryptedData = readEncryptedDataFromCard()

// Customer nhập PIN
val customerPin = "1234"

// Derive AES key
val aesKey = AESEncryptionManager.generateKeyFromPIN(customerPin, salt)

// Decrypt data
try {
    val member = CardDataEncryptionManager.decryptMemberData(encryptedData, aesKey)
    // Hiển thị thông tin
    println("Họ tên: ${member.fullName}")
    println("Ngày sinh: ${member.birthDate}")
    println("CCCD: ${member.cccdNumber}")
} catch (e: Exception) {
    // PIN sai → Decrypt thất bại
    println("PIN không đúng hoặc dữ liệu bị hỏng")
}
```

### 4. **Encrypt/Decrypt Individual Fields**

```kotlin
// Encrypt string
val encryptedName = AESEncryptionManager.encryptString("Nguyễn Văn A", aesKey)

// Decrypt string
val name = AESEncryptionManager.decryptString(encryptedName, aesKey)

// Encrypt Long (balance)
val encryptedBalance = AESEncryptionManager.encryptLong(500000L, aesKey)

// Decrypt Long
val balance = AESEncryptionManager.decryptLong(encryptedBalance, aesKey)
```

## Card Data Structure

### **Dữ liệu trên thẻ (không mã hóa):**
- Member ID (8-10 bytes) - Công khai để tìm thẻ
- Salt (16 bytes) - Dùng để derive key
- PIN hash (32 bytes) - PBKDF2 hash để verify

### **Dữ liệu được mã hóa:**
- Full Name (encrypted)
- Birth Date (encrypted)
- CCCD Number (encrypted)
- Photo Path (encrypted)
- Start Date (encrypted)
- Expire Date (encrypted)
- Package Type (encrypted)
- Balance (encrypted)

### **Cấu trúc bộ nhớ thẻ (ước tính):**

```
Offset  | Size | Field              | Encrypted
--------|------|--------------------|-----------
0x00    | 10   | Member ID          | No
0x0A    | 16   | Salt               | No
0x1A    | 32   | PIN Hash           | No
0x3A    | 80   | Encrypted FullName | Yes
0x8A    | 40   | Encrypted BirthDate| Yes
0xB2    | 60   | Encrypted CCCD     | Yes
0xEE    | 100  | Encrypted PhotoPath| Yes
0x152   | 40   | Encrypted StartDate| Yes
0x17A   | 40   | Encrypted ExpireDate| Yes
0x1A2   | 50   | Encrypted Package  | Yes
0x1D4   | 32   | Encrypted Balance  | Yes

Total: ~500 bytes (dư để mở rộng)
```

## Security Features

### ✅ **Confidentiality (Bảo mật)**
- AES-256-GCM encryption
- Dữ liệu nhạy cảm được mã hóa (tên, CCCD, số dư)
- Không thể đọc được nếu không có PIN đúng

### ✅ **Integrity (Toàn vẹn)**
- GCM mode cung cấp authentication tag
- Phát hiện được nếu dữ liệu bị chỉnh sửa
- Decrypt sẽ fail nếu data bị tamper

### ✅ **Key Derivation**
- PBKDF2 với 10,000 iterations
- Salt ngẫu nhiên cho mỗi thẻ
- Rainbow table attack không hiệu quả

### ✅ **Forward Secrecy**
- Mỗi thẻ có salt riêng
- Compromise 1 thẻ không ảnh hưởng thẻ khác

## Implementation Steps

### Bước 1: Tích hợp vào JCardSimService

```kotlin
class JCardSimService : SmartcardService {
    private val saltRegistry = mutableMapOf<String, ByteArray>()
    
    override fun createCard(member: Member, pin: String): Boolean {
        // 1. Generate salt
        val salt = AESEncryptionManager.generateSalt()
        saltRegistry[member.memberId] = salt
        
        // 2. Derive AES key
        val aesKey = AESEncryptionManager.generateKeyFromPIN(pin, salt)
        
        // 3. Encrypt member data
        val encryptedData = CardDataEncryptionManager.encryptMemberData(member, aesKey)
        
        // 4. Write to card: memberId + salt + encryptedData
        // ... APDU commands ...
        
        return true
    }
    
    override fun readCardData(): Member? {
        // 1. Read memberId and salt from card
        val memberId = readMemberIdFromCard()
        val salt = readSaltFromCard(memberId)
        
        // 2. Read encrypted data
        val encryptedData = readEncryptedDataFromCard()
        
        // 3. Get PIN from user (through verification)
        val pin = getCurrentVerifiedPIN() // Đã verify trước đó
        
        // 4. Derive AES key
        val aesKey = AESEncryptionManager.generateKeyFromPIN(pin, salt)
        
        // 5. Decrypt data
        return CardDataEncryptionManager.decryptMemberData(encryptedData, aesKey)
    }
}
```

### Bước 2: Update AdminView - Thêm ảnh

```kotlin
@Composable
fun AdminView() {
    var photoFile by remember { mutableStateOf<File?>(null) }
    
    // Button chụp ảnh
    Button(onClick = {
        // Mở camera hoặc file picker
        photoFile = capturePhoto() // Implement camera capture
    }) {
        Text("📷 Chụp ảnh khách hàng")
    }
    
    // Hiển thị ảnh preview
    photoFile?.let { file ->
        Image(
            bitmap = loadImageBitmap(file.inputStream()),
            contentDescription = "Ảnh khách hàng"
        )
    }
}
```

### Bước 3: Update CreateCardDialog

```kotlin
var birthDate by remember { mutableStateOf<LocalDate?>(null) }
var cccdNumber by remember { mutableStateOf("") }
var photoPath by remember { mutableStateOf<String?>(null) }

// UI fields cho ngày sinh, CCCD
OutlinedTextField(
    value = cccdNumber,
    onValueChange = { cccdNumber = it },
    label = { Text("Số CCCD/CMND") }
)

// Date picker cho ngày sinh
DatePicker(
    selectedDate = birthDate,
    onDateChange = { birthDate = it }
)
```

## Testing

```kotlin
@Test
fun testEncryptDecrypt() {
    // Generate key
    val salt = AESEncryptionManager.generateSalt()
    val pin = "1234"
    val key = AESEncryptionManager.generateKeyFromPIN(pin, salt)
    
    // Create member
    val member = Member(...)
    
    // Encrypt
    val encrypted = CardDataEncryptionManager.encryptMemberData(member, key)
    
    // Decrypt
    val decrypted = CardDataEncryptionManager.decryptMemberData(encrypted, key)
    
    // Verify
    assertEquals(member.fullName, decrypted.fullName)
    assertEquals(member.balance, decrypted.balance)
}

@Test
fun testWrongPINFails() {
    val salt = AESEncryptionManager.generateSalt()
    val correctKey = AESEncryptionManager.generateKeyFromPIN("1234", salt)
    val wrongKey = AESEncryptionManager.generateKeyFromPIN("5678", salt)
    
    val member = Member(...)
    val encrypted = CardDataEncryptionManager.encryptMemberData(member, correctKey)
    
    // Decrypt với wrong key → Exception
    assertThrows<Exception> {
        CardDataEncryptionManager.decryptMemberData(encrypted, wrongKey)
    }
}
```

## Notes

1. **Salt Storage**: Salt phải lưu trên thẻ (plaintext) để derive key
2. **PIN Verification**: Verify PIN trước khi decrypt (tránh brute force)
3. **Error Handling**: Decrypt fail = PIN sai hoặc data corrupted
4. **Photo Storage**: Lưu path trên thẻ, ảnh thật lưu trên server/local storage
5. **Performance**: AES-GCM rất nhanh (~1ms cho 1KB data)

## Security Checklist

- ✅ AES-256 (not AES-128)
- ✅ GCM mode (authenticated encryption)
- ✅ Random IV cho mỗi encryption
- ✅ PBKDF2 với ≥10,000 iterations
- ✅ Random salt cho mỗi thẻ
- ✅ PIN verification trước khi decrypt
- ✅ Sensitive data (CCCD, balance) được encrypt
- ✅ Public data (memberId) không encrypt (để query)
