package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.FullInvoiceDetails
import com.example.data.local.GrainDatabase
import com.example.data.local.InventoryAdjustmentEntity
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.InvoiceType
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ShrinkageAdjustmentType
import com.example.data.local.WeightEntity
import com.example.data.repository.GrainRepository
import com.example.domain.CalculationEngine
import com.example.domain.InvoiceCalculation
import com.example.domain.WarehouseSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val titleAr: String) {
    DASHBOARD("الرئيسية"),
    PURCHASE_WORKSPACE("عملية شراء"),
    WAREHOUSE("الشونة والمبيعات"),
    INVOICES("الفواتير والعمليات"),
    CUSTOMERS("إدارة العملاء"),
    REPORTS("التقارير والإحصائيات"),
    DEFINITIONS("التعريفات والوحدات"),
    SETTINGS("الإعدادات والترخيص")
}

enum class DashboardPeriod(val labelAr: String) {
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    ALL("كل الفترات")
}

data class InvoiceWithCalc(
    val details: FullInvoiceDetails,
    val calc: InvoiceCalculation
)

@OptIn(ExperimentalCoroutinesApi::class)
class GrainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GrainRepository(GrainDatabase.getInstance(application))
    private val prefs = application.getSharedPreferences("grain_workspace_prefs", Context.MODE_PRIVATE)

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _dashboardPeriod = MutableStateFlow(DashboardPeriod.ALL)
    val dashboardPeriod: StateFlow<DashboardPeriod> = _dashboardPeriod.asStateFlow()

    private val _activeInvoiceId = MutableStateFlow<Long?>(null)
    val activeInvoiceId: StateFlow<Long?> = _activeInvoiceId.asStateFlow()

    private val _activeSectionId = MutableStateFlow<Long?>(null)
    val activeSectionId: StateFlow<Long?> = _activeSectionId.asStateFlow()

    private val _keypadInput = MutableStateFlow("")
    val keypadInput: StateFlow<String> = _keypadInput.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val customers: StateFlow<List<CustomerEntity>> = repository.customersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.productsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pricingUnits: StateFlow<List<PricingUnitEntity>> = repository.pricingUnitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryAdjustments: StateFlow<List<InventoryAdjustmentEntity>> = repository.inventoryAdjustmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity> = repository.settingsFlow
        .combine(flowOf(AppSettingsEntity())) { dbSettings, fallback -> dbSettings ?: fallback }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val allInvoicesWithCalc: StateFlow<List<InvoiceWithCalc>> = repository.allInvoicesFlow
        .combine(flowOf(Unit)) { list, _ ->
            list.map { details ->
                InvoiceWithCalc(
                    details = details,
                    calc = CalculationEngine.calculateInvoice(details)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouseSummary: StateFlow<WarehouseSummary> = combine(
        products,
        pricingUnits,
        allInvoicesWithCalc,
        inventoryAdjustments
    ) { prods, units, invs, adjs ->
        CalculationEngine.calculateWarehouseSummary(
            products = prods,
            pricingUnits = units,
            allInvoices = invs.map { it.details to it.calc },
            adjustments = adjs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WarehouseSummary(
            productStocks = emptyList(),
            totalAvailableWeightKg = 0.0,
            totalStockValuation = 0.0,
            totalPurchasedWeightKg = 0.0,
            totalSoldWeightKg = 0.0,
            totalShrinkageWeightKg = 0.0,
            overallShrinkagePercent = 0.0,
            totalSalesRevenue = 0.0,
            totalSalesCostOfGoods = 0.0,
            totalSalesExpenses = 0.0,
            totalDirectProfit = 0.0,
            totalNetProfitAfterExpenses = 0.0
        )
    )

    val activeInvoiceWithCalc: StateFlow<InvoiceWithCalc?> = _activeInvoiceId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeInvoiceById(id)
        }
        .combine(flowOf(Unit)) { details, _ ->
            details?.let {
                InvoiceWithCalc(
                    details = it,
                    calc = CalculationEngine.calculateInvoice(it)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Restore active draft purchase invoice automatically if app was closed or restarted
        viewModelScope.launch {
            val savedInvId = prefs.getLong(KEY_ACTIVE_INVOICE_ID, -1L).takeIf { it > 0L }
            val savedSecId = prefs.getLong(KEY_ACTIVE_SECTION_ID, -1L).takeIf { it > 0L }

            if (savedInvId != null) {
                val existing = repository.getInvoiceByIdOnce(savedInvId)
                if (
                    existing != null &&
                    existing.invoice.status == InvoiceStatus.DRAFT.name &&
                    existing.invoice.invoiceType == InvoiceType.PURCHASE.name
                ) {
                    _activeInvoiceId.value = savedInvId
                    _activeSectionId.value = savedSecId
                        ?: existing.sections.maxByOrNull { it.section.sectionOrder }?.section?.id
                    return@launch
                }
            }

            // Fallback: check if there is any existing DRAFT purchase invoice to resume
            val all = repository.allInvoicesFlow.first()
            val latestDraft = all.firstOrNull {
                it.invoice.status == InvoiceStatus.DRAFT.name &&
                    it.invoice.invoiceType == InvoiceType.PURCHASE.name
            }
            if (latestDraft != null) {
                setActiveWorkspaceIds(
                    invoiceId = latestDraft.invoice.id,
                    sectionId = latestDraft.sections.maxByOrNull { it.section.sectionOrder }?.section?.id
                )
            }
        }
    }

    private fun setActiveWorkspaceIds(invoiceId: Long?, sectionId: Long?) {
        _activeInvoiceId.value = invoiceId
        _activeSectionId.value = sectionId
        prefs.edit().apply {
            if (invoiceId != null) putLong(KEY_ACTIVE_INVOICE_ID, invoiceId) else remove(KEY_ACTIVE_INVOICE_ID)
            if (sectionId != null) putLong(KEY_ACTIVE_SECTION_ID, sectionId) else remove(KEY_ACTIVE_SECTION_ID)
        }.apply()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openPurchaseTabOrResumeDraft() {
        viewModelScope.launch {
            val currentId = _activeInvoiceId.value
            if (currentId != null) {
                val currentInv = repository.getInvoiceByIdOnce(currentId)
                if (
                    currentInv != null &&
                    currentInv.invoice.status == InvoiceStatus.DRAFT.name &&
                    currentInv.invoice.invoiceType == InvoiceType.PURCHASE.name
                ) {
                    _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
                    return@launch
                }
            }
            // Look for any suspended/draft purchase invoice first
            val all = repository.allInvoicesFlow.first()
            val latestDraft = all.firstOrNull {
                it.invoice.status == InvoiceStatus.DRAFT.name &&
                    it.invoice.invoiceType == InvoiceType.PURCHASE.name
            }
            if (latestDraft != null) {
                setActiveWorkspaceIds(
                    invoiceId = latestDraft.invoice.id,
                    sectionId = latestDraft.sections.maxByOrNull { it.section.sectionOrder }?.section?.id
                )
                _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
            } else {
                startNewPurchase()
            }
        }
    }

    fun setDashboardPeriod(period: DashboardPeriod) {
        _dashboardPeriod.value = period
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    // --- Keypad Controls ---
    fun onKeypadDigit(digit: String) {
        val current = _keypadInput.value
        if (digit == ".") {
            if (current.contains(".")) return
            _keypadInput.value = if (current.isEmpty()) "0." else "$current."
        } else {
            if (current.length >= 8) return
            val parts = current.split(".")
            if (parts.size == 2 && parts[1].length >= 2) return
            _keypadInput.value = current + digit
        }
    }

    fun onKeypadBackspace() {
        val current = _keypadInput.value
        if (current.isNotEmpty()) {
            _keypadInput.value = current.dropLast(1)
        }
    }

    fun onKeypadClear() {
        _keypadInput.value = ""
    }

    // --- Purchase Workspace Actions ---
    fun startNewPurchase(
        customer: CustomerEntity? = null,
        initialProduct: ProductEntity? = null,
        initialUnit: PricingUnitEntity? = null,
        initialPrice: Double = 4000.0
    ) {
        viewModelScope.launch {
            val currentId = _activeInvoiceId.value
            if (currentId != null && customer == null) {
                val existing = repository.getInvoiceByIdOnce(currentId)
                val totalWeights = existing?.sections?.sumOf { it.weights.size } ?: -1
                if (
                    existing != null &&
                    existing.invoice.status == InvoiceStatus.DRAFT.name &&
                    existing.invoice.invoiceType == InvoiceType.PURCHASE.name &&
                    totalWeights == 0
                ) {
                    setActiveWorkspaceIds(
                        invoiceId = existing.invoice.id,
                        sectionId = existing.sections.firstOrNull()?.section?.id
                    )
                    _keypadInput.value = ""
                    _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
                    return@launch
                }
            }

            createBlankDraftInvoiceInternal(customer, initialProduct, initialUnit, initialPrice)
        }
    }

    fun suspendCurrentInvoiceAndStartNew() {
        viewModelScope.launch {
            val currentInv = activeInvoiceWithCalc.value?.details?.invoice
            createBlankDraftInvoiceInternal(
                customer = null,
                initialProduct = null,
                initialUnit = null,
                initialPrice = 4000.0
            )
            if (currentInv != null) {
                showMessage("تم تعليق الفاتورة #${currentInv.invoiceNumber} وحفظ بياناتها، وفتح فاتورة جديدة")
            } else {
                showMessage("تم فتح فاتورة جديدة")
            }
        }
    }

    private suspend fun createBlankDraftInvoiceInternal(
        customer: CustomerEntity?,
        initialProduct: ProductEntity?,
        initialUnit: PricingUnitEntity?,
        initialPrice: Double
    ) {
        val activeProducts = products.value.filter { it.isActive }.ifEmpty { products.value }
        val activeUnits = pricingUnits.value.filter { it.isActive }.ifEmpty { pricingUnits.value }

        val chosenProduct = initialProduct ?: activeProducts.firstOrNull()
            ?: ProductEntity(id = 1, name = "قمح")
        val chosenUnit = initialUnit
            ?: activeUnits.firstOrNull { it.linkedProductId == chosenProduct.id }
            ?: activeUnits.firstOrNull()
            ?: PricingUnitEntity(id = 1, name = "طن", weightKg = 1000.0)

        val newId = repository.createNewDraftInvoice(
            customer = customer,
            initialProduct = chosenProduct,
            initialUnit = chosenUnit,
            initialUnitPrice = initialPrice
        )
        val created = repository.getInvoiceByIdOnce(newId)
        setActiveWorkspaceIds(
            invoiceId = newId,
            sectionId = created?.sections?.firstOrNull()?.section?.id
        )
        _keypadInput.value = ""
        _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
    }

    fun openExistingInvoiceInWorkspace(invoiceId: Long) {
        viewModelScope.launch {
            val existing = repository.getInvoiceByIdOnce(invoiceId) ?: return@launch
            if (existing.invoice.invoiceType == InvoiceType.SALE.name) {
                // Sales invoices are bulk-weight invoices; navigate to Invoices screen if not already there
                _currentScreen.value = AppScreen.INVOICES
                showMessage("فاتورة البيع #${existing.invoice.invoiceNumber} بالوزن الكلي المقطوع")
                return@launch
            }
            setActiveWorkspaceIds(
                invoiceId = invoiceId,
                sectionId = existing.sections.minByOrNull { it.section.sectionOrder }?.section?.id
            )
            _keypadInput.value = ""
            _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
        }
    }

    fun selectActiveSection(sectionId: Long) {
        setActiveWorkspaceIds(_activeInvoiceId.value, sectionId)
    }

    fun addWeightFromKeypad(auditReason: String? = null) {
        val weightVal = _keypadInput.value.toDoubleOrNull()
        if (weightVal == null || weightVal <= 0.0) {
            showMessage("يرجى إدخال وزن صحيح أكبر من صفر")
            return
        }
        val inv = activeInvoiceWithCalc.value ?: return
        val targetSectionId = _activeSectionId.value
            ?: inv.calc.sectionCalculations.lastOrNull()?.section?.id
            ?: return

        viewModelScope.launch {
            repository.addWeight(
                invoiceId = inv.details.invoice.id,
                sectionId = targetSectionId,
                weightKg = weightVal,
                auditReason = auditReason
            )
            _keypadInput.value = ""
        }
    }

    fun updateWeightValue(
        weight: WeightEntity,
        newWeightKg: Double,
        auditReason: String? = null
    ) {
        val inv = activeInvoiceWithCalc.value ?: return
        if (newWeightKg <= 0.0) {
            showMessage("الوزن يجب أن يكون أكبر من صفر")
            return
        }
        viewModelScope.launch {
            repository.updateWeight(
                invoiceId = inv.details.invoice.id,
                weight = weight,
                newWeightKg = newWeightKg,
                auditReason = auditReason
            )
            showMessage("تم تحديث وزن الشكارة #${weight.sequenceNumber}")
        }
    }

    fun deleteWeightItem(
        weight: WeightEntity,
        auditReason: String? = null
    ) {
        val inv = activeInvoiceWithCalc.value ?: return
        viewModelScope.launch {
            repository.deleteWeight(
                invoiceId = inv.details.invoice.id,
                weight = weight,
                auditReason = auditReason
            )
            showMessage("تم حذف الشكارة #${weight.sequenceNumber} وإعادة ترتيب القسم")
        }
    }

    fun addSectionToActiveInvoice(
        product: ProductEntity,
        unit: PricingUnitEntity,
        unitPrice: Double,
        auditReason: String? = null
    ) {
        val inv = activeInvoiceWithCalc.value ?: return
        viewModelScope.launch {
            val newSecId = repository.addNewSection(
                invoiceId = inv.details.invoice.id,
                product = product,
                unit = unit,
                unitPrice = unitPrice,
                auditReason = auditReason
            )
            setActiveWorkspaceIds(inv.details.invoice.id, newSecId)
            showMessage("تم إضافة قسم جديد (${product.name})")
        }
    }

    fun updateSectionSettings(
        section: InvoiceSectionEntity,
        product: ProductEntity,
        unit: PricingUnitEntity,
        newPrice: Double,
        preserveHistoricalWeight: Boolean = false,
        auditReason: String? = null
    ) {
        viewModelScope.launch {
            repository.updateSectionConfig(
                section = section,
                product = product,
                unit = unit,
                newUnitPrice = newPrice,
                preserveHistoricalUnitWeight = preserveHistoricalWeight,
                auditReason = auditReason
            )
        }
    }

    fun deleteSectionFromActiveInvoice(
        section: InvoiceSectionEntity,
        auditReason: String? = null
    ) {
        val inv = activeInvoiceWithCalc.value ?: return
        if (inv.calc.sectionCalculations.size <= 1) {
            showMessage("لا يمكن حذف القسم الوحيد في الفاتورة")
            return
        }
        viewModelScope.launch {
            repository.deleteSection(
                invoiceId = inv.details.invoice.id,
                section = section,
                auditReason = auditReason
            )
            val updated = repository.getInvoiceByIdOnce(inv.details.invoice.id)
            setActiveWorkspaceIds(inv.details.invoice.id, updated?.sections?.firstOrNull()?.section?.id)
            showMessage("تم حذف القسم وإعادة حساب الفاتورة")
        }
    }

    fun updateActiveInvoiceHeader(
        customer: CustomerEntity?,
        notes: String,
        auditReason: String? = null
    ) {
        val inv = activeInvoiceWithCalc.value ?: return
        viewModelScope.launch {
            repository.updateInvoiceHeader(
                invoice = inv.details.invoice,
                customer = customer,
                notes = notes,
                auditReason = auditReason
            )
        }
    }

    fun approveActiveInvoice() {
        val inv = activeInvoiceWithCalc.value ?: return
        if (inv.calc.totalBagCount == 0) {
            showMessage("لا يمكن حفظ واعتماد فاتورة فارغة بدون أوزان")
            return
        }
        viewModelScope.launch {
            val approvedNum = inv.details.invoice.invoiceNumber
            repository.approveInvoice(inv.details.invoice)
            createBlankDraftInvoiceInternal(
                customer = null,
                initialProduct = null,
                initialUnit = null,
                initialPrice = 4000.0
            )
            showMessage("تم حفظ واعتماد الفاتورة #$approvedNum وإضافتها لرصيد الشونة")
        }
    }

    // --- Live Warehouse & Bulk Sales Actions ---
    @Suppress("LongParameterList")
    fun createBulkSaleInvoice(
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
        notes: String,
        onCreated: ((Long) -> Unit)? = null
    ) {
        if (bulkWeightKg <= 0.0) {
            showMessage("يرجى إدخال وزن كلي صحيح أكبر من صفر")
            return
        }
        if (sellingUnitPrice <= 0.0) {
            showMessage("يرجى إدخال سعر بيع الوحدة")
            return
        }
        viewModelScope.launch {
            val invoiceId = repository.createBulkSaleInvoice(
                customer = customer,
                customCustomerName = customCustomerName,
                product = product,
                unit = unit,
                bulkWeightKg = bulkWeightKg,
                sellingUnitPrice = sellingUnitPrice,
                costPerKgSnapshot = costPerKgSnapshot,
                saleExpenses = saleExpenses,
                initialPaidAmount = initialPaidAmount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            val created = repository.getInvoiceByIdOnce(invoiceId)
            val invNum = created?.invoice?.invoiceNumber ?: invoiceId.toString()
            showMessage("تم إصدار فاتورة البيع والتوريد #$invNum وخصم ${CalculationEngine.formatWeight(bulkWeightKg)} كجم من الشونة")
            onCreated?.invoke(invoiceId)
        }
    }

    fun recordShrinkageAdjustment(
        productId: Long?,
        productName: String,
        weightLossKg: Double,
        referencePurchasedKg: Double,
        reason: String
    ) {
        if (weightLossKg <= 0.0) {
            showMessage("يرجى إدخال وزن الجفاف/العجز بالكيلوجرام")
            return
        }
        viewModelScope.launch {
            repository.recordInventoryAdjustment(
                productId = productId,
                productName = productName,
                adjustmentType = ShrinkageAdjustmentType.MANUAL_SHRINKAGE,
                weightLossKg = weightLossKg,
                referencePurchasedKg = referencePurchasedKg,
                reason = reason.ifBlank { "تسوية جفاف وغربلة يدوية" }
            )
            showMessage("تم خصم ${CalculationEngine.formatWeight(weightLossKg)} كجم عجز/جفاف من محصول $productName")
        }
    }

    fun zeroOutBatchStock(
        productId: Long?,
        productName: String,
        remainingBookStockKg: Double,
        referencePurchasedKg: Double,
        notes: String
    ) {
        if (remainingBookStockKg <= 0.0) {
            showMessage("رصيد المحصول صفر بالفعل في الشونة")
            return
        }
        viewModelScope.launch {
            repository.recordInventoryAdjustment(
                productId = productId,
                productName = productName,
                adjustmentType = ShrinkageAdjustmentType.BATCH_ZERO_OUT,
                weightLossKg = remainingBookStockKg,
                referencePurchasedKg = referencePurchasedKg,
                reason = notes.ifBlank { "تصفير الدفعة عند نفاد المخزن فعلياً (فروق جفاف طبيعي)" }
            )
            val pct = if (referencePurchasedKg > 0.0) {
                CalculationEngine.formatNumber((remainingBookStockKg / referencePurchasedKg) * 100.0, 2)
            } else "0"
            showMessage("تم تصفير دفعة $productName وتسجيل ${CalculationEngine.formatWeight(remainingBookStockKg)} كجم كعجز جفاف ($pct%)")
        }
    }

    fun deleteInventoryAdjustment(adjustment: InventoryAdjustmentEntity) {
        viewModelScope.launch {
            repository.deleteInventoryAdjustment(adjustment)
            showMessage("تم إلغاء حركة تسوية الجفاف (${CalculationEngine.formatWeight(adjustment.weightLossKg)} كجم) وإعادتها للشونة")
        }
    }

    fun deleteInvoiceById(invoiceId: Long) {
        viewModelScope.launch {
            val inv = repository.getInvoiceByIdOnce(invoiceId) ?: return@launch
            repository.deleteInvoice(inv.invoice)
            if (_activeInvoiceId.value == invoiceId) {
                setActiveWorkspaceIds(null, null)
                _currentScreen.value = AppScreen.INVOICES
            }
            showMessage("تم حذف الفاتورة #${inv.invoice.invoiceNumber}")
        }
    }

    // --- Payments & Overpayment Settlement ---
    fun recordInvoicePayment(
        invoiceId: Long,
        amount: Double,
        method: String,
        notes: String
    ) {
        if (amount <= 0.0) {
            showMessage("قيمة الدفعة يجب أن تكون أكبر من صفر")
            return
        }
        viewModelScope.launch {
            repository.recordPayment(invoiceId, amount, method, notes)
            showMessage("تم تسجيل دفعة سداد بقيمة ${CalculationEngine.formatCurrency(amount, settings.value.currencySymbol)}")
        }
    }

    fun settleOverpaidInvoice(
        invoiceId: Long,
        overpaidAmount: Double,
        settleAsCustomerCredit: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val full = repository.getInvoiceByIdOnce(invoiceId) ?: return@launch
            repository.settleOverpayment(
                invoice = full.invoice,
                overpaidAmount = overpaidAmount,
                settleAsCustomerCredit = settleAsCustomerCredit,
                notes = notes
            )
            val msg = if (settleAsCustomerCredit && full.invoice.customerId != null) {
                "تم ترحيل الفائض (${CalculationEngine.formatCurrency(overpaidAmount)}) لرصيد العميل الدائن"
            } else {
                "تم تسجيل عملية رد نقدي للفائض (${CalculationEngine.formatCurrency(overpaidAmount)})"
            }
            showMessage(msg)
        }
    }

    // --- Customers Management ---
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showMessage("تم حفظ بيانات العميل: ${customer.name}")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val deleted = repository.deleteCustomer(customer)
            if (deleted) {
                showMessage("تم حذف العميل ${customer.name}")
            } else {
                showMessage("لا يمكن حذف العميل لوجود فواتير مسجلة باسمه حفاظًا على السجل المحاسبي")
            }
        }
    }

    // --- Definitions & Settings ---
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showMessage("تم حفظ محصول: ${product.name}")
        }
    }

    fun savePricingUnit(unit: PricingUnitEntity) {
        viewModelScope.launch {
            repository.savePricingUnit(unit)
            showMessage("تم حفظ وحدة التسعير: ${unit.name} (${unit.weightKg} كجم)")
        }
    }

    fun saveAppSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            showMessage("تم حفظ إعدادات التطبيق والفواتير")
        }
    }

    fun exportDatabaseBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                onJsonReady(json)
            } catch (e: Exception) {
                showMessage("تعذر تصدير النسخة الاحتياطية: ${e.localizedMessage}")
            }
        }
    }

    fun importDatabaseBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                repository.importBackupJson(jsonString)
                showMessage("تم استعادة النسخة الاحتياطية بنجاح")
            } catch (e: Exception) {
                showMessage("ملف النسخة الاحتياطية غير صالح: ${e.localizedMessage}")
            }
        }
    }

    companion object {
        private const val KEY_ACTIVE_INVOICE_ID = "active_invoice_id"
        private const val KEY_ACTIVE_SECTION_ID = "active_section_id"
    }
}
