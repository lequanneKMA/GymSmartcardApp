package app.security

import app.model.Member
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.crypto.SecretKey

/**
 * Card Data Encryption Manager
 * Handles encryption/decryption of Member data on smartcard
 */
class CardDataEncryptionManager {
    
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        /**
         * Encrypt Member data to store on card
         * @param member Member data
         * @param key AES key derived from PIN
         * @return Encrypted card data
         */
        fun encryptMemberData(member: Member, key: SecretKey): EncryptedCardData {
            return EncryptedCardData(
                memberId = member.memberId, // ID không mã hóa (cần để tìm thẻ)
                encryptedFullName = AESEncryptionManager.encryptString(member.fullName, key),
                encryptedBirthDate = member.birthDate?.let { 
                    AESEncryptionManager.encryptString(it.format(dateFormatter), key) 
                },
                encryptedCCCD = member.cccdNumber?.let {
                    AESEncryptionManager.encryptString(it, key)
                },
                encryptedPhotoPath = member.photoPath?.let {
                    AESEncryptionManager.encryptString(it, key)
                },
                encryptedPhotoData = member.photoData?.let {
                    AESEncryptionManager.encrypt(it, key) // Encrypt binary photo data
                },
                encryptedStartDate = AESEncryptionManager.encryptString(
                    member.startDate.format(dateFormatter), key
                ),
                encryptedExpireDate = AESEncryptionManager.encryptString(
                    member.expireDate.format(dateFormatter), key
                ),
                encryptedPackageType = AESEncryptionManager.encryptString(member.packageType, key),
                encryptedBalance = AESEncryptionManager.encryptLong(member.balance, key)
            )
        }
        
        /**
         * Decrypt card data to Member object
         * @param encryptedData Encrypted card data
         * @param key AES key derived from PIN
         * @return Decrypted Member object
         */
        fun decryptMemberData(encryptedData: EncryptedCardData, key: SecretKey): Member {
            return Member(
                memberId = encryptedData.memberId,
                fullName = AESEncryptionManager.decryptString(encryptedData.encryptedFullName, key),
                birthDate = encryptedData.encryptedBirthDate?.let {
                    LocalDate.parse(AESEncryptionManager.decryptString(it, key), dateFormatter)
                },
                cccdNumber = encryptedData.encryptedCCCD?.let {
                    AESEncryptionManager.decryptString(it, key)
                },
                photoPath = encryptedData.encryptedPhotoPath?.let {
                    AESEncryptionManager.decryptString(it, key)
                },
                photoData = encryptedData.encryptedPhotoData?.let {
                    AESEncryptionManager.decrypt(it, key) // Decrypt binary photo data
                },
                startDate = LocalDate.parse(
                    AESEncryptionManager.decryptString(encryptedData.encryptedStartDate, key),
                    dateFormatter
                ),
                expireDate = LocalDate.parse(
                    AESEncryptionManager.decryptString(encryptedData.encryptedExpireDate, key),
                    dateFormatter
                ),
                packageType = AESEncryptionManager.decryptString(encryptedData.encryptedPackageType, key),
                balance = AESEncryptionManager.decryptLong(encryptedData.encryptedBalance, key)
            )
        }
    }
}

/**
 * Encrypted card data structure
 */
data class EncryptedCardData(
    val memberId: String,                    // Không mã hóa (ID công khai)
    val encryptedFullName: ByteArray,        // Họ tên (encrypted)
    val encryptedBirthDate: ByteArray?,      // Ngày sinh (encrypted)
    val encryptedCCCD: ByteArray?,           // Số CCCD (encrypted)
    val encryptedPhotoPath: ByteArray?,      // Đường dẫn ảnh (encrypted) - legacy
    val encryptedPhotoData: ByteArray?,      // Dữ liệu ảnh (encrypted) - LƯU TRÊN THẺ
    val encryptedStartDate: ByteArray,       // Ngày bắt đầu (encrypted)
    val encryptedExpireDate: ByteArray,      // Ngày hết hạn (encrypted)
    val encryptedPackageType: ByteArray,     // Gói tập (encrypted)
    val encryptedBalance: ByteArray          // Số dư (encrypted)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedCardData

        if (memberId != other.memberId) return false
        if (!encryptedFullName.contentEquals(other.encryptedFullName)) return false
        if (encryptedBirthDate != null) {
            if (other.encryptedBirthDate == null) return false
            if (!encryptedBirthDate.contentEquals(other.encryptedBirthDate)) return false
        } else if (other.encryptedBirthDate != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = memberId.hashCode()
        result = 31 * result + encryptedFullName.contentHashCode()
        result = 31 * result + (encryptedBirthDate?.contentHashCode() ?: 0)
        return result
    }
}
