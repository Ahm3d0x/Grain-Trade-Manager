package com.example.security

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class UnlockMethod(val labelAr: String) {
    PIN("الرقم السري (PIN)"),
    PATTERN("نقش 9 نقاط"),
    BIOMETRIC("بصمة الهاتف")
}

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED
}

data class AppLockConfig(
    val isPinEnabled: Boolean = false,
    val isPatternEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lockOnBackground: Boolean = true
) {
    val isLockActive: Boolean
        get() = isPinEnabled || isPatternEnabled || isBiometricEnabled

    val activeMethodCount: Int
        get() = (if (isPinEnabled) 1 else 0) +
            (if (isPatternEnabled) 1 else 0) +
            (if (isBiometricEnabled) 1 else 0)

    val hasManualFallback: Boolean
        get() = isPinEnabled || isPatternEnabled

    val defaultManualMethod: UnlockMethod?
        get() = when {
            isPinEnabled -> UnlockMethod.PIN
            isPatternEnabled -> UnlockMethod.PATTERN
            else -> null
        }

    fun activeMethodsSummaryAr(): String {
        val list = mutableListOf<String>()
        if (isPinEnabled) list.add("رقم سري (4 أرقام)")
        if (isPatternEnabled) list.add("نقش 9 نقاط")
        if (isBiometricEnabled) list.add("بصمة الهاتف")
        return if (list.isEmpty()) "غير مفعّل" else list.joinToString(" + ")
    }
}

/**
 * مدير قفل وحماية البرنامج:
 * - يدعم 3 طرق حماية مستقلة أو مجمّعة (PIN من 4 أرقام، نقش 9 نقاط، وبصمة الهاتف).
 * - يخزن الرمز والنقش بتشفير غير قابل للاسترجاع (PBKDF2-HMAC-SHA256 مع Salt عشوائي) ومقارنة زمنية ثابتة.
 * - يستخدم BiometricPrompt الرسمي من أندرويد للتحقق ببصمة قفل وفتح الهاتف نفسها.
 */
object AppLockManager {

    private const val PREFS_NAME = "ehsebly_app_lock_prefs"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_PATTERN_ENABLED = "pattern_enabled"
    private const val KEY_PATTERN_HASH = "pattern_hash"
    private const val KEY_PATTERN_SALT = "pattern_salt"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_LOCK_ON_BACKGROUND = "lock_on_background"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until_ms"

    private const val PBKDF2_ITERATIONS = 10_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    const val PIN_LENGTH = 4
    const val MIN_PATTERN_DOTS = 4
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30_000L

    /**
     * يمنع إعادة القفل التلقائي أثناء ظهور نافذة البصمة الخاصة بالنظام
     */
    @Volatile
    var isSystemAuthInProgress: Boolean = false

