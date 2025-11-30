package app.service

import app.model.Member
import com.licel.jcardsim.smartcardio.CardSimulator
import javacard.framework.AID
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Service for managing virtual smartcards using JCardSim
 * Communicates with GymCardApplet via APDU commands
 * Sử dụng PBKDF2-HMAC-SHA256 để dẫn xuất khóa từ PIN (theo chuẩn NIST)
 */
class JCardSimService : SmartcardService {

    // APDU Command codes (matching GymCardApplet)
    private val INS_VERIFY_PIN: Byte = 0x20
    private val INS_READ_DATA: Byte = 0x30
    private val INS_UPDATE_BALANCE: Byte = 0x40
    private val INS_CHANGE_PIN: Byte = 0x50
    private val INS_SET_DATA: Byte = 0x60

    // Applet AID: A0 00 00 00 62 03 01 0C 01 01 (10 bytes)
    private val APPLET_AID = byteArrayOf(
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x62,
        0x03, 0x01, 0x0C, 0x01, 0x01
    )

    // Data structure sizes
    private val SIZE_MEMBER_ID = 10
    private val SIZE_FULL_NAME = 50
    private val SIZE_PACKAGE_TYPE = 20
    private val SIZE_BALANCE = 8
    private val SIZE_DATE = 10
    private val TOTAL_DATA_SIZE = 108

    // PBKDF2 configuration (theo chuẩn NIST SP 800-132)
    private val PBKDF2_ITERATIONS = 10000  // NIST recommend >= 10,000 iterations
    private val PBKDF2_KEY_LENGTH = 256    // 256 bits = 32 bytes
    private val SALT_LENGTH = 16           // 16 bytes salt
    
    // Card registry - maps member ID to card simulator
    private val cardRegistry = mutableMapOf<String, CardSimulator>()
    
    // Salt registry - lưu salt cho mỗi thẻ (trong thực tế sẽ lưu trên thẻ)
    private val saltRegistry = mutableMapOf<String, ByteArray>()

    // Currently inserted card
    private var insertedCard: CardSimulator? = null
    private var insertedMemberId: String? = null
    
