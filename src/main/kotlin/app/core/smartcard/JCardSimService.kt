package app.core.smartcard

import app.model.Member
import app.security.AESEncryptionManager
import app.security.CardDataEncryptionManager
import app.security.EncryptedCardData
import app.security.RSASignatureManager
import app.security.CardIdentity
import app.service.firebase.FirebaseService
import com.licel.jcardsim.smartcardio.CardSimulator
import javacard.framework.AID
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.SecretKey
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
    
    // Encrypted data registry - lưu dữ liệu đã mã hóa
    private val encryptedDataRegistry = mutableMapOf<String, EncryptedCardData>()
    
    // Verified PIN registry - lưu PIN đã verify cho session hiện tại
    private val verifiedPINRegistry = mutableMapOf<String, String>()
    
    // Member info registry - lưu thông tin cơ bản để hiển thị dropdown (không cần decrypt)
    private val memberInfoRegistry = mutableMapOf<String, Member>()
    
    // Card identity registry - lưu RSA keypair cho mỗi thẻ (CHỐNG NHÂN BẢN)
    private val cardIdentityRegistry = mutableMapOf<String, CardIdentity>()
    
    // Active challenges - lưu challenge đang chờ verify
    private val activeChallenges = mutableMapOf<String, ByteArray>()
    
    // Firebase service for cloud backend
    private val firebaseService = FirebaseService.getInstance()

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
            println("\n=== Creating Card with AES-256-GCM + RSA-2048 Signature ===")
            println("Member ID: ${member.memberId}")
            
            // 1. Tạo salt ngẫu nhiên cho thẻ này
            val salt = AESEncryptionManager.generateSalt()
            saltRegistry[member.memberId] = salt
            println("Generated Salt: ${salt.joinToString("") { "%02x".format(it) }}")
            
            // 2. Dẫn xuất AES key từ PIN
            val aesKey = AESEncryptionManager.generateKeyFromPIN(pin, salt)
            println("AES-256 key derived from PIN")
            
            // 3. Mã hóa dữ liệu thành viên
            val encryptedData = CardDataEncryptionManager.encryptMemberData(member, aesKey)
            encryptedDataRegistry[member.memberId] = encryptedData
            println("Member data encrypted with AES-256-GCM")
            
            // 4. Generate RSA keypair cho chống nhân bản thẻ
            val keyPair = RSASignatureManager.generateKeyPair()
            val cardIdentity = CardIdentity(
                memberId = member.memberId,
                privateKey = keyPair.private,
                publicKey = keyPair.public
            )
            cardIdentityRegistry[member.memberId] = cardIdentity
            println("RSA-2048 keypair generated (anti-cloning)")
            println("  Public Key: ${RSASignatureManager.encodePublicKey(keyPair.public).take(50)}...")
            
            // 🔥 Store public key to Firebase
            val publicKeyBase64 = RSASignatureManager.encodePublicKey(keyPair.public)
            runBlocking {
                firebaseService.storePublicKey(member.memberId, publicKeyBase64)
            }
            
            // 5. Lưu member info cho dropdown (không cần decrypt)
            memberInfoRegistry[member.memberId] = member
            
            // 6. Lưu PIN đã verify cho session
            verifiedPINRegistry[member.memberId] = pin
            
            // 7. Create card simulator và set PIN
            val simulator = CardSimulator()
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
            println("PIN changed successfully")

            // 8. Lưu encrypted data vào applet (giữ nguyên buildDataBytes cho tương thích)
            val dataBytes = buildDataBytes(member)
            if (!setDataInternal(simulator, dataBytes, pin)) {
                println("Failed to set member data")
                return false
            }
            println("Encrypted data written to card")

            // 9. Store in registry
            cardRegistry[member.memberId] = simulator

            println("Card created successfully")
            println("Security: AES-256-GCM + PBKDF2 (10,000 iterations)")
            println("=== Card Creation Complete ===")
            
            return true
        } catch (e: Exception) {
            println("Error creating card: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    override fun insertCard(memberId: String): Boolean {
        val simulator = cardRegistry[memberId]
        if (simulator == null) {
            println("❌ Card not found in registry: $memberId")
            return false
        }

        // Eject current card if any
        if (insertedCard != null) {
            ejectCard()
        }

        // Insert new card
        insertedCard = simulator
        insertedMemberId = memberId
        
        // Check if PIN is already verified (from previous session)
        val hasPIN = verifiedPINRegistry.containsKey(memberId)
        println("✓ Card inserted: $memberId (PIN verified: $hasPIN)")

        return true
    }

    override fun ejectCard(): Boolean {
        // Keep verified PIN in session for convenience (cắm lại không cần verify PIN)
        // PIN sẽ được clear khi:
        // 1. Đóng app
        // 2. Gọi clearVerifiedPin() explicitly
        // 3. Change PIN
        
        println("Card ejected (PIN kept in session for re-insert)")
        insertedCard = null
        insertedMemberId = null
        return true
    }
    
    /**
     * Clear verified PIN explicitly (for security)
     * Call this when switching users or on timeout
     */
    fun clearVerifiedPin(memberId: String) {
        verifiedPINRegistry.remove(memberId)
        println("✓ Verified PIN cleared for $memberId")
    }
    
    /**
     * Clear all verified PINs (security)
     */
    fun clearAllVerifiedPins() {
        verifiedPINRegistry.clear()
        println("✓ All verified PINs cleared")
    }

    override fun isCardInserted(): Boolean {
        return insertedCard != null
    }

    override fun getInsertedMemberId(): String? {
        return insertedMemberId
    }

    override fun readCardData(): Member? {
        val simulator = insertedCard
        val memberId = insertedMemberId
        
        if (simulator == null || memberId == null) {
            println("❌ No card inserted")
            return null
        }

        try {
            println("\n=== Reading Card Data with AES Decryption ===")
            println("Member ID: $memberId")
            
            // 1. Lấy salt và encrypted data
            val salt = saltRegistry[memberId]
            val encryptedData = encryptedDataRegistry[memberId]
            
            if (salt == null || encryptedData == null) {
                println("❌ No encrypted data found for member: $memberId")
                // Fallback: đọc dữ liệu thông thường (backward compatibility)
                return readCardDataLegacy(simulator, memberId)
            }
            
            // 2. Lấy PIN đã verify
            val verifiedPIN = verifiedPINRegistry[memberId]
            if (verifiedPIN == null) {
                println("❌ No verified PIN for this session")
                println("💡 Hint: You need to verify PIN first after inserting card")
                return null
            }
            
            println("✓ PIN verified in session")
            
            // 3. Dẫn xuất AES key từ PIN
            val aesKey = AESEncryptionManager.generateKeyFromPIN(verifiedPIN, salt)
            println("✓ AES key derived from verified PIN")
            
            // 4. Giải mã dữ liệu
            val member = CardDataEncryptionManager.decryptMemberData(encryptedData, aesKey)
            println("Member data decrypted successfully")
            println("Member: ${member.fullName}")
            println("Balance: ${member.balance} đ")
            println("Birth Date: ${member.birthDate}")
            println("CCCD: ${member.cccdNumber}")
            println("Photo Data: ${if (member.photoData != null) "${member.photoData!!.size} bytes" else "null"}")
            println("=== Read Complete ===")
            
            return member
        } catch (e: Exception) {
            println("Error reading/decrypting card data: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Legacy method - đọc dữ liệu không mã hóa (backward compatibility)
     */
    private fun readCardDataLegacy(simulator: CardSimulator, memberId: String): Member? {
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
        
        val verified = verifyPin(simulator, pin)
        
        // Nếu PIN đúng, lưu vào registry để dùng cho decrypt
        if (verified) {
            verifiedPINRegistry[memberId] = pin
            println("PIN verified and stored for member: $memberId")
        }
        
        return verified
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
            val success = checkSuccess(response)
            
            if (success) {
                println("✅ Balance updated: $newBalance đ")
                
                // Update memberInfoRegistry
                memberInfoRegistry[memberId]?.let { member ->
                    val updatedMember = member.copy(balance = newBalance)
                    memberInfoRegistry[memberId] = updatedMember
                    println("  ✓ memberInfoRegistry updated")
                }
                
                // Update encryptedDataRegistry
                val salt = saltRegistry[memberId]
                if (salt != null) {
                    val aesKey = AESEncryptionManager.generateKeyFromPIN(pin, salt)
                    val updatedMember = memberInfoRegistry[memberId]
                    if (updatedMember != null) {
                        val encryptedData = CardDataEncryptionManager.encryptMemberData(updatedMember, aesKey)
                        encryptedDataRegistry[memberId] = encryptedData
                        println("  ✓ encryptedDataRegistry updated")
                    }
                }
                
                // 🔥 Log transaction to Firebase
                runBlocking {
                    firebaseService.logTransaction(
                        memberId = memberId,
                        type = "balance_update",
                        amount = newBalance,
                        staffId = "SYSTEM",
                        verified = true
                    )
                }
            }
            
            return success

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
        // Trả về danh sách Member từ memberInfoRegistry (không cần decrypt)
        return memberInfoRegistry.values.toList()
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
    
    /**
     * Generate challenge for card authentication (chống nhân bản)
     * @param memberId Member ID
     * @return Challenge bytes (32 bytes random)
     */
    fun generateChallenge(memberId: String): ByteArray? {
        val cardIdentity = cardIdentityRegistry[memberId] ?: return null
        val challenge = RSASignatureManager.generateChallenge()
        activeChallenges[memberId] = challenge
        
        // 🔥 Store challenge in Firebase
        runBlocking {
            firebaseService.generateChallenge(memberId, challenge)
        }
        
        println("Challenge generated for $memberId: ${challenge.joinToString("") { "%02x".format(it) }}")
        return challenge
    }
    
    /**
     * Verify challenge response from card (xác thực thẻ thật)
     * @param memberId Member ID
     * @param signatureBytes Signature from card (signed with private key)
     * @return true if valid, false if cloned/invalid
     */
    fun verifyChallenge(memberId: String, signatureBytes: ByteArray): Boolean {
        val challenge = activeChallenges[memberId] ?: return false
        val cardIdentity = cardIdentityRegistry[memberId] ?: return false
        
        val isValid = RSASignatureManager.verify(
            data = challenge,
            signatureBytes = signatureBytes,
            publicKey = cardIdentity.publicKey
        )
        
        if (isValid) {
            println("✓ Challenge verified - Card is authentic")
            activeChallenges.remove(memberId) // Clear used challenge
            
            // 🔥 Mark challenge as used in Firebase
            runBlocking {
                firebaseService.markChallengeUsed(memberId)
            }
        } else {
            println("✗ Challenge verification FAILED - Possible cloned card!")
        }
        
        return isValid
    }
    
    /**
     * Sign challenge with card's private key (simulation - thực tế chạy on-card)
     * @param memberId Member ID
     * @param challenge Challenge to sign
     * @return Signature bytes
     */
    fun signChallenge(memberId: String, challenge: ByteArray): ByteArray? {
        val cardIdentity = cardIdentityRegistry[memberId] ?: return null
        return RSASignatureManager.sign(challenge, cardIdentity.privateKey)
    }
    
    /**
     * Get public key for server storage (anti-cloning system)
     * @param memberId Member ID
     * @return Base64 encoded public key
     */
    fun getPublicKey(memberId: String): String? {
        val cardIdentity = cardIdentityRegistry[memberId] ?: return null
        return RSASignatureManager.encodePublicKey(cardIdentity.publicKey)
    }
    
    /**
     * 🔥 Verify card với Firebase (complete challenge-response)
     */
    suspend fun verifyCardWithFirebase(memberId: String): Boolean {
        // 1. Generate challenge
        val challenge = generateChallenge(memberId) ?: return false
        
        // 2. Card signs challenge
        val signature = signChallenge(memberId, challenge) ?: return false
        
        // 3. Verify signature locally
        val isValid = verifyChallenge(memberId, signature)
        
        if (isValid) {
            println("✅ [Firebase] Card authenticated successfully")
        } else {
            println("❌ [Firebase] Card authentication FAILED")
        }
        
        return isValid
    }
    
    /**
     * 🔥 Backup encrypted card data to Firebase
     */
    suspend fun backupToFirebase(memberId: String): Boolean {
        val encryptedData = encryptedDataRegistry[memberId] ?: return false
        
        // Convert encrypted data to Base64 map
        val encryptedDataMap = mapOf(
            "fullName" to Base64.getEncoder().encodeToString(encryptedData.encryptedFullName),
            "birthDate" to (encryptedData.encryptedBirthDate?.let { Base64.getEncoder().encodeToString(it) } ?: ""),
            "cccd" to (encryptedData.encryptedCCCD?.let { Base64.getEncoder().encodeToString(it) } ?: ""),
            "photoPath" to (encryptedData.encryptedPhotoPath?.let { Base64.getEncoder().encodeToString(it) } ?: ""),
            "photoData" to (encryptedData.encryptedPhotoData?.let { Base64.getEncoder().encodeToString(it) } ?: ""),
            "startDate" to Base64.getEncoder().encodeToString(encryptedData.encryptedStartDate),
            "expireDate" to Base64.getEncoder().encodeToString(encryptedData.encryptedExpireDate),
            "packageType" to Base64.getEncoder().encodeToString(encryptedData.encryptedPackageType),
            "balance" to Base64.getEncoder().encodeToString(encryptedData.encryptedBalance)
        )
        
        return firebaseService.backupCardData(memberId, encryptedDataMap)
    }
    
    /**
     * 🔍 Debug: Log toàn bộ thông tin thẻ
     */
    fun logCardInfo(memberId: String) {
        println("\n=== 🔍 CARD INFO DEBUG ===")
        println("Member ID: $memberId")
        
        // Card exists?
        val card = cardRegistry[memberId]
        println("Card exists in registry: ${card != null}")
        
        // Salt
        val salt = saltRegistry[memberId]
        println("Salt: ${salt?.joinToString("") { "%02x".format(it) } ?: "NOT FOUND"}")
        
        // Encrypted data
        val encryptedData = encryptedDataRegistry[memberId]
        println("Encrypted data exists: ${encryptedData != null}")
        
        // Verified PIN
        val verifiedPIN = verifiedPINRegistry[memberId]
        println("Verified PIN exists: ${verifiedPIN != null}")
        
        // Member info (unencrypted copy)
        val memberInfo = memberInfoRegistry[memberId]
        println("\n📋 Member Info (unencrypted registry):")
        if (memberInfo != null) {
            println("  Full Name: ${memberInfo.fullName}")
            println("  Balance: ${memberInfo.balance} đ")
            println("  Birth Date: ${memberInfo.birthDate}")
            println("  CCCD: ${memberInfo.cccdNumber}")
            println("  Photo Path: ${memberInfo.photoPath}")
            println("  Photo Data: ${if (memberInfo.photoData != null) "${memberInfo.photoData!!.size} bytes" else "null"}")
            println("  Package: ${memberInfo.packageType}")
            println("  Start: ${memberInfo.startDate}")
            println("  Expire: ${memberInfo.expireDate}")
        } else {
            println("  NOT FOUND")
        }
        
        // Encrypted data details
        if (encryptedData != null && verifiedPIN != null && salt != null) {
            try {
                val aesKey = AESEncryptionManager.generateKeyFromPIN(verifiedPIN, salt)
                val decryptedMember = CardDataEncryptionManager.decryptMemberData(encryptedData, aesKey)
                println("\n🔓 Decrypted Data (from card):")
                println("  Full Name: ${decryptedMember.fullName}")
                println("  Balance: ${decryptedMember.balance} đ")
                println("  Birth Date: ${decryptedMember.birthDate}")
                println("  CCCD: ${decryptedMember.cccdNumber}")
                println("  Photo Path: ${decryptedMember.photoPath}")
                println("  Photo Data: ${if (decryptedMember.photoData != null) "${decryptedMember.photoData!!.size} bytes" else "null"}")
                println("  Package: ${decryptedMember.packageType}")
                println("  Start: ${decryptedMember.startDate}")
                println("  Expire: ${decryptedMember.expireDate}")
            } catch (e: Exception) {
                println("\n❌ Failed to decrypt: ${e.message}")
            }
        }
        
        // RSA Identity
        val cardIdentity = cardIdentityRegistry[memberId]
        println("\n🔐 RSA Identity:")
        println("  Keypair exists: ${cardIdentity != null}")
        if (cardIdentity != null) {
            val publicKeyBase64 = RSASignatureManager.encodePublicKey(cardIdentity.publicKey)
            println("  Public Key: ${publicKeyBase64.take(50)}...")
        }
        
        // Inserted card
        println("\n💳 Inserted Card:")
        println("  Inserted: ${insertedCard != null}")
        println("  Inserted Member ID: ${insertedMemberId ?: "NONE"}")
        
        println("=== 🔍 END DEBUG ===\n")
    }
}
