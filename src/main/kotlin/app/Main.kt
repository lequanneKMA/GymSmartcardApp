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

// ==================== REUSABLE COMPONENTS ====================

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

@Composable
private fun AppTopBar(role: Role) {
    TopAppBar(
        title = {
            Text(
                when (role) {
                    Role.ADMIN -> "Gym Smartcard - Admin"
                    Role.STAFF -> "Gym Smartcard - Nhân viên"
                    Role.CUSTOMER -> "Gym Smartcard - Khách hàng"
                },
                fontSize = 18.sp
            )
        },
        backgroundColor = when (role) {
            Role.ADMIN -> Color(0xFFD32F2F)
            Role.STAFF -> Color(0xFF2E7D32)
            Role.CUSTOMER -> Color(0xFF1976D2)
        },
        contentColor = Color.White,
        elevation = 4.dp
    )
}

@Composable
private fun AppSidebar(state: AppState, showRoleSwitcher: Boolean = true) {
    Column(
        Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFFFFFFFF))
            .padding(20.dp)
    ) {
        // Role Switcher (only for Admin/Staff window)
        if (showRoleSwitcher) {
            RoleSwitcher(
                currentRole = state.currentRole,
                onChange = { role ->
                    if (role == Role.ADMIN) {
                        state.showAdminPasswordDialog = true
                        state.requestedRole = role
                    } else {
                        state.currentRole = role
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
        }

        // Functions card
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

                    when (state.currentRole) {
                        Role.ADMIN -> {
                            MenuItem("• Tạo thành viên mới")
                            MenuItem("• Quản lý thành viên")
                            MenuItem("• Đặt lại mã PIN")
                        }
                        Role.STAFF -> {
                            MenuItem("• Check-in/out")
                            MenuItem("• Quét thẻ")
                            MenuItem("• Gia hạn gói")
                            MenuItem("• Lịch sử giao dịch")
                        }
                        Role.CUSTOMER -> {
                            // Customer không có menu này
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Card Reader Panel (chỉ hiện cho Admin/Staff)
        if (state.currentRole != Role.CUSTOMER) {
            CardReaderPanel(state)
        }

        Spacer(Modifier.weight(1f))

        // System status
        SystemStatusCard()
    }
}

@Composable
private fun CardReaderPanel(state: AppState) {
    Card(
        Modifier.fillMaxWidth(),
        elevation = 3.dp,
        backgroundColor = Color(0xFFF5F5F5)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "🔌 Đầu đọc thẻ",
                fontSize = 16.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Card selector dropdown
            var expandedCards by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.selectedCardForInsert ?: "Chọn thẻ...",
                    onValueChange = { },
                    label = { Text("Thẻ khả dụng") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        TextButton(onClick = { 
                            state.refreshAvailableCards()
                            expandedCards = !expandedCards 
                        }) {
                            Text(if (expandedCards) "▲" else "▼")
                        }
                    }
                )

                DropdownMenu(
                    expanded = expandedCards,
                    onDismissRequest = { expandedCards = false }
                ) {
                    if (state.availableCards.isEmpty()) {
                        DropdownMenuItem(onClick = { }) {
                            Text("Không có thẻ", fontSize = 13.sp)
                        }
                    } else {
                        state.availableCards.forEach { cardId ->
                            DropdownMenuItem(onClick = {
                                state.selectedCardForInsert = cardId
                                expandedCards = false
                            }) {
                                Text("Thẻ: $cardId", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Insert/Eject buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        state.selectedCardForInsert?.let { cardId ->
                            state.insertCard(cardId)
                            state.toast = "Đã cắm thẻ: $cardId"
                        } ?: run {
                            state.toast = "Vui lòng chọn thẻ"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.insertedCardId == null && state.selectedCardForInsert != null,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Cắm thẻ", fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        state.ejectCard()
                        state.toast = "Đã rút thẻ"
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.insertedCardId != null,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Rút thẻ", fontSize = 13.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Card status
            Card(
                backgroundColor = if (state.insertedCardId != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.insertedCardId != null) "●" else "○",
                        color = if (state.insertedCardId != null) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.insertedCardId?.let { "Thẻ: $it" } ?: "Không có thẻ",
                        fontSize = 13.sp,
                        color = Color(0xFF424242)
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemStatusCard() {
    Card(
        Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFE8F5E9),
        elevation = 2.dp
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(0xFF4CAF50), shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Trạng thái: Online",
                fontSize = 13.sp,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun ToastMessage(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
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

@Composable
private fun MainContent(state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(24.dp)
    ) {
        when (state.currentRole) {
            Role.ADMIN -> AdminView(
                member = state.adminScannedMember,
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
                },
                onScan = { state.adminScan() },
                isCardLocked = { memberId -> state.isCardLocked(memberId) },
                onUnlockCard = { memberId -> state.unlockCard(memberId) }
            )
            Role.STAFF -> StaffView(
                member = state.scannedMember,
                pendingTransaction = state.pendingTransaction,
                onScan = { state.scan() },
                onCreateTransaction = { transaction -> state.createTransaction(transaction) },
                onConfirmPayment = { state.confirmTransaction() },
                onRejectPayment = { state.cancelTransaction() }
            )
            Role.CUSTOMER -> CustomerView(
                member = state.scannedMember,
                state = state,
                tempScannedMember = state.tempScannedMember,
                pendingTransaction = state.pendingTransaction,
                cart = state.cart,
                cartTotal = state.getCartTotal(),
                onAddToCart = { item -> state.addToCart(item) },
                onRemoveFromCart = { item -> state.removeFromCart(item) },
                onClearCart = { state.clearCart() },
                onCreateTransaction = { transaction -> state.createTransaction(transaction) },
                onConfirm = { state.confirmTransaction() },
                onCancel = { state.cancelTransaction() },
                onShowToast = { message -> state.toast = message },
                pinRequestActive = state.pinRequestActive,
                pinAttemptsLeft = state.pinAttemptsLeft,
                pinRequestReason = state.pinRequestReason,
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

// ==================== MAIN APPLICATION ====================

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
                    AppTopBar(state.currentRole)

                    Row(Modifier.fillMaxSize()) {
                        AppSidebar(state, showRoleSwitcher = true)
                        MainContent(state)
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
                    ToastMessage(message) { state.toast = null }
                }
            }
        }
    }

    // Separate Customer window (optional)
    if (showCustomerWindow) {
        Window(
            onCloseRequest = { showCustomerWindow = false },
            title = "Gym Smartcard - Khách hàng",
            state = WindowState(placement = WindowPlacement.Maximized)
        ) {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        AppTopBar(Role.CUSTOMER)
                        
                        CustomerView(
                            member = state.scannedMember,
                            state = state,
                            tempScannedMember = state.tempScannedMember,
                            pendingTransaction = state.pendingTransaction,
                            cart = state.cart,
                            cartTotal = state.getCartTotal(),
                            onAddToCart = { item -> state.addToCart(item) },
                            onRemoveFromCart = { item -> state.removeFromCart(item) },
                            onClearCart = { state.clearCart() },
                            onCreateTransaction = { transaction -> state.createTransaction(transaction) },
                            onConfirm = { state.confirmTransaction() },
                            onCancel = { state.cancelTransaction() },
                            onShowToast = { message -> state.toast = message },
                            pinRequestActive = state.pinRequestActive,
                            pinAttemptsLeft = state.pinAttemptsLeft,
                            pinRequestReason = state.pinRequestReason,
                            onVerifyPin = { pin -> state.verifyCardPin(pin) },
                            onPinCancelled = {
                                state.pinRequestActive = false
                                state.toast = "Đã hủy xác thực PIN"
                            },
                            cardService = state.cardService
                        )
                    }

                    // Toast for Customer window
                    state.toast?.let { message ->
                        ToastMessage(message) { state.toast = null }
                    }
                }
            }
        }
    }
}
