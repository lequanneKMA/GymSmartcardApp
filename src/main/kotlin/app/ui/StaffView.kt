package app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.model.Member
import app.model.Transaction
import app.model.TransactionType

@Composable
fun StaffView(
    member: Member?,
    pendingTransaction: Transaction?,
    onScan: () -> Unit,
    onCreateTransaction: (Transaction) -> Unit,
    onConfirmPayment: () -> Unit,
    onRejectPayment: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bảng điều khiển - Nhân viên", fontSize = 20.sp)
            Row {
                Button(onClick = onScan) { Text("Quét thẻ") }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (member == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có thẻ được quét", color = androidx.compose.ui.graphics.Color.Gray)
            }
        } else {
            Card(Modifier.fillMaxWidth(), elevation = 6.dp, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${member.fullName}", fontSize = 18.sp)
                        Text("ID: ${member.memberId}")
                        Text("Gói: ${member.packageType}")
                        Text("Từ: ${member.startDate} → ${member.expireDate}")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Số dư: ${"%,d".format(member.balance)} đ", fontSize = 16.sp, color = Color(0xFF2E7D32))
                    }
                }
            }

            // Hiển thị pending transaction nếu có
            if (pendingTransaction != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color(0xFFFFF3E0)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "🔔 Yêu cầu từ khách hàng",
                            fontSize = 18.sp,
                            color = Color(0xFFE65100)
                        )
                        Spacer(Modifier.height(12.dp))

                        Text(
                            pendingTransaction.description,
                            fontSize = 16.sp,
                            color = Color(0xFF424242)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Số tiền: ${"%,d".format(pendingTransaction.amount.toLong())} đ",
                            fontSize = 20.sp,
                            color = if (pendingTransaction.type == TransactionType.TOP_UP) {
                                Color(0xFF2E7D32)
                            } else {
                                Color(0xFFD32F2F)
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onRejectPayment,
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFD32F2F)
                                )
                            ) {
                                Text("❌ Hủy", fontSize = 14.sp, color = Color.White)
                            }

                            Button(
                                onClick = onConfirmPayment,
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF2E7D32)
                                )
                            ) {
                                Text("✓ Xác nhận", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Lịch sử giao dịch ", fontSize = 16.sp, color = Color(0xFF212121))
        }
    }
}