    fun getConfig(context: Context): AppLockConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasPin = prefs.getBoolean(KEY_PIN_ENABLED, false) &&
            !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PIN_SALT, null).isNullOrBlank()
        val hasPattern = prefs.getBoolean(KEY_PATTERN_ENABLED, false) &&
            !prefs.getString(KEY_PATTERN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PATTERN_SALT, null).isNullOrBlank()
        val hasBio = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val lockOnBg = prefs.getBoolean(KEY_LOCK_ON_BACKGROUND, true)
        return AppLockConfig(
            isPinEnabled = hasPin,
            isPatternEnabled = hasPattern,
            isBiometricEnabled = hasBio,
            lockOnBackground = lockOnBg
        )
    }

    fun isLockActive(context: Context): Boolean = getConfig(context).isLockActive

    fun shouldLockOnBackground(context: Context): Boolean {
        val cfg = getConfig(context)
        return cfg.isLockActive && cfg.lockOnBackground
    }

    fun setLockOnBackground(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOCK_ON_BACKGROUND, enabled).apply()
    }

    fun savePin(context: Context, pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = generateSaltHex()
        val hash = hashSecret(pin, salt)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, true)
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
        resetFailedAttempts(context)
        return true
    }

    fun disablePin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PIN_ENABLED, false)
        val salt = prefs.getString(KEY_PIN_SALT, null)
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (!enabled || salt.isNullOrBlank() || storedHash.isNullOrBlank()) return false
        return verifySecret(pin, salt, storedHash)
    }

    fun savePattern(context: Context, dots: List<Int>): Boolean {
        val normalized = normalizePattern(dots) ?: return false
        val salt = generateSaltHex()
        val hash = hashSecret(normalized, salt)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PATTERN_ENABLED, true)
            .putString(KEY_PATTERN_SALT, salt)
            .putString(KEY_PATTERN_HASH, hash)
            .apply()
        resetFailedAttempts(context)
        return true
    }

    fun disablePattern(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PATTERN_ENABLED, false)
            .remove(KEY_PATTERN_HASH)
            .remove(KEY_PATTERN_SALT)
            .apply()
    }

    fun verifyPattern(context: Context, dots: List<Int>): Boolean {
        val normalized = normalizePattern(dots) ?: return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PATTERN_ENABLED, false)
        val salt = prefs.getString(KEY_PATTERN_SALT, null)
        val storedHash = prefs.getString(KEY_PATTERN_HASH, null)
        if (!enabled || salt.isNullOrBlank() || storedHash.isNullOrBlank()) return false
        return verifySecret(normalized, salt, storedHash)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun disableAllLocks(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_PATTERN_ENABLED, false)
            .remove(KEY_PATTERN_HASH)
            .remove(KEY_PATTERN_SALT)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun getRemainingLockoutMs(context: Context, nowMs: Long = System.currentTimeMillis()): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (until - nowMs).coerceAtLeast(0L)
    }

    fun recordFailedAttempt(context: Context, nowMs: Long = System.currentTimeMillis()): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, count)
        if (count >= MAX_FAILED_ATTEMPTS) {
            editor.putLong(KEY_LOCKOUT_UNTIL, nowMs + LOCKOUT_DURATION_MS)
            editor.putInt(KEY_FAILED_ATTEMPTS, 0)
        }
        editor.apply()
        return count
    }

    fun resetFailedAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        val bm = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (bm.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            else -> BiometricAvailability.HARDWARE_UNAVAILABLE
        }
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "فتح برنامج احسبلي",
        subtitle: String = "استخدم بصمة الهاتف للتحقق من هويتك",
        allowDeviceCredentialFallback: Boolean = false,
        negativeButtonText: String = "استخدام طريقة أخرى",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isSystemAuthInProgress = false
                resetFailedAttempts(activity)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isSystemAuthInProgress = false
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("لم تتطابق البصمة، حاول مرة أخرى")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (allowDeviceCredentialFallback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
        } else {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            builder.setNegativeButtonText(negativeButtonText)
        }

        try {
            isSystemAuthInProgress = true
            prompt.authenticate(builder.build())
        } catch (e: Exception) {
            isSystemAuthInProgress = false
            onError("تعذر تشغيل مستشعر البصمة: ${e.localizedMessage ?: "خطأ غير متوقع"}")
        }
    }

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return current as? FragmentActivity
    }

    // ---- Pure-Kotlin Cryptographic & Pattern Validation Helpers (Unit-Testable) ----

    fun isValidPin(pin: String): Boolean {
        return pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }
    }

    fun normalizePattern(dots: List<Int>): String? {
        if (dots.size < MIN_PATTERN_DOTS) return null
        if (dots.any { it !in 0..8 }) return null
        if (dots.distinct().size != dots.size) return null
        return dots.joinToString("-")
    }

    /**
     * يحسب النقطة الوسطية عند الانتقال المباشر بين نقطتين غير متجاورتين على نفس الصف أو العمود أو القطر
     * مثال: من 0 إلى 2 يرجع 1، من 0 إلى 8 يرجع 4، من 2 إلى 6 يرجع 4.
     */
    fun getIntermediateDot(from: Int, to: Int): Int? {
        if (from !in 0..8 || to !in 0..8 || from == to) return null
        val r1 = from / 3
        val c1 = from % 3
        val r2 = to / 3
        val c2 = to % 3
        if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
            val midR = (r1 + r2) / 2
            val midC = (c1 + c2) / 2
            val mid = midR * 3 + midC
            if (mid != from && mid != to) return mid
        }
        return null
    }

    fun generateSaltHex(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashSecret(secret: String, saltHex: String): String {
        val saltBytes = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(secret.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        return try {
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hashBytes = skf.generateSecret(spec).encoded
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            // Fallback for minimal JVMs without PBKDF2WithHmacSHA256
            val digest = MessageDigest.getInstance("SHA-256")
            var current = (saltHex + ":" + secret).toByteArray(Charsets.UTF_8)
            repeat(2000) {
                current = digest.digest(current)
            }
            current.joinToString("") { "%02x".format(it) }
        } finally {
            spec.clearPassword()
        }
    }

    fun verifySecret(secret: String, saltHex: String, expectedHashHex: String): Boolean {
        val computed = hashSecret(secret, saltHex)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
        )
    }
}
