package app.security

import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * RSA Signature Manager
 * Chống clone thẻ bằng digital signature với challenge-response protocol
 */
class RSASignatureManager {
    
    companion object {
        private const val ALGORITHM = "RSA"
        private const val KEY_SIZE = 2048
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        
        /**
         * Generate RSA keypair cho thẻ mới
         * @return Pair(privateKey, publicKey)
         */
        fun generateKeyPair(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
            keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
            return keyPairGenerator.generateKeyPair()
        }
        
        /**
         * Sign data với private key (thực hiện trên thẻ)
         * @param data Data cần ký
         * @param privateKey Private key của thẻ
         * @return Signature bytes
         */
        fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(privateKey)
            signature.update(data)
            return signature.sign()
        }
        
        /**
         * Verify signature với public key (thực hiện trên server)
         * @param data Data gốc
         * @param signatureBytes Signature cần verify
         * @param publicKey Public key của thẻ
         * @return true nếu signature hợp lệ
         */
        fun verify(data: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
            return try {
                val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
                signature.initVerify(publicKey)
                signature.update(data)
                signature.verify(signatureBytes)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        
        /**
         * Generate random challenge (server gửi cho thẻ)
         * @return Random 32-byte challenge
         */
        fun generateChallenge(): ByteArray {
            val challenge = ByteArray(32)
            SecureRandom().nextBytes(challenge)
            return challenge
        }
        
        /**
         * Encode public key thành Base64 string để lưu
         */
        fun encodePublicKey(publicKey: PublicKey): String {
            return Base64.getEncoder().encodeToString(publicKey.encoded)
        }
        
        /**
         * Decode public key từ Base64 string
         */
        fun decodePublicKey(encodedKey: String): PublicKey {
            val keyBytes = Base64.getDecoder().decode(encodedKey)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            return keyFactory.generatePublic(keySpec)
        }
        
        /**
         * Challenge-Response Protocol
         * 
         * Flow:
         * 1. Server tạo random challenge
         * 2. Gửi challenge cho thẻ
         * 3. Thẻ ký challenge bằng private key
         * 4. Gửi signature về server
         * 5. Server verify bằng public key
         * 
         * → Nếu verify thành công = thẻ thật (có private key)
         * → Nếu fail = thẻ giả (không có private key)
         */
    }
}

/**
 * Card Identity
 * Lưu RSA keypair cho mỗi thẻ
 */
data class CardIdentity(
    val memberId: String,
    val privateKey: PrivateKey,     // Lưu trên thẻ (không export)
    val publicKey: PublicKey        // Lưu trên server
)
