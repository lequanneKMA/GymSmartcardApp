package app.ui.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.model.Member
import app.model.Transaction
import app.model.TransactionType
import app.model.CartItem
import app.core.state.AppState
import app.core.smartcard.SmartcardService
import app.ui.shared.MemberInfoCard
import app.ui.dialog.PinVerificationDialog
import java.text.DecimalFormat

private val moneyFormatter = DecimalFormat("#,###")

@Composable
fun CustomerView(
    member: Member?,
    state: AppState,
    pendingTransaction: Transaction?,
    cart: List<CartItem>,
    cartTotal: Double,
    onAddToCart: (CartItem) -> Unit,
    onRemoveFromCart: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onCreateTransaction: (Transaction) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onShowToast: (String) -> Unit,
    pinRequestActive: Boolean = false,
    pinAttemptsLeft: Int = 3,
    pinRequestReason: String = "",
    tempScannedMember: Member? = null,
    onVerifyPin: (String) -> Boolean = { false },
    onPinCancelled: () -> Unit = {},
    cardService: SmartcardService? = null
) {
    // PIN verification dialog state (must be outside member null check)
    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var verifyPin by remember { mutableStateOf("") }
    var verifyError by remember { mutableStateOf("") }
    
    // Monitor pinRequestActive to show dialog
    LaunchedEffect(pinRequestActive) {
        if (pinRequestActive) {
            showPinVerifyDialog = true
            verifyPin = ""
            verifyError = ""
        }
    }
    
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 5.dp)
    ) {
        Text("Màn hình Khách hàng", fontSize = 20.sp, color = Color(0xFF212121))
        Spacer(Modifier.height(8.dp))

        if (member == null) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Vui lòng quét thẻ hoặc nhập ID",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            // State variables
            var showTopUpDialog by remember { mutableStateOf(false) }
            var topUpAmount by remember { mutableStateOf("") }
            var selectedMethod by remember { mutableStateOf("") }
            var showStoreDialog by remember { mutableStateOf(false) }
            var showTopUpQRDialog by remember { mutableStateOf(false) }
            var showPackageDialog by remember { mutableStateOf(false) }
            var showChangePinDialog by remember { mutableStateOf(false) }
            var oldPin by remember { mutableStateOf("") }
            var newPin by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }

            // Chia thành 2 cột
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cột trái: Thông tin + Chức năng
                Column(
                    Modifier.weight(0.4f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card thông tin thành viên
                    MemberInfoCard(member)

                    Spacer(Modifier.height(24.dp))

                    // Nút chức năng
                    if (pendingTransaction == null) {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showTopUpDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("💰 Nạp tiền", fontSize = 16.sp, color = Color.White)
                            }

                            Button(
                                onClick = { showPackageDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF1976D2)
                                )
                            ) {
                                Text("📅 Gói tập", fontSize = 16.sp, color = Color.White)
                            }

                            Button(
                                onClick = { showStoreDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFFF6F00)
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🛒 Mua đồ", fontSize = 16.sp, color = Color.White)
                                    if (cart.isNotEmpty()) {
                                        Text(" (${cart.size})", fontSize = 16.sp, color = Color.White)
                                    }
                                }
                            }

                            Button(
                                onClick = { showChangePinDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF757575)
                                )
                            ) {
                                Text("🔐 Đổi mã PIN", fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }

                    // Hiển thị pending transaction trong cột trái
                    if (pendingTransaction != null) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            elevation = 4.dp,
                            backgroundColor = Color(0xFFFFF9C4)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "⏳ Chờ xác nhận...",
                                    fontSize = 14.sp,
                                    color = Color(0xFFF57F17)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Loại: ${
                                        when (pendingTransaction.type) {
                                            TransactionType.TOP_UP -> "Nạp tiền"
                                            TransactionType.PURCHASE -> "Mua đồ"
                                            TransactionType.EXTEND_PACKAGE -> "Gia hạn gói"
                                            TransactionType.CHANGE_PIN -> "Đổi PIN"
                                        }
                                    }",
                                    fontSize = 13.sp,
                                    color = Color(0xFF424242)
                                )
                                Text(
                                    "Số tiền: ${moneyFormatter.format(pendingTransaction.amount.toLong())} đ",
                                    fontSize = 13.sp,
                                    color = Color(0xFF424242)
                                )
                            }
                        }
                    }
                }

                // Cột phải: Hiển thị các dialog/form
                Box(
                    Modifier.weight(0.6f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when {
                        showPackageDialog -> {
                            PackageDialogContent(
                                member = member,
                                state = state,
                                onCreateTransaction = onCreateTransaction,
                                onDismiss = { showPackageDialog = false }
                            )
                        }
                        showStoreDialog -> {
                            StoreDialogContent(
                                member = member,
                                state = state,
                                cart = cart,
                                cartTotal = cartTotal,
                                onAddToCart = onAddToCart,
                                onRemoveFromCart = onRemoveFromCart,
                                onClearCart = onClearCart,
                                onCreateTransaction = onCreateTransaction,
                                onDismiss = { showStoreDialog = false }
                            )
                        }
                        showTopUpDialog -> {
                            TopUpAmountDialogContent(
                                topUpAmount = topUpAmount,
                                onTopUpAmountChange = { topUpAmount = it },
                                selectedMethod = selectedMethod,
                                onMethodSelect = { selectedMethod = it },
                                onConfirm = {
                                    val amount = topUpAmount.toDoubleOrNull()
                                    if (amount != null && amount > 0 && selectedMethod.isNotEmpty()) {
                                        if (selectedMethod == "QR Code") {
                                            showTopUpDialog = false
                                            showTopUpQRDialog = true
                                        } else {
                                            onCreateTransaction(
                                                Transaction(
                                                    TransactionType.TOP_UP,
                                                    amount,
                                                    "Nạp tiền qua Tiền mặt",
                                                    member.memberId
                                                )
                                            )
                                            showTopUpDialog = false
                                            topUpAmount = ""
                                            selectedMethod = ""
                                        }
                                    }
                                },
                                onDismiss = {
                                    showTopUpDialog = false
                                    topUpAmount = ""
                                    selectedMethod = ""
                                }
                            )
                        }
                        showTopUpQRDialog -> {
                            TopUpQRDialogContent(
                                amount = topUpAmount.toDoubleOrNull() ?: 0.0,
                                member = member,
                                onCreateTransaction = { transaction ->
                                    onCreateTransaction(transaction)
                                    topUpAmount = ""
                                    selectedMethod = ""
                                },
                                onDismiss = { showTopUpQRDialog = false }
                            )
                        }
                        showChangePinDialog -> {
                            ChangePinDialogContent(
                                oldPin = oldPin,
                                onOldPinChange = { oldPin = it },
                                newPin = newPin,
                                onNewPinChange = { newPin = it },
                                confirmPin = confirmPin,
                                onConfirmPinChange = { confirmPin = it },
                                onConfirm = {
                                    when {
                                        oldPin.isEmpty() -> {
                                            onShowToast("Vui lòng nhập mã PIN hiện tại")
                                        }
                                        newPin.isEmpty() -> {
                                            onShowToast("Vui lòng nhập mã PIN mới")
                                        }
                                        confirmPin.isEmpty() -> {
                                            onShowToast("Vui lòng xác nhận mã PIN mới")
                                        }
                                        newPin.length != 4 -> {
                                            onShowToast("Mã PIN phải có 4 số")
                                        }
                                        newPin != confirmPin -> {
                                            onShowToast("Mã PIN mới không khớp")
                                        }
                                        oldPin != "1234" -> {
                                            onShowToast("Mã PIN hiện tại không đúng, vui lòng thử lại")
                                        }
                                        else -> {
                                            onShowToast("Đổi mã PIN thành công!")
                                            showChangePinDialog = false
                                            oldPin = ""
                                            newPin = ""
                                            confirmPin = ""
                                        }
                                    }
                                },
                                onDismiss = {
                                    showChangePinDialog = false
                                    oldPin = ""
                                    newPin = ""
                                    confirmPin = ""
                                }
                            )
                        }
                    }
                }
            }
            
        }
        
        // PIN Verification Dialog (triggered by staff scan) - shown even when member is null
        if (showPinVerifyDialog && tempScannedMember != null) {
            val memberToVerify = tempScannedMember!!
                AlertDialog(
                    onDismissRequest = {
                        showPinVerifyDialog = false
                        verifyPin = ""
                        verifyError = ""
                        onPinCancelled()
                    },
                    title = {
                        Column {
                            Text("🔐 Xác thực mã PIN", fontSize = 20.sp, color = Color(0xFF1976D2))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (pinRequestReason.isNotEmpty()) pinRequestReason else "Nhân viên đang yêu cầu xác thực thẻ",
                                fontSize = 14.sp,
                                color = Color(0xFF616161)
                            )
                        }
                    },
                    text = {
                        Column(Modifier.fillMaxWidth()) {
                            Card(
                                backgroundColor = Color(0xFFF5F5F5),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "Thông tin thẻ:",
                                        fontSize = 12.sp,
                                        color = Color(0xFF757575)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Mã thẻ: ${memberToVerify.memberId}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF212121)
                                    )
                                    Text(
                                        "Tên: ${memberToVerify.fullName}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF212121)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                "Số lần thử còn lại: $pinAttemptsLeft/3",
                                fontSize = 14.sp,
                                color = if (pinAttemptsLeft <= 1) Color.Red else Color(0xFFFF6F00),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = verifyPin,
                                onValueChange = { 
                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                        verifyPin = it
                                        verifyError = ""
                                    }
                                },
                                label = { Text("Nhập mã PIN (4 số)") },
                                placeholder = { Text("••••") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = verifyError.isNotEmpty(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF1976D2),
                                    unfocusedBorderColor = Color(0xFFBDBDBD),
                                    errorBorderColor = Color.Red
                                )
                            )
                            
                            if (verifyError.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    backgroundColor = Color(0xFFFFEBEE),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("❌", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            verifyError,
                                            color = Color(0xFFC62828),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (verifyPin.isEmpty()) {
                                    verifyError = "Vui lòng nhập mã PIN"
                                    return@Button
                                }
                                
                                if (verifyPin.length != 4) {
                                    verifyError = "Mã PIN phải có đúng 4 số"
                                    return@Button
                                }
                                
                                // Verify PIN using callback
                                val isValid = onVerifyPin(verifyPin)
                                
                                if (isValid) {
                                    showPinVerifyDialog = false
                                    verifyPin = ""
                                    verifyError = ""
                                } else {
                                    // pinAttemptsLeft đã được giảm bởi onVerifyPin()
                                    verifyError = "Mã PIN không đúng"
                                    verifyPin = ""
                                    
                                    if (pinAttemptsLeft <= 0) {
                                        showPinVerifyDialog = false
                                        onShowToast("Thẻ đã bị khóa do nhập sai PIN quá 3 lần")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF4CAF50)
                            ),
                            enabled = verifyPin.length == 4
                        ) {
                            Text("✓ Xác nhận", color = Color.White, fontSize = 14.sp)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                showPinVerifyDialog = false
                                verifyPin = ""
                                verifyError = ""
                                onPinCancelled()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF757575)
                            )
                        ) {
                            Text("Hủy bỏ", fontSize = 14.sp)
                        }
                    }
                )
            }
        }
    }


