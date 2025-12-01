package app.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.model.Role

@Composable
fun RoleSwitcher(currentRole: Role, onChange: (Role) -> Unit) {
    Column {
        Text(
            "Vai trò:",
            fontSize = 14.sp,
            color = Color(0xFF424242),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onChange(Role.ADMIN) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (currentRole == Role.ADMIN) Color(0xFFD32F2F) else Color(0xFFE0E0E0),
                    contentColor = if (currentRole == Role.ADMIN) Color.White else Color(0xFF757575)
                )
            ) {
                Text("Admin", fontSize = 13.sp)
            }
            Button(
                onClick = { onChange(Role.STAFF) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (currentRole == Role.STAFF) Color(0xFF2E7D32) else Color(0xFFE0E0E0),
                    contentColor = if (currentRole == Role.STAFF) Color.White else Color(0xFF757575)
                )
            ) {
                Text("Nhân viên", fontSize = 13.sp)
            }
            Button(
                onClick = { onChange(Role.CUSTOMER) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (currentRole == Role.CUSTOMER) Color(0xFF1976D2) else Color(0xFFE0E0E0),
                    contentColor = if (currentRole == Role.CUSTOMER) Color.White else Color(0xFF757575)
                )
            ) {
                Text("Khách", fontSize = 13.sp)
            }
        }
    }
}
