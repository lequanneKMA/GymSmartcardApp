package app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.model.CartItem
import app.model.Member
import app.model.Role
import app.model.Transaction
import app.service.SmartcardService
import app.service.JCardSimService
import java.text.DecimalFormat

private val moneyFormatter = DecimalFormat("#,###")

class AppState(val cardService: SmartcardService = JCardSimService()) {
    var currentRole by mutableStateOf(Role.STAFF)
    var scannedMember by mutableStateOf<Member?>(null)  // For Customer view (after PIN verified)
    var adminScannedMember by mutableStateOf<Member?>(null)  // For Admin view only
    var toast by mutableStateOf<String?>(null)
    var pendingTransaction by mutableStateOf<Transaction?>(null)
    val cart = mutableStateListOf<CartItem>()

    // Admin password protection
    var showAdminPasswordDialog by mutableStateOf(false)
    var requestedRole by mutableStateOf<Role?>(null)

    // Card reader state
    var availableCards by mutableStateOf<List<String>>(emptyList())
    var insertedCardId by mutableStateOf<String?>(null)
    var selectedCardForInsert by mutableStateOf<String?>(null)
    
    // PIN request state (for customer view)
    var pinRequestActive by mutableStateOf(false)
    var pinAttemptsLeft by mutableStateOf(3)
    var pinRequestReason by mutableStateOf("")
    
    // Temporary storage for scanned member (before PIN verification)
    var tempScannedMember by mutableStateOf<Member?>(null)
    var verifiedPin by mutableStateOf<String?>(null)
    
    // Card lock tracking
    private val lockedCards = mutableSetOf<String>()
    
    // PIN Verification Manager
    val pinVerificationManager = PinVerificationManager(
        cardService = cardService,
        onCardLocked = { memberId -> lockCard(memberId) },
        isCardLocked = { memberId -> isCardLocked(memberId) },
        onCardLockedClearData = {
            // Clear customer view when card is locked
            scannedMember = null
            tempScannedMember = null
            verifiedPin = null
            pinRequestActive = false
        }
    )

    init {
        refreshAvailableCards()
    }
    
    fun isCardLocked(memberId: String): Boolean {
        return lockedCards.contains(memberId)
    }
    
    fun lockCard(memberId: String) {
        lockedCards.add(memberId)
    }
    
    fun unlockCard(memberId: String) {
        lockedCards.remove(memberId)
        // Reset PIN attempts when unlocking
        pinVerificationManager.resetAttempts(memberId)
        if (tempScannedMember?.memberId == memberId) {
            pinAttemptsLeft = 3
        }
        
        // Tự động eject card để reset trạng thái PIN trên applet
        if (insertedCardId == memberId) {
            ejectCard()
            toast = "Đã mở khóa và rút thẻ - Vui lòng cắm lại thẻ để sử dụng"
        }
    }

    fun refreshAvailableCards() {
        availableCards = cardService.getAllCards().map { it.memberId }
    }

    fun insertCard(memberId: String) {
        if (cardService.insertCard(memberId)) {
            insertedCardId = memberId
            toast = "Đã cắm thẻ: $memberId"
        } else {
            toast = "Không thể cắm thẻ"
        }
    }

    fun ejectCard() {
        if (cardService.ejectCard()) {
            insertedCardId = null
            scannedMember = null
            adminScannedMember = null
            toast = "Đã rút thẻ"
        }
    }

    // Admin scan - bypass PIN verification
    fun adminScan() {
        val m = cardService.readCardData()
        if (m != null) {
            // Admin có quyền truy cập trực tiếp, không cần PIN
            adminScannedMember = m
            scannedMember = null  // Clear customer view
            tempScannedMember = null
            verifiedPin = null
            pinRequestActive = false
            toast = "Admin đã quét thẻ: ${m.memberId}"
        } else {
            toast = "Không có thẻ nào được cắm"
        }
    }
    
    // Staff scan - requires customer PIN verification
    fun scan() {
        val m = cardService.readCardData()
        if (m != null) {
            // Lưu tạm thông tin thẻ, chưa hiển thị cho khách hàng
            tempScannedMember = m
            scannedMember = null // Chưa set member
            verifiedPin = null
            
            // Lấy số lần thử còn lại của thẻ này từ PIN manager
            pinAttemptsLeft = pinVerificationManager.getAttemptsLeft(m.memberId)
            
            // Kích hoạt yêu cầu nhập PIN ở màn hình khách hàng
            pinRequestActive = true
            pinRequestReason = "Nhân viên yêu cầu xác thực thẻ"
            toast = "Đã quét thẻ: ${m.memberId} - Vui lòng khách hàng nhập mã PIN"
        } else {
            toast = "Không có thẻ nào được cắm"
        }
    }
    
