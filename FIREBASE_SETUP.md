# Firebase Setup Guide - Gym Smartcard App

## 🔥 Firebase Services Recommended

### 1. **Firebase Realtime Database** hoặc **Firestore**
- Store public keys (RSA)
- Store member info (encrypted)
- Store transaction logs
- Real-time sync

### 2. **Firebase Authentication**
- Admin login
- Staff login
- Role-based access control

### 3. **Firebase Cloud Storage**
- Store photos (backup từ card)
- Store transaction receipts
- Store audit logs

### 4. **Firebase Cloud Functions**
- Challenge generation API
- Signature verification API
- Payment processing
- Automated backups

---

## 📦 Project Dependencies

Add to `build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...
    
    // Firebase Admin SDK (for server-side operations)
    implementation("com.google.firebase:firebase-admin:9.2.0")
    
    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    
    // Firebase Firestore (alternative to Realtime DB)
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")
    
    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
    
    // Firebase Auth
    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
```

---

## 🗄️ Database Structure (Firestore)

### Collection: `cards`
```json
{
  "memberId": "ID12345",
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI...",  // RSA public key
  "createdAt": "2024-12-02T10:30:00Z",
  "lastVerified": "2024-12-02T15:45:00Z",
  "status": "active"  // active, suspended, expired
}
```

### Collection: `challenges`
```json
{
  "memberId": "ID12345",
  "challenge": "8f3a9c2d1e4b5a6f...",  // Base64 encoded
  "createdAt": "2024-12-02T15:45:00Z",
  "expiresAt": "2024-12-02T15:46:00Z",  // TTL 60 seconds
  "used": false
}
```

### Collection: `transactions`
```json
{
  "transactionId": "TXN001",
  "memberId": "ID12345",
  "type": "topup",  // topup, payment, checkin
  "amount": 50000,
  "timestamp": "2024-12-02T15:45:00Z",
  "staffId": "STAFF01",
  "verified": true  // RSA signature verified
}
```

### Collection: `members_backup`
```json
{
  "memberId": "ID12345",
  "encryptedData": {
    "fullName": "encrypted_base64...",
    "balance": "encrypted_base64...",
    "photoData": "encrypted_base64..."  // Backup from card
  },
  "lastBackup": "2024-12-02T15:45:00Z"
}
```

---

## 🔧 Firebase Service Implementation

### Create: `FirebaseService.kt`

