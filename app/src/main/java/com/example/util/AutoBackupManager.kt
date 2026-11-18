package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.GrainDatabase
import com.example.data.local.InvoiceType
import com.example.data.repository.GrainRepository
import com.example.domain.CalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoBackupEntry(
    val fileName: String,
    val filePath: String,
    val dayKey: String,           // yyyy-MM-dd
    val timestamp: Long,          // epoch millis
    val fileSizeBytes: Long,
    val isConsolidatedDaily: Boolean,
    val isWithinDetailedTwoDays: Boolean,
    val changeSummary: String,
    val invoicesCount: Int,
    val customersCount: Int
)

data class AutoBackupStatusSummary(
    val isEnabled: Boolean,
    val folderPath: String,
    val customFolderUri: String?,
    val lastBackupTimestamp: Long,
    val lastBackupChangeSummary: String,
    val totalDaysCount: Int,
    val detailedTwoDaysEntries: List<AutoBackupEntry>,
    val consolidatedDailyEntries: List<AutoBackupEntry>,
    val totalStorageBytes: Long
)

sealed class AutoBackupResult {
    data class Created(val entry: AutoBackupEntry) : AutoBackupResult()
    data object NoChanges : AutoBackupResult()
    data object Disabled : AutoBackupResult()
    data class Error(val message: String) : AutoBackupResult()
}

object AutoBackupManager {
    private const val PREFS_NAME = "ehsebly_auto_backup_prefs"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_DATA_HASH = "last_data_hash"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_CHANGE_SUMMARY = "last_change_summary"
    private const val KEY_LAST_INVOICES_COUNT = "last_invoices_count"
    private const val KEY_LAST_SALE_INVOICES_COUNT = "last_sale_invoices_count"
    private const val KEY_LAST_WEIGHTS_COUNT = "last_weights_count"
    private const val KEY_LAST_CUSTOMERS_COUNT = "last_customers_count"
    private const val KEY_LAST_PAYMENTS_COUNT = "last_payments_count"
    private const val KEY_LAST_ADJUSTMENTS_COUNT = "last_adjustments_count"
    private const val KEY_CUSTOM_FOLDER_URI = "custom_folder_uri"

    const val BACKUP_FOLDER_NAME = "Ehsebly_AutoBackups"
    const val DETAILED_HISTORY_DAYS = 2
    const val CONSOLIDATED_DAILY_DAYS = 28
    const val MAX_RETENTION_DAYS = DETAILED_HISTORY_DAYS + CONSOLIDATED_DAILY_DAYS // 30 days total

    private const val INDEX_FILE_NAME = "backup_metadata_index.json"
    private val backupMutex = Mutex()

