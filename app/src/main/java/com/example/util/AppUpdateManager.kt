package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val isMandatory: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersionName: String, val currentVersionCode: Int) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdateManager {
    private const val PREFS_NAME = "gtm_app_update_prefs"
    private const val KEY_REPO_OR_MANIFEST_URL = "repo_or_manifest_url"
    private const val KEY_CACHED_VERSION_CODE = "cached_version_code"
    private const val KEY_CACHED_VERSION_NAME = "cached_version_name"
    private const val KEY_CACHED_DOWNLOAD_URL = "cached_download_url"
    private const val KEY_CACHED_RELEASE_NOTES = "cached_release_notes"
    private const val KEY_CACHED_MANDATORY = "cached_mandatory"
    private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"

    const val OFFICIAL_GITHUB_REPO = "https://github.com/Ahm3d0x/Grain-Trade-Manager"
    const val OFFICIAL_APK_DOWNLOAD_URL =
        "https://github.com/Ahm3d0x/Grain-Trade-Manager/raw/main/GrainTradeManager-release.apk"
    private const val WHATSAPP_INTL = "201140409832"

    val currentVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val currentVersionName: IntOrStringVersion
        get() = BuildConfig.VERSION_NAME

    fun getConfiguredUpdateUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_REPO_OR_MANIFEST_URL, "")?.trim() ?: ""
        return saved.ifEmpty { OFFICIAL_GITHUB_REPO }
    }

    fun setConfiguredUpdateUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REPO_OR_MANIFEST_URL, url.trim().ifEmpty { OFFICIAL_GITHUB_REPO }).apply()
    }

    /**
     * Returns any cached newer update if its versionCode is strictly greater than the installed versionCode.
     */
    fun getCachedAvailableUpdate(context: Context): AppUpdateInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedCode = prefs.getInt(KEY_CACHED_VERSION_CODE, -1)
        if (cachedCode <= currentVersionCode) {
            return null
        }
        val versionName = prefs.getString(KEY_CACHED_VERSION_NAME, "$cachedCode.0") ?: "$cachedCode.0"
        val downloadUrl = (prefs.getString(KEY_CACHED_DOWNLOAD_URL, "") ?: "")
            .ifEmpty { OFFICIAL_APK_DOWNLOAD_URL }
        val releaseNotes = prefs.getString(
            KEY_CACHED_RELEASE_NOTES,
            "يتوفر إصدار جديد من برنامج احسبلي يتضمن تحسينات ومميزات جديدة."
        ) ?: ""
        val isMandatory = prefs.getBoolean(KEY_CACHED_MANDATORY, false)

        return AppUpdateInfo(
            versionCode = cachedCode,
            versionName = versionName,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
            isMandatory = isMandatory
        )
    }

    fun saveAvailableUpdate(context: Context, info: AppUpdateInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_CACHED_VERSION_CODE, info.versionCode)
            .putString(KEY_CACHED_VERSION_NAME, info.versionName)
            .putString(KEY_CACHED_DOWNLOAD_URL, info.downloadUrl.ifBlank { OFFICIAL_APK_DOWNLOAD_URL })
            .putString(KEY_CACHED_RELEASE_NOTES, info.releaseNotes)
            .putBoolean(KEY_CACHED_MANDATORY, info.isMandatory)
            .remove(KEY_DISMISSED_VERSION_CODE)
            .apply()
    }

    fun clearCachedUpdate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_CACHED_VERSION_CODE)
            .remove(KEY_CACHED_VERSION_NAME)
            .remove(KEY_CACHED_DOWNLOAD_URL)
            .remove(KEY_CACHED_RELEASE_NOTES)
            .remove(KEY_CACHED_MANDATORY)
            .remove(KEY_DISMISSED_VERSION_CODE)
            .apply()
    }

    fun isUpdateDismissedForSession(context: Context, versionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DISMISSED_VERSION_CODE, -1) == versionCode
    }

    fun dismissUpdatePrompt(context: Context, versionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DISMISSED_VERSION_CODE, versionCode).apply()
    }

    /**
     * Converts a standard GitHub repo URL (e.g. https://github.com/Ahm3d0x/Grain-Trade-Manager)
     * or raw URL into candidate raw `version.json` URLs and default APK download URL.
     */
    private fun resolveCandidateManifestUrls(rawInput: String): Pair<List<String>, String> {
        val cleaned = rawInput.trim().removeSuffix("/").ifEmpty { OFFICIAL_GITHUB_REPO }

        if (cleaned.endsWith(".json", ignoreCase = true)) {
            val defaultApk = cleaned.substringBeforeLast("/") + "/GrainTradeManager-release.apk"
            return listOf(cleaned) to defaultApk
        }

        val githubRegex = Regex("""https?://github\.com/([^/]+)/([^/]+).*""", RegexOption.IGNORE_CASE)
        val match = githubRegex.matchEntire(cleaned)
        if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git")
            val mainManifest = "https://raw.githubusercontent.com/$owner/$repo/main/version.json"
            val masterManifest = "https://raw.githubusercontent.com/$owner/$repo/master/version.json"
            val defaultApk = "https://github.com/$owner/$repo/raw/main/GrainTradeManager-release.apk"
            return listOf(mainManifest, masterManifest) to defaultApk
        }

        return listOf(
            "https://raw.githubusercontent.com/Ahm3d0x/Grain-Trade-Manager/main/version.json",
            "https://raw.githubusercontent.com/Ahm3d0x/Grain-Trade-Manager/master/version.json"
        ) to OFFICIAL_APK_DOWNLOAD_URL
    }

    /**
     * Checks the remote `version.json` on GitHub (Ahm3d0x/Grain-Trade-Manager) over HTTPS.
     */
    suspend fun checkForUpdates(context: Context, customUrl: String? = null): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            val configured = (customUrl ?: getConfiguredUpdateUrl(context)).trim().ifEmpty { OFFICIAL_GITHUB_REPO }
            val (candidateUrls, fallbackApkUrl) = resolveCandidateManifestUrls(configured)
            var lastError = "تعذر الاتصال بخادم التحديثات، يرجى التأكد من الاتصال بالإنترنت"

            for (manifestUrl in candidateUrls) {
                try {
                    val url = URL(manifestUrl + "?t=" + System.currentTimeMillis())
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Cache-Control", "no-cache")
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        val responseText = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                            it.readText()
                        }
                        conn.disconnect()

                        val json = JSONObject(responseText)
                        val remoteVersionCode = json.optInt("versionCode", 1)
                        val remoteVersionName = json.optString("versionName", "$remoteVersionCode.0")
                        val rawDownloadUrl = json.optString("downloadUrl", "").trim()
                        val effectiveDownloadUrl = rawDownloadUrl.ifEmpty { fallbackApkUrl }
                        val releaseNotes = json.optString(
                            "releaseNotes",
                            "يتوفر تحديث جديد (الإصدار $remoteVersionName) يتضمن تحسينات ومميزات جديدة."
                        )
                        val isMandatory = json.optBoolean("isMandatory", false)

                        if (remoteVersionCode > currentVersionCode) {
                            val info = AppUpdateInfo(
                                versionCode = remoteVersionCode,
                                versionName = remoteVersionName,
                                downloadUrl = effectiveDownloadUrl,
                                releaseNotes = releaseNotes,
                                isMandatory = isMandatory
                            )
                            saveAvailableUpdate(context, info)
                            return@withContext UpdateCheckResult.UpdateAvailable(info)
                        } else {
                            clearCachedUpdate(context)
                            return@withContext UpdateCheckResult.UpToDate(
                                currentVersionName = currentVersionName,
                                currentVersionCode = currentVersionCode
                            )
                        }
                    } else {
                        lastError = "أنت تستخدم أحدث إصدار حاليًا (v$currentVersionName)"
                        conn.disconnect()
                    }
                } catch (_: Exception) {
                    lastError = "تعذر التحقق من التحديثات: تأكد من الاتصال بالإنترنت"
                }
            }

            val cached = getCachedAvailableUpdate(context)
            if (cached != null) {
                UpdateCheckResult.UpdateAvailable(cached)
            } else {
                UpdateCheckResult.Error(lastError)
            }
        }

    fun openDownloadUrl(context: Context, downloadUrl: String) {
        val cleanUrl = downloadUrl.trim().ifEmpty { OFFICIAL_APK_DOWNLOAD_URL }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح رابط التحميل المباشر، يرجى طلب التحديث عبر واتساب", Toast.LENGTH_LONG).show()
        }
    }

    fun requestUpdateViaWhatsApp(context: Context, targetVersionName: String) {
        val verText = if (targetVersionName.isNotBlank()) "الإصدار $targetVersionName" else "أحدث إصدار"
        val msg = "السلام عليكم باشمهندس أحمد، أود الحصول على رابط تحميل التحديث الجديد ($verText) لبرنامج احسبلي.\nالإصدار الحالي لدي: v$currentVersionName"
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$WHATSAPP_INTL?text=" + Uri.encode(msg))
            )
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
        }
    }
}

private typealias IntOrStringVersion = String
