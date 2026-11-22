package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppSettingsEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.FullInvoiceDetails
import com.example.data.local.GrainDatabase
import com.example.data.local.InventoryAdjustmentEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.InvoiceType
import com.example.data.local.PaymentEntity
import com.example.data.local.PaymentRecordType
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ProfitCalculationMode
import com.example.data.local.ShrinkageAdjustmentType
import com.example.data.local.WeightEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class GrainRepository(private val db: GrainDatabase) {
    private val dao = db.grainDao()

    val customersFlow: Flow<List<CustomerEntity>> = dao.observeCustomers()
    val productsFlow: Flow<List<ProductEntity>> = dao.observeProducts()
    val pricingUnitsFlow: Flow<List<PricingUnitEntity>> = dao.observePricingUnits()
    val allInvoicesFlow: Flow<List<FullInvoiceDetails>> = dao.observeAllFullInvoices()
    val inventoryAdjustmentsFlow: Flow<List<InventoryAdjustmentEntity>> = dao.observeInventoryAdjustments()
    val settingsFlow: Flow<AppSettingsEntity?> = dao.observeSettings()

    fun observeInvoiceById(invoiceId: Long): Flow<FullInvoiceDetails?> =
        dao.observeFullInvoiceById(invoiceId)

    suspend fun getInvoiceByIdOnce(invoiceId: Long): FullInvoiceDetails? =
        dao.getFullInvoiceByIdOnce(invoiceId)

    // --- Customers ---
    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            dao.insertCustomer(customer)
        } else {
            dao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun canDeleteCustomer(customerId: Long): Boolean {
        return dao.countInvoicesForCustomer(customerId) == 0
    }

    suspend fun deleteCustomer(customer: CustomerEntity): Boolean {
        if (!canDeleteCustomer(customer.id)) return false
        dao.deleteCustomer(customer)
        return true
    }

    // --- Products & Pricing Units ---
    suspend fun saveProduct(product: ProductEntity): Long {
        return if (product.id == 0L) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
            product.id
        }
    }

    suspend fun saveProductWithPricingUnit(
        product: ProductEntity,
        selectedUnitId: Long?,
        newUnitName: String? = null,
        newUnitWeightKg: Double? = null
    ): Long = db.withTransaction {
        val cleanName = product.name.trim()
        val cleanDesc = product.description.trim()
        val creatingNewUnit = !newUnitName.isNullOrBlank() && newUnitWeightKg != null && newUnitWeightKg > 0.0

        val baseProduct = product.copy(
            name = cleanName,
            description = cleanDesc,
            defaultUnitId = if (creatingNewUnit) product.defaultUnitId else selectedUnitId
        )

        val productId = if (baseProduct.id == 0L) {
            dao.insertProduct(baseProduct)
        } else {
            dao.updateProduct(baseProduct)
            baseProduct.id
        }

        if (creatingNewUnit) {
            val createdUnitId = dao.insertPricingUnit(
                PricingUnitEntity(
                    name = newUnitName!!.trim(),
                    weightKg = newUnitWeightKg!!,
                    linkedProductId = productId,
                    linkedProductName = cleanName,
                    isActive = true
                )
            )
            dao.updateProductDefaultUnit(productId, createdUnitId)
        }

        productId
    }

    suspend fun savePricingUnit(unit: PricingUnitEntity): Long = db.withTransaction {
        val unitId = if (unit.id == 0L) {
            dao.insertPricingUnit(unit)
        } else {
            dao.updatePricingUnit(unit)
            unit.id
        }
        if (unit.linkedProductId != null) {
            dao.updateProductDefaultUnit(unit.linkedProductId, unitId)
        }
        unitId
    }

    // --- Settings ---
    suspend fun saveSettings(settings: AppSettingsEntity) {
        dao.saveSettings(settings)
    }

    // --- Invoices & Sections (Purchase & Bulk Sales) ---
    suspend fun createNewDraftInvoice(
        customer: CustomerEntity?,
        initialProduct: ProductEntity,
        initialUnit: PricingUnitEntity,
        initialUnitPrice: Double,
        notes: String = ""
    ): Long = db.withTransaction {
        val currentSettings = dao.getSettingsOnce() ?: AppSettingsEntity()
        val existingInvoices = dao.getAllInvoicesOnce()
        val maxExistingNum = existingInvoices.mapNotNull { it.invoiceNumber.toIntOrNull() }.maxOrNull() ?: 0
        val nextNumber = maxOf(currentSettings.startingInvoiceNumber, maxExistingNum + 1)

        val invoiceId = dao.insertInvoice(
            InvoiceEntity(
                invoiceNumber = nextNumber.toString(),
                invoiceType = InvoiceType.PURCHASE.name,
                customerId = customer?.id,
                customerNameSnapshot = customer?.name ?: "عميل نقدي (بدون تسجيل)",
                customerPhoneSnapshot = customer?.phone ?: "",
                invoiceDate = System.currentTimeMillis(),
                status = InvoiceStatus.DRAFT.name,
                notes = notes
            )
        )

        dao.insertSection(
            InvoiceSectionEntity(
                invoiceId = invoiceId,
                sectionOrder = 1,
                productId = initialProduct.id,
                productNameSnapshot = initialProduct.name,
                unitId = initialUnit.id,
                unitNameSnapshot = initialUnit.name,
                unitWeightSnapshot = initialUnit.weightKg,
                unitPrice = initialUnitPrice
            )
        )

        dao.saveSettings(currentSettings.copy(startingInvoiceNumber = nextNumber + 1))
        invoiceId
    }

    /**
     * Creates an approved Bulk Sale Invoice (فاتورة بيع وتوريد بالوزن الكلي المقطوع)
     * with cost-per-kg snapshot, optional sale expenses, and optional initial payment.
     */
    @Suppress("LongParameterList")
    suspend fun createBulkSaleInvoice(
        customer: CustomerEntity?,
        customCustomerName: String,
        product: ProductEntity,
        unit: PricingUnitEntity,
        bulkWeightKg: Double,
        sellingUnitPrice: Double,
        costPerKgSnapshot: Double,
        saleExpenses: Double,
        initialPaidAmount: Double,
        paymentMethod: String,
        notes: String
    ): Long = db.withTransaction {
        val currentSettings = dao.getSettingsOnce() ?: AppSettingsEntity()
        val existingInvoices = dao.getAllInvoicesOnce()
        val maxExistingNum = existingInvoices.mapNotNull { it.invoiceNumber.toIntOrNull() }.maxOrNull() ?: 0
        val nextNumber = maxOf(currentSettings.startingInvoiceNumber, maxExistingNum + 1)

        val buyerName = customer?.name
            ?: customCustomerName.trim().ifBlank { "مطحن / تاجر جملة (نقدي)" }
        val buyerPhone = customer?.phone ?: ""

        val invoiceId = dao.insertInvoice(
            InvoiceEntity(
                invoiceNumber = nextNumber.toString(),
                invoiceType = InvoiceType.SALE.name,
                customerId = customer?.id,
                customerNameSnapshot = buyerName,
                customerPhoneSnapshot = buyerPhone,
                invoiceDate = System.currentTimeMillis(),
                status = InvoiceStatus.APPROVED.name,
                saleExpenses = saleExpenses.coerceAtLeast(0.0),
                costPerKgSnapshot = costPerKgSnapshot.coerceAtLeast(0.0),
                notes = notes.trim()
            )
        )

        val sectionId = dao.insertSection(
            InvoiceSectionEntity(
                invoiceId = invoiceId,
                sectionOrder = 1,
                productId = product.id,
                productNameSnapshot = product.name,
                unitId = unit.id,
                unitNameSnapshot = unit.name,
                unitWeightSnapshot = unit.weightKg,
                unitPrice = sellingUnitPrice
            )
        )

        // Store the single bulk weight entry representing the total scale weight
        dao.insertWeight(
            WeightEntity(
                sectionId = sectionId,
                sequenceNumber = 1,
                weightKg = bulkWeightKg
            )
        )

        if (initialPaidAmount > 0.0) {
            dao.insertPayment(
                PaymentEntity(
                    invoiceId = invoiceId,
                    amount = initialPaidAmount,
                    type = PaymentRecordType.PAYMENT.name,
                    paymentMethod = paymentMethod.ifBlank { "نقدي" },
                    notes = "دفعة تحصيل عند إصدار فاتورة البيع"
                )
            )
        }

        dao.insertAuditLog(
            AuditLogEntity(
                invoiceId = invoiceId,
                actionType = "إصدار فاتورة بيع وتوريد مقطوعة",
                targetSection = "الشونة والمبيعات (${product.name})",
                oldValue = "وزن مقطوع: $bulkWeightKg كجم",
                newValue = "سعر بيع: $sellingUnitPrice / ${unit.name}",
                reason = notes.ifBlank { "توريد وبيع من الشونة" }
            )
        )

        dao.saveSettings(currentSettings.copy(startingInvoiceNumber = nextNumber + 1))
        invoiceId
    }

    // --- Inventory Adjustments (Shrinkage / Dryness / Batch Zero-Out) ---
    suspend fun recordInventoryAdjustment(
        productId: Long?,
        productName: String,
        adjustmentType: ShrinkageAdjustmentType,
        weightLossKg: Double,
        referencePurchasedKg: Double,
        reason: String
    ): Long {
        return dao.insertInventoryAdjustment(
            InventoryAdjustmentEntity(
                productId = productId,
                productNameSnapshot = productName,
                adjustmentType = adjustmentType.name,
                weightLossKg = weightLossKg,
                referencePurchasedKg = referencePurchasedKg,
                reason = reason.trim().ifBlank { adjustmentType.labelAr },
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteInventoryAdjustment(adjustment: InventoryAdjustmentEntity) {
        dao.deleteInventoryAdjustment(adjustment)
    }

    suspend fun updateInvoiceHeader(
        invoice: InvoiceEntity,
        customer: CustomerEntity?,
        notes: String,
        auditReason: String? = null
    ) = db.withTransaction {
        val newCustomerName = customer?.name ?: "عميل نقدي (بدون تسجيل)"
        val newCustomerPhone = customer?.phone ?: ""
        if (!auditReason.isNullOrBlank()) {
            dao.insertAuditLog(
                AuditLogEntity(
                    invoiceId = invoice.id,
                    actionType = "تعديل بيانات الفاتورة الأساسية",
                    targetSection = "رأس الفاتورة",
                    oldValue = "${invoice.customerNameSnapshot} | ${invoice.notes}",
                    newValue = "$newCustomerName | $notes",
                    reason = auditReason
                )
            )
        }
        dao.updateInvoice(
            invoice.copy(
                customerId = customer?.id,
                customerNameSnapshot = newCustomerName,
                customerPhoneSnapshot = newCustomerPhone,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun approveInvoice(invoice: InvoiceEntity) = db.withTransaction {
        dao.updateInvoice(
            invoice.copy(
                status = InvoiceStatus.APPROVED.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                invoiceId = invoice.id,
                actionType = "اعتماد الفاتورة",
                targetSection = "الفاتورة #${invoice.invoiceNumber}",
                oldValue = "مسودة",
                newValue = "معتمدة",
                reason = "اعتماد نهائي للفاتورة"
            )
        )
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        dao.deleteInvoice(invoice)
    }

    suspend fun addNewSection(
        invoiceId: Long,
        product: ProductEntity,
        unit: PricingUnitEntity,
        unitPrice: Double,
        auditReason: String? = null
    ): Long = db.withTransaction {
        val existingSections = dao.getSectionsForInvoice(invoiceId)
        val nextOrder = (existingSections.maxOfOrNull { it.sectionOrder } ?: 0) + 1
        val newSectionId = dao.insertSection(
            InvoiceSectionEntity(
                invoiceId = invoiceId,
                sectionOrder = nextOrder,
                productId = product.id,
                productNameSnapshot = product.name,
                unitId = unit.id,
                unitNameSnapshot = unit.name,
                unitWeightSnapshot = unit.weightKg,
                unitPrice = unitPrice
            )
        )
        if (!auditReason.isNullOrBlank()) {
            dao.insertAuditLog(
                AuditLogEntity(
                    invoiceId = invoiceId,
                    actionType = "إضافة قسم جديد",
                    targetSection = "القسم $nextOrder (${product.name})",
                    oldValue = "عدد الأقسام ${existingSections.size}",
                    newValue = "${product.name} بسعر $unitPrice / ${unit.name}",
                    reason = auditReason
                )
            )
        }
        dao.touchInvoiceTimestamp(invoiceId)
        newSectionId
    }

    suspend fun updateSectionConfig(
        section: InvoiceSectionEntity,
        product: ProductEntity,
        unit: PricingUnitEntity,
        newUnitPrice: Double,
        preserveHistoricalUnitWeight: Boolean = false,
        auditReason: String? = null
    ) = db.withTransaction {
        val effectiveWeightSnapshot = if (preserveHistoricalUnitWeight && section.unitId == unit.id) {
            section.unitWeightSnapshot
        } else {
            unit.weightKg
        }
        if (!auditReason.isNullOrBlank()) {
            dao.insertAuditLog(
                AuditLogEntity(
                    invoiceId = section.invoiceId,
                    actionType = "تعديل تسعير أو محصول القسم",
                    targetSection = "القسم ${section.sectionOrder}",
                    oldValue = "${section.productNameSnapshot} - ${section.unitPrice} لكل ${section.unitNameSnapshot} (${section.unitWeightSnapshot} كجم)",
                    newValue = "${product.name} - $newUnitPrice لكل ${unit.name} ($effectiveWeightSnapshot كجم)",
                    reason = auditReason
                )
            )
        }
        dao.updateSection(
            section.copy(
                productId = product.id,
                productNameSnapshot = product.name,
                unitId = unit.id,
                unitNameSnapshot = unit.name,
                unitWeightSnapshot = effectiveWeightSnapshot,
                unitPrice = newUnitPrice
            )
        )
        dao.touchInvoiceTimestamp(section.invoiceId)
    }

    suspend fun deleteSection(
        invoiceId: Long,
        section: InvoiceSectionEntity,
        auditReason: String? = null
    ) {
        dao.deleteSectionAndReorder(invoiceId, section, auditReason)
    }

    // --- Weights ---
    suspend fun addWeight(
        invoiceId: Long,
        sectionId: Long,
        weightKg: Double,
        auditReason: String? = null
    ) {
        dao.addWeightToSection(invoiceId, sectionId, weightKg, auditReason)
    }

    suspend fun updateWeight(
        invoiceId: Long,
        weight: WeightEntity,
        newWeightKg: Double,
        auditReason: String? = null
    ) {
        dao.updateWeightInSection(invoiceId, weight, newWeightKg, auditReason)
    }

    suspend fun deleteWeight(
        invoiceId: Long,
        weight: WeightEntity,
        auditReason: String? = null
    ) {
        dao.deleteWeightAndResequence(invoiceId, weight, auditReason)
    }

    // --- Payments & Overpayment Settlement ---
    suspend fun recordPayment(
        invoiceId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String
    ) = db.withTransaction {
        dao.insertPayment(
            PaymentEntity(
                invoiceId = invoiceId,
                amount = amount,
                type = PaymentRecordType.PAYMENT.name,
                paymentMethod = paymentMethod,
                notes = notes
            )
        )
        dao.touchInvoiceTimestamp(invoiceId)
    }

    suspend fun settleOverpayment(
        invoice: InvoiceEntity,
        overpaidAmount: Double,
        settleAsCustomerCredit: Boolean,
        notes: String
    ) = db.withTransaction {
        val recordType = if (settleAsCustomerCredit && invoice.customerId != null) {
            PaymentRecordType.CREDIT_TRANSFER
        } else {
            PaymentRecordType.REFUND
        }

        dao.insertPayment(
            PaymentEntity(
                invoiceId = invoice.id,
                amount = overpaidAmount,
                type = recordType.name,
                paymentMethod = if (recordType == PaymentRecordType.CREDIT_TRANSFER) "ترحيل لرصيد العميل" else "رد نقدي",
                notes = notes.ifBlank { recordType.labelAr }
            )
        )

        if (recordType == PaymentRecordType.CREDIT_TRANSFER && invoice.customerId != null) {
            val customer = dao.getCustomerById(invoice.customerId)
            if (customer != null) {
                dao.updateCustomer(
                    customer.copy(creditBalance = customer.creditBalance + overpaidAmount)
                )
            }
        }

        dao.insertAuditLog(
            AuditLogEntity(
                invoiceId = invoice.id,
                actionType = "تسوية فائض مدفوعات",
                targetSection = "الحساب المالي",
                oldValue = "فائض مسدد: $overpaidAmount",
                newValue = recordType.labelAr,
                reason = notes.ifBlank { "معالجة فائض السداد بعد تعديل الفاتورة" }
            )
        )
        dao.touchInvoiceTimestamp(invoice.id)
    }

    // --- Backup & Restore (Offline JSON) ---
    suspend fun exportBackupJson(): String = db.withTransaction {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val settings = dao.getSettingsOnce() ?: AppSettingsEntity()
        root.put("settings", JSONObject().apply {
            put("merchantName", settings.merchantName)
            put("merchantPhone", settings.merchantPhone)
            put("merchantAddress", settings.merchantAddress)
            put("currencySymbol", settings.currencySymbol)
            put("startingInvoiceNumber", settings.startingInvoiceNumber)
            put("invoiceFooterNotes", settings.invoiceFooterNotes)
            put("profitCalculationMode", settings.profitCalculationMode)
        })

        val customersArr = JSONArray()
        dao.getAllCustomersOnce().forEach { c ->
            customersArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("notes", c.notes)
                put("creditBalance", c.creditBalance)
                put("createdAt", c.createdAt)
            })
        }
        root.put("customers", customersArr)

        val productsArr = JSONArray()
        dao.getAllProductsOnce().forEach { p ->
            productsArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("defaultUnitId", p.defaultUnitId ?: JSONObject.NULL)
                put("isActive", p.isActive)
            })
        }
        root.put("products", productsArr)

        val unitsArr = JSONArray()
        dao.getAllPricingUnitsOnce().forEach { u ->
            unitsArr.put(JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("weightKg", u.weightKg)
                put("linkedProductId", u.linkedProductId ?: JSONObject.NULL)
                put("linkedProductName", u.linkedProductName ?: "")
                put("isActive", u.isActive)
            })
        }
        root.put("pricingUnits", unitsArr)

        val invoicesArr = JSONArray()
        dao.getAllInvoicesOnce().forEach { i ->
            invoicesArr.put(JSONObject().apply {
                put("id", i.id)
                put("invoiceNumber", i.invoiceNumber)
                put("invoiceType", i.invoiceType)
                put("customerId", i.customerId ?: JSONObject.NULL)
                put("customerNameSnapshot", i.customerNameSnapshot)
                put("customerPhoneSnapshot", i.customerPhoneSnapshot)
                put("invoiceDate", i.invoiceDate)
                put("status", i.status)
                put("saleExpenses", i.saleExpenses)
                put("costPerKgSnapshot", i.costPerKgSnapshot)
                put("notes", i.notes)
                put("createdAt", i.createdAt)
                put("updatedAt", i.updatedAt)
            })
        }
        root.put("invoices", invoicesArr)

        val sectionsArr = JSONArray()
        dao.getAllSectionsOnce().forEach { s ->
            sectionsArr.put(JSONObject().apply {
                put("id", s.id)
                put("invoiceId", s.invoiceId)
                put("sectionOrder", s.sectionOrder)
                put("productId", s.productId)
                put("productNameSnapshot", s.productNameSnapshot)
                put("unitId", s.unitId)
                put("unitNameSnapshot", s.unitNameSnapshot)
                put("unitWeightSnapshot", s.unitWeightSnapshot)
                put("unitPrice", s.unitPrice)
            })
        }
        root.put("sections", sectionsArr)

        val weightsArr = JSONArray()
        dao.getAllWeightsOnce().forEach { w ->
            weightsArr.put(JSONObject().apply {
                put("id", w.id)
                put("sectionId", w.sectionId)
                put("sequenceNumber", w.sequenceNumber)
                put("weightKg", w.weightKg)
                put("createdAt", w.createdAt)
            })
        }
        root.put("weights", weightsArr)

        val paymentsArr = JSONArray()
        dao.getAllPaymentsOnce().forEach { pay ->
            paymentsArr.put(JSONObject().apply {
                put("id", pay.id)
                put("invoiceId", pay.invoiceId)
                put("amount", pay.amount)
                put("type", pay.type)
                put("paymentMethod", pay.paymentMethod)
                put("paymentDate", pay.paymentDate)
                put("notes", pay.notes)
            })
        }
        root.put("payments", paymentsArr)

        val logsArr = JSONArray()
        dao.getAllAuditLogsOnce().forEach { log ->
            logsArr.put(JSONObject().apply {
                put("id", log.id)
                put("invoiceId", log.invoiceId)
                put("actionType", log.actionType)
                put("targetSection", log.targetSection)
                put("oldValue", log.oldValue)
                put("newValue", log.newValue)
                put("reason", log.reason)
                put("timestamp", log.timestamp)
            })
        }
        root.put("auditLogs", logsArr)

        val adjustmentsArr = JSONArray()
        dao.getAllInventoryAdjustmentsOnce().forEach { adj ->
            adjustmentsArr.put(JSONObject().apply {
                put("id", adj.id)
                put("productId", adj.productId ?: JSONObject.NULL)
                put("productNameSnapshot", adj.productNameSnapshot)
                put("adjustmentType", adj.adjustmentType)
                put("weightLossKg", adj.weightLossKg)
                put("referencePurchasedKg", adj.referencePurchasedKg)
                put("reason", adj.reason)
                put("createdAt", adj.createdAt)
            })
        }
        root.put("inventoryAdjustments", adjustmentsArr)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = db.withTransaction {
        val root = JSONObject(jsonString)
        dao.clearAllWeights()
        dao.clearAllSections()
        dao.clearAllPayments()
        dao.clearAllAuditLogs()
        dao.clearAllInventoryAdjustments()
        dao.clearAllInvoices()
        dao.clearAllPricingUnits()
        dao.clearAllProducts()
        dao.clearAllCustomers()

        root.optJSONObject("settings")?.let { s ->
            dao.saveSettings(
                AppSettingsEntity(
                    id = 1,
                    merchantName = s.optString("merchantName", ""),
                    merchantPhone = s.optString("merchantPhone", ""),
                    merchantAddress = s.optString("merchantAddress", ""),
                    currencySymbol = s.optString("currencySymbol", "ج.م"),
                    startingInvoiceNumber = s.optInt("startingInvoiceNumber", 1001),
                    invoiceFooterNotes = s.optString("invoiceFooterNotes", ""),
                    profitCalculationMode = s.optString(
                        "profitCalculationMode",
                        ProfitCalculationMode.WITH_EXPENSES.name
                    )
                )
            )
        }

        root.optJSONArray("customers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertCustomer(
                    CustomerEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        address = o.optString("address", ""),
                        notes = o.optString("notes", ""),
                        creditBalance = o.optDouble("creditBalance", 0.0),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        root.optJSONArray("products")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val defUnitId = if (o.isNull("defaultUnitId")) null else o.optLong("defaultUnitId").takeIf { it > 0L }
                dao.insertProduct(
                    ProductEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        description = o.optString("description", ""),
                        defaultUnitId = defUnitId,
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
        }

        root.optJSONArray("pricingUnits")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val linkedId = if (o.isNull("linkedProductId")) null else o.optLong("linkedProductId")
                dao.insertPricingUnit(
                    PricingUnitEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        weightKg = o.getDouble("weightKg"),
                        linkedProductId = linkedId,
                        linkedProductName = o.optString("linkedProductName", ""),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
        }

        root.optJSONArray("invoices")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val custId = if (o.isNull("customerId")) null else o.optLong("customerId")
                dao.insertInvoice(
                    InvoiceEntity(
                        id = o.getLong("id"),
                        invoiceNumber = o.getString("invoiceNumber"),
                        invoiceType = o.optString("invoiceType", InvoiceType.PURCHASE.name),
                        customerId = custId,
                        customerNameSnapshot = o.optString("customerNameSnapshot", "عميل نقدي"),
                        customerPhoneSnapshot = o.optString("customerPhoneSnapshot", ""),
                        invoiceDate = o.optLong("invoiceDate", System.currentTimeMillis()),
                        status = o.optString("status", InvoiceStatus.DRAFT.name),
                        saleExpenses = o.optDouble("saleExpenses", 0.0),
                        costPerKgSnapshot = o.optDouble("costPerKgSnapshot", 0.0),
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        root.optJSONArray("sections")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertSection(
                    InvoiceSectionEntity(
                        id = o.getLong("id"),
                        invoiceId = o.getLong("invoiceId"),
                        sectionOrder = o.getInt("sectionOrder"),
                        productId = o.getLong("productId"),
                        productNameSnapshot = o.getString("productNameSnapshot"),
                        unitId = o.getLong("unitId"),
                        unitNameSnapshot = o.getString("unitNameSnapshot"),
                        unitWeightSnapshot = o.getDouble("unitWeightSnapshot"),
                        unitPrice = o.getDouble("unitPrice")
                    )
                )
            }
        }

        root.optJSONArray("weights")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertWeight(
                    WeightEntity(
                        id = o.getLong("id"),
                        sectionId = o.getLong("sectionId"),
                        sequenceNumber = o.getInt("sequenceNumber"),
                        weightKg = o.getDouble("weightKg"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        root.optJSONArray("payments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertPayment(
                    PaymentEntity(
                        id = o.getLong("id"),
                        invoiceId = o.getLong("invoiceId"),
                        amount = o.getDouble("amount"),
                        type = o.optString("type", PaymentRecordType.PAYMENT.name),
                        paymentMethod = o.optString("paymentMethod", "نقدي"),
                        paymentDate = o.optLong("paymentDate", System.currentTimeMillis()),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        root.optJSONArray("auditLogs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertAuditLog(
                    AuditLogEntity(
                        id = o.getLong("id"),
                        invoiceId = o.getLong("invoiceId"),
                        actionType = o.getString("actionType"),
                        targetSection = o.optString("targetSection", ""),
                        oldValue = o.optString("oldValue", ""),
                        newValue = o.optString("newValue", ""),
                        reason = o.optString("reason", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        root.optJSONArray("inventoryAdjustments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val prodId = if (o.isNull("productId")) null else o.optLong("productId")
                dao.insertInventoryAdjustment(
                    InventoryAdjustmentEntity(
                        id = o.getLong("id"),
                        productId = prodId,
                        productNameSnapshot = o.optString("productNameSnapshot", ""),
                        adjustmentType = o.optString(
                            "adjustmentType",
                            ShrinkageAdjustmentType.MANUAL_SHRINKAGE.name
                        ),
                        weightLossKg = o.getDouble("weightLossKg"),
                        referencePurchasedKg = o.optDouble("referencePurchasedKg", 0.0),
                        reason = o.optString("reason", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }
        true
    }
}