```kotlin
package app.service.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileInputStream
import java.util.*

/**
 * Firebase Service
 * Kết nối với Firebase Backend cho:
 * - Public key storage (anti-cloning)
 * - Challenge-response verification
 * - Transaction logging
 * - Photo backup
 */
class FirebaseService {
    
    private var firestore: FirebaseFirestore? = null
    private var database: Database? = null
    private var storage: FirebaseStorage? = null
    private var auth: FirebaseAuth? = null
    
    companion object {
        private var instance: FirebaseService? = null
        
        fun getInstance(): FirebaseService {
            if (instance == null) {
                instance = FirebaseService()
            }
            return instance!!
        }
    }
    
    /**
     * Initialize Firebase với service account
     * Download JSON từ Firebase Console > Project Settings > Service Accounts
     */
    fun initialize(serviceAccountPath: String) {
        try {
            val serviceAccount = FileInputStream(serviceAccountPath)
            val credentials = GoogleCredentials.fromStream(serviceAccount)
            
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setDatabaseUrl("https://your-project-id-default-rtdb.firebaseio.com")
                .setStorageBucket("your-project-id.appspot.com")
                .build()
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }
            
            firestore = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()
            auth = FirebaseAuth.getInstance()
            
            println("✅ Firebase initialized successfully")
        } catch (e: Exception) {
            println("❌ Firebase initialization failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Store public key khi tạo thẻ mới
     */
    suspend fun storePublicKey(memberId: String, publicKeyBase64: String): Boolean {
        return try {
            val cardData = hashMapOf(
                "memberId" to memberId,
                "publicKey" to publicKeyBase64,
                "createdAt" to System.currentTimeMillis(),
                "status" to "active"
            )
            
            firestore?.collection("cards")
                ?.document(memberId)
                ?.set(cardData)
                ?.await()
            
            println("✅ Public key stored for $memberId")
            true
        } catch (e: Exception) {
            println("❌ Failed to store public key: ${e.message}")
            false
        }
    }
    
    /**
     * Get public key để verify signature
     */
    suspend fun getPublicKey(memberId: String): String? {
        return try {
            val doc = firestore?.collection("cards")
                ?.document(memberId)
                ?.get()
                ?.await()
            
            doc?.getString("publicKey")
        } catch (e: Exception) {
            println("❌ Failed to get public key: ${e.message}")
            null
        }
    }
    
    /**
     * Generate và store challenge
     */
    suspend fun generateChallenge(memberId: String, challengeBytes: ByteArray): Boolean {
        return try {
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
                ?.set(challengeData)
                ?.await()
            
            println("✅ Challenge generated for $memberId")
            true
        } catch (e: Exception) {
            println("❌ Failed to generate challenge: ${e.message}")
            false
        }
    }
    
    /**
     * Verify challenge và mark as used
     */
    suspend fun verifyChallenge(memberId: String): Pair<ByteArray?, Boolean> {
        return try {
            val doc = firestore?.collection("challenges")
                ?.document(memberId)
                ?.get()
                ?.await()
            
            if (doc == null || !doc.exists()) {
                return null to false
            }
            
            val used = doc.getBoolean("used") ?: true
            val expiresAt = doc.getLong("expiresAt") ?: 0
            val now = System.currentTimeMillis()
            
            if (used || now > expiresAt) {
                println("❌ Challenge expired or already used")
                return null to false
            }
            
            val challengeBase64 = doc.getString("challenge") ?: return null to false
            val challengeBytes = Base64.getDecoder().decode(challengeBase64)
            
            // Mark as used
            firestore?.collection("challenges")
                ?.document(memberId)
                ?.update("used", true)
                ?.await()
            
            challengeBytes to true
        } catch (e: Exception) {
            println("❌ Failed to verify challenge: ${e.message}")
            null to false
        }
    }
    
    /**
     * Log transaction
     */
    suspend fun logTransaction(
        memberId: String,
        type: String,
        amount: Long,
        staffId: String,
        verified: Boolean
    ): Boolean {
        return try {
            val txnId = "TXN_${System.currentTimeMillis()}"
            
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
                ?.set(txnData)
                ?.await()
            
            println("✅ Transaction logged: $txnId")
            true
        } catch (e: Exception) {
            println("❌ Failed to log transaction: ${e.message}")
            false
        }
    }
    
    /**
     * Backup encrypted data từ card
     */
    suspend fun backupCardData(
        memberId: String,
        encryptedDataMap: Map<String, String>
    ): Boolean {
        return try {
            val backupData = hashMapOf(
                "memberId" to memberId,
                "encryptedData" to encryptedDataMap,
                "lastBackup" to System.currentTimeMillis()
            )
            
            firestore?.collection("members_backup")
                ?.document(memberId)
                ?.set(backupData)
                ?.await()
            
            println("✅ Card data backed up for $memberId")
            true
        } catch (e: Exception) {
            println("❌ Failed to backup card data: ${e.message}")
            false
        }
    }
    
    /**
     * Upload photo to Firebase Storage
     */
    suspend fun uploadPhoto(memberId: String, photoBytes: ByteArray): String? {
        return try {
            val fileName = "photos/${memberId}_${System.currentTimeMillis()}.png"
            val storageRef = storage?.reference?.child(fileName)
            
            storageRef?.putBytes(photoBytes)?.await()
            
            val downloadUrl = storageRef?.downloadUrl?.await()
            
            println("✅ Photo uploaded: $downloadUrl")
            downloadUrl?.toString()
        } catch (e: Exception) {
            println("❌ Failed to upload photo: ${e.message}")
            null
        }
    }
}

// Extension function for await()
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.tasks.await(this)
}
```

---

## 🔐 Firebase Security Rules

### Firestore Rules (`firestore.rules`)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Cards collection - Read/Write only by server
    match /cards/{memberId} {
      allow read: if request.auth != null;
      allow write: if request.auth.token.admin == true;
    }
    
    // Challenges - Short-lived, server only
    match /challenges/{memberId} {
      allow read, write: if request.auth != null;
    }
    
    // Transactions - Append only, no delete
    match /transactions/{txnId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if false;
    }
    
    // Backups - Admin only
    match /members_backup/{memberId} {
      allow read, write: if request.auth.token.admin == true;
    }
  }
}
```

### Storage Rules (`storage.rules`)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Photos - Admin can upload, anyone authenticated can read
    match /photos/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth.token.admin == true;
    }
  }
}
```

---

## 🚀 Integration với JCardSimService

Update `JCardSimService.kt`:

