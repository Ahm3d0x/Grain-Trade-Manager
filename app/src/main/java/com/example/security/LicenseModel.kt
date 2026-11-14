package com.example.security

import java.util.Locale

data class LicenseHeader(
    val alg: String,
    val ver: String,
    val type: String
)

data class LicensePayload(
    val licenseId: String,
    val app: String,
    val appVersion: String,
    val clientName: String,
    val machineId: String,
    val tier: String,
    val issuedAt: String,
    val expiresAt: String,
    val isLifetime: Boolean,
    val maxUsers: Int,
    val maxBranches: Int,
    val features: List<String>
) {
    /**
     * Canonical string representation for digital signature verification.
     * Byte-for-byte identical to the JavaScript generator's canonicalizePayload() output.
     */
    fun toCanonicalString(): String {
        val sortedFeatures = features.sorted()
        val featuresJson = "[" + sortedFeatures.joinToString(",") { escapeJson(it) } + "]"

        return "{" +
            "\"app\":" + escapeJson(app.trim()) + "," +
            "\"appVersion\":" + escapeJson(appVersion.trim()) + "," +
            "\"clientName\":" + escapeJson(clientName.trim()) + "," +
            "\"expiresAt\":" + escapeJson(expiresAt.trim()) + "," +
            "\"features\":" + featuresJson + "," +
            "\"isLifetime\":" + (if (isLifetime) "true" else "false") + "," +
            "\"issuedAt\":" + escapeJson(issuedAt.trim()) + "," +
            "\"licenseId\":" + escapeJson(licenseId.trim()) + "," +
            "\"machineId\":" + escapeJson(machineId.trim().uppercase(Locale.ROOT)) + "," +
            "\"maxBranches\":" + maxBranches + "," +
            "\"maxUsers\":" + maxUsers + "," +
            "\"tier\":" + escapeJson(tier.trim()) +
            "}"
    }

    private fun escapeJson(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000c' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0x00..0x1F) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}

data class LicenseCertificate(
    val header: LicenseHeader,
    val payload: LicensePayload,
    val signatureHex: String,
    val rawKey: String
)

sealed class LicenseValidationResult {
    data class Valid(
        val certificate: LicenseCertificate,
        val daysRemaining: Long
    ) : LicenseValidationResult()

    data class Expired(
        val expiredAt: String,
        val certificate: LicenseCertificate
    ) : LicenseValidationResult()

    data class MachineMismatch(
        val expectedMachineId: String,
        val actualDeviceMachineId: String
    ) : LicenseValidationResult()

    data class AppMismatch(
        val targetApp: String
    ) : LicenseValidationResult()

    object InvalidSignature : LicenseValidationResult()
    data class MalformedKey(val reason: String) : LicenseValidationResult()
    object NotActivated : LicenseValidationResult()
}
