package app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.model.Member
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminView(
    member: Member?,
    onShowToast: (String) -> Unit,
    onCreateCard: (Member, String) -> Boolean,
    onDeleteCard: (String) -> Boolean,
    onScan: () -> Unit,
    isCardLocked: (String) -> Boolean = { false },
    onUnlockCard: (String) -> Unit = {}
) {
    var showCreateCardDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(20.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quản lý Admin", fontSize = 22.sp, color = Color(0xFFD32F2F))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("📇 Quét thẻ")
                }
                Button(
                    onClick = { showCreateCardDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("➕ Tạo thẻ mới")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (member == null) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Vui lòng quét thẻ từ giao diện Nhân viên để quản lý thành viên",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            var fullName by remember { mutableStateOf(member.fullName) }
            var packageType by remember { mutableStateOf(member.packageType) }
            var balance by remember { mutableStateOf(member.balance.toString()) }
            var pin by remember { mutableStateOf("") }
            var showPinField by remember { mutableStateOf(false) }

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thông tin thành viên
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Thông tin thành viên", fontSize = 18.sp, color = Color(0xFF212121))
                        Spacer(Modifier.height(12.dp))

                        Text("ID: ${member.memberId}", fontSize = 14.sp, color = Color.Gray)

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Họ tên") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        // Dropdown gói tập
                        var expandedPackage by remember { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = packageType,
                                onValueChange = { },
                                label = { Text("Gói tập") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    TextButton(onClick = { expandedPackage = !expandedPackage }) {
                                        Text(if (expandedPackage) "▲" else "▼")
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = expandedPackage,
                                onDismissRequest = { expandedPackage = false }
                            ) {
                                listOf("1 Tháng", "3 Tháng", "6 Tháng", "1 Năm").forEach { option ->
                                    DropdownMenuItem(onClick = {
                                        packageType = option
                                        expandedPackage = false
                                    }) {
                                        Text(option)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = balance,
                            onValueChange = { balance = it },
                            label = { Text("Số dư (VNĐ)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onShowToast("Đã cập nhật thông tin thành viên")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF1976D2)
                            )
                        ) {
                            Text("Lưu thay đổi", color = Color.White)
                        }
                    }
                }

                // Quản lý PIN
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quản lý mã PIN", fontSize = 18.sp, color = Color(0xFF212121))
                            Switch(
                                checked = showPinField,
                                onCheckedChange = { showPinField = it }
                            )
                        }

                        if (showPinField) {
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = pin,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                        pin = it
                                    }
                                },
                                label = { Text("Mã PIN mới (4 số)") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Nhập 4 số") },
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (pin.length == 4) {
                                        onShowToast("Đã đặt mã PIN mới: $pin cho ${member.memberId}")
                                        pin = ""
                                        showPinField = false
                                    } else {
                                        onShowToast("Mã PIN phải có 4 số")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFD32F2F)
                                ),
                                enabled = pin.length == 4
                            ) {
                                Text("Đặt mã PIN mới", color = Color.White)
                            }
                        }
                    }
                }

                // Trạng thái khóa thẻ
                val cardLocked = member?.let { isCardLocked(it.memberId) } ?: false
                if (cardLocked) {
                    Card(
                        Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        backgroundColor = Color(0xFFFFF3E0)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text("🔒", fontSize = 24.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Thẻ đã bị khóa",
                                    fontSize = 18.sp,
                                    color = Color(0xFFE65100)
                                )
                            }

                            Text(
                                "Thẻ này đã bị khóa do nhập sai mã PIN 3 lần liên tiếp.",
                                fontSize = 14.sp,
                                color = Color(0xFF424242),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                "⚠️ Lưu ý: Thẻ bị khóa trên applet, cần rút thẻ và cắm lại để reset trạng thái.",
                                fontSize = 12.sp,
                                color = Color(0xFFD84315),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Button(
                                onClick = {
                                    onUnlockCard(member.memberId)
                                    onShowToast("Đã mở khóa thẻ ${member.memberId} - Vui lòng rút thẻ và cắm lại")
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFFF6F00)
                                )
                            ) {
                                Text("🔓 Mở khóa & Rút thẻ", color = Color.White)
                            }
                        }
                    }
                }

                // Xóa thành viên
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    backgroundColor = Color(0xFFFFEBEE)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Vùng nguy hiểm", fontSize = 18.sp, color = Color(0xFFD32F2F))
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (onDeleteCard(member.memberId)) {
                                    onShowToast("Đã xóa thẻ ${member.memberId}")
                                } else {
                                    onShowToast("Xóa thẻ thất bại")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text("Xóa thành viên", color = Color.White)
                        }
                    }
                }
            }
        }

        // Dialog tạo thẻ mới
        if (showCreateCardDialog) {
            CreateCardDialog(
                onDismiss = { showCreateCardDialog = false },
                onCreate = { newMember, initialPin ->
                    if (onCreateCard(newMember, initialPin)) {
                        onShowToast("Đã tạo thẻ ${newMember.memberId} thành công!")
                        showCreateCardDialog = false
                    } else {
                        onShowToast("Tạo thẻ thất bại! ID đã tồn tại")
                    }
                }
            )
        }
    }
}

/**
 * Dialog tạo thẻ mới
 */
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun CreateCardDialog(
    onDismiss: () -> Unit,
    onCreate: (Member, String) -> Unit
) {
    var memberId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var packageType by remember { mutableStateOf("1 Tháng") }
    var initialPin by remember { mutableStateOf("1234") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tạo thẻ thành viên mới", fontSize = 20.sp)
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = memberId,
                    onValueChange = { memberId = it.uppercase() },
                    label = { Text("ID thẻ (vd: ID12345)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Dropdown gói tập
                var expanded by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = packageType,
                        onValueChange = { },
                        label = { Text("Gói tập") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { expanded = true }) {
                                Text("▼")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("1 Tháng", "3 Tháng", "6 Tháng", "1 Năm").forEach { option ->
                            DropdownMenuItem(onClick = {
                                packageType = option
                                expanded = false
                            }) {
                                Text(option)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = initialPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            initialPin = it
                        }
                    },
                    label = { Text("Mã PIN ban đầu (4 số)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "⚠️ Thẻ sẽ được tạo với số dư ban đầu 0đ. Admin có thể nạp tiền sau.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (memberId.isNotEmpty() && fullName.isNotEmpty() && initialPin.length == 4) {
                        val startDate = java.time.LocalDate.now()
                        val expireDate = startDate.plusMonths(when(packageType) {
                            "1 Tháng" -> 1
                            "3 Tháng" -> 3
                            "6 Tháng" -> 6
                            "1 Năm" -> 12
                            else -> 1
                        }.toLong())

                        val newMember = Member(
                            memberId = memberId,
                            fullName = fullName,
                            startDate = startDate,
                            expireDate = expireDate,
                            packageType = packageType,
                            balance = 0L
                        )

                        onCreate(newMember, initialPin)
                    }
                },
                enabled = memberId.isNotEmpty() && fullName.isNotEmpty() && initialPin.length == 4
            ) {
                Text("Tạo thẻ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

