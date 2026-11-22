package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GrainDao {

    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT COUNT(*) FROM invoices WHERE customerId = :customerId")
    suspend fun countInvoicesForCustomer(customerId: Long): Int

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY id ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET defaultUnitId = :unitId WHERE id = :productId")
    suspend fun updateProductDefaultUnit(productId: Long, unitId: Long?)

    // --- Pricing Units ---
    @Query("SELECT * FROM pricing_units ORDER BY id ASC")
    fun observePricingUnits(): Flow<List<PricingUnitEntity>>

    @Query("SELECT * FROM pricing_units ORDER BY id ASC")
    suspend fun getAllPricingUnitsOnce(): List<PricingUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricingUnit(unit: PricingUnitEntity): Long

    @Update
    suspend fun updatePricingUnit(unit: PricingUnitEntity)

    // --- Invoices ---
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC, id DESC")
    fun observeAllFullInvoices(): Flow<List<FullInvoiceDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    fun observeFullInvoiceById(invoiceId: Long): Flow<FullInvoiceDetails?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getFullInvoiceByIdOnce(invoiceId: Long): FullInvoiceDetails?

    @Query("SELECT * FROM invoices ORDER BY id ASC")
    suspend fun getAllInvoicesOnce(): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    // --- Invoice Sections ---
    @Query("SELECT * FROM invoice_sections WHERE invoiceId = :invoiceId ORDER BY sectionOrder ASC")
    suspend fun getSectionsForInvoice(invoiceId: Long): List<InvoiceSectionEntity>

    @Query("SELECT * FROM invoice_sections ORDER BY id ASC")
    suspend fun getAllSectionsOnce(): List<InvoiceSectionEntity>

    @Query("SELECT * FROM invoice_sections WHERE id = :sectionId LIMIT 1")
    suspend fun getSectionById(sectionId: Long): InvoiceSectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: InvoiceSectionEntity): Long

    @Update
    suspend fun updateSection(section: InvoiceSectionEntity)

    @Delete
    suspend fun deleteSection(section: InvoiceSectionEntity)

    // --- Weights ---
    @Query("SELECT * FROM weights WHERE sectionId = :sectionId ORDER BY sequenceNumber ASC")
    suspend fun getWeightsForSection(sectionId: Long): List<WeightEntity>

    @Query("SELECT * FROM weights ORDER BY id ASC")
    suspend fun getAllWeightsOnce(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntity): Long

    @Update
    suspend fun updateWeight(weight: WeightEntity)

    @Delete
    suspend fun deleteWeight(weight: WeightEntity)

    // --- Payments ---
    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForInvoice(invoiceId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY id ASC")
    suspend fun getAllPaymentsOnce(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    fun observeAuditLogsForInvoice(invoiceId: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY id ASC")
    suspend fun getAllAuditLogsOnce(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    // --- Inventory Adjustments (Shrinkage / Dryness / Batch Zero-Out) ---
    @Query("SELECT * FROM inventory_adjustments ORDER BY createdAt DESC, id DESC")
    fun observeInventoryAdjustments(): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM inventory_adjustments ORDER BY id ASC")
    suspend fun getAllInventoryAdjustmentsOnce(): List<InventoryAdjustmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryAdjustment(adjustment: InventoryAdjustmentEntity): Long

    @Delete
    suspend fun deleteInventoryAdjustment(adjustment: InventoryAdjustmentEntity)

    @Query("DELETE FROM inventory_adjustments")
    suspend fun clearAllInventoryAdjustments()

    // --- App Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)

    // --- Transactional Business Operations ---
    @Transaction
    suspend fun addWeightToSection(
        invoiceId: Long,
        sectionId: Long,
        weightKg: Double,
        auditReason: String? = null
    ) {
        val currentWeights = getWeightsForSection(sectionId)
        val nextSeq = (currentWeights.maxOfOrNull { it.sequenceNumber } ?: 0) + 1
        insertWeight(
            WeightEntity(
                sectionId = sectionId,
                sequenceNumber = nextSeq,
                weightKg = weightKg
            )
        )
        val section = getSectionById(sectionId)
        if (!auditReason.isNullOrBlank()) {
            insertAuditLog(
                AuditLogEntity(
                    invoiceId = invoiceId,
                    actionType = "إضافة وزن جديد",
                    targetSection = "القسم ${section?.sectionOrder ?: 1} (${section?.productNameSnapshot ?: ""})",
                    oldValue = "${currentWeights.size} شكارة",
                    newValue = "شكارة #$nextSeq بوزن $weightKg كجم",
                    reason = auditReason
                )
            )
        }
        touchInvoiceTimestamp(invoiceId)
    }

    @Transaction
    suspend fun updateWeightInSection(
        invoiceId: Long,
        weight: WeightEntity,
        newWeightKg: Double,
        auditReason: String? = null
    ) {
        val oldVal = weight.weightKg
        updateWeight(weight.copy(weightKg = newWeightKg))
        val section = getSectionById(weight.sectionId)
        if (!auditReason.isNullOrBlank()) {
            insertAuditLog(
                AuditLogEntity(
                    invoiceId = invoiceId,
                    actionType = "تعديل وزن شكارة #${weight.sequenceNumber}",
                    targetSection = "القسم ${section?.sectionOrder ?: 1} (${section?.productNameSnapshot ?: ""})",
                    oldValue = "$oldVal كجم",
                    newValue = "$newWeightKg كجم",
                    reason = auditReason
                )
            )
        }
        touchInvoiceTimestamp(invoiceId)
    }

    @Transaction
    suspend fun deleteWeightAndResequence(
        invoiceId: Long,
        weight: WeightEntity,
        auditReason: String? = null
    ) {
        val section = getSectionById(weight.sectionId)
        deleteWeight(weight)
        val remaining = getWeightsForSection(weight.sectionId).sortedBy { it.sequenceNumber }
        remaining.forEachIndexed { index, item ->
            val expectedSeq = index + 1
            if (item.sequenceNumber != expectedSeq) {
                updateWeight(item.copy(sequenceNumber = expectedSeq))
            }
        }
        if (!auditReason.isNullOrBlank()) {
            insertAuditLog(
                AuditLogEntity(
                    invoiceId = invoiceId,
                    actionType = "حذف وزن شكارة #${weight.sequenceNumber}",
                    targetSection = "القسم ${section?.sectionOrder ?: 1} (${section?.productNameSnapshot ?: ""})",
                    oldValue = "${weight.weightKg} كجم",
                    newValue = "تم الحذف وإعادة الترقيم",
                    reason = auditReason
                )
            )
        }
        touchInvoiceTimestamp(invoiceId)
    }

    @Transaction
    suspend fun deleteSectionAndReorder(
        invoiceId: Long,
        section: InvoiceSectionEntity,
        auditReason: String? = null
    ) {
        deleteSection(section)
        val remaining = getSectionsForInvoice(invoiceId).sortedBy { it.sectionOrder }
        remaining.forEachIndexed { index, sec ->
            val expectedOrder = index + 1
            if (sec.sectionOrder != expectedOrder) {
                updateSection(sec.copy(sectionOrder = expectedOrder))
            }
        }
        if (!auditReason.isNullOrBlank()) {
            insertAuditLog(
                AuditLogEntity(
                    invoiceId = invoiceId,
                    actionType = "حذف قسم كامل",
                    targetSection = "القسم ${section.sectionOrder} (${section.productNameSnapshot})",
                    oldValue = "سعر ${section.unitPrice} / ${section.unitNameSnapshot}",
                    newValue = "محذوف",
                    reason = auditReason
                )
            )
        }
        touchInvoiceTimestamp(invoiceId)
    }

    @Query("UPDATE invoices SET updatedAt = :now WHERE id = :invoiceId")
    suspend fun touchInvoiceTimestamp(invoiceId: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM weights")
    suspend fun clearAllWeights()

    @Query("DELETE FROM invoice_sections")
    suspend fun clearAllSections()

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllAuditLogs()

    @Query("DELETE FROM invoices")
    suspend fun clearAllInvoices()

    @Query("DELETE FROM pricing_units")
    suspend fun clearAllPricingUnits()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}
