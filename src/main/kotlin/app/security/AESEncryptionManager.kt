package app.security

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES Encryption Manager
 * Generates AES-256 key from PIN using PBKDF2, encrypts/decrypts card data
 */
class AESEncryptionManager {
    
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256 // AES-256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val SALT_LENGTH = 16
        
        /**
         * Generate AES key from PIN using PBKDF2
         * @param pin 4-digit PIN
         * @param salt Salt for key derivation (16 bytes)
         * @return AES-256 SecretKey
         */
        fun generateKeyFromPIN(pin: String, salt: ByteArray): SecretKey {
            // Sử dụng PBKDF2 để derive key từ PIN
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = javax.crypto.spec.PBEKeySpec(
                pin.toCharArray(),
                salt,
                10000, // 10,000 iterations
                KEY_SIZE
            )
            val tmp = factory.generateSecret(spec)
            return SecretKeySpec(tmp.encoded, ALGORITHM)
        }
        
        /**
         * Generate random salt
         * @return 16-byte random salt
         */
        fun generateSalt(): ByteArray {
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            return salt
        }
        
        /**
         * Encrypt data with AES-GCM
         * @param plaintext Data to encrypt
         * @param key AES key
         * @return Encrypted data (IV + ciphertext + tag)
         */
        fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Generate random IV
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
            val ciphertext = cipher.doFinal(plaintext)
            
            // Combine IV + ciphertext
            return iv + ciphertext
        }
        
        /**
         * Decrypt data with AES-GCM
         * @param ciphertext Encrypted data (IV + ciphertext + tag)
         * @param key AES key
         * @return Decrypted plaintext
         */
        fun decrypt(ciphertext: ByteArray, key: SecretKey): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Extract IV (first 12 bytes)
            val iv = ciphertext.sliceArray(0 until GCM_IV_LENGTH)
            val actualCiphertext = ciphertext.sliceArray(GCM_IV_LENGTH until ciphertext.size)
            
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            
            return cipher.doFinal(actualCiphertext)
        }
        
        /**
         * Encrypt string
         */
        fun encryptString(plaintext: String, key: SecretKey): ByteArray {
            return encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        }
        
        /**
         * Decrypt string
         */
        fun decryptString(ciphertext: ByteArray, key: SecretKey): String {
            return String(decrypt(ciphertext, key), Charsets.UTF_8)
        }
        
        /**
         * Encrypt Long value
         */
        fun encryptLong(value: Long, key: SecretKey): ByteArray {
            val buffer = ByteBuffer.allocate(8)
            buffer.putLong(value)
            return encrypt(buffer.array(), key)
        }
        
        /**
         * Decrypt Long value
         */
        fun decryptLong(ciphertext: ByteArray, key: SecretKey): Long {
            val plaintext = decrypt(ciphertext, key)
            val buffer = ByteBuffer.wrap(plaintext)
            return buffer.long
        }
    }
}
