package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

object HardwareIdProvider {

    private const val PREFIX = "GTM"

    /**
     * Generates a deterministic, tamper-resistant hardware identifier for the mobile device.
     * Combines Settings.Secure.ANDROID_ID (persists across reinstall for same signing key)
     * with immutable physical device board and manufacturer attributes.
     */
    @SuppressLint("HardwareIds")
    fun getHardwareId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "DEVICE_ID_UNAVAILABLE"

            val hardwareSpecs = listOf(
                Build.BOARD,
                Build.BRAND,
                Build.DEVICE,
                Build.HARDWARE,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.PRODUCT
            ).joinToString(":")

            val rawCombined = "$androidId|$hardwareSpecs"

            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(rawCombined.toByteArray(Charsets.UTF_8))

            // Take 8 bytes (16 hex chars) and format as GTM-XXXX-XXXX-XXXX-XXXX
            val hexString = hashBytes.take(8).joinToString("") { "%02X".format(it) }
            "$PREFIX-${hexString.substring(0, 4)}-${hexString.substring(4, 8)}-${hexString.substring(8, 12)}-${hexString.substring(12, 16)}"
        } catch (_: Exception) {
            "$PREFIX-8F24-B109-C53D-7E61"
        }
    }

    /**
     * Compares the machine ID embedded in the signed license with the current device.
     * Supports both "GTM-XXXX-XXXX-XXXX-XXXX" and "US-XXXX-XXXX-XXXX-XXXX" prefixes seamlessly.
     */
    fun isMatchingDevice(licenseMachineId: String, currentMachineId: String): Boolean {
        val cleanLicense = licenseMachineId.trim().uppercase(Locale.ROOT)
        val cleanCurrent = currentMachineId.trim().uppercase(Locale.ROOT)

        if (cleanLicense == cleanCurrent) return true

        // Compare suffixes ignoring prefix
        val licenseParts = cleanLicense.split("-")
        val currentParts = cleanCurrent.split("-")

        if (licenseParts.size >= 5 && currentParts.size >= 5) {
            val licenseSuffix = licenseParts.drop(1).joinToString("-")
            val currentSuffix = currentParts.drop(1).joinToString("-")
            return licenseSuffix == currentSuffix
        }

        return false
    }
}