```kotlin
class JCardSimService : SmartcardService {
    
    private val firebaseService = FirebaseService.getInstance()
    
    override fun createCard(member: Member, pin: String): Boolean {
        try {
            // ... existing code ...
            
            // 4. Generate RSA keypair cho chống nhân bản thẻ
            val keyPair = RSASignatureManager.generateKeyPair()
            val cardIdentity = CardIdentity(
                memberId = member.memberId,
                privateKey = keyPair.private,
                publicKey = keyPair.public
            )
            cardIdentityRegistry[member.memberId] = cardIdentity
            
            // ✅ Store public key to Firebase
            val publicKeyBase64 = RSASignatureManager.encodePublicKey(keyPair.public)
            runBlocking {
                firebaseService.storePublicKey(member.memberId, publicKeyBase64)
            }
            
            // ✅ Upload photo to Firebase Storage (backup)
            if (member.photoData != null) {
                runBlocking {
                    firebaseService.uploadPhoto(member.memberId, member.photoData!!)
                }
            }
            
            // ... rest of code ...
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Verify card với Firebase challenge
     */
    suspend fun verifyCardWithFirebase(memberId: String): Boolean {
        // 1. Generate challenge
        val challenge = RSASignatureManager.generateChallenge()
        
        // 2. Store challenge in Firebase
        if (!firebaseService.generateChallenge(memberId, challenge)) {
            return false
        }
        
        // 3. Card signs challenge
        val signature = signChallenge(memberId, challenge) ?: return false
        
        // 4. Get public key from Firebase
        val publicKeyBase64 = firebaseService.getPublicKey(memberId) ?: return false
        val publicKey = RSASignatureManager.decodePublicKey(publicKeyBase64)
        
        // 5. Verify signature
        val isValid = RSASignatureManager.verify(challenge, signature, publicKey)
        
        // 6. Mark challenge as used
        firebaseService.verifyChallenge(memberId)
        
        return isValid
    }
}
```

---

## 📱 Alternative: Supabase (Open Source Firebase)

Nếu không muốn dùng Firebase, có thể dùng **Supabase**:

### Advantages
- ✅ Open source (self-hostable)
- ✅ PostgreSQL backend (powerful queries)
- ✅ Built-in REST API
- ✅ Real-time subscriptions
- ✅ Row-level security
- ✅ Free tier generous hơn Firebase

### Supabase Dependencies

```kotlin
dependencies {
    // Supabase Kotlin Client
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")
    
    // Ktor for HTTP
    implementation("io.ktor:ktor-client-cio:2.3.7")
}
```

### Supabase Table Schema

```sql
-- Cards table
CREATE TABLE cards (
    member_id TEXT PRIMARY KEY,
    public_key TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    last_verified TIMESTAMP,
    status TEXT DEFAULT 'active'
);

-- Challenges table (with TTL)
CREATE TABLE challenges (
    member_id TEXT PRIMARY KEY,
    challenge TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE
);

-- Transactions table
CREATE TABLE transactions (
    transaction_id TEXT PRIMARY KEY,
    member_id TEXT REFERENCES cards(member_id),
    type TEXT NOT NULL,
    amount BIGINT NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    staff_id TEXT,
    verified BOOLEAN DEFAULT FALSE
);

-- Auto-delete expired challenges
CREATE OR REPLACE FUNCTION delete_expired_challenges()
RETURNS void AS $$
BEGIN
    DELETE FROM challenges WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;
```

---

## 🎯 Recommendation

### For this project, I recommend:

**🔥 Firebase Firestore** because:
1. ✅ Easy setup (no server management)
2. ✅ Real-time sync
3. ✅ Good free tier (50K reads/day, 20K writes/day)
4. ✅ Automatic scaling
5. ✅ Built-in security rules
6. ✅ Good Kotlin SDK support

### Setup Steps:

1. **Create Firebase Project**
   - Go to https://console.firebase.google.com
   - Create new project: "GymSmartcardApp"

2. **Enable Firestore**
   - Firestore Database → Create database
   - Start in test mode (change rules later)

3. **Download Service Account**
   - Project Settings → Service Accounts
   - Generate new private key (JSON)
   - Save to `firebase-credentials.json`

4. **Add to `.gitignore`**
   ```
   firebase-credentials.json
   google-services.json
   ```

5. **Initialize in Main.kt**
   ```kotlin
   fun main() {
       // Initialize Firebase
       FirebaseService.getInstance().initialize("firebase-credentials.json")
       
       // Start app
       application {
           // ...
       }
   }
   ```

Làm Firebase setup không? 🔥