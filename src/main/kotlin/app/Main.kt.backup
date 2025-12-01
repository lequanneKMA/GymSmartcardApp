package app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import app.model.Role
import app.state.AppState
import app.ui.*

// Màn hình với Role Switcher
@Composable
fun MainWindow(state: AppState) {
    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (state.currentRole) {
                                    Role.ADMIN -> "Gym Smartcard - Admin"
                                    Role.STAFF -> "Gym Smartcard - Nhân viên"
                                    Role.CUSTOMER -> "Gym Smartcard - Khách hàng"
                                },
                                fontSize = 18.sp
                            )
                        }
                    },
                    backgroundColor = when (state.currentRole) {
                        Role.ADMIN -> Color(0xFFD32F2F)
                        Role.STAFF -> Color(0xFF2E7D32)
                        Role.CUSTOMER -> Color(0xFF1976D2)
                    },
                    contentColor = Color.White,
                    elevation = 4.dp
                )

                Row(Modifier.fillMaxSize()) {
                    // Sidebar
                    Column(
                        Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFFFFF))
                            .padding(20.dp)
                    ) {
                        // Role Switcher with Admin password protection
                        RoleSwitcher(
                            currentRole = state.currentRole,
                            onChange = { role ->
                                if (role == Role.ADMIN) {
                                    // Show password dialog for Admin
                                    state.showAdminPasswordDialog = true
                                    state.requestedRole = role
                                } else {
                                    state.currentRole = role
                                }
                            }
                        )

                        Spacer(Modifier.height(20.dp))

                        // Chức năng theo role
                        if (state.currentRole != Role.CUSTOMER) {
                            Card(
                                Modifier.fillMaxWidth(),
                                elevation = 3.dp,
                                backgroundColor = Color(0xFFFAFAFA)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Chức năng",
                                        fontSize = 16.sp,
                                        color = Color(0xFF212121),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    if (state.currentRole == Role.ADMIN) {
                                        MenuItem("• Tạo thành viên mới")
                                        MenuItem("• Quản lý thành viên")
                                        MenuItem("• Quản lý hàng hóa")
                                        MenuItem("• Báo cáo thống kê")
                                    } else {
                                        MenuItem("• Check-in/out")
                                        MenuItem("• Quét thẻ")
                                        MenuItem("• Gia hạn gói")
                                        MenuItem("• Lịch sử giao dịch")
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                        }

                        Button(
                            onClick = { state.scan() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = when (state.currentRole) {
                                    Role.ADMIN -> Color(0xFFD32F2F)
                                    Role.STAFF -> Color(0xFF2E7D32)
                                    Role.CUSTOMER -> Color(0xFF1976D2)
                                }
                            )
                        ) {
                            Text("Quét thẻ", fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { state.clear() },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Clear", fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(20.dp))

                        // Card Reader Simulator Panel
                        Card(
                            Modifier.fillMaxWidth(),
                            elevation = 3.dp,
                            backgroundColor = Color(0xFFFFF3E0)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "🔌 Đầu đọc thẻ",
                                    fontSize = 14.sp,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Hiển thị trạng thái thẻ đang cắm
                                if (state.insertedCardId != null) {
                                    Text(
                                        "✅ Đã cắm: ${state.insertedCardId}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Button(
                                        onClick = { state.ejectCard() },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFFFF6F00)
                                        )
                                    ) {
                                        Text("Rút thẻ", fontSize = 13.sp, color = Color.White)
                                    }
                                } else {
                                    Text(
                                        "❌ Chưa cắm thẻ",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD32F2F),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Dropdown chọn thẻ
                                    var expanded by remember { mutableStateOf(false) }
                                    var selectedCard by remember { mutableStateOf<String?>(null) }

                                    Box(Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { 
                                                state.refreshAvailableCards()
                                                expanded = true 
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Text(
                                                selectedCard ?: "Chọn thẻ...",
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            if (state.availableCards.isEmpty()) {
                                                DropdownMenuItem(onClick = { }) {
                                                    Text("Không có thẻ nào", fontSize = 12.sp)
                                                }
                                            } else {
                                                state.availableCards.forEach { cardId ->
                                                    DropdownMenuItem(
                                                        onClick = {
                                                            selectedCard = cardId
                                                            expanded = false
                                                        }
                                                    ) {
                                                        Text(cardId, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            selectedCard?.let { state.insertCard(it) }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        enabled = selectedCard != null,
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFF4CAF50)
                                        )
                                    ) {
                                        Text("Cắm thẻ", fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.small)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Trạng thái: Online",
                                color = Color(0xFF2E7D32),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Content area
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5))
                            .padding(24.dp)
                    ) {
                        when (state.currentRole) {
                            Role.ADMIN -> AdminView(
                                member = state.scannedMember,
                                onShowToast = { message -> state.toast = message },
                                onCreateCard = { member, pin ->
                                    val result = state.cardService.createCard(member, pin)
                                    if (result) {
                                        state.refreshAvailableCards()
                                    }
                                    result
                                },
                                onDeleteCard = { memberId ->
                                    val result = state.cardService.deleteCard(memberId)
                                    if (result) {
                                        state.refreshAvailableCards()
                                    }
                                    result
                                }
                            )
                            Role.STAFF -> StaffView(
                                member = state.scannedMember,
                                pendingTransaction = state.pendingTransaction,
                                onScan = { state.scan() },
                                onCreateTransaction = { transaction ->
                                    state.createTransaction(transaction)
                                },
                                onConfirmPayment = { state.confirmTransaction() },
                                onRejectPayment = { state.cancelTransaction() }
                            )
                            Role.CUSTOMER -> CustomerView(
                                member = state.scannedMember,
                                pendingTransaction = state.pendingTransaction,
                                cart = state.cart,
                                cartTotal = state.getCartTotal(),
                                onAddToCart = { item -> state.addToCart(item) },
                                onRemoveFromCart = { item -> state.removeFromCart(item) },
                                onClearCart = { state.clearCart() },
                                onCreateTransaction = { transaction ->
                                    state.createTransaction(transaction)
                                },
                                onConfirm = { state.confirmTransaction() },
                                onCancel = { state.cancelTransaction() },
                                onShowToast = { message -> state.toast = message },
                                pinRequestActive = state.pinRequestActive,
                                pinAttemptsLeft = state.pinAttemptsLeft,
                                pinRequestReason = state.pinRequestReason,
                                tempScannedMember = state.tempScannedMember,
                                onVerifyPin = { pin -> state.verifyCardPin(pin) },
                                onPinCancelled = {
                                    state.pinRequestActive = false
                                    state.toast = "Đã hủy xác thực PIN"
                                },
                                cardService = state.cardService
                            )
                        }
                    }
                }
            }


            // Admin Password Dialog
            if (state.showAdminPasswordDialog) {
                AdminPasswordDialog(
                    onDismiss = {
                        state.showAdminPasswordDialog = false
                        state.requestedRole = null
                    },
                    onSuccess = {
                        state.currentRole = state.requestedRole ?: Role.STAFF
                        state.showAdminPasswordDialog = false
                        state.requestedRole = null
                        state.toast = "Đã đăng nhập Admin"
                    }
                )
            }

            // Toast
            state.toast?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(2500)
                    state.toast = null
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        backgroundColor = Color(0xFF323232),
                        elevation = 8.dp
                    ) {
                        Text(
                            message,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = Color(0xFF424242),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

fun main() = application {
    val state = remember { AppState() }
    var showCustomerWindow by remember { mutableStateOf(false) }

    // Main window for Admin/Staff
    Window(
        onCloseRequest = ::exitApplication,
        title = "Gym Smartcard - System",
        state = WindowState(placement = WindowPlacement.Maximized)
    ) {
        MaterialTheme {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when (state.currentRole) {
                                        Role.ADMIN -> "Gym Smartcard - Admin"
                                        Role.STAFF -> "Gym Smartcard - Nhân viên"
                                        Role.CUSTOMER -> "Gym Smartcard - Khách hàng"
                                    },
                                    fontSize = 18.sp
                                )
                            }
                        },
                        backgroundColor = when (state.currentRole) {
                            Role.ADMIN -> Color(0xFFD32F2F)
                            Role.STAFF -> Color(0xFF2E7D32)
                            Role.CUSTOMER -> Color(0xFF1976D2)
                        },
                        contentColor = Color.White,
                        elevation = 4.dp
                    )

                    Row(Modifier.fillMaxSize()) {
                        // Sidebar
                        Column(
                            Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFFFFFFF))
                                .padding(20.dp)
                        ) {
                            // Role Switcher (Admin/Staff only)
                            RoleSwitcher(
                                currentRole = state.currentRole,
                                onChange = { role ->
                                    when (role) {
                                        Role.ADMIN -> {
                                            state.showAdminPasswordDialog = true
                                            state.requestedRole = role
                                        }
                                        Role.STAFF -> {
                                            state.currentRole = role
                                        }
                                        Role.CUSTOMER -> {
                                            // Open separate Customer window
                                            showCustomerWindow = true
                                        }
                                    }
                                }
                            )

                            Spacer(Modifier.height(20.dp))

                            // Functions card
                            Card(
                                Modifier.fillMaxWidth(),
                                elevation = 3.dp,
                                backgroundColor = Color(0xFFFAFAFA)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Chức năng",
                                        fontSize = 16.sp,
                                        color = Color(0xFF212121),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    if (state.currentRole == Role.ADMIN) {
                                        MenuItem("• Tạo thành viên mới")
                                        MenuItem("• Quản lý thành viên")
                                        MenuItem("• Đặt lại mã PIN")
                                        MenuItem("• Báo cáo thống kê")
                                    } else {
                                        MenuItem("• Check-in/out")
                                        MenuItem("• Quét thẻ")
                                        MenuItem("• Gia hạn gói")
                                        MenuItem("• Lịch sử giao dịch")
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = { state.scan() },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = when (state.currentRole) {
                                        Role.ADMIN -> Color(0xFFD32F2F)
                                        Role.STAFF -> Color(0xFF2E7D32)
                                        Role.CUSTOMER -> Color(0xFF1976D2)
                                    }
                                )
                            ) {
                                Text("Quét thẻ", fontSize = 14.sp)
                            }

                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { state.clear() },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("Clear", fontSize = 14.sp)
                            }

                            Spacer(Modifier.height(20.dp))

                            // Card Reader Simulator Panel
                            Card(
                                Modifier.fillMaxWidth(),
                                elevation = 3.dp,
                                backgroundColor = Color(0xFFFFF3E0)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "🔌 Đầu đọc thẻ ảo",
                                        fontSize = 14.sp,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Hiển thị trạng thái thẻ đang cắm
                                    if (state.insertedCardId != null) {
                                        Text(
                                            "✅ Đã cắm: ${state.insertedCardId}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Button(
                                            onClick = { state.ejectCard() },
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFFFF6F00)
                                            )
                                        ) {
                                            Text("Rút thẻ", fontSize = 13.sp, color = Color.White)
                                        }
                                    } else {
                                        Text(
                                            "❌ Chưa cắm thẻ",
                                            fontSize = 12.sp,
                                            color = Color(0xFFD32F2F),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        // Dropdown chọn thẻ
                                        var expanded by remember { mutableStateOf(false) }
                                        var selectedCard by remember { mutableStateOf<String?>(null) }

                                        Box(Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { 
                                                    state.refreshAvailableCards()
                                                    expanded = true 
                                                },
                                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                            ) {
                                                Text(
                                                    selectedCard ?: "Chọn thẻ...",
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                if (state.availableCards.isEmpty()) {
                                                    DropdownMenuItem(onClick = { }) {
                                                        Text("Không có thẻ nào", fontSize = 12.sp)
                                                    }
                                                } else {
                                                    state.availableCards.forEach { cardId ->
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                selectedCard = cardId
                                                                expanded = false
                                                            }
                                                        ) {
                                                            Text(cardId, fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                selectedCard?.let { state.insertCard(it) }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            enabled = selectedCard != null,
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFF4CAF50)
                                            )
                                        ) {
                                            Text("Cắm thẻ", fontSize = 13.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.small)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Trạng thái: Online",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Content area
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5))
                                .padding(24.dp)
                        ) {
                            when (state.currentRole) {
                                Role.ADMIN -> AdminView(
                                    member = state.scannedMember,
                                    onShowToast = { message -> state.toast = message },
                                    onCreateCard = { member, pin ->
                                        val result = state.cardService.createCard(member, pin)
                                        if (result) {
                                            state.refreshAvailableCards()
                                        }
                                        result
                                    },
                                    onDeleteCard = { memberId ->
                                        val result = state.cardService.deleteCard(memberId)
                                        if (result) {
                                            state.refreshAvailableCards()
                                        }
                                        result
                                    }
                                )
                                Role.STAFF -> StaffView(
                                    member = state.scannedMember,
                                    pendingTransaction = state.pendingTransaction,
                                    onScan = { state.scan() },
                                    onCreateTransaction = { transaction ->
                                        state.createTransaction(transaction)
                                    },
                                    onConfirmPayment = { state.confirmTransaction() },
                                    onRejectPayment = { state.cancelTransaction() }
                                )
                                Role.CUSTOMER -> {
                                    // Empty state, customer window is separate
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Màn hình khách hàng đã mở trong cửa sổ riêng",
                                            fontSize = 16.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Admin Password Dialog
                if (state.showAdminPasswordDialog) {
                    AdminPasswordDialog(
                        onDismiss = {
                            state.showAdminPasswordDialog = false
                            state.requestedRole = null
                        },
                        onSuccess = {
                            state.currentRole = state.requestedRole ?: Role.STAFF
                            state.showAdminPasswordDialog = false
                            state.requestedRole = null
                            state.toast = "Đã đăng nhập Admin"
                        }
                    )
                }

                // Toast
                state.toast?.let { message ->
                    LaunchedEffect(message) {
                        kotlinx.coroutines.delay(2500)
                        state.toast = null
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            backgroundColor = Color(0xFF323232),
                            elevation = 8.dp
                        ) {
                            Text(
                                message,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Separate Customer window
    if (showCustomerWindow) {
        Window(
            onCloseRequest = { showCustomerWindow = false },
            title = "Gym Smartcard - Khách hàng",
            state = WindowState(placement = WindowPlacement.Maximized)
        ) {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Gym Smartcard - Khách hàng", fontSize = 18.sp) },
                            backgroundColor = Color(0xFF1976D2),
                            contentColor = Color.White,
                            elevation = 4.dp
                        )

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5))
                                .padding(24.dp)
                        ) {
                            CustomerView(
                                member = state.scannedMember,
                                pendingTransaction = state.pendingTransaction,
                                cart = state.cart,
                                cartTotal = state.getCartTotal(),
                                onAddToCart = { item -> state.addToCart(item) },
                                onRemoveFromCart = { item -> state.removeFromCart(item) },
                                onClearCart = { state.clearCart() },
                                onCreateTransaction = { transaction ->
                                    state.createTransaction(transaction)
                                },
                                onConfirm = { state.confirmTransaction() },
                                onCancel = { state.cancelTransaction() },
                                onShowToast = { message -> state.toast = message },
                                pinRequestActive = state.pinRequestActive,
                                pinAttemptsLeft = state.pinAttemptsLeft,
                                pinRequestReason = state.pinRequestReason,
                                tempScannedMember = state.tempScannedMember,
                                onVerifyPin = { pin -> state.verifyCardPin(pin) },
                                onPinCancelled = {
                                    state.pinRequestActive = false
                                    state.toast = "Đã hủy xác thực PIN"
                                },
                                cardService = state.cardService
                            )
                        }
                    }

                    // Toast for Customer window
                    state.toast?.let { message ->
                        LaunchedEffect(message) {
                            kotlinx.coroutines.delay(2500)
                            state.toast = null
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Card(
                                backgroundColor = Color(0xFF323232),
                                elevation = 8.dp
                            ) {
                                Text(
                                    message,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
