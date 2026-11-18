package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.WeightEntity
import com.example.domain.CalculationEngine
import com.example.domain.SectionCalculation
import com.example.domain.WeightGroup
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusSettlementBg
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.theme.WheatGoldLight
import com.example.ui.viewmodel.InvoiceWithCalc
import com.example.util.PdfInvoiceExporter

@Composable
fun PurchaseWorkspaceScreen(
    invoiceWithCalc: InvoiceWithCalc?,
    heldDraftInvoices: List<InvoiceWithCalc> = emptyList(),
    activeSectionId: Long?,
    keypadInput: String,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    settings: AppSettingsEntity,
    onStartNewPurchase: () -> Unit,
    onSuspendAndStartNew: () -> Unit = {},
    onSwitchToHeldInvoice: (Long) -> Unit = {},
    onSelectSection: (Long) -> Unit,
    onKeypadDigit: (String) -> Unit,
    onKeypadBackspace: () -> Unit,
    onKeypadClear: () -> Unit,
    onAddWeightFromKeypad: (String?) -> Unit,
    onUpdateWeight: (WeightEntity, Double, String?) -> Unit,
    onDeleteWeight: (WeightEntity, String?) -> Unit,
    onAddSection: (ProductEntity, PricingUnitEntity, Double, String?) -> Unit,
    onUpdateSection: (InvoiceSectionEntity, ProductEntity, PricingUnitEntity, Double, String?) -> Unit,
    onDeleteSection: (InvoiceSectionEntity, String?) -> Unit,
    onUpdateHeader: (CustomerEntity?, String, String?) -> Unit,
    onQuickCreateCustomer: (CustomerEntity) -> Unit,
    onApproveInvoice: () -> Unit,
    onRecordPayment: (Double, String, String) -> Unit,
    onSettleOverpayment: (Double, Boolean, String) -> Unit,
    onDeleteInvoice: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    if (invoiceWithCalc == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "لا توجد عملية شراء مفتوحة حاليًا",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onStartNewPurchase,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("start_purchase_empty_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء عملية شراء جديدة الآن")
                }
            }
        }
        return
    }

    val inv = invoiceWithCalc.details.invoice
    val calc = invoiceWithCalc.calc
    val isApproved = inv.status == InvoiceStatus.APPROVED.name
    val sectionsCount = calc.sectionCalculations.size
    val summaryStepIndex = sectionsCount + 1

    // Other suspended/draft invoices excluding the current one
    val otherDrafts = remember(heldDraftInvoices, inv.id) {
        heldDraftInvoices.filter {
            it.details.invoice.id != inv.id &&
                it.details.invoice.status == InvoiceStatus.DRAFT.name
        }
    }

    // Wizard step state:
    // 0 = Customer & initial setup
    // 1..sectionsCount = Individual Section Page
    // sectionsCount + 1 = Final Summary & Pricing Page
    var currentStep by remember(inv.id) {
        mutableIntStateOf(if (calc.totalBagCount > 0) 1 else 0)
    }

    LaunchedEffect(sectionsCount, activeSectionId) {
        val matchedIndex = calc.sectionCalculations.indexOfFirst { it.section.id == activeSectionId }
        if (matchedIndex >= 0 && currentStep in 1..sectionsCount) {
            currentStep = matchedIndex + 1
        } else if (currentStep > summaryStepIndex) {
            currentStep = summaryStepIndex
        }
    }

    var showAddSectionDialog by remember { mutableStateOf(false) }
    var editingSection by remember { mutableStateOf<InvoiceSectionEntity?>(null) }
    var editingWeight by remember { mutableStateOf<WeightEntity?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showDeleteInvoiceConfirm by remember { mutableStateOf(false) }
    var showHeldInvoicesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("purchase_workspace_screen")
    ) {
        // 1. Compact Top Bar + Wizard Step Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "فاتورة #${inv.invoiceNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            softWrap = false,
                            maxLines = 1
                        )
                        InvoiceStatusBadge(status = inv.status)
                        Text(
                            text = "• ${inv.customerNameSnapshot}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Button to switch between held/suspended invoices if any exist
                        if (otherDrafts.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusSettlementBg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showHeldInvoicesDialog = true }
                                    .testTag("open_held_invoices_button")
                            ) {
                                Text(
                                    text = "معلقة (${otherDrafts.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusSettlementPurple,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Hold / Suspend Current Invoice & Start New One
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSuspendAndStartNew() }
                                .testTag("suspend_and_new_invoice_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isApproved) "فاتورة جديدة" else "تعليق + جديد",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        if (invoiceWithCalc.details.auditLogs.isNotEmpty()) {
                            IconButton(
                                onClick = { showAuditLogsDialog = true },
                                modifier = Modifier.size(32.dp).testTag("open_audit_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "سجل التعديلات",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showDeleteInvoiceConfirm = true },
                            modifier = Modifier.size(32.dp).testTag("delete_invoice_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف الفاتورة",
                                tint = StatusUnpaidRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Multi-Step Navigation Pills (Customer -> Section 1 -> Section 2 ... -> [+ قسم جديد] -> Summary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        WizardStepPill(
                            title = "1. العميل",
                            subtitle = inv.customerNameSnapshot.take(12),
                            isSelected = currentStep == 0,
                            onClick = { currentStep = 0 },
                            testTag = "step_customer"
                        )
                    }

                    items(calc.sectionCalculations.size) { index ->
                        val secCalc = calc.sectionCalculations[index]
                        val stepNum = index + 1
                        WizardStepPill(
                            title = "قسم ${secCalc.section.sectionOrder}: ${secCalc.section.productNameSnapshot}",
                            subtitle = "${secCalc.bagCount} شكارة (${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم)",
                            isSelected = currentStep == stepNum,
                            onClick = {
                                onSelectSection(secCalc.section.id)
                                currentStep = stepNum
                            },
                            testTag = "section_tab_${secCalc.section.sectionOrder}"
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WheatGoldLight,
                            modifier = Modifier
                                .border(1.dp, WheatGold, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAddSectionDialog = true }
                                .testTag("add_section_top_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = WheatGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "إضافة قسم جديد",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WheatGold,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    item {
                        WizardStepPill(
                            title = "الملخص والمحاسبة",
                            subtitle = CalculationEngine.formatCurrency(calc.totalInvoiceAmount, settings.currencySymbol),
                            isSelected = currentStep == summaryStepIndex,
                            onClick = { currentStep = summaryStepIndex },
                            testTag = "step_summary"
                        )
                    }
                }
            }
        }

        // 2. Active Step Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                currentStep == 0 -> {
                    val firstSection = calc.sectionCalculations.firstOrNull()?.section
                    CustomerAndGrainStepPage(
                        invoice = inv,
                        firstSection = firstSection,
                        customers = customers,
                        products = products,
                        pricingUnits = pricingUnits,
                        currencySymbol = settings.currencySymbol,
                        onUpdateCustomerAndNotes = { cust, notes ->
                            onUpdateHeader(cust, notes, null)
                        },
                        onQuickCreateCustomer = onQuickCreateCustomer,
                        onUpdateFirstSection = { prod, unit, price ->
                            if (firstSection != null) {
                                onUpdateSection(firstSection, prod, unit, price, null)
                            }
                        },
                        onProceedToWeights = {
                            firstSection?.let { onSelectSection(it.id) }
                            currentStep = 1
                        }
                    )
                }

                currentStep in 1..sectionsCount -> {
                    val secCalc = calc.sectionCalculations[currentStep - 1]
                    SectionWeighingStepPage(
                        secCalc = secCalc,
                        keypadInput = keypadInput,
                        currencySymbol = settings.currencySymbol,
                        onEditSectionConfig = { editingSection = secCalc.section },
                        onDigit = onKeypadDigit,
                        onBackspace = onKeypadBackspace,
                        onClear = onKeypadClear,
                        onSubmitWeight = {
                            val autoReason = if (isApproved) "إضافة وزن" else null
                            onAddWeightFromKeypad(autoReason)
                        },
                        onWeightClick = { w -> editingWeight = w },
                        onGoToPreviousStep = { currentStep = (currentStep - 1).coerceAtLeast(0) },
                        onGoToNextStep = {
                            if (currentStep < sectionsCount) {
                                val nextSec = calc.sectionCalculations[currentStep].section
                                onSelectSection(nextSec.id)
                                currentStep += 1
                            } else {
                                currentStep = summaryStepIndex
                            }
                        },
                        hasNextSection = currentStep < sectionsCount
                    )
                }

                else -> {
                    FinalSummaryStepPage(
                        invoiceWithCalc = invoiceWithCalc,
                        settings = settings,
                        onJumpToSection = { index, secId ->
                            onSelectSection(secId)
                            currentStep = index + 1
                        },
                        onEditSection = { editingSection = it },
                        onOpenPaymentDialog = { showPaymentDialog = true },
                        onOpenSettlementDialog = { showSettlementDialog = true },
                        onApproveInvoice = onApproveInvoice,
                        onStartNewBlankInvoice = onSuspendAndStartNew,
                        onShareWhatsAppPdf = {
                            PdfInvoiceExporter.shareInvoicePdf(
                                context = context,
                                details = invoiceWithCalc.details,
                                settings = settings,
                                whatsAppOnly = true
                            )
                        },
                        onShareGeneralPdf = {
                            PdfInvoiceExporter.saveInvoicePdfToPhone(
                                context = context,
                                details = invoiceWithCalc.details,
                                settings = settings
                            )
                        },
                        onPrintPdf = {
                            PdfInvoiceExporter.printInvoicePdf(
                                context = context,
                                details = invoiceWithCalc.details,
                                settings = settings
                            )
                        },
                        onDeleteInvoice = { showDeleteInvoiceConfirm = true }
                    )
                }
            }
        }
    }

    // --- Dialogs ---
    if (showHeldInvoicesDialog) {
        AlertDialog(
            onDismissRequest = { showHeldInvoicesDialog = false },
            title = {
                Text(
                    text = "الفواتير المعلقة / المسودات المحفوظة (${otherDrafts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(otherDrafts, key = { it.details.invoice.id }) { draftItem ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldLight.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showHeldInvoicesDialog = false
                                    onSwitchToHeldInvoice(draftItem.details.invoice.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "فاتورة #${draftItem.details.invoice.invoiceNumber} — ${draftItem.details.invoice.customerNameSnapshot}",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "${draftItem.calc.totalBagCount} شكارة • ${CalculationEngine.formatWeight(draftItem.calc.totalWeightKg)} كجم",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "استكمال ◀",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHeldInvoicesDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showAddSectionDialog) {
        AddOrEditSectionDialog(
            isEdit = false,
            products = products,
            pricingUnits = pricingUnits,
            isApprovedInvoice = isApproved,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddSectionDialog = false },
            onConfirm = { prod, unit, price, reason ->
                onAddSection(prod, unit, price, reason)
                currentStep = sectionsCount + 1
            }
        )
    }

    editingSection?.let { sec ->
        AddOrEditSectionDialog(
            isEdit = true,
            existingSection = sec,
            products = products,
            pricingUnits = pricingUnits,
            isApprovedInvoice = isApproved,
            currencySymbol = settings.currencySymbol,
            onDismiss = { editingSection = null },
            onConfirm = { prod, unit, price, reason ->
                onUpdateSection(sec, prod, unit, price, reason)
            },
            onDeleteSection = if (calc.sectionCalculations.size > 1) {
                { reason -> onDeleteSection(sec, reason) }
            } else null
        )
    }

    editingWeight?.let { w ->
        val activeSecOrder = calc.sectionCalculations.find { it.section.id == w.sectionId }?.section?.sectionOrder ?: 1
        EditOrDeleteWeightDialog(
            weight = w,
            sectionOrder = activeSecOrder,
            isApprovedInvoice = isApproved,
            onDismiss = { editingWeight = null },
            onUpdateWeight = { newKg, reason -> onUpdateWeight(w, newKg, reason) },
            onDeleteWeight = { reason -> onDeleteWeight(w, reason) }
        )
    }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            invoiceNumber = inv.invoiceNumber,
            totalInvoiceAmount = calc.totalInvoiceAmount,
            netPaidAmount = calc.netPaidAmount,
            remainingAmount = calc.remainingAmount,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showPaymentDialog = false },
            onRecordPayment = onRecordPayment
        )
    }

    if (showSettlementDialog) {
        OverpaymentSettlementDialog(
            invoiceNumber = inv.invoiceNumber,
            overpaidAmount = calc.overpaidAmount,
            hasRegisteredCustomer = inv.customerId != null,
            customerName = inv.customerNameSnapshot,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showSettlementDialog = false },
            onSettle = { asCredit, notes ->
                onSettleOverpayment(calc.overpaidAmount, asCredit, notes)
            }
        )
    }

    if (showAuditLogsDialog) {
        AuditLogsDialog(
            invoiceNumber = inv.invoiceNumber,
            logs = invoiceWithCalc.details.auditLogs.sortedByDescending { it.timestamp },
            onDismiss = { showAuditLogsDialog = false }
        )
    }

    if (showDeleteInvoiceConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteInvoiceConfirm = false },
            title = { Text("حذف الفاتورة #${inv.invoiceNumber}") },
            text = { Text("هل أنت متأكد من حذف هذه الفاتورة نهائيًا؟ ستختفي الفاتورة من سجل العميل وجميع الحسابات.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteInvoiceConfirm = false
                        onDeleteInvoice(inv.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed),
                    modifier = Modifier.testTag("confirm_delete_invoice_button")
                ) {
                    Text("حذف الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteInvoiceConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun WizardStepPill(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.background,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isSelected) EmeraldPrimary else WheatCardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = if (isSelected) WheatGoldLight else MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CustomerAndGrainStepPage(
    invoice: com.example.data.local.InvoiceEntity,
    firstSection: InvoiceSectionEntity?,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    currencySymbol: String,
    onUpdateCustomerAndNotes: (CustomerEntity?, String) -> Unit,
    onQuickCreateCustomer: (CustomerEntity) -> Unit,
    onUpdateFirstSection: (ProductEntity, PricingUnitEntity, Double) -> Unit,
    onProceedToWeights: () -> Unit
) {
    val activeProducts = remember(products) { products.filter { it.isActive }.ifEmpty { products } }
    val activeUnits = remember(pricingUnits) { pricingUnits.filter { it.isActive }.ifEmpty { pricingUnits } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember(invoice.customerId, customers) {
        mutableStateOf(customers.find { it.id == invoice.customerId })
    }
    var notesText by remember(invoice.notes) { mutableStateOf(invoice.notes) }

    var showNewCustomerInline by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }

    var selectedProduct by remember(firstSection?.productId, activeProducts) {
        mutableStateOf(
            activeProducts.find { it.id == firstSection?.productId }
                ?: activeProducts.firstOrNull()
                ?: ProductEntity(id = 1, name = "قمح")
        )
    }
    var selectedUnit by remember(firstSection?.unitId, activeUnits) {
        mutableStateOf(
            activeUnits.find { it.id == firstSection?.unitId }
                ?: activeUnits.find { it.linkedProductId == selectedProduct.id }
                ?: activeUnits.firstOrNull()
                ?: PricingUnitEntity(id = 1, name = "طن", weightKg = 1000.0)
        )
    }
    var unitPriceText by remember(firstSection?.unitPrice) {
        mutableStateOf(firstSection?.unitPrice?.let { CalculationEngine.formatPlainInt(it) } ?: "4000")
    }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. اختيار العميل",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showNewCustomerInline = !showNewCustomerInline }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showNewCustomerInline) "قائمة العملاء" else "عميل جديد")
                        }
                    }

                    if (showNewCustomerInline) {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("اسم العميل الجديد") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text("رقم الهاتف") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (newCustomerName.isNotBlank()) {
                                    val c = CustomerEntity(name = newCustomerName.trim(), phone = newCustomerPhone.trim())
                                    onQuickCreateCustomer(c)
                                    showNewCustomerInline = false
                                    newCustomerName = ""
                                    newCustomerPhone = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ العميل")
                        }
                    } else {
                        EhseblySearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "ابحث باسم العميل أو رقم الهاتف..."
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 165.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                val isCash = selectedCustomer == null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCash) EmeraldLight else MaterialTheme.colorScheme.background)
                                        .border(1.dp, if (isCash) EmeraldPrimary else WheatCardBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedCustomer = null
                                            onUpdateCustomerAndNotes(null, notesText)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "عميل نقدي (بدون تسجيل)",
                                        fontWeight = if (isCash) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            items(filteredCustomers, key = { it.id }) { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) EmeraldLight else MaterialTheme.colorScheme.background)
                                        .border(1.dp, if (isSelected) EmeraldPrimary else WheatCardBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedCustomer = cust
                                            onUpdateCustomerAndNotes(cust, notesText)
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        if (cust.phone.isNotBlank()) {
                                            Text(cust.phone, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. تحديد نوع الحبوب وتسعير القسم الأول",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    GrainProductDropdownSelector(
                        products = activeProducts,
                        selectedProduct = selectedProduct,
                        label = "نوع الحبوب المختار",
                        subtitleFormatter = { prod ->
                            val linked = activeUnits.find { it.linkedProductId == prod.id }
                            linked?.let { "الوحدة القياسية: ${it.name} (${CalculationEngine.formatWeight(it.weightKg)} كجم)" }
                        },
                        onProductSelected = { prod ->
                            selectedProduct = prod
                            val linkedUnit = activeUnits.find { it.linkedProductId == prod.id } ?: selectedUnit
                            selectedUnit = linkedUnit
                            val p = unitPriceText.toDoubleOrNull() ?: 4000.0
                            onUpdateFirstSection(prod, linkedUnit, p)
                        }
                    )

                    PricingUnitDropdownSelector(
                        units = activeUnits,
                        selectedUnit = selectedUnit,
                        label = "وحدة التسعير لـ (${selectedProduct.name})",
                        onUnitSelected = { unit ->
                            selectedUnit = unit
                            val p = unitPriceText.toDoubleOrNull() ?: 4000.0
                            onUpdateFirstSection(selectedProduct, unit, p)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = {
                                unitPriceText = it
                                val p = it.toDoubleOrNull()
                                if (p != null && p > 0) {
                                    onUpdateFirstSection(selectedProduct, selectedUnit, p)
                                }
                            },
                            label = { Text("سعر ${selectedUnit.name} ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = {
                                notesText = it
                                onUpdateCustomerAndNotes(selectedCustomer, it)
                            },
                            label = { Text("ملاحظات (اختياري)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val previewPrice = unitPriceText.toDoubleOrNull() ?: 0.0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المحصول: ${selectedProduct.name}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${CalculationEngine.formatCurrency(previewPrice, currencySymbol)} / ${selectedUnit.name} (${CalculationEngine.formatWeight(selectedUnit.weightKg)} كجم)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val p = unitPriceText.toDoubleOrNull() ?: 4000.0
                    onUpdateCustomerAndNotes(selectedCustomer, notesText)
                    onUpdateFirstSection(selectedProduct, selectedUnit, p)
                    onProceedToWeights()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("proceed_to_weights_button")
            ) {
                Text(
                    text = "التالي: إدخال أوزان القسم الأول (${selectedProduct.name})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SectionWeighingStepPage(
    secCalc: SectionCalculation,
    keypadInput: String,
    currencySymbol: String,
    onEditSectionConfig: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmitWeight: () -> Unit,
    onWeightClick: (WeightEntity) -> Unit,
    onGoToPreviousStep: () -> Unit,
    onGoToNextStep: () -> Unit,
    hasNextSection: Boolean
) {
    val sec = secCalc.section

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact Section Info Strip + Swapped Keypad (C in top bar, "+ إضافة" in bottom-right of keypad!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldLight)
                            .clickable(onClick = onEditSectionConfig)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("edit_active_section_banner"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قسم ${sec.sectionOrder}: ${sec.productNameSnapshot} (${CalculationEngine.formatNumber(sec.unitPrice)} $currencySymbol/${sec.unitNameSnapshot})",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${secCalc.bagCount} شكارة • ${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل القسم",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Top Display Row: Weight Input Box + "C" (Clear) Button (Swapped as requested!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(23.dp)),
                            shape = RoundedCornerShape(23.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (keypadInput.isEmpty()) "0" else keypadInput,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (keypadInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("keypad_display_text")
                                )
                                Text(
                                    text = "شكارة #${secCalc.bagCount + 1} (كجم)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        // "C" Button now sits where "وزن جديد +" used to be!
                        Surface(
                            modifier = Modifier
                                .width(78.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .border(1.dp, WheatGold, RoundedCornerShape(23.dp))
                                .clickable { onClear() }
                                .testTag("keypad_key_C"),
                            shape = RoundedCornerShape(23.dp),
                            color = WheatGoldLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "C",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WheatGold
                                )
                            }
                        }
                    }

                    // Compact 4-Row Numeric Keypad:
                    // Bottom-right key (where "C" used to be) is now the prominent "+ إضافة" (Add Weight) button!
                    val rows = listOf(
                        listOf("7", "8", "9"),
                        listOf("4", "5", "6"),
                        listOf("1", "2", "3"),
                        listOf("ADD", "0", "⌫")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowKeys.forEach { key ->
                                    val isAddKey = key == "ADD"
                                    Surface(
                                        modifier = Modifier
                                            .weight(if (isAddKey) 1.35f else 1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = 1.dp,
                                                color = if (isAddKey) EmeraldPrimary else WheatCardBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                when (key) {
                                                    "ADD" -> onSubmitWeight()
                                                    "⌫" -> onBackspace()
                                                    else -> onDigit(key)
                                                }
                                            }
                                            .testTag(if (isAddKey) "submit_weight_button" else "keypad_key_$key"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = when (key) {
                                            "ADD" -> EmeraldPrimary
                                            "⌫" -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.background
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            when (key) {
                                                "ADD" -> {
                                                    Text(
                                                        text = "إضافة +",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        softWrap = false,
                                                        maxLines = 1
                                                    )
                                                }
                                                "⌫" -> {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = "حذف رقم",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                else -> {
                                                    Text(
                                                        text = key,
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 5-Weight Columns Table directly underneath the keypad
        item {
            FiveWeightColumnsSection(
                secCalc = secCalc,
                onWeightClick = onWeightClick
            )
        }

        // 3. Bottom Step Navigation Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onGoToPreviousStep,
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("السابق", softWrap = false, maxLines = 1)
                }

                Button(
                    onClick = onGoToNextStep,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(2f)
                        .height(46.dp)
                        .testTag("next_step_from_weights_button")
                ) {
                    Text(
                        text = if (hasNextSection) "القسم التالي" else "الملخص والمحاسبة النهائية",
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FinalSummaryStepPage(
    invoiceWithCalc: InvoiceWithCalc,
    settings: AppSettingsEntity,
    onJumpToSection: (Int, Long) -> Unit,
    onEditSection: (InvoiceSectionEntity) -> Unit,
    onOpenPaymentDialog: () -> Unit,
    onOpenSettlementDialog: () -> Unit,
    onApproveInvoice: () -> Unit,
    onStartNewBlankInvoice: () -> Unit,
    onShareWhatsAppPdf: () -> Unit,
    onShareGeneralPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDeleteInvoice: () -> Unit
) {
    val inv = invoiceWithCalc.details.invoice
    val calc = invoiceWithCalc.calc
    val isApproved = inv.status == InvoiceStatus.APPROVED.name

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (calc.overpaidAmount > 0.01) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, StatusSettlementPurple, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusSettlementBg)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusSettlementPurple)
                            Text(
                                text = "فائض مسدد: ${CalculationEngine.formatCurrency(calc.overpaidAmount, settings.currencySymbol)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = StatusSettlementPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onOpenSettlementDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSettlementPurple),
                            modifier = Modifier.fillMaxWidth().testTag("open_settlement_dialog_button")
                        ) {
                            Text("تسوية المبلغ الزائد الآن")
                        }
                    }
                }
            }
        }

        // Sections & Pricing Breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ملخص التسعير والأقسام — فاتورة #${inv.invoiceNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    calc.sectionCalculations.forEachIndexed { idx, secCalc ->
                        val sec = secCalc.section
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onJumpToSection(idx, sec.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "القسم ${sec.sectionOrder}: ${sec.productNameSnapshot}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldLight,
                                        modifier = Modifier.clickable { onEditSection(sec) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${CalculationEngine.formatNumber(sec.unitPrice)} ${settings.currencySymbol}/${sec.unitNameSnapshot}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EmeraldPrimary,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل السعر",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "الوزن (${secCalc.bagCount} شكارة • ${secCalc.groups.size} أعمدة):",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "وحدة المحاسبة: ${sec.unitNameSnapshot} (${CalculationEngine.formatWeight(sec.unitWeightSnapshot)} كجم)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "إجمالي القسم: ${CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي الوزن الكلي (${calc.totalBagCount} شكارة):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي النهائي للفاتورة:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = CalculationEngine.formatCurrency(calc.totalInvoiceAmount, settings.currencySymbol),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusPaidGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي المدفوع الصافي:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CalculationEngine.formatCurrency(calc.netPaidAmount, settings.currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المتبقي المستحق:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        PaymentStatusBadge(paymentStatus = calc.paymentStatus)
                        Text(
                            text = CalculationEngine.formatCurrency(calc.remainingAmount, settings.currencySymbol),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (calc.remainingAmount > 0) StatusUnpaidRed else StatusPaidGreen
                        )
                    }
                }
            }
        }

        // Actions Card (Payments, Save & Approve & Reset Workspace, PDF Export, Print, Delete)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isApproved) {
                        Button(
                            onClick = onApproveInvoice,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("workspace_approve_invoice_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حفظ واعتماد الفاتورة (وتجهيز فاتورة جديدة)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartNewBlankInvoice,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تفريغ الشاشة وبدء فاتورة جديدة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = onOpenPaymentDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = WheatGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("workspace_record_payment_button")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دفعة سداد", softWrap = false, maxLines = 1)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShareWhatsAppPdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_whatsapp_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", softWrap = false, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onShareGeneralPdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WheatGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ بالهاتف", softWrap = false, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onPrintPdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("print_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", softWrap = false, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onDeleteInvoice,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusUnpaidRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("summary_delete_invoice_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف هذه الفاتورة نهائيًا")
                    }
                }
            }
        }
    }
}

@Composable
private fun FiveWeightColumnsSection(
    secCalc: SectionCalculation,
    onWeightClick: (WeightEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جدول الأوزان (5 أوزان بالعمود)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1
                )
                Text(
                    text = "${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم (${secCalc.bagCount} شكارة)",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1
                )
            }

            if (secCalc.groups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "أدخل الوزن واضغط «إضافة +» ليظهر هنا في أعمدة خماسية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                val pairedGroups = secCalc.groups.chunked(2)
                pairedGroups.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        pair.forEach { group ->
                            WeightColumnCard(
                                modifier = Modifier.weight(1f),
                                group = group,
                                onWeightClick = onWeightClick
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightColumnCard(
    modifier: Modifier = Modifier,
    group: WeightGroup,
    onWeightClick: (WeightEntity) -> Unit
) {
    Surface(
        modifier = modifier.border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "العمود ${group.groupIndex}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )

            group.weights.forEach { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onWeightClick(w) }
                        .padding(vertical = 3.dp, horizontal = 2.dp)
                        .testTag("weight_item_${w.sectionId}_${w.sequenceNumber}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شكارة ${w.sequenceNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CalculationEngine.formatWeight(w.weightKg),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (group.remainingSlots > 0) {
                Text(
                    text = "${group.remainingSlots} أماكن متبقية",
                    style = MaterialTheme.typography.bodySmall,
                    color = WheatGold
                )
            }

            HorizontalDivider(color = WheatCardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المجموع",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = CalculationEngine.formatWeight(group.groupTotalKg),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
    }
}
