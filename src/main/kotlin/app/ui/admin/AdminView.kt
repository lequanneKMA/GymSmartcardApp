package app.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.model.Member
import app.manager.photo.PhotoManager
import app.util.toImageBitmap
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
            
            // Load ảnh nếu có
            val memberPhoto = remember(member.photoPath) {
                PhotoManager.loadPhoto(member.photoPath)
            }

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left column - Photo
                            Column(
                                modifier = Modifier.width(120.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .border(2.dp, Color(0xFF2196F3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (memberPhoto != null) {
                                        Image(
                                            bitmap = memberPhoto.toImageBitmap(),
                                            contentDescription = "Member photo",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("Chưa có ảnh", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                
                                if (member.photoPath != null) {
                                    Text(
                                        "✓ Đã có ảnh",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Right column - Info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ID: ${member.memberId}", fontSize = 14.sp, color = Color.Gray)
                                
                                member.birthDate?.let { birthDate ->
                                    Text(
                                        "Ngày sinh: ${birthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF424242)
                                    )
                                }
                                
                                if (member.cccdNumber != null) {
                                    Text(
                                        "CCCD: ${member.cccdNumber}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF424242)
                                    )
                                }
                            }
                        }

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
 * Dialog tạo thẻ mới với đầy đủ thông tin
 */
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun CreateCardDialog(
    onDismiss: () -> Unit,
    onCreate: (Member, String) -> Unit
) {
    var memberId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }  // Format: dd/MM/yyyy
    var cccdNumber by remember { mutableStateOf("") }
    var packageType by remember { mutableStateOf("1 Tháng") }
    var initialPin by remember { mutableStateOf("1234") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var photoPreview by remember { mutableStateOf<java.awt.image.BufferedImage?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tạo thẻ thành viên mới", fontSize = 20.sp)
        },
        text = {
            Column(
                Modifier.fillMaxWidth().height(550.dp).verticalScroll(rememberScrollState()),
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

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { 
                        // Only allow digits and /
                        if (it.length <= 10 && it.all { char -> char.isDigit() || char == '/' }) {
                            birthDate = it
                        }
                    },
                    label = { Text("Ngày sinh (dd/MM/yyyy)") },
                    placeholder = { Text("01/01/1990") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cccdNumber,
                    onValueChange = { 
                        // Only allow digits, max 12
                        if (it.length <= 12 && it.all { char -> char.isDigit() }) {
                            cccdNumber = it
                        }
                    },
                    label = { Text("Số CCCD (12 số)") },
                    placeholder = { Text("001234567890") },
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

                // Photo section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    backgroundColor = Color(0xFFF5F5F5)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Ảnh thành viên", fontSize = 14.sp, color = Color(0xFF424242))
                        
                        // Photo preview
                        if (photoPreview != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .border(1.dp, Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = photoPreview!!.toImageBitmap(),
                                    contentDescription = "Photo preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .border(1.dp, Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa chọn ảnh", color = Color.Gray)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Open file picker
                                    val fileDialog = FileDialog(Frame(), "Chọn ảnh", FileDialog.LOAD)
                                    fileDialog.setFilenameFilter { _, name -> 
                                        name.lowercase().endsWith(".jpg") || 
                                        name.lowercase().endsWith(".jpeg") || 
                                        name.lowercase().endsWith(".png") ||
                                        name.lowercase().endsWith(".bmp")
                                    }
                                    fileDialog.isVisible = true
                                    
                                    val selectedFile = fileDialog.file
                                    val selectedDir = fileDialog.directory
                                    
                                    if (selectedFile != null && selectedDir != null) {
                                        val file = File(selectedDir, selectedFile)
                                        photoPreview = PhotoManager.loadPhoto(file.absolutePath)
                                        // Don't save yet, will save when creating card
                                        photoPath = file.absolutePath
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF2196F3)
                                )
                            ) {
                                Text("📁 Chọn ảnh", color = Color.White, fontSize = 12.sp)
                            }

                            if (photoPath != null) {
                                OutlinedButton(
                                    onClick = {
                                        photoPath = null
                                        photoPreview = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🗑️ Xóa", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

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
                        // Parse birth date
                        val parsedBirthDate = if (birthDate.isNotEmpty()) {
                            try {
                                val parts = birthDate.split("/")
                                if (parts.size == 3) {
                                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        // Save photo if selected
                        val savedPhotoPath = if (photoPath != null && photoPreview != null) {
                            PhotoManager.savePhoto(photoPreview!!, memberId)
                        } else null

                        val startDate = LocalDate.now()
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
                            birthDate = parsedBirthDate,
                            cccdNumber = if (cccdNumber.isNotEmpty()) cccdNumber else null,
                            photoPath = savedPhotoPath,
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

