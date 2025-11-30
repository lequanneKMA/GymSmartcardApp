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
    onDeleteCard: (String) -> Boolean
) {

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
    }
}


