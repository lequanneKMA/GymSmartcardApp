package app.core.smartcard

import app.model.Member

/**
 * Interface for smartcard service operations
 */
interface SmartcardService {
    fun createCard(member: Member, pin: String): Boolean
    fun insertCard(memberId: String): Boolean
    fun ejectCard(): Boolean
    fun isCardInserted(): Boolean
    fun getInsertedMemberId(): String?
    fun readCardData(): Member?
    fun verifyPin(memberId: String, pin: String): Boolean
    fun updateBalance(memberId: String, newBalance: Long, pin: String): Boolean
    fun changePin(memberId: String, oldPin: String, newPin: String): Boolean
    fun deleteCard(memberId: String): Boolean
    fun getAllCards(): List<Member>
}