    private val fileRegex = Regex(
        """^ehsebly_backup_(\d{4}-\d{2}-\d{2})_(session_\d{2}-\d{2}-\d{2}|daily)_(\d+)\.json$"""
    )

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getCustomFolderUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_FOLDER_URI, null)?.takeIf { it.isNotBlank() }
    }

    fun setCustomFolderUri(context: Context, uriString: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (uriString.isNullOrBlank()) remove(KEY_CUSTOM_FOLDER_URI)
            else putString(KEY_CUSTOM_FOLDER_URI, uriString)
        }.apply()
    }

    /**
     * Returns the dedicated device backup directory:
     * `/storage/emulated/0/Android/data/<package>/files/Ehsebly_AutoBackups`
     * (falls back to internal `filesDir/Ehsebly_AutoBackups` if external storage is unavailable).
     */
    fun getBackupDirectory(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, BACKUP_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Computes a deterministic SHA-256 hash of the exported database JSON,
     * ignoring transient fields like `exportedAt` and `autoBackupMeta`, and ignoring
     * empty untouched draft invoices that have 0 weights and no notes.
     */
    fun computeDataHash(rawJson: String): String {
        val root = JSONObject(rawJson)
        val canonical = JSONObject()

        canonical.put("settings", root.optJSONObject("settings") ?: JSONObject())
        canonical.put("customers", root.optJSONArray("customers") ?: JSONArray())
        canonical.put("products", root.optJSONArray("products") ?: JSONArray())
        canonical.put("pricingUnits", root.optJSONArray("pricingUnits") ?: JSONArray())

        val weightsArr = root.optJSONArray("weights") ?: JSONArray()
        val sectionsArr = root.optJSONArray("sections") ?: JSONArray()
        val invoicesArr = root.optJSONArray("invoices") ?: JSONArray()

        // Identify sections that have at least one weight
        val sectionIdsWithWeights = mutableSetOf<Long>()
        for (i in 0 until weightsArr.length()) {
            val w = weightsArr.optJSONObject(i) ?: continue
            sectionIdsWithWeights.add(w.optLong("sectionId"))
        }

        val invoiceIdsWithWeights = mutableSetOf<Long>()
        for (i in 0 until sectionsArr.length()) {
            val s = sectionsArr.optJSONObject(i) ?: continue
            if (sectionIdsWithWeights.contains(s.optLong("id"))) {
                invoiceIdsWithWeights.add(s.optLong("invoiceId"))
            }
        }

        // Normalize invoices (ignore empty blank auto-created drafts so merely opening the app doesn't count as a data change)
        val normalizedInvoices = JSONArray()
        for (i in 0 until invoicesArr.length()) {
            val inv = invoicesArr.optJSONObject(i) ?: continue
            val invId = inv.optLong("id")
            val isDraft = inv.optString("status") == "DRAFT"
            val hasNotes = inv.optString("notes", "").isNotBlank()
            val hasCustomCustomer = !inv.isNull("customerId")
            if (isDraft && !invoiceIdsWithWeights.contains(invId) && !hasNotes && !hasCustomCustomer) {
                continue
            }
            val copy = JSONObject(inv.toString())
            normalizedInvoices.put(copy)
        }

        val keptInvoiceIds = mutableSetOf<Long>()
        for (i in 0 until normalizedInvoices.length()) {
            keptInvoiceIds.add(normalizedInvoices.getJSONObject(i).optLong("id"))
        }

        val normalizedSections = JSONArray()
        for (i in 0 until sectionsArr.length()) {
            val s = sectionsArr.optJSONObject(i) ?: continue
            if (keptInvoiceIds.contains(s.optLong("invoiceId"))) {
                normalizedSections.put(s)
            }
        }

        canonical.put("invoices", normalizedInvoices)
        canonical.put("sections", normalizedSections)
        canonical.put("weights", weightsArr)
        canonical.put("payments", root.optJSONArray("payments") ?: JSONArray())
        canonical.put("inventoryAdjustments", root.optJSONArray("inventoryAdjustments") ?: JSONArray())

        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(canonical.toString().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Automatically checks if the database has changed since the last backup,
     * writes a new timestamped backup file if changed (or if `force == true`),
     * and enforces the 30-day retention policy (2 days full session history + 28 days consolidated single daily file).
     */
    suspend fun performAutoBackupIfChanged(
        context: Context,
        database: GrainDatabase = GrainDatabase.getInstance(context),
        force: Boolean = false,
        triggerReason: String = "إغلاق التطبيق"
    ): AutoBackupResult = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                if (!force && !isAutoBackupEnabled(context)) {
                    return@withContext AutoBackupResult.Disabled
                }

                val repository = GrainRepository(database)
                val rawJson = repository.exportBackupJson()
                val currentHash = computeDataHash(rawJson)

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastHash = prefs.getString(KEY_LAST_DATA_HASH, "") ?: ""
                val backupDir = getBackupDirectory(context)

                val existingFiles = listValidBackupFiles(backupDir)
                if (!force && currentHash == lastHash && existingFiles.isNotEmpty()) {
                    // Still run retention policy in case the date rolled over
                    applyRetentionPolicy(backupDir)
                    return@withContext AutoBackupResult.NoChanges
                }

                val now = System.currentTimeMillis()
                val root = JSONObject(rawJson)
                val invoicesArr = root.optJSONArray("invoices") ?: JSONArray()
                val weightsArr = root.optJSONArray("weights") ?: JSONArray()
                val customersArr = root.optJSONArray("customers") ?: JSONArray()
                val paymentsArr = root.optJSONArray("payments") ?: JSONArray()
                val adjustmentsArr = root.optJSONArray("inventoryAdjustments") ?: JSONArray()

                var purchaseCount = 0
                var saleCount = 0
                for (i in 0 until invoicesArr.length()) {
                    val inv = invoicesArr.optJSONObject(i) ?: continue
                    if (inv.optString("invoiceType") == InvoiceType.SALE.name) {
                        saleCount++
                    } else {
                        purchaseCount++
                    }
                }
                val totalInvoices = purchaseCount + saleCount
                val weightsCount = weightsArr.length()
                val customersCount = customersArr.length()
                val paymentsCount = paymentsArr.length()
                val adjustmentsCount = adjustmentsArr.length()

                val prevInvoices = prefs.getInt(KEY_LAST_INVOICES_COUNT, -1)
                val prevSales = prefs.getInt(KEY_LAST_SALE_INVOICES_COUNT, -1)
                val prevWeights = prefs.getInt(KEY_LAST_WEIGHTS_COUNT, -1)
                val prevCustomers = prefs.getInt(KEY_LAST_CUSTOMERS_COUNT, -1)
                val prevPayments = prefs.getInt(KEY_LAST_PAYMENTS_COUNT, -1)
                val prevAdjustments = prefs.getInt(KEY_LAST_ADJUSTMENTS_COUNT, -1)

                val changeSummary = buildChangeSummary(
                    prevInvoices = prevInvoices,
                    currInvoices = totalInvoices,
                    prevSales = prevSales,
                    currSales = saleCount,
                    prevWeights = prevWeights,
                    currWeights = weightsCount,
                    prevCustomers = prevCustomers,
                    currCustomers = customersCount,
                    prevPayments = prevPayments,
                    currPayments = paymentsCount,
                    prevAdjustments = prevAdjustments,
                    currAdjustments = adjustmentsCount,
                    triggerReason = triggerReason
                )

                // Embed autoBackupMeta into the backup JSON itself so each file is self-describing
                root.put("autoBackupMeta", JSONObject().apply {
                    put("createdAt", now)
                    put("triggerReason", triggerReason)
                    put("changeSummary", changeSummary)
                    put("invoicesCount", totalInvoices)
                    put("customersCount", customersCount)
                    put("dataHash", currentHash)
                })

                val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val timeFormat = SimpleDateFormat("HH-mm-ss", Locale.US)
                val dayKey = dayFormat.format(Date(now))
                val timeKey = timeFormat.format(Date(now))

                val fileName = "ehsebly_backup_${dayKey}_session_${timeKey}_${now}.json"
                val targetFile = File(backupDir, fileName)
                val finalJsonBytes = root.toString(2).toByteArray(Charsets.UTF_8)
                targetFile.writeBytes(finalJsonBytes)

                // Save metadata index entry
                saveMetadataForFile(
                    backupDir = backupDir,
                    fileName = fileName,
                    changeSummary = changeSummary,
                    invoicesCount = totalInvoices,
                    customersCount = customersCount
                )

                // Apply the 30-day retention rule:
                // - Last 2 days: keep ALL session files
                // - Previous 28 days (Day 3 to Day 30): keep ONLY the latest file of each day (consolidate & delete earlier ones)
                // - Beyond 30 days: delete
                applyRetentionPolicy(backupDir)

                // Update preferences with latest state
                prefs.edit()
                    .putString(KEY_LAST_DATA_HASH, currentHash)
                    .putLong(KEY_LAST_BACKUP_TIME, now)
                    .putString(KEY_LAST_CHANGE_SUMMARY, changeSummary)
                    .putInt(KEY_LAST_INVOICES_COUNT, totalInvoices)
                    .putInt(KEY_LAST_SALE_INVOICES_COUNT, saleCount)
                    .putInt(KEY_LAST_WEIGHTS_COUNT, weightsCount)
                    .putInt(KEY_LAST_CUSTOMERS_COUNT, customersCount)
                    .putInt(KEY_LAST_PAYMENTS_COUNT, paymentsCount)
                    .putInt(KEY_LAST_ADJUSTMENTS_COUNT, adjustmentsCount)
                    .apply()

                val entriesAfterPrune = listBackupEntries(backupDir)
                val createdEntry = entriesAfterPrune.firstOrNull { it.timestamp == now }
                    ?: AutoBackupEntry(
                        fileName = targetFile.name,
                        filePath = targetFile.absolutePath,
                        dayKey = dayKey,
                        timestamp = now,
                        fileSizeBytes = targetFile.length(),
                        isConsolidatedDaily = false,
                        isWithinDetailedTwoDays = true,
                        changeSummary = changeSummary,
                        invoicesCount = totalInvoices,
                        customersCount = customersCount
                    )

                AutoBackupResult.Created(createdEntry)
            } catch (e: Exception) {
                AutoBackupResult.Error(e.localizedMessage ?: "خطأ غير متوقع أثناء النسخ الاحتياطي التلقائي")
            }
        }
    }

    /**
     * Enforces the user's exact retention policy on `backupDir`:
     * - Newest 2 distinct days (`DETAILED_HISTORY_DAYS = 2`): keep ALL session backup files (all changes across open/close cycles).
     * - Next 28 distinct days (`CONSOLIDATED_DAILY_DAYS = 28`, i.e. from Day 3 up to Day 30):
     *   consolidate each day so ONLY the LAST (latest) backup file of that day is kept, and delete all earlier session files of that day.
     * - Any day beyond the 30 days (`index >= 30`): delete all files for that day.
     */
    fun applyRetentionPolicy(backupDir: File) {
        val parsedFiles = listValidBackupFiles(backupDir)
        if (parsedFiles.isEmpty()) return

        val metadataMap = loadMetadataIndex(backupDir)
        val groupedByDay = parsedFiles.groupBy { it.dayKey }
        val sortedDaysDesc = groupedByDay.keys.sortedDescending()

        sortedDaysDesc.forEachIndexed { dayIndex, dayKey ->
            val filesOfDay = groupedByDay[dayKey].orEmpty().sortedByDescending { it.timestamp }
            when {
                dayIndex < DETAILED_HISTORY_DAYS -> {
                    // Last 2 days: keep ALL session files for full change tracking
                }

                dayIndex < MAX_RETENTION_DAYS -> {
                    // Days 3..30 (28 days): keep ONLY the final (latest) backup file of the day
                    val latestToKeep = filesOfDay.first()
                    val earlierToDelete = filesOfDay.drop(1)
                    earlierToDelete.forEach { oldItem ->
                        oldItem.file.delete()
                        metadataMap.remove(oldItem.file.name)
                    }

                    // Rename the kept file to `_daily_` if it was a `_session_` file
                    if (!latestToKeep.isDaily) {
                        val dailyFileName = "ehsebly_backup_${dayKey}_daily_${latestToKeep.timestamp}.json"
                        val dailyFile = File(backupDir, dailyFileName)
                        val oldMeta = metadataMap.remove(latestToKeep.file.name)
                        if (latestToKeep.file.renameTo(dailyFile)) {
                            if (oldMeta != null) {
                                metadataMap.put(dailyFileName, oldMeta)
                            }
                        }
                    }
                }

                else -> {
                    // Older than 30 days: delete all files of this day
                    filesOfDay.forEach { expired ->
                        expired.file.delete()
                        metadataMap.remove(expired.file.name)
                    }
                }
            }
        }

        saveMetadataIndex(backupDir, metadataMap)
    }

    fun getStatusSummary(context: Context): AutoBackupStatusSummary {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backupDir = getBackupDirectory(context)
        applyRetentionPolicy(backupDir)

        val allEntries = listBackupEntries(backupDir)
        val detailedEntries = allEntries.filter { it.isWithinDetailedTwoDays }
        val consolidatedEntries = allEntries.filter { !it.isWithinDetailedTwoDays }
        val distinctDays = allEntries.map { it.dayKey }.distinct().size
        val totalBytes = allEntries.sumOf { it.fileSizeBytes }

        return AutoBackupStatusSummary(
            isEnabled = isAutoBackupEnabled(context),
            folderPath = backupDir.absolutePath,
            customFolderUri = getCustomFolderUri(context),
            lastBackupTimestamp = prefs.getLong(KEY_LAST_BACKUP_TIME, allEntries.firstOrNull()?.timestamp ?: 0L),
            lastBackupChangeSummary = prefs.getString(
                KEY_LAST_CHANGE_SUMMARY,
                allEntries.firstOrNull()?.changeSummary ?: "لم يتم إجراء نسخ احتياطي تلقائي بعد"
            ) ?: "",
            totalDaysCount = distinctDays,
            detailedTwoDaysEntries = detailedEntries,
            consolidatedDailyEntries = consolidatedEntries,
            totalStorageBytes = totalBytes
        )
    }

    fun listBackupEntries(backupDir: File): List<AutoBackupEntry> {
        val parsed = listValidBackupFiles(backupDir)
        if (parsed.isEmpty()) return emptyList()

        val metadataMap = loadMetadataIndex(backupDir)
        val sortedDaysDesc = parsed.map { it.dayKey }.distinct().sortedDescending()
        val detailedTwoDaysSet = sortedDaysDesc.take(DETAILED_HISTORY_DAYS).toSet()

        return parsed.sortedByDescending { it.timestamp }.map { item ->
            val metaObj = metadataMap[item.file.name]
            val summary = metaObj?.changeSummary?.takeIf { it.isNotBlank() }
                ?: if (item.isDaily) "النسخة النهائية الموحدة ليوم ${item.dayKey}" else "نسخة جلسة عمل (${item.dayKey})"
            val invCount = metaObj?.invoicesCount ?: 0
            val custCount = metaObj?.customersCount ?: 0

            AutoBackupEntry(
                fileName = item.file.name,
                filePath = item.file.absolutePath,
                dayKey = item.dayKey,
                timestamp = item.timestamp,
                fileSizeBytes = item.file.length(),
                isConsolidatedDaily = item.isDaily || !detailedTwoDaysSet.contains(item.dayKey),
                isWithinDetailedTwoDays = detailedTwoDaysSet.contains(item.dayKey),
                changeSummary = summary,
                invoicesCount = invCount,
                customersCount = custCount
            )
        }
    }

    fun readBackupFileJson(entry: AutoBackupEntry): String? {
        return try {
            val file = File(entry.filePath)
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        } catch (_: Exception) {
            null
        }
    }

    fun deleteBackupEntry(context: Context, entry: AutoBackupEntry): Boolean {
        val backupDir = getBackupDirectory(context)
        val file = File(entry.filePath)
        val deleted = file.exists() && file.delete()
        if (deleted) {
            val meta = loadMetadataIndex(backupDir)
            meta.remove(entry.fileName)
            saveMetadataIndex(backupDir, meta)
        }
        return deleted
    }

    private data class ParsedBackupFile(
        val file: File,
        val dayKey: String,
        val isDaily: Boolean,
        val timestamp: Long
    )

    private data class BackupMetaRecord(
        val changeSummary: String,
        val invoicesCount: Int,
        val customersCount: Int
    )

    private fun listValidBackupFiles(backupDir: File): List<ParsedBackupFile> {
        val files = backupDir.listFiles() ?: return emptyList()
        return files.mapNotNull { f ->
            if (!f.isFile) return@mapNotNull null
            val match = fileRegex.matchEntire(f.name) ?: return@mapNotNull null
            val dayKey = match.groupValues[1]
            val modeToken = match.groupValues[2]
            val ts = match.groupValues[3].toLongOrNull() ?: f.lastModified()
            ParsedBackupFile(
                file = f,
                dayKey = dayKey,
                isDaily = modeToken == "daily",
                timestamp = ts
            )
        }
    }

    @Suppress("LongParameterList")
    private fun buildChangeSummary(
        prevInvoices: Int,
        currInvoices: Int,
        prevSales: Int,
        currSales: Int,
        prevWeights: Int,
        currWeights: Int,
        prevCustomers: Int,
        currCustomers: Int,
        prevPayments: Int,
        currPayments: Int,
        prevAdjustments: Int,
        currAdjustments: Int,
        triggerReason: String
    ): String {
        if (prevInvoices < 0) {
            return "نسخة شاملة ($currInvoices فاتورة • $currWeights وزنة • $currCustomers عميل)"
        }
        val diffs = mutableListOf<String>()
        val invDiff = currInvoices - prevInvoices
        val saleDiff = currSales - prevSales
        val weightDiff = currWeights - prevWeights
        val custDiff = currCustomers - prevCustomers
        val payDiff = currPayments - prevPayments
        val adjDiff = currAdjustments - prevAdjustments

        if (invDiff != 0) {
            val sign = if (invDiff > 0) "+$invDiff" else "$invDiff"
            diffs.add("$sign فاتورة")
        }
        if (saleDiff != 0) {
            val sign = if (saleDiff > 0) "+$saleDiff" else "$saleDiff"
            diffs.add("$sign نقلة بيع")
        }
        if (weightDiff != 0) {
            val sign = if (weightDiff > 0) "+$weightDiff" else "$weightDiff"
            diffs.add("$sign وزنة")
        }
        if (payDiff != 0) {
            val sign = if (payDiff > 0) "+$payDiff" else "$payDiff"
            diffs.add("$sign دفعة سداد")
        }
        if (adjDiff != 0) {
            val sign = if (adjDiff > 0) "+$adjDiff" else "$adjDiff"
            diffs.add("$sign تسوية جفاف")
        }
        if (custDiff != 0) {
            val sign = if (custDiff > 0) "+$custDiff" else "$custDiff"
            diffs.add("$sign عميل")
        }

        return if (diffs.isEmpty()) {
            "تعديل في بيانات الفواتير أو الإعدادات ($triggerReason) • إجمالي $currInvoices فاتورة"
        } else {
            diffs.joinToString(" • ") + " ($currInvoices فاتورة)"
        }
    }

    private fun loadMetadataIndex(backupDir: File): MutableMap<String, BackupMetaRecord> {
        val map = mutableMapOf<String, BackupMetaRecord>()
        val indexFile = File(backupDir, INDEX_FILE_NAME)
        if (!indexFile.exists()) return map
        return try {
            indexFile.readLines(Charsets.UTF_8).forEach { line ->
                val parts = line.split("\t")
                if (parts.size >= 4) {
                    val fName = parts[0]
                    val invCount = parts[1].toIntOrNull() ?: 0
                    val custCount = parts[2].toIntOrNull() ?: 0
                    val summary = parts[3]
                    map[fName] = BackupMetaRecord(summary, invCount, custCount)
                }
            }
            map
        } catch (_: Throwable) {
            map
        }
    }

    private fun saveMetadataIndex(backupDir: File, indexMap: Map<String, BackupMetaRecord>) {
        try {
            val indexFile = File(backupDir, INDEX_FILE_NAME)
            val content = indexMap.entries.joinToString("\n") { (fileName, rec) ->
                val cleanSummary = rec.changeSummary.replace("\t", " ").replace("\n", " ")
                "$fileName\t${rec.invoicesCount}\t${rec.customersCount}\t$cleanSummary"
            }
            indexFile.writeText(content, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
    }

    private fun saveMetadataForFile(
        backupDir: File,
        fileName: String,
        changeSummary: String,
        invoicesCount: Int,
        customersCount: Int
    ) {
        val indexMap = loadMetadataIndex(backupDir)
        indexMap[fileName] = BackupMetaRecord(
            changeSummary = changeSummary,
            invoicesCount = invoicesCount,
            customersCount = customersCount
        )
        saveMetadataIndex(backupDir, indexMap)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return "${CalculationEngine.formatNumber(kb, 1)} KB"
        val mb = kb / 1024.0
        return "${CalculationEngine.formatNumber(mb, 2)} MB"
    }
}
