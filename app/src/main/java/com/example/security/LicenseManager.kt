package com.example.security

import android.content.Context
import android.os.Environment
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Enterprise Offline License Manager for Grain Trade Manager.
 *
 * Security Features:
 * - Asymmetric Cryptography: Only the developer's private Ed25519 key (isolated in developer-license-authority)
 *   can issue licenses. Decompiling the APK only reveals the public key, making unauthorized key generation mathematically impossible.
 * - Hardware Binding: Binds strictly to device hardware attributes.
 * - Multi-Layer Permanent Persistence: Saves in SharedPreferences and external document backups so that the license
 *   survives app updates, OS reboots, and app reinstalls.
 * - Anti-Rollback Clock Protection: Detects system time tampering.
 */
object LicenseManager {

    private const val PREFS_NAME = "gtm_secure_license_vault"
    private const val KEY_RAW_LICENSE = "license_raw_key"
    private const val KEY_LAST_CHECK_TS = "license_last_verified_ts"
    private const val BACKUP_FILENAME = ".gtm_trade_manager_license.dat"

    // Master Public Key for Eng. Ahmed Attia / UltraSoft License Authority v6.0
    // Raw 32 bytes hex derived from the developer's Ed25519 Authority Key
    private const val MASTER_PUBLIC_KEY_HEX = "7fa8b86c9a372f2304e2b9bfd56b72299fced510e070e2a66b0a50b4487c3dec"

    private val masterPublicKeyBytes: ByteArray by lazy {
        hexToByteArray(MASTER_PUBLIC_KEY_HEX)
    }

    /**
     * Checks current license status across all storage layers.
     * If SharedPreferences is empty but an external backup exists (e.g. after app reinstall),
     * it automatically validates and restores the license seamlessly.
     */
    fun checkLicenseStatus(context: Context): LicenseValidationResult {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var rawKey = prefs.getString(KEY_RAW_LICENSE, null)

        // If not in SharedPreferences, check external persistent storage (reinstall recovery)
        if (rawKey.isNullOrBlank()) {
            rawKey = readFromExternalBackup(context)
            if (!rawKey.isNullOrBlank()) {
                val restoreResult = validateRawKey(context, rawKey)
                if (restoreResult is LicenseValidationResult.Valid) {
                    // Auto-restore to SharedPreferences
                    prefs.edit().putString(KEY_RAW_LICENSE, rawKey).apply()
                    return restoreResult
                }
            }
        }

        if (rawKey.isNullOrBlank()) {
            return LicenseValidationResult.NotActivated
        }

        return validateRawKey(context, rawKey)
    }

    /**
     * Validates and activates a new license key entered by the user.
     * Persists across all storage layers on success.
     */
    fun activateLicense(context: Context, rawKeyInput: String): LicenseValidationResult {
        val cleanKey = rawKeyInput.trim()
        val result = validateRawKey(context, cleanKey)

        if (result is LicenseValidationResult.Valid) {
            // Save to primary app storage
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_RAW_LICENSE, cleanKey)
                .putLong(KEY_LAST_CHECK_TS, System.currentTimeMillis())
                .apply()

            // Save to external persistent backup (for survival across reinstall)
            saveToExternalBackup(context, cleanKey)
        }

