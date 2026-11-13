package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.FullInvoiceDetails
import com.example.data.local.GrainDatabase
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.PaymentStatus
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.WeightEntity
import com.example.data.repository.GrainRepository
import com.example.domain.CalculationEngine
import com.example.domain.InvoiceCalculation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppScreen(val titleAr: String) {
    DASHBOARD("الرئيسية"),
    PURCHASE_WORKSPACE("عملية شراء"),
    INVOICES("الفواتير والعمليات"),
    CUSTOMERS("إدارة العملاء"),
    DEFINITIONS("التعريفات والإعدادات")
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

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
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

    fun filterInvoicesByPeriod(
        invoices: List<InvoiceWithCalc>,
        period: DashboardPeriod
    ): List<InvoiceWithCalc> {
        if (period == DashboardPeriod.ALL) return invoices
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val startMillis = when (period) {
            DashboardPeriod.TODAY -> cal.timeInMillis
            DashboardPeriod.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            DashboardPeriod.THIS_MONTH -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            DashboardPeriod.ALL -> 0L
        }
        return invoices.filter { it.details.invoice.invoiceDate >= startMillis }
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

    fun setKeypadValue(value: String) {
        _keypadInput.value = value
    }

    // --- Purchase Workspace Actions ---
    fun startNewPurchase(
        customer: CustomerEntity? = null,
        initialProduct: ProductEntity? = null,
        initialUnit: PricingUnitEntity? = null,
        initialPrice: Double = 4000.0
    ) {
        viewModelScope.launch {
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
            _activeInvoiceId.value = newId
            _activeSectionId.value = created?.sections?.firstOrNull()?.section?.id
            _keypadInput.value = ""
            _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
        }
    }

    fun openExistingInvoiceInWorkspace(invoiceId: Long) {
        viewModelScope.launch {
            val existing = repository.getInvoiceByIdOnce(invoiceId)
            _activeInvoiceId.value = invoiceId
            _activeSectionId.value = existing?.sections?.minByOrNull { it.section.sectionOrder }?.section?.id
            _keypadInput.value = ""
            _currentScreen.value = AppScreen.PURCHASE_WORKSPACE
        }
    }

    fun selectActiveSection(sectionId: Long) {
        _activeSectionId.value = sectionId
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
            _activeSectionId.value = newSecId
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
            showMessage("تم تحديث إعدادات وسعر القسم ${section.sectionOrder}")
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
            _activeSectionId.value = updated?.sections?.firstOrNull()?.section?.id
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
            showMessage("تم تحديث بيانات العميل والملاحظات")
        }
    }

    fun approveActiveInvoice() {
        val inv = activeInvoiceWithCalc.value ?: return
        if (inv.calc.totalBagCount == 0) {
            showMessage("لا يمكن اعتماد فاتورة فارغة بدون أوزان")
            return
        }
        viewModelScope.launch {
            repository.approveInvoice(inv.details.invoice)
            showMessage("تم اعتماد الفاتورة رقم #${inv.details.invoice.invoiceNumber} بنجاح")
        }
    }

    fun deleteInvoiceById(invoiceId: Long) {
        viewModelScope.launch {
            val inv = repository.getInvoiceByIdOnce(invoiceId) ?: return@launch
            repository.deleteInvoice(inv.invoice)
            if (_activeInvoiceId.value == invoiceId) {
                _activeInvoiceId.value = null
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
}
