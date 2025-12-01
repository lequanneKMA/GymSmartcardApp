package app.manager.pin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.core.smartcard.SmartcardService

/**
 * PIN Verification Manager
 * Manages PIN verification for various operations (authentication, payment, package renewal)
 */
class PinVerificationManager(
    private val cardService: SmartcardService,
    private val onCardLocked: (String) -> Unit = {},
    private val isCardLocked: (String) -> Boolean = { false },
    private val onCardLockedClearData: () -> Unit = {}
) {
    // PIN verification state
    var isVerifying by mutableStateOf(false)
    var verificationReason by mutableStateOf("")
    var attemptsLeft by mutableStateOf(3)
    var lastError by mutableStateOf<String?>(null)
    
    // Current verification context
    private var currentMemberId: String? = null
    private var onSuccessCallback: ((String) -> Unit)? = null
    private var onFailureCallback: (() -> Unit)? = null
    
    // Track attempts per card
    private val attemptsMap = mutableMapOf<String, Int>()
    
    /**
     * Start PIN verification for a member
     * @param memberId The member ID to verify PIN for
     * @param reason Reason for verification (displayed to user)
     * @param onSuccess Callback when PIN is correct
     * @param onFailure Callback when verification fails/cancelled
     */
    fun startVerification(
        memberId: String,
        reason: String,
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit = {}
    ) {
        // Check if card is locked
        if (isCardLocked(memberId)) {
            lastError = "⚠️ Thẻ đã bị khóa - Vui lòng liên hệ Admin để mở khóa"
            onFailure()
            return
        }
        
        currentMemberId = memberId
        verificationReason = reason
        attemptsLeft = attemptsMap.getOrPut(memberId) { 3 }
        onSuccessCallback = onSuccess
        onFailureCallback = onFailure
        isVerifying = true
        lastError = null
    }
    
    /**
     * Verify the entered PIN
     * @param pin The PIN to verify
     * @return true if PIN is correct, false otherwise
     */
    fun verifyPin(pin: String): Boolean {
        val memberId = currentMemberId ?: return false
        
        // Check if card is locked
        if (isCardLocked(memberId)) {
            lastError = "⚠️ Thẻ đã bị khóa - Vui lòng liên hệ Admin để mở khóa"
            cancelVerification()
            return false
        }
        
        // Verify PIN with card service
        val verified = cardService.verifyPin(memberId, pin)
        
        if (verified) {
            // PIN correct
            attemptsLeft = 3
            attemptsMap[memberId] = 3 // Reset attempts
            lastError = null
            isVerifying = false
            onSuccessCallback?.invoke(pin)
            clearCallbacks()
            return true
        } else {
            // PIN incorrect
            attemptsLeft--
            attemptsMap[memberId] = attemptsLeft
            
            if (attemptsLeft <= 0) {
                // Out of attempts, lock card
                onCardLocked(memberId)
                onCardLockedClearData()  // Clear customer view data
                lastError = "🔒 Thẻ đã bị khóa do nhập sai PIN 3 lần - Liên hệ Admin để mở"
                isVerifying = false
                onFailureCallback?.invoke()
                clearCallbacks()
            } else {
                lastError = "Mã PIN không đúng - Còn $attemptsLeft lượt thử"
            }
            return false
        }
    }
    
    /**
     * Cancel current verification
     */
    fun cancelVerification() {
        isVerifying = false
        lastError = null
        onFailureCallback?.invoke()
        clearCallbacks()
    }
    
    /**
     * Reset attempts for a card (used when admin unlocks)
     */
    fun resetAttempts(memberId: String) {
        attemptsMap[memberId] = 3
        if (currentMemberId == memberId) {
            attemptsLeft = 3
        }
    }
    
    /**
     * Get attempts left for a specific card
     */
    fun getAttemptsLeft(memberId: String): Int {
        return attemptsMap.getOrPut(memberId) { 3 }
    }
    
    private fun clearCallbacks() {
        currentMemberId = null
        onSuccessCallback = null
        onFailureCallback = null
    }
}
