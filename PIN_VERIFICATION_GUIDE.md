# PIN Verification Manager - Hướng dẫn sử dụng

## Tổng quan

`PinVerificationManager` là class quản lý xác thực PIN tập trung, có thể tái sử dụng cho nhiều mục đích:
- Xác thực khi quét thẻ
- Thanh toán đồ dùng
- Gia hạn gói tập
- Các giao dịch khác cần xác thực

## Tính năng

✅ Theo dõi số lần nhập sai cho từng thẻ
✅ Tự động khóa thẻ sau 3 lần nhập sai
✅ Callback onSuccess và onFailure linh hoạt
✅ UI component tái sử dụng (`PinVerificationDialog`)
✅ Quản lý state tập trung

## Cách sử dụng

### 1. Khởi tạo trong AppState

```kotlin
val pinVerificationManager = PinVerificationManager(
    cardService = cardService,
    onCardLocked = { memberId -> lockCard(memberId) },
    isCardLocked = { memberId -> isCardLocked(memberId) }
)
```

### 2. Sử dụng cho thanh toán

```kotlin
// Trong dialog thanh toán (Store/Package)
Button(
    onClick = {
        // Yêu cầu xác thực PIN trước khi thanh toán
        state.pinVerificationManager.startVerification(
            memberId = member.memberId,
            reason = "Xác nhận thanh toán ${cartTotal} VNĐ",
            onSuccess = { pin ->
                // PIN đúng, thực hiện giao dịch
                onCreateTransaction(
                    Transaction(
                        type = TransactionType.PURCHASE,
                        amount = cartTotal,
                        description = "Mua đồ dùng",
                        memberId = member.memberId
                    )
                )
                onClearCart()
                onDismiss()
            },
            onFailure = {
                // Hủy thanh toán hoặc thẻ bị khóa
                // Error message đã được manager xử lý
            }
        )
    }
) {
    Text("Thanh toán")
}

// Hiển thị PIN dialog
PinVerificationDialog(
    manager = state.pinVerificationManager,
    title = "Xác nhận thanh toán",
    onDismiss = { /* Xử lý khi hủy */ }
)
```

### 3. Sử dụng cho gia hạn gói tập

```kotlin
Button(
    onClick = {
        state.pinVerificationManager.startVerification(
            memberId = member.memberId,
            reason = "Gia hạn gói ${selectedPackage} - ${packagePrice} VNĐ",
            onSuccess = { pin ->
                // Thực hiện gia hạn
                onCreateTransaction(
                    Transaction(
                        type = TransactionType.PACKAGE_RENEWAL,
                        amount = packagePrice,
                        description = "Gia hạn gói $selectedPackage",
                        memberId = member.memberId
                    )
                )
                onDismiss()
            },
            onFailure = {
                // Hủy gia hạn
            }
        )
    }
) {
    Text("Xác nhận gia hạn")
}

// Hiển thị PIN dialog
PinVerificationDialog(
    manager = state.pinVerificationManager,
    title = "Xác nhận gia hạn gói tập"
)
```

### 4. Sử dụng PinVerificationDialog (UI Component)

```kotlin
@Composable
fun YourScreen(state: AppState) {
    // Your UI code...
    
    // Hiển thị PIN verification dialog
    PinVerificationDialog(
        manager = state.pinVerificationManager,
        title = "Xác thực PIN",  // Tùy chỉnh title
        onDismiss = {
            // Xử lý khi đóng dialog (cancel hoặc hoàn thành)
        }
    )
}
```

## API Reference

### PinVerificationManager

#### Properties
- `isVerifying: Boolean` - Dialog có đang hiển thị không
- `verificationReason: String` - Lý do xác thực (hiển thị cho user)
- `attemptsLeft: Int` - Số lần thử còn lại
- `lastError: String?` - Error message cuối cùng

#### Methods

**startVerification()**
```kotlin
fun startVerification(
    memberId: String,           // ID thẻ cần verify
    reason: String,             // Lý do (hiển thị cho user)
    onSuccess: (String) -> Unit, // Callback khi PIN đúng (nhận PIN)
    onFailure: () -> Unit = {}  // Callback khi fail/cancel
)
```

**verifyPin()**
```kotlin
fun verifyPin(pin: String): Boolean  // Verify PIN, trả về true nếu đúng
```

**cancelVerification()**
```kotlin
fun cancelVerification()  // Hủy verification hiện tại
```

**resetAttempts()**
```kotlin
fun resetAttempts(memberId: String)  // Reset số lần thử (dùng khi admin unlock)
```

**getAttemptsLeft()**
```kotlin
fun getAttemptsLeft(memberId: String): Int  // Lấy số lần thử còn lại của thẻ
```

## Ví dụ đầy đủ

```kotlin
@Composable
fun StoreCheckoutDialog(
    member: Member,
    cartTotal: Double,
    state: AppState,
    onCreateTransaction: (Transaction) -> Unit,
    onClearCart: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Tổng tiền: ${cartTotal} VNĐ")
            
            Button(
                onClick = {
                    state.pinVerificationManager.startVerification(
                        memberId = member.memberId,
                        reason = "Thanh toán ${cartTotal} VNĐ",
                        onSuccess = { pin ->
                            // Tạo giao dịch
                            onCreateTransaction(
                                Transaction(
                                    type = TransactionType.PURCHASE,
                                    amount = cartTotal,
                                    description = "Mua đồ dùng",
                                    memberId = member.memberId
                                )
                            )
                            onClearCart()
                            onDismiss()
                        },
                        onFailure = {
                            showPinDialog = false
                            // state.toast sẽ hiển thị lỗi
                        }
                    )
                    showPinDialog = true
                }
            ) {
                Text("Thanh toán")
            }
        }
    }
    
    // PIN Dialog
    if (showPinDialog) {
        PinVerificationDialog(
            manager = state.pinVerificationManager,
            title = "Xác nhận thanh toán",
            onDismiss = { showPinDialog = false }
        )
    }
}
```

## Lưu ý

1. **Số lần thử được lưu theo thẻ**: Rút thẻ và cắm lại không reset counter
2. **Tự động khóa thẻ**: Sau 3 lần sai, thẻ bị khóa - chỉ Admin mở được
3. **Callbacks được gọi 1 lần**: Sau khi success/failure, callbacks tự động clear
4. **Thread-safe**: Manager xử lý state một cách an toàn

## Migration từ code cũ

**Trước:**
```kotlin
// Code cũ - xác thực PIN trực tiếp
if (cardService.verifyPin(memberId, pin)) {
    // Xử lý thành công
} else {
    // Xử lý thất bại
}
```

**Sau:**
```kotlin
// Code mới - dùng manager với UI
state.pinVerificationManager.startVerification(
    memberId = memberId,
    reason = "Mô tả giao dịch",
    onSuccess = { pin -> /* Xử lý thành công */ },
    onFailure = { /* Xử lý thất bại */ }
)

// Hiển thị dialog
PinVerificationDialog(manager = state.pinVerificationManager)
```
