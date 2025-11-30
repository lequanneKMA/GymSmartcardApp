package app.smartcard

import app.model.Member

interface SmartcardService {
    fun scanCard(): Member?
    fun clear()
    fun topUp(memberId: String, amount: Double): Boolean
    fun verifyPin(memberId: String, pin: String): Boolean
    fun changePin(memberId: String, oldPin: String, newPin: String): Boolean
}