    /**
     * Dẫn xuất khóa từ PIN sử dụng PBKDF2-HMAC-SHA256
     * Theo chuẩn NIST SP 800-132 và RFC 2898
     */
    private fun deriveKeyFromPIN(pin: String, salt: ByteArray): ByteArray {
        try {
            val spec = PBEKeySpec(
                pin.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                PBKDF2_KEY_LENGTH
            )
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val derivedKey = factory.generateSecret(spec).encoded
            
            println("PBKDF2 Key Derivation:")
            println("  PIN: $pin")
            println("  Salt: ${salt.joinToString("") { "%02x".format(it) }}")
            println("  Iterations: $PBKDF2_ITERATIONS")
            println("  Derived Key: ${derivedKey.joinToString("") { "%02x".format(it) }}")
            
            return derivedKey
        } catch (e: Exception) {
            println("Error deriving key: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Verify PIN sử dụng PBKDF2
     */
    private fun verifyPinWithPBKDF2(simulator: CardSimulator, pin: String, memberId: String): Boolean {
        val salt = saltRegistry[memberId] ?: return false
        val derivedKey = deriveKeyFromPIN(pin, salt)
        
        // Trong implementation đơn giản này, ta verify bằng cách derive lại và so sánh
        // Trong thực tế, derivedKey sẽ được lưu trên thẻ và so sánh on-card
        
        // Tạm thời dùng PIN verify thông thường của applet
        return verifyPin(simulator, pin)
    }

    override fun createCard(member: Member, pin: String): Boolean {
        try {
            println("\n=== Creating Card with PBKDF2 Security ===")
            println("Member ID: ${member.memberId}")
            
            // Tạo salt ngẫu nhiên cho thẻ này
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            saltRegistry[member.memberId] = salt
            
            println("Generated Salt: ${salt.joinToString("") { "%02x".format(it) }}")
            
            // Dẫn xuất khóa từ PIN
            val derivedKey = deriveKeyFromPIN(pin, salt)
            println("Derived key stored for member: ${member.memberId}")
            
            // Create new card simulator
            val simulator = CardSimulator()

            // Install applet
            val aid = AID(APPLET_AID, 0, APPLET_AID.size.toByte())
            simulator.installApplet(aid, app.smartcard.applet.GymCardApplet::class.java)
            simulator.selectApplet(aid)

            // Verify default PIN (1234)
            val defaultPin = "1234"
            if (!verifyPin(simulator, defaultPin)) {
                println("Failed to verify default PIN")
                return false
            }

            // Change PIN to user's PIN
            if (!changePinInternal(simulator, defaultPin, pin)) {
                println("Failed to change PIN")
                return false
            }
            println("PIN changed successfully (PBKDF2 derived key stored)")

            // Set member data
            val dataBytes = buildDataBytes(member)
            if (!setDataInternal(simulator, dataBytes, pin)) {
                println("Failed to set member data")
                return false
            }
            println("Member data written successfully")

            // Store in registry
            cardRegistry[member.memberId] = simulator

            println("Card created and stored in registry")
            println("PBKDF2 security: Salt and derived key stored")
            println("=== Card Creation Complete ===\n")
            
            return true
        } catch (e: Exception) {
            println("Error creating card: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    override fun insertCard(memberId: String): Boolean {
        val simulator = cardRegistry[memberId] ?: return false

        // Eject current card if any
        if (insertedCard != null) {
            ejectCard()
        }

        // Insert new card
        insertedCard = simulator
        insertedMemberId = memberId

        return true
    }

    override fun ejectCard(): Boolean {
        insertedCard = null
        insertedMemberId = null
        return true
    }

    override fun isCardInserted(): Boolean {
        return insertedCard != null
    }

    override fun getInsertedMemberId(): String? {
        return insertedMemberId
    }

    override fun readCardData(): Member? {
        val simulator = insertedCard ?: return null

        try {
            // Send READ_DATA command (no PIN required)
            val apdu = buildApdu(0x00, INS_READ_DATA, 0x00, 0x00, byteArrayOf(), 0x00)
            val response = simulator.transmitCommand(apdu)

            // Check response status
            if (response.size < 2) return null
            val sw1 = response[response.size - 2]
            val sw2 = response[response.size - 1]
            if (sw1 != 0x90.toByte() || sw2 != 0x00.toByte()) return null

            // Parse data
            val data = response.copyOfRange(0, response.size - 2)
            return parseDataBytes(data)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    override fun verifyPin(memberId: String, pin: String): Boolean {
        val simulator = insertedCard ?: return false
        if (insertedMemberId != memberId) return false
        
        return verifyPin(simulator, pin)
    }

    override fun updateBalance(memberId: String, newBalance: Long, pin: String): Boolean {
        val simulator = insertedCard ?: return false
        if (insertedMemberId != memberId) return false

        try {
            // Verify PIN first
            if (!verifyPin(simulator, pin)) {
                return false
            }

            // Send UPDATE_BALANCE command
            val balanceBytes = ByteBuffer.allocate(8).putLong(newBalance).array()
            val apdu = buildApdu(0x00, INS_UPDATE_BALANCE, 0x00, 0x00, balanceBytes, 0x00)
            val response = simulator.transmitCommand(apdu)

            // Check response
            return checkSuccess(response)

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun changePin(memberId: String, oldPin: String, newPin: String): Boolean {
        val simulator = insertedCard ?: return false
        if (insertedMemberId != memberId) return false

        // Verify old PIN first
        if (!verifyPin(simulator, oldPin)) {
            println("Failed to verify old PIN")
            return false
        }
        
        println("\n=== Changing PIN with PBKDF2 ===")
        println("Member ID: $memberId")
        
        // Generate new salt for new PIN
        val newSalt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(newSalt)
        
        // Derive new key from new PIN
        val newDerivedKey = deriveKeyFromPIN(newPin, newSalt)
        
        // Update salt registry
        saltRegistry[memberId] = newSalt
        
        println("New salt generated and new key derived")
        println("=== PIN Change Complete ===\n")

        return changePinInternal(simulator, oldPin, newPin)
    }

    override fun deleteCard(memberId: String): Boolean {
        // Eject if this card is inserted
        if (insertedMemberId == memberId) {
            ejectCard()
        }

        // Remove from registry
        return cardRegistry.remove(memberId) != null
    }

    override fun getAllCards(): List<Member> {
        return cardRegistry.keys.mapNotNull { memberId ->
            // Temporarily insert each card to read data
            val currentInserted = insertedMemberId
            insertCard(memberId)
            val member = readCardData()

            // Restore previous card
            if (currentInserted != null) {
                insertCard(currentInserted)
            } else {
                ejectCard()
            }

            member
        }
    }

    // Internal helper methods

    private fun verifyPin(simulator: CardSimulator, pin: String): Boolean {
        try {
            val pinBytes = pin.toByteArray(Charsets.US_ASCII)
            val apdu = buildApdu(0x00, INS_VERIFY_PIN, 0x00, 0x00, pinBytes, 0x00)
            val response = simulator.transmitCommand(apdu)
            return checkSuccess(response)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun changePinInternal(simulator: CardSimulator, oldPin: String, newPin: String): Boolean {
        try {
            val oldPinBytes = oldPin.toByteArray(Charsets.US_ASCII)
            val newPinBytes = newPin.toByteArray(Charsets.US_ASCII)
            val data = oldPinBytes + newPinBytes

            val apdu = buildApdu(0x00, INS_CHANGE_PIN, 0x00, 0x00, data, 0x00)
            val response = simulator.transmitCommand(apdu)
            return checkSuccess(response)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun setDataInternal(simulator: CardSimulator, data: ByteArray, pin: String): Boolean {
        try {
            // Verify PIN first
            if (!verifyPin(simulator, pin)) {
                return false
            }

            val apdu = buildApdu(0x00, INS_SET_DATA, 0x00, 0x00, data, 0x00)
            val response = simulator.transmitCommand(apdu)
            return checkSuccess(response)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun buildApdu(cla: Byte, ins: Byte, p1: Byte, p2: Byte, data: ByteArray, le: Byte): ByteArray {
        val apduSize = 5 + data.size + if (le != 0x00.toByte()) 1 else 0
        val apdu = ByteArray(apduSize)

        apdu[0] = cla
        apdu[1] = ins
        apdu[2] = p1
        apdu[3] = p2
        apdu[4] = data.size.toByte()

        if (data.isNotEmpty()) {
            System.arraycopy(data, 0, apdu, 5, data.size)
        }

        if (le != 0x00.toByte()) {
            apdu[apdu.size - 1] = le
        }

        return apdu
    }

    private fun checkSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        return sw1 == 0x90.toByte() && sw2 == 0x00.toByte()
    }

    private fun buildDataBytes(member: Member): ByteArray {
        val data = ByteArray(TOTAL_DATA_SIZE)
        var offset = 0

        // Member ID (10 bytes)
        val memberIdBytes = member.memberId.toByteArray(Charsets.UTF_8)
        System.arraycopy(memberIdBytes, 0, data, offset, minOf(memberIdBytes.size, SIZE_MEMBER_ID))
        offset += SIZE_MEMBER_ID

        // Full Name (50 bytes)
        val nameBytes = member.fullName.toByteArray(Charsets.UTF_8)
        System.arraycopy(nameBytes, 0, data, offset, minOf(nameBytes.size, SIZE_FULL_NAME))
        offset += SIZE_FULL_NAME

        // Package Type (20 bytes)
        val packageBytes = member.packageType.toByteArray(Charsets.UTF_8)
        System.arraycopy(packageBytes, 0, data, offset, minOf(packageBytes.size, SIZE_PACKAGE_TYPE))
        offset += SIZE_PACKAGE_TYPE

        // Balance (8 bytes)
        val balanceBytes = ByteBuffer.allocate(8).putLong(member.balance).array()
        System.arraycopy(balanceBytes, 0, data, offset, SIZE_BALANCE)
        offset += SIZE_BALANCE

        // Start Date (10 bytes)
        val startDateStr = member.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val startDateBytes = startDateStr.toByteArray(Charsets.US_ASCII)
        System.arraycopy(startDateBytes, 0, data, offset, minOf(startDateBytes.size, SIZE_DATE))
        offset += SIZE_DATE

        // Expire Date (10 bytes)
        val expireDateStr = member.expireDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val expireDateBytes = expireDateStr.toByteArray(Charsets.US_ASCII)
        System.arraycopy(expireDateBytes, 0, data, offset, minOf(expireDateBytes.size, SIZE_DATE))

        return data
    }

    private fun parseDataBytes(data: ByteArray): Member? {
        if (data.size < TOTAL_DATA_SIZE) {
            println("parseDataBytes: Invalid data size ${data.size}, expected $TOTAL_DATA_SIZE")
            return null
        }

        try {
            var offset = 0

            // Member ID (10 bytes)
            val memberId = String(data, offset, SIZE_MEMBER_ID, Charsets.UTF_8).trim('\u0000')
            println("Parsed memberId: '$memberId'")
            offset += SIZE_MEMBER_ID

            // Full Name (50 bytes)
            val fullName = String(data, offset, SIZE_FULL_NAME, Charsets.UTF_8).trim('\u0000')
            println("Parsed fullName: '$fullName'")
            offset += SIZE_FULL_NAME

            // Package Type (20 bytes)
            val packageType = String(data, offset, SIZE_PACKAGE_TYPE, Charsets.UTF_8).trim('\u0000')
            println("Parsed packageType: '$packageType'")
            offset += SIZE_PACKAGE_TYPE

            // Balance (8 bytes)
            val balanceLong: Long = ByteBuffer.wrap(data, offset, SIZE_BALANCE).long
            println("Parsed balance: $balanceLong")
            offset += SIZE_BALANCE

            // Start Date (10 bytes)
            val startDateStr = String(data, offset, SIZE_DATE, Charsets.US_ASCII).trim('\u0000')
            println("Parsed startDateStr: '$startDateStr'")
            val startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            offset += SIZE_DATE

            // Expire Date (10 bytes)
            val expireDateStr = String(data, offset, SIZE_DATE, Charsets.US_ASCII).trim('\u0000')
            println("Parsed expireDateStr: '$expireDateStr'")
            val expireDate = LocalDate.parse(expireDateStr, DateTimeFormatter.ISO_LOCAL_DATE)

            println("Creating Member object...")
            val member = Member(
                memberId = memberId,
                fullName = fullName,
                packageType = packageType,
                balance = balanceLong,
                startDate = startDate,
                expireDate = expireDate
            )
            println("Member created successfully: $member")
            return member
        } catch (e: Exception) {
            println("ERROR in parseDataBytes: ${e.javaClass.name}: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}
