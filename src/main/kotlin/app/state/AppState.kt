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
    var scannedMember by mutableStateOf<Member?>(null)
    var toast by mutableStateOf<String?>(null)
    var pendingTransaction by mutableStateOf<Transaction?>(null)
    val cart = mutableStateListOf<CartItem>()

    // Admin password protection
    var showAdminPasswordDialog by mutableStateOf(false)
    var requestedRole by mutableStateOf<Role?>(null)

    // Card reader state
    var availableCards by mutableStateOf<List<String>>(emptyList())
    var insertedCardId by mutableStateOf<String?>(null)
    
    // PIN request state (for customer view)
    var pinRequestActive by mutableStateOf(false)
    var pinAttemptsLeft by mutableStateOf(3)
    var pinRequestReason by mutableStateOf("")
    
    // Temporary storage for scanned member (before PIN verification)
    var tempScannedMember by mutableStateOf<Member?>(null)
    var verifiedPin by mutableStateOf<String?>(null)

    init {
        refreshAvailableCards()
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
            toast = "Đã rút thẻ"
        }
    }

    fun scan() {
        val m = cardService.readCardData()
        if (m != null) {
            // Lưu tạm thông tin thẻ, chưa hiển thị cho khách hàng
            tempScannedMember = m
            scannedMember = null // Chưa set member
            verifiedPin = null
            
            // Kích hoạt yêu cầu nhập PIN ở màn hình khách hàng
            pinRequestActive = true
            pinAttemptsLeft = 3
            pinRequestReason = "Nhân viên yêu cầu xác thực thẻ"
            toast = "Đã quét thẻ: ${m.memberId} - Vui lòng khách hàng nhập mã PIN"
        } else {
            toast = "Không có thẻ nào được cắm"
        }
    }
    
    fun verifyCardPin(pin: String): Boolean {
        val member = tempScannedMember ?: return false
        
        // Verify PIN with card service
        val verified = cardService.verifyPin(member.memberId, pin)
        
        if (verified) {
            // PIN đúng, hiển thị thông tin khách hàng
            scannedMember = member
            verifiedPin = pin
            pinRequestActive = false
            tempScannedMember = null
            toast = "Xác thực thành công - Chào mừng ${member.fullName}"
            return true
        } else {
            // PIN sai, giảm số lần thử
            pinAttemptsLeft--
            if (pinAttemptsLeft <= 0) {
                // Hết lượt thử, xóa thông tin
                pinRequestActive = false
                tempScannedMember = null
                toast = "Xác thực thất bại - Đã hết lượt thử"
            } else {
                toast = "Mã PIN sai - Còn $pinAttemptsLeft lượt thử"
            }
            return false
        }
    }

    fun clear() {
        scannedMember = null
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
