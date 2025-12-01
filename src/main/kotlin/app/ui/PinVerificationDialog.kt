package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.state.PinVerificationManager

/**
 * Reusable PIN Verification Dialog
 * Can be used for authentication, payment confirmation, package renewal, etc.
 */
@Composable
fun PinVerificationDialog(
    manager: PinVerificationManager,
    title: String = "Xác thực PIN",
    onDismiss: () -> Unit = {}
) {
    if (!manager.isVerifying) return
    
    var pin by remember { mutableStateOf("") }
    
    // Reset PIN when dialog opens
    LaunchedEffect(manager.isVerifying) {
        if (manager.isVerifying) {
            pin = ""
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight(),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 20.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (manager.verificationReason.isNotEmpty()) {
                    Text(
                        manager.verificationReason,
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Attempts remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Số lần thử còn lại:",
                        fontSize = 14.sp,
                        color = Color(0xFF424242)
                    )
                    Text(
                        "${manager.attemptsLeft}",
                        fontSize = 18.sp,
                        color = when {
                            manager.attemptsLeft == 3 -> Color(0xFF4CAF50)
                            manager.attemptsLeft == 2 -> Color(0xFFFF9800)
                            else -> Color(0xFFD32F2F)
                        }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                        }
                    },
                    label = { Text("Mã PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = manager.lastError != null,
                    placeholder = { Text("Nhập 4 số") }
                )
                
                if (manager.lastError != null) {
                    Text(
                        manager.lastError!!,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            manager.cancelVerification()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Hủy", fontSize = 14.sp)
                    }
                    
                    Button(
                        onClick = {
                            if (pin.length == 4) {
                                val verified = manager.verifyPin(pin)
                                if (!verified && manager.isVerifying) {
                                    // PIN wrong but still have attempts, keep dialog open
                                    pin = ""
                                } else if (!manager.isVerifying) {
                                    // Verification completed (success or locked)
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1976D2)
                        ),
                        enabled = pin.length == 4
                    ) {
                        Text("Xác nhận", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
