package app.service.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Firebase Service
 * Kết nối với Firebase Backend cho:
 * - Public key storage (anti-cloning)
 * - Challenge-response verification
 * - Transaction logging
 * - Card data backup
 */
class FirebaseService private constructor() {
    
    private var firestore: Firestore? = null
    private var isInitialized = false
    
    companion object {
        @Volatile
        private var instance: FirebaseService? = null
        
        fun getInstance(): FirebaseService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseService().also { instance = it }
            }
        }
    }
    
    /**
     * Initialize Firebase với service account
     * Download JSON từ Firebase Console > Project Settings > Service Accounts
     * @param serviceAccountPath Path to firebase-credentials.json
     */
    fun initialize(serviceAccountPath: String? = null) {
        try {
            if (isInitialized) {
                println("⚠️ Firebase already initialized")
                return
            }
            
            val options = if (serviceAccountPath != null) {
                // Production mode with service account
                val serviceAccount = FileInputStream(serviceAccountPath)
                val credentials = GoogleCredentials.fromStream(serviceAccount)
                
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()
            } else {
                // Development mode - use default credentials
                println("⚠️ Running in development mode without Firebase credentials")
                println("⚠️ Firebase features will be disabled")
                isInitialized = false
                return
            }
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
            
            firestore = FirestoreClient.getFirestore()
            isInitialized = true
            
            println("✅ Firebase initialized successfully")
        } catch (e: Exception) {
            println("❌ Firebase initialization failed: ${e.message}")
            println("⚠️ Running without Firebase backend")
            isInitialized = false
        }
    }
    
    /**
     * Check if Firebase is available
     */
    fun isAvailable(): Boolean = isInitialized && firestore != null
    
    /**
     * Store public key khi tạo thẻ mới
     */
    suspend fun storePublicKey(memberId: String, publicKeyBase64: String): Boolean {
        if (!isAvailable()) {
            println("⚠️ Firebase not available, skipping public key storage")
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val cardData = hashMapOf(
                    "memberId" to memberId,
                    "publicKey" to publicKeyBase64,
                    "createdAt" to System.currentTimeMillis(),
                    "lastVerified" to System.currentTimeMillis(),
                    "status" to "active"
                )
                
                firestore?.collection("cards")
                    ?.document(memberId)
                    ?.set(cardData as Map<String, Any>)
                    ?.get(10, TimeUnit.SECONDS)
                
                println("✅ [Firebase] Public key stored for $memberId")
                true
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to store public key: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get public key để verify signature
     */
    suspend fun getPublicKey(memberId: String): String? {
        if (!isAvailable()) return null
        
        return withContext(Dispatchers.IO) {
            try {
                val doc = firestore?.collection("cards")
                    ?.document(memberId)
                    ?.get()
                    ?.get(10, TimeUnit.SECONDS)
                
                val publicKey = doc?.getString("publicKey")
                
                if (publicKey != null) {
                    // Update last verified timestamp
                    firestore?.collection("cards")
                        ?.document(memberId)
                        ?.update("lastVerified", System.currentTimeMillis())
                        ?.get(5, TimeUnit.SECONDS)
                }
                
                publicKey
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to get public key: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Generate và store challenge
     */
    suspend fun generateChallenge(memberId: String, challengeBytes: ByteArray): Boolean {
        if (!isAvailable()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                val challengeBase64 = Base64.getEncoder().encodeToString(challengeBytes)
                val now = System.currentTimeMillis()
                
                val challengeData = hashMapOf(
                    "memberId" to memberId,
                    "challenge" to challengeBase64,
                    "createdAt" to now,
                    "expiresAt" to (now + 60000), // TTL 60 seconds
                    "used" to false
                )
                
                firestore?.collection("challenges")
                    ?.document(memberId)
                    ?.set(challengeData as Map<String, Any>)
                    ?.get(10, TimeUnit.SECONDS)
                
                println("✅ [Firebase] Challenge generated for $memberId")
                true
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to generate challenge: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get và verify challenge (check expiry và used status)
     */
    suspend fun getChallenge(memberId: String): ByteArray? {
        if (!isAvailable()) return null
        
        return withContext(Dispatchers.IO) {
            try {
                val doc = firestore?.collection("challenges")
                    ?.document(memberId)
                    ?.get()
                    ?.get(10, TimeUnit.SECONDS)
                
                if (doc == null || !doc.exists()) {
                    println("❌ [Firebase] No challenge found for $memberId")
                    return@withContext null
                }
                
                val used = doc.getBoolean("used") ?: true
                val expiresAt = doc.getLong("expiresAt") ?: 0
                val now = System.currentTimeMillis()
                
                if (used) {
                    println("❌ [Firebase] Challenge already used")
                    return@withContext null
                }
                
                if (now > expiresAt) {
                    println("❌ [Firebase] Challenge expired")
                    return@withContext null
                }
                
                val challengeBase64 = doc.getString("challenge") ?: return@withContext null
                Base64.getDecoder().decode(challengeBase64) as ByteArray
                
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to get challenge: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Mark challenge as used
     */
    suspend fun markChallengeUsed(memberId: String): Boolean {
        if (!isAvailable()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                firestore?.collection("challenges")
                    ?.document(memberId)
                    ?.update("used", true)
                    ?.get(5, TimeUnit.SECONDS)
                
                println("✅ [Firebase] Challenge marked as used for $memberId")
                true
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to mark challenge as used: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Log transaction
     */
    suspend fun logTransaction(
        memberId: String,
        type: String, // topup, payment, checkin
        amount: Long,
        staffId: String,
        verified: Boolean
    ): Boolean {
        if (!isAvailable()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                val txnId = "TXN_${System.currentTimeMillis()}_${memberId}"
                
                val txnData = hashMapOf(
                    "transactionId" to txnId,
                    "memberId" to memberId,
                    "type" to type,
                    "amount" to amount,
                    "timestamp" to System.currentTimeMillis(),
                    "staffId" to staffId,
                    "verified" to verified
                )
                
                firestore?.collection("transactions")
                    ?.document(txnId)
                    ?.set(txnData as Map<String, Any>)
                    ?.get(10, TimeUnit.SECONDS)
                
                println("✅ [Firebase] Transaction logged: $txnId ($type, ${amount}đ, verified=$verified)")
                true
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to log transaction: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Backup encrypted data từ card
     */
    suspend fun backupCardData(
        memberId: String,
        encryptedDataMap: Map<String, String>
    ): Boolean {
        if (!isAvailable()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                val backupData = hashMapOf(
                    "memberId" to memberId,
                    "encryptedData" to encryptedDataMap,
                    "lastBackup" to System.currentTimeMillis()
                )
                
                firestore?.collection("members_backup")
                    ?.document(memberId)
                    ?.set(backupData as Map<String, Any>)
                    ?.get(10, TimeUnit.SECONDS)
                
                println("✅ [Firebase] Card data backed up for $memberId")
                true
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to backup card data: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get transaction history
     */
    suspend fun getTransactionHistory(memberId: String, limit: Int = 50): List<Map<String, Any>> {
        if (!isAvailable()) return emptyList()
        
        return withContext(Dispatchers.IO) {
            try {
                val querySnapshot = firestore?.collection("transactions")
                    ?.whereEqualTo("memberId", memberId)
                    ?.orderBy("timestamp", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    ?.limit(limit)
                    ?.get()
                    ?.get(30, TimeUnit.SECONDS)
                
                querySnapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to get transaction history: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Update card status (active, suspended, expired)
     */
    suspend fun updateCardStatus(memberId: String, status: String): Boolean {
        if (!isAvailable()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                firestore?.collection("cards")
                    ?.document(memberId)
                    ?.update("status", status)
                    ?.get(5, TimeUnit.SECONDS)
                
                println("✅ [Firebase] Card status updated: $memberId → $status")
                true
            } catch (e: Exception) {
                println("❌ [Firebase] Failed to update card status: ${e.message}")
                false
            }
        }
    }
}