        return result
    }

    /**
     * Validates a license key without persisting.
     */
    fun validateRawKey(context: Context, rawKey: String): LicenseValidationResult {
        val certificate = parseLicenseKey(rawKey)
            ?: return LicenseValidationResult.MalformedKey("صيغة كود الترخيص غير صالحة")

        val payload = certificate.payload

        // 1. Target Application verification
        val isTargetAppValid = payload.app.equals("GrainTradeManager", ignoreCase = true) ||
            payload.app.equals("UltraSoftERP", ignoreCase = true)

        if (!isTargetAppValid) {
            return LicenseValidationResult.AppMismatch(payload.app)
        }

        // 2. Hardware ID verification
        val currentDeviceMachineId = HardwareIdProvider.getHardwareId(context)
        if (!HardwareIdProvider.isMatchingDevice(payload.machineId, currentDeviceMachineId)) {
            return LicenseValidationResult.MachineMismatch(
                expectedMachineId = payload.machineId,
                actualDeviceMachineId = currentDeviceMachineId
            )
        }

        // 3. Cryptographic Signature Verification (Ed25519)
        val canonicalPayloadString = payload.toCanonicalString()
        val canonicalBytes = canonicalPayloadString.toByteArray(Charsets.UTF_8)
        val signatureBytes = hexToByteArray(certificate.signatureHex)

        val isSignatureValid = Ed25519Verifier.verify(
            publicKeyBytes = masterPublicKeyBytes,
            messageBytes = canonicalBytes,
            signatureBytes = signatureBytes
        )

        if (!isSignatureValid) {
            return LicenseValidationResult.InvalidSignature
        }

        // 4. Expiration verification
        if (payload.isLifetime) {
            return LicenseValidationResult.Valid(certificate, daysRemaining = 36500L)
        }

        val expiryTimestamp = parseIsoDate(payload.expiresAt)
        val currentTimestamp = System.currentTimeMillis()

        // Anti-rollback check: ensure system clock wasn't set back more than 24 hours
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckTs = prefs.getLong(KEY_LAST_CHECK_TS, 0L)
        if (lastCheckTs > 0 && currentTimestamp < (lastCheckTs - 86400000L)) {
            return LicenseValidationResult.MalformedKey("تم اكتشاف تلاعب في ساعة وتاريخ الجهاز")
        }
        prefs.edit().putLong(KEY_LAST_CHECK_TS, currentTimestamp).apply()

        if (currentTimestamp > expiryTimestamp) {
            return LicenseValidationResult.Expired(
                expiredAt = payload.expiresAt,
                certificate = certificate
            )
        }

        val diffMillis = expiryTimestamp - currentTimestamp
        val daysRemaining = (diffMillis / (1000L * 60 * 60 * 24)).coerceAtLeast(0L)

        return LicenseValidationResult.Valid(certificate, daysRemaining)
    }

    /**
     * Parses a raw license key string (e.g. "GTM6-..." or "US6-...") into a LicenseCertificate.
     */
    fun parseLicenseKey(rawKey: String): LicenseCertificate? {
        return try {
            val stripped = when {
                rawKey.startsWith("GTM6-", ignoreCase = true) -> rawKey.substring(5)
                rawKey.startsWith("US6-", ignoreCase = true) -> rawKey.substring(4)
                else -> rawKey
            }.trim()

            val decodedBytes = Base64.decode(stripped, Base64.DEFAULT)
            val jsonString = String(decodedBytes, Charsets.UTF_8)
            val root = JSONObject(jsonString)

            val headerObj = root.getJSONObject("header")
            val header = LicenseHeader(
                alg = headerObj.optString("alg", "Ed25519"),
                ver = headerObj.optString("ver", "1.0"),
                type = headerObj.optString("type", "GrainTradeManager-License")
            )

            val payloadObj = root.getJSONObject("payload")
            val featuresList = mutableListOf<String>()
            val featuresArr = payloadObj.optJSONArray("features")
            if (featuresArr != null) {
                for (i in 0 until featuresArr.length()) {
                    featuresList.add(featuresArr.getString(i))
                }
            } else {
                featuresList.add("all")
            }

            val payload = LicensePayload(
                licenseId = payloadObj.optString("licenseId", ""),
                app = payloadObj.optString("app", "GrainTradeManager"),
                appVersion = payloadObj.optString("appVersion", "1.0"),
                clientName = payloadObj.optString("clientName", "عميل مرخص"),
                machineId = payloadObj.optString("machineId", "").uppercase(Locale.ROOT),
                tier = payloadObj.optString("tier", "Enterprise"),
                issuedAt = payloadObj.optString("issuedAt", ""),
                expiresAt = payloadObj.optString("expiresAt", ""),
                isLifetime = payloadObj.optBoolean("isLifetime", false),
                maxUsers = payloadObj.optInt("maxUsers", 100),
                maxBranches = payloadObj.optInt("maxBranches", 10),
                features = featuresList
            )

            val signature = root.getString("signature")

            LicenseCertificate(
                header = header,
                payload = payload,
                signatureHex = signature,
                rawKey = rawKey
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes the license from both primary storage and persistent backup.
     */
    fun clearLicense(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val backupFile = File(docsDir, BACKUP_FILENAME)
            if (backupFile.exists()) backupFile.delete()

            val appExtDir = context.getExternalFilesDir(null)
            if (appExtDir != null) {
                val appBackup = File(appExtDir, BACKUP_FILENAME)
                if (appBackup.exists()) appBackup.delete()
            }
        } catch (_: Exception) {
            // Ignore backup deletion errors
        }
    }

    private fun saveToExternalBackup(context: Context, rawKey: String) {
        // 1. Try public Documents directory (survives app uninstall)
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!docsDir.exists()) docsDir.mkdirs()
            val backupFile = File(docsDir, BACKUP_FILENAME)
            backupFile.writeText(rawKey, Charsets.UTF_8)
        } catch (_: Exception) {
            // Scoped storage might restrict on some devices without SAF
        }

        // 2. Also try external files directory
        try {
            val appExtDir = context.getExternalFilesDir(null)
            if (appExtDir != null) {
                val appBackup = File(appExtDir, BACKUP_FILENAME)
                appBackup.writeText(rawKey, Charsets.UTF_8)
            }
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun readFromExternalBackup(context: Context): String? {
        // 1. Try public Documents directory
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val backupFile = File(docsDir, BACKUP_FILENAME)
            if (backupFile.exists() && backupFile.length() > 20) {
                return backupFile.readText(Charsets.UTF_8).trim()
            }
        } catch (_: Exception) {
            // Ignore
        }

        // 2. Try app external files dir
        try {
            val appExtDir = context.getExternalFilesDir(null)
            if (appExtDir != null) {
                val appBackup = File(appExtDir, BACKUP_FILENAME)
                if (appBackup.exists() && appBackup.length() > 20) {
                    return appBackup.readText(Charsets.UTF_8).trim()
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        return null
    }

    private fun parseIsoDate(isoString: String): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ROOT)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(isoString)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next
            }
        }
        return 0L
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
