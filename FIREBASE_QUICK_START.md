# Firebase Setup Instructions

## 📋 Prerequisites
- Firebase account (free tier is enough)
- Java 17 or higher

## 🔥 Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **"Add project"**
3. Enter project name: `GymSmartcardApp` (or your choice)
4. Disable Google Analytics (optional)
5. Click **"Create project"**

## 📝 Step 2: Enable Firestore Database

1. In Firebase Console, go to **"Build" → "Firestore Database"**
2. Click **"Create database"**
3. Select **"Start in test mode"** (we'll set proper rules later)
4. Choose location: `asia-southeast1` (Singapore) or closest to you
5. Click **"Enable"**

## 🔑 Step 3: Download Service Account Credentials

1. Go to **"Project Settings"** (gear icon)
2. Click **"Service accounts"** tab
3. Click **"Generate new private key"**
4. Click **"Generate key"** in confirmation dialog
5. A JSON file will download (e.g., `gymsmartcardapp-xxxxx.json`)
6. **Rename it to `firebase-credentials.json`**
7. **Move it to project root** (same folder as `build.gradle.kts`)

⚠️ **IMPORTANT**: Never commit this file to Git! It's already in `.gitignore`

## 🛡️ Step 4: Set Security Rules

### Firestore Rules

1. Go to **"Firestore Database" → "Rules"** tab
2. Replace with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Cards collection - Public keys for anti-cloning
    match /cards/{memberId} {
      allow read: if true;  // Allow read for verification
      allow write: if request.auth != null;  // Only authenticated can write
    }
    
    // Challenges - Short-lived (60s TTL)
    match /challenges/{memberId} {
      allow read, write: if true;  // Temporary for development
    }
    
    // Transactions - Append-only log
    match /transactions/{txnId} {
      allow read: if true;
      allow create: if true;
      allow update, delete: if false;  // No modifications
    }
    
    // Backups - Admin only (implement auth later)
    match /members_backup/{memberId} {
      allow read, write: if true;  // Temporary for development
    }
  }
}
```

3. Click **"Publish"**

## ✅ Step 5: Verify Setup

1. Make sure `firebase-credentials.json` is in project root
2. Run the application:
   ```bash
   .\gradlew.bat run
   ```
3. Check console output for:
   ```
   🔥 Initializing Firebase...
   ✅ Firebase initialized successfully
   ```

If you see this, Firebase is working! 🎉

## 🧪 Test Firebase Integration

### Test 1: Create Card
1. Open Admin view
2. Create a new card
3. Check console for:
   ```
   ✅ [Firebase] Public key stored for ID12345
   ```
4. Verify in Firebase Console:
   - Go to Firestore Database
   - You should see `cards` collection with your member ID

### Test 2: Transaction Logging
1. Update member balance
2. Check console for:
   ```
   ✅ [Firebase] Transaction logged: TXN_xxxxx
   ```
3. Verify in Firebase Console:
   - `transactions` collection should have new entry

## 🔒 Production Security Rules

For production, update Firestore rules with proper authentication:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function - check if user is admin
    function isAdmin() {
      return request.auth != null && request.auth.token.admin == true;
    }
    
    // Cards - Read for verification, Admin write only
    match /cards/{memberId} {
      allow read: if request.auth != null;
      allow create, update: if isAdmin();
      allow delete: if false;
    }
    
    // Challenges - Short-lived, authenticated only
    match /challenges/{memberId} {
      allow read, write: if request.auth != null;
    }
    
    // Transactions - Append-only, authenticated
    match /transactions/{txnId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if false;
    }
    
    // Backups - Admin only
    match /members_backup/{memberId} {
      allow read, write: if isAdmin();
    }
  }
}
```

## 📊 Database Structure

### Collection: `cards`
```json
{
  "memberId": "ID12345",
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI...",
  "createdAt": 1701436800000,
  "lastVerified": 1701523200000,
  "status": "active"
}
```

### Collection: `challenges`
```json
{
  "memberId": "ID12345",
  "challenge": "8f3a9c2d1e4b5a6f7c8d9e0f1a2b3c4d...",
  "createdAt": 1701523200000,
  "expiresAt": 1701523260000,
  "used": false
}
```

### Collection: `transactions`
```json
{
  "transactionId": "TXN_1701523200000_ID12345",
  "memberId": "ID12345",
  "type": "topup",
  "amount": 50000,
  "timestamp": 1701523200000,
  "staffId": "STAFF01",
  "verified": true
}
```

### Collection: `members_backup`
```json
{
  "memberId": "ID12345",
  "encryptedData": {
    "fullName": "base64_encrypted...",
    "balance": "base64_encrypted...",
    "photoData": "base64_encrypted..."
  },
  "lastBackup": 1701523200000
}
```

## 🆘 Troubleshooting

### Error: "Firebase initialization failed"
- ✅ Check `firebase-credentials.json` is in project root
- ✅ Check file is valid JSON (no syntax errors)
- ✅ Check Firebase project is active

### Error: "Permission denied"
- ✅ Check Firestore rules allow the operation
- ✅ For development, use test mode rules (allow read, write: if true)

### Warning: "Firebase not available"
- App will work in local mode (no cloud backup)
- Create `firebase-credentials.json` to enable Firebase

## 📈 Monitoring

### View Data in Firebase Console
1. Go to **Firestore Database**
2. Browse collections: `cards`, `challenges`, `transactions`, `members_backup`
3. Click any document to see details

### View Logs
1. Console output shows all Firebase operations:
   - ✅ Success: Green checkmark
   - ❌ Error: Red X with error message
   - ⚠️ Warning: Yellow triangle

## 🎯 Free Tier Limits

Firebase free tier (Spark plan) includes:
- ✅ 1 GB stored data
- ✅ 50K reads/day
- ✅ 20K writes/day
- ✅ 20K deletes/day
- ✅ 10 GB/month data transfer

**More than enough for gym use case!** (~100-1000 members)

## 🚀 Next Steps

After Firebase is working:
1. ✅ Test challenge-response authentication
2. ✅ Test transaction logging
3. ✅ Test card data backup
4. ✅ Set up proper security rules
5. ✅ Add Firebase Authentication for admin/staff login

---

**Questions?** Check Firebase Console logs or console output for detailed error messages.