// Content versions của các dialog để hiển thị trong cột phải
@Composable
fun PackageDialogContent(
    member: Member,
    state: AppState,
    onCreateTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().fillMaxHeight(),
        elevation = 4.dp
    ) {
        Column(Modifier.padding(20.dp).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chọn gói tập", fontSize = 18.sp, color = Color(0xFF212121))
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            val packages = listOf(
                "Thêm 1 ngày" to 30000.0,
                "Thêm 1 tuần" to 150000.0,
                "Thêm 1 tháng" to 500000.0,
                "Thêm 3 tháng" to 1200000.0,
                "Thêm 6 tháng" to 2200000.0,
                "Thêm 1 năm" to 4000000.0
            )

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                packages.forEach { (packageName, price) ->
                    Card(
                        Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        backgroundColor = Color(0xFFFAFAFA)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Yêu cầu xác thực PIN trước khi gia hạn
                                    state.pinVerificationManager.startVerification(
                                        memberId = member.memberId,
                                        reason = "Gia hạn $packageName - ${moneyFormatter.format(price.toLong())} đ",
                                        onSuccess = { pin ->
                                            onCreateTransaction(
                                                Transaction(
                                                    TransactionType.EXTEND_PACKAGE,
                                                    price,
                                                    packageName,
                                                    member.memberId
                                                )
                                            )
                                            onDismiss()
                                        },
                                        onFailure = {
                                            // Hủy giao dịch
                                        }
                                    )
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                packageName,
                                fontSize = 15.sp,
                                color = Color(0xFF212121)
                            )
                            Text(
                                "${price.toLong().toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")} đ",
                                fontSize = 15.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // PIN Verification Dialog
    PinVerificationDialog(
        manager = state.pinVerificationManager,
        title = "Xác nhận gia hạn gói tập"
    )
}

@Composable
fun StoreDialogContent(
    member: Member,
    state: AppState,
    cart: List<CartItem>,
    cartTotal: Double,
    onAddToCart: (CartItem) -> Unit,
    onRemoveFromCart: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onCreateTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().fillMaxHeight(),
        elevation = 4.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cửa hàng", fontSize = 18.sp, color = Color(0xFF212121))
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chia 2 cột: Hàng hóa bên trái, Giỏ hàng bên phải
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cột trái: Danh sách hàng hóa
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        "Hàng hóa",
                        fontSize = 15.sp,
                        color = Color(0xFF212121)
                    )
                    Spacer(Modifier.height(8.dp))

                    val items = listOf(
                        "Nước uống" to 15000.0,
                        "Khăn tập" to 10000.0,
                        "Protein shake" to 45000.0,
                        "Thuê tủ" to 20000.0,
                        "Găng tay" to 50000.0,
                        "Áo tập" to 120000.0,
                        "Bình nước" to 35000.0,
                        "Dây nhảy" to 25000.0,
                        "Băng cổ tay" to 15000.0,
                        "Thảm tập Yoga" to 200000.0
                    )

                    Column(
                        Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items.chunked(2).forEach { rowItems ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { (name, price) ->
                                    Card(
                                        Modifier.weight(1f).height(70.dp),
                                        elevation = 2.dp,
                                        backgroundColor = Color(0xFFFAFAFA)
                                    ) {
                                        Button(
                                            onClick = {
                                                onAddToCart(app.model.CartItem(name, price))
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFFFAFAFA)
                                            )
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    name,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF212121)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "${price.toLong().toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")} đ",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF1976D2)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cột phải: Giỏ hàng
                Card(
                    Modifier.weight(1f).fillMaxHeight(),
                    elevation = 2.dp,
                    backgroundColor = Color(0xFFF5F5F5)
                ) {
                    Column(Modifier.padding(12.dp).fillMaxHeight()) {
                        Text(
                            "Giỏ hàng",
                            fontSize = 15.sp,
                            color = Color(0xFF212121)
                        )
                        Spacer(Modifier.height(8.dp))

                        if (cart.isEmpty()) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Chưa có sản phẩm",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            Column(
                                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cart.forEach { item ->
                                    Card(
                                        Modifier.fillMaxWidth(),
                                        elevation = 1.dp,
                                        backgroundColor = Color.White
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    item.name,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF212121)
                                                )
                                                Text(
                                                    "x${item.quantity}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF757575)
                                                )
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    "${(item.price * item.quantity).toLong().toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")} đ",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF1976D2)
                                                )
                                                TextButton(
                                                    onClick = { onRemoveFromCart(item) },
                                                    modifier = Modifier.height(20.dp),
                                                    contentPadding = PaddingValues(2.dp)
                                                ) {
                                                    Text("Xóa", fontSize = 10.sp, color = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Divider()
                            Spacer(Modifier.height(8.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Tổng cộng:",
                                    fontSize = 16.sp,
                                    color = Color(0xFF212121)
                                )
                                Text(
                                    "${cartTotal.toLong().toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")} đ",
                                    fontSize = 17.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cart.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onClearCart() },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Xóa hết", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            val itemsList = cart.joinToString(", ") { "${it.name} x${it.quantity}" }
                            // Yêu cầu xác thực PIN trước khi thanh toán
                            state.pinVerificationManager.startVerification(
                                memberId = member.memberId,
                                reason = "Thanh toán ${moneyFormatter.format(cartTotal.toLong())} đ",
                                onSuccess = { pin ->
                                    onCreateTransaction(
                                        Transaction(
                                            TransactionType.PURCHASE,
                                            cartTotal,
                                            "Mua đồ: $itemsList",
                                            member.memberId
                                        )
                                    )
                                    onDismiss()
                                },
                                onFailure = {
                                    // Hủy thanh toán
                                }
                            )
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text("Thanh toán", fontSize = 14.sp, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Đóng", fontSize = 14.sp)
                    }
                }
            }
        }
    }
    
    // PIN Verification Dialog
    PinVerificationDialog(
        manager = state.pinVerificationManager,
        title = "Xác nhận thanh toán"
    )
}

@Composable
fun TopUpAmountDialogContent(
    topUpAmount: String,
    onTopUpAmountChange: (String) -> Unit,
    selectedMethod: String,
    onMethodSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nạp tiền vào tài khoản", fontSize = 17.sp, color = Color(0xFF212121))
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Chọn nhanh:", fontSize = 13.sp, color = Color(0xFF757575))
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("50000", "100000", "200000", "500000").forEach { amount ->
                    OutlinedButton(
                        onClick = { onTopUpAmountChange(amount) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${amount.toInt() / 1000}K", fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            TextField(
                value = topUpAmount,
                onValueChange = onTopUpAmountChange,
                label = { Text("Số tiền (VNĐ)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("Phương thức:", fontSize = 13.sp, color = Color(0xFF757575))
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onMethodSelect("QR Code") },
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (selectedMethod == "QR Code") Color(0xFF1976D2) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        "📱 QR Code",
                        fontSize = 13.sp,
                        color = if (selectedMethod == "QR Code") Color.White else Color(0xFF757575)
                    )
                }

                Button(
                    onClick = { onMethodSelect("Tiền mặt") },
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (selectedMethod == "Tiền mặt") Color(0xFF1976D2) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        "💵 Tiền mặt",
                        fontSize = 13.sp,
                        color = if (selectedMethod == "Tiền mặt") Color.White else Color(0xFF757575)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Hủy", fontSize = 14.sp)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(44.dp),
                    enabled = topUpAmount.toDoubleOrNull() != null &&
                            topUpAmount.toDoubleOrNull()!! > 0 &&
                            selectedMethod.isNotEmpty()
                ) {
                    Text("Xác nhận", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TopUpQRDialogContent(
    amount: Double,
    member: Member,
    onCreateTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quét mã QR để nạp tiền", fontSize = 17.sp, color = Color(0xFF212121))
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Số tiền: ${amount.toLong().toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")} đ",
                fontSize = 22.sp,
                color = Color(0xFF2E7D32)
            )

            Spacer(Modifier.height(20.dp))

            Card(
                Modifier.size(280.dp),
                elevation = 3.dp,
                backgroundColor = Color.White
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val qrImage = remember {
                        try {
                            val resourceStream = object {}.javaClass.getResourceAsStream("/qr.jpg")
                            if (resourceStream != null) {
                                loadImageBitmap(resourceStream)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (qrImage != null) {
                        Image(
                            bitmap = qrImage,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📱", fontSize = 60.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("QR Code", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Vui lòng quét mã và nạp tiền",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Hủy", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        onCreateTransaction(
                            Transaction(
                                TransactionType.TOP_UP,
                                amount,
                                "Nạp tiền qua QR Code",
                                member.memberId
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("Đã thanh toán", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChangePinDialogContent(
    oldPin: String,
    onOldPinChange: (String) -> Unit,
    newPin: String,
    onNewPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đổi mã PIN", fontSize = 18.sp, color = Color(0xFF212121))
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Mã PIN hiện tại:", fontSize = 13.sp, color = Color(0xFF757575))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = oldPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        onOldPinChange(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập 4 số") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(Modifier.height(12.dp))

            Text("Mã PIN mới:", fontSize = 13.sp, color = Color(0xFF757575))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = newPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        onNewPinChange(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập 4 số mới") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(Modifier.height(12.dp))

            Text("Xác nhận mã PIN mới:", fontSize = 13.sp, color = Color(0xFF757575))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        onConfirmPinChange(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập lại 4 số mới") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Hủy", fontSize = 14.sp)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(44.dp),
                    enabled = oldPin.length == 4 && newPin.length == 4 && confirmPin.length == 4
                ) {
                    Text("Xác nhận", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}