    fun verifyCardPin(pin: String): Boolean {
        val member = tempScannedMember ?: return false
        
        // Use PIN verification manager
        pinVerificationManager.startVerification(
            memberId = member.memberId,
            reason = "Xác thực thẻ",
            onSuccess = { verifiedPinValue ->
                // PIN đúng, hiển thị thông tin khách hàng
                scannedMember = member
                verifiedPin = verifiedPinValue
                pinRequestActive = false
                tempScannedMember = null
                pinAttemptsLeft = 3
                toast = "Xác thực thành công - Chào mừng ${member.fullName}"
            },
            onFailure = {
                // Xác thực thất bại hoặc thẻ bị khóa
                pinRequestActive = false
                tempScannedMember = null
                toast = pinVerificationManager.lastError ?: "Xác thực thất bại"
            }
        )
        
        // Verify the PIN
        val verified = pinVerificationManager.verifyPin(pin)
        
        // Update UI state
        if (!verified && pinVerificationManager.attemptsLeft > 0) {
            pinAttemptsLeft = pinVerificationManager.attemptsLeft
            toast = pinVerificationManager.lastError ?: "Mã PIN không đúng"
        }
        
        return verified
    }

    fun clear() {
        scannedMember = null
        adminScannedMember = null
        tempScannedMember = null
        verifiedPin = null
        pinRequestActive = false
        pinAttemptsLeft = 3
        pinRequestReason = ""
        toast = "Đã xóa thông tin thẻ"
    }


    // Tạo giao dịch mới
    fun createTransaction(transaction: Transaction) {
        pendingTransaction = transaction
    }

    // Xác nhận giao dịch
    fun confirmTransaction() {
        val transaction = pendingTransaction
        val member = scannedMember

        if (transaction == null || member == null) {
            toast = "Không có giao dịch hoặc thẻ"
            pendingTransaction = null
            return
        }

        when (transaction.type) {
            app.model.TransactionType.TOP_UP -> {
                val pin = verifiedPin
                if (pin == null) {
                    toast = "Lỗi: Chưa xác thực PIN"
                    pendingTransaction = null
                    return
                }
                val newBalance = member.balance + transaction.amount.toLong()
                val ok = cardService.updateBalance(member.memberId, newBalance, pin)
                if (ok) {
                    scannedMember = cardService.readCardData()
                    toast = "Nạp tiền thành công: ${moneyFormatter.format(transaction.amount.toLong())} đ"
                } else {
                    toast = "Nạp tiền thất bại"
                }
            }
            app.model.TransactionType.PURCHASE -> {
                val pin = verifiedPin
                if (pin == null) {
                    toast = "Lỗi: Chưa xác thực PIN"
                    pendingTransaction = null
                    return
                }
                if (member.balance >= transaction.amount) {
                    val newBalance = member.balance - transaction.amount.toLong()
                    val ok = cardService.updateBalance(member.memberId, newBalance, pin)
                    if (ok) {
                        scannedMember = cardService.readCardData()
                        toast = "Thanh toán thành công: ${moneyFormatter.format(transaction.amount.toLong())} đ"
                        clearCart() // Xóa giỏ hàng sau khi thanh toán
                    } else {
                        toast = "Thanh toán thất bại"
                    }
                } else {
                    toast = "Số dư không đủ"
                }
            }
            app.model.TransactionType.EXTEND_PACKAGE -> {
                val pin = verifiedPin
                if (pin == null) {
                    toast = "Lỗi: Chưa xác thực PIN"
                    pendingTransaction = null
                    return
                }
                if (member.balance >= transaction.amount) {
                    val newBalance = member.balance - transaction.amount.toLong()
                    val ok = cardService.updateBalance(member.memberId, newBalance, pin)
                    if (ok) {
                        scannedMember = cardService.readCardData()
                        toast = "Gia hạn gói thành công"
                    } else {
                        toast = "Gia hạn thất bại"
                    }
                } else {
                    toast = "Số dư không đủ"
                }
            }
            app.model.TransactionType.CHANGE_PIN -> {
                // Đổi PIN xử lý hoàn toàn ở CustomerView
                toast = "Yêu cầu đổi PIN"
            }
        }

        pendingTransaction = null
    }

    // Hủy giao dịch
    fun cancelTransaction() {
        pendingTransaction = null
        toast = "Đã hủy giao dịch"
    }

    // Thêm vào giỏ hàng
    fun addToCart(item: CartItem) {
        val existingItem = cart.find { it.name == item.name }
        if (existingItem != null) {
            cart[cart.indexOf(existingItem)] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            cart.add(item)
        }
        toast = "Đã thêm ${item.name} vào giỏ"
    }

    // Xóa khỏi giỏ hàng
    fun removeFromCart(item: CartItem) {
        cart.remove(item)
        toast = "Đã xóa ${item.name}"
    }

    // Tính tổng giỏ hàng
    fun getCartTotal(): Double {
        return cart.sumOf { it.price * it.quantity }
    }

    // Xóa giỏ hàng
    fun clearCart() {
        cart.clear()
    }
}
