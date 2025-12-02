package app.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.model.Member
import app.manager.photo.PhotoManager
import app.util.toImageBitmap
import java.time.format.DateTimeFormatter

@Composable
fun MemberInfoCard(member: Member) {
    // Load ảnh ưu tiên từ photoData (on-card), fallback về photoPath
    val memberPhoto = remember(member.photoData, member.photoPath) {
        when {
            member.photoData != null -> PhotoManager.byteArrayToImage(member.photoData)
            member.photoPath != null -> PhotoManager.loadPhoto(member.photoPath)
            else -> null
        }
    }

    Card(
        Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo column
            if (memberPhoto != null) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = memberPhoto.toImageBitmap(),
                        contentDescription = "Member photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, fontSize = 18.sp, color = Color(0xFF212121))
                Text("ID: ${member.memberId}", fontSize = 14.sp, color = Color(0xFF757575))
                
                // Hiển thị ngày sinh nếu có
                member.birthDate?.let { birthDate ->
                    Text(
                        "Sinh: ${birthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
                
                // Hiển thị CCCD nếu có
                if (member.cccdNumber != null) {
                    Text(
                        "CCCD: ${member.cccdNumber}",
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Số dư: %,d đ".format(member.balance),
                    fontSize = 22.sp,
                    color = Color(0xFF2E7D32)
                )

                Spacer(Modifier.height(12.dp))

                // Hiển thị hạn gói tập
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Gói tập: ${member.packageType}",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hạn: ${member.expireDate}",
                            fontSize = 16.sp,
                            color = Color(0xFF1976D2),
                            style = androidx.compose.ui.text.font.FontWeight.Bold.let {
                                androidx.compose.ui.text.TextStyle(fontWeight = it)
                            }
                        )
                    }

                    // Kiểm tra còn bao nhiêu ngày
                    val daysRemaining = try {
                        val today = java.time.LocalDate.now()
                        java.time.temporal.ChronoUnit.DAYS.between(today, member.expireDate).toInt()
                    } catch (e: Exception) {
                        -1
                    }

                    // Hiển thị trạng thái
                    Card(
                        elevation = 2.dp,
                        backgroundColor = when {
                            daysRemaining < 0 -> Color(0xFFD32F2F) // Hết hạn - đỏ
                            daysRemaining <= 7 -> Color(0xFFFF6F00) // Sắp hết hạn - cam
                            else -> Color(0xFF2E7D32) // Còn hạn - xanh
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            when {
                                daysRemaining < 0 -> "HẾT HẠN"
                                daysRemaining == 0 -> "HÔM NAY"
                                daysRemaining <= 7 -> "Còn $daysRemaining ngày"
                                else -> "Còn $daysRemaining ngày"
                            },
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = androidx.compose.ui.text.font.FontWeight.Bold.let {
                                androidx.compose.ui.text.TextStyle(fontWeight = it)
                            }
                        )
                    }
                }
            }
        }
    }
}
