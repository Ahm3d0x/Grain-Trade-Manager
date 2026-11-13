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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseWorkspaceScreen(
    invoiceWithCalc: InvoiceWithCalc?,
    activeSectionId: Long?,
    keypadInput: String,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    settings: AppSettingsEntity,
    onStartNewPurchase: () -> Unit,
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
    onSettleOverpayment: (Double, Boolean, String) -> Unit
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
    val activeSecCalc = calc.sectionCalculations.find { it.section.id == activeSectionId }
        ?: calc.sectionCalculations.lastOrNull()

    var showCustomerDialog by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var editingSection by remember { mutableStateOf<InvoiceSectionEntity?>(null) }
    var editingWeight by remember { mutableStateOf<WeightEntity?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showAddWeightAuditDialog by remember { mutableStateOf(false) }
    var pendingWeightAuditReason by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd نوفمبر yyyy", Locale("ar", "EG")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("purchase_workspace_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Invoice Header Card (Matches Attachment #1)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "عملية شراء — فاتورة #${inv.invoiceNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeSecCalc?.section?.productNameSnapshot ?: "حبوب"} • ${dateFormat.format(Date(inv.invoiceDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (invoiceWithCalc.details.auditLogs.isNotEmpty()) {
                                IconButton(
                                    onClick = { showAuditLogsDialog = true },
                                    modifier = Modifier.testTag("open_audit_logs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "سجل التعديلات",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                            InvoiceStatusBadge(status = inv.status)
                        }
                    }

                    // Customer Selector Strip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCustomerDialog = true }
                            .testTag("select_customer_strip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                                Column {
                                    Text(
                                        text = "العميل: ${inv.customerNameSnapshot}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (inv.customerPhoneSnapshot.isNotBlank()) {
                                        Text(
                                            text = inv.customerPhoneSnapshot,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "تغيير العميل",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    // Twin Summary Boxes: Bag Count & Total Weight (Matches Attachment #1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "${activeSecCalc?.bagCount ?: 0}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "عدد الشكاير (القسم ${activeSecCalc?.section?.sectionOrder ?: 1})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = CalculationEngine.formatWeight(activeSecCalc?.totalWeightKg ?: 0.0),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إجمالي وزن القسم (كجم)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overpayment Alert Banner (If invoice total became less than net paid)
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
                                text = "تنبيه محاسبي: يوجد مبلغ فائض مدفوع بقيمة ${CalculationEngine.formatCurrency(calc.overpaidAmount, settings.currencySymbol)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = StatusSettlementPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "تم تخفيض قيمة الفاتورة عن المبلغ المسدد مسبقًا. اضغط لمعالجة الفرق برد نقدي أو ترحيله لرصيد العميل.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { showSettlementDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSettlementPurple),
                            modifier = Modifier.fillMaxWidth().testTag("open_settlement_dialog_button")
                        ) {
                            Text("تسوية المبلغ الزائد الآن")
                        }
                    }
                }
            }
        }

        // 2. Section Tabs & Add New Section Divider Button (Supports returning to previous sections)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أقسام الفاتورة (اضغط للتنقل أو الرجوع لقسم سابق)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddSectionDialog = true },
                        modifier = Modifier.testTag("add_section_top_button")
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة فاصل / قسم جديد")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(calc.sectionCalculations, key = { it.section.id }) { secItem ->
                        val isSelected = secItem.section.id == activeSecCalc?.section?.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) EmeraldPrimary else WheatCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSection(secItem.section.id) }
                                .testTag("section_tab_${secItem.section.sectionOrder}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "القسم ${secItem.section.sectionOrder}: ${secItem.section.productNameSnapshot}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${secItem.bagCount} شكارة • ${CalculationEngine.formatWeight(secItem.totalWeightKg)} كجم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) WheatGoldLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Active Section Pricing Bar + Built-in One-Handed Quick Keypad
        if (activeSecCalc != null) {
            item {
                ActiveSectionAndKeypadCard(
                    secCalc = activeSecCalc,
                    keypadInput = keypadInput,
                    currencySymbol = settings.currencySymbol,
                    onEditSection = { editingSection = activeSecCalc.section },
                    onDigit = onKeypadDigit,
                    onBackspace = onKeypadBackspace,
                    onClear = onKeypadClear,
                    onSubmitWeight = {
                        if (isApproved) {
                            val v = keypadInput.toDoubleOrNull()
                            if (v != null && v > 0.0) {
                                showAddWeightAuditDialog = true
                            } else {
                                onAddWeightFromKeypad(null)
                            }
                        } else {
                            onAddWeightFromKeypad(null)
                        }
                    }
                )
            }

            // 4. 5-Weight Grouping Columns (Matches Attachment #1 Layout)
            item {
                FiveWeightColumnsSection(
                    secCalc = activeSecCalc,
                    onWeightClick = { w -> editingWeight = w }
                )
            }
        }

        // 5. Invoice Segmentation & Multi-Section Financial Breakdown (Matches Attachment #3)
        item {
            InvoiceSegmentationOverviewCard(
                invoiceNumber = inv.invoiceNumber,
                calc = calc,
                currencySymbol = settings.currencySymbol,
                onSelectSection = onSelectSection,
                onEditSection = { editingSection = it },
                onAddSection = { showAddSectionDialog = true }
            )
        }

        // 6. Bottom Action Buttons (Record Payment, Approve, Export PDF, WhatsApp, Print)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إجراءات الاعتماد والسداد والتصدير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPaymentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WheatGold),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("workspace_record_payment_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل دفعة")
                        }

                        if (!isApproved) {
                            Button(
                                onClick = onApproveInvoice,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("workspace_approve_invoice_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اعتماد الفاتورة")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                PdfInvoiceExporter.shareInvoicePdf(
                                    context = context,
                                    details = invoiceWithCalc.details,
                                    settings = settings,
                                    whatsAppOnly = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_whatsapp_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("واتساب PDF")
                        }

                        OutlinedButton(
                            onClick = {
                                PdfInvoiceExporter.shareInvoicePdf(
                                    context = context,
                                    details = invoiceWithCalc.details,
                                    settings = settings,
                                    whatsAppOnly = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WheatGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة PDF")
                        }

                        OutlinedButton(
                            onClick = {
                                PdfInvoiceExporter.printInvoicePdf(
                                    context = context,
                                    details = invoiceWithCalc.details,
                                    settings = settings
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("print_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة")
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showCustomerDialog) {
        CustomerPickerDialog(
            customers = customers,
            currentCustomerId = inv.customerId,
            currentNotes = inv.notes,
            isApprovedInvoice = isApproved,
            onDismiss = { showCustomerDialog = false },
            onSelectCustomerAndNotes = onUpdateHeader,
            onQuickCreateCustomer = onQuickCreateCustomer
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
            onConfirm = onAddSection
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
        EditOrDeleteWeightDialog(
            weight = w,
            sectionOrder = activeSecCalc?.section?.sectionOrder ?: 1,
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

    if (showAddWeightAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAddWeightAuditDialog = false },
            title = { Text("سبب إضافة وزن لفاتورة معتمدة") },
            text = {
                OutlinedTextField(
                    value = pendingWeightAuditReason,
                    onValueChange = { pendingWeightAuditReason = it },
                    label = { Text("سبب الإضافة (إلزامي)") },
                    placeholder = { Text("مثال: إضافة شكارة منسية في القسم") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingWeightAuditReason.isNotBlank()) {
                            onAddWeightFromKeypad(pendingWeightAuditReason.trim())
                            pendingWeightAuditReason = ""
                            showAddWeightAuditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تأكيد وإضافة الوزن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeightAuditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ActiveSectionAndKeypadCard(
    secCalc: SectionCalculation,
    keypadInput: String,
    currencySymbol: String,
    onEditSection: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmitWeight: () -> Unit
) {
    val sec = secCalc.section
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Pricing Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onEditSection)
                    .testTag("edit_active_section_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "القسم ${sec.sectionOrder}: ${sec.productNameSnapshot} • السعر: ${CalculationEngine.formatNumber(sec.unitPrice)} $currencySymbol / ${sec.unitNameSnapshot}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Text(
                            text = "وزن الوحدة: ${CalculationEngine.formatWeight(sec.unitWeightSnapshot)} كجم • سعر الكيلو: ${CalculationEngine.formatNumber(secCalc.pricePerKg, 3)} $currencySymbol • إجمالي القسم: ${CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل تسعير القسم",
                        tint = EmeraldPrimary
                    )
                }
            }

            // Weight Entry Header + Display Box + "وزن جديد" Button (Matches Attachment #1)
            Text(
                text = "تسجيل وزن جديد (كجم) — شكارة رقم #${secCalc.bagCount + 1} في القسم ${sec.sectionOrder}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(27.dp)),
                    shape = RoundedCornerShape(27.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (keypadInput.isEmpty()) "0.0" else keypadInput,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (keypadInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("keypad_display_text")
                        )
                        Text(
                            text = "كجم صافي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onSubmitWeight,
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("submit_weight_button")
                ) {
                    Text(
                        text = "وزن جديد +",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Built-in One-Handed Quick Numeric Keypad
            val rows = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3"),
                listOf("C", "0", ".", "⌫")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (key) {
                                            "C" -> onClear()
                                            "⌫" -> onBackspace()
                                            else -> onDigit(key)
                                        }
                                    }
                                    .testTag("keypad_key_$key"),
                                shape = RoundedCornerShape(12.dp),
                                color = when (key) {
                                    "C" -> WheatGoldLight
                                    "⌫" -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.background
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "⌫") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "حذف رقم",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 18.sp,
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

@Composable
private fun FiveWeightColumnsSection(
    secCalc: SectionCalculation,
    onWeightClick: (WeightEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الأوزان المسجلة (تجميع خماسي تلقائي)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${secCalc.bagCount} وزنًا • ${secCalc.groups.size} أعمدة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (secCalc.groups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لم يتم تسجيل أوزان في هذا القسم بعد. أدخل الوزن من لوحة الأرقام واضغط «وزن جديد +».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Render groups in pairs of 2 columns side-by-side (Matches Attachment #1)
                val pairedGroups = secCalc.groups.chunked(2)
                pairedGroups.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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

            HorizontalDivider(color = WheatCardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إجمالي أوزان القسم ${secCalc.section.sectionOrder}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${secCalc.bagCount} شكارة • ${secCalc.groups.size} مجموعات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
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
        modifier = modifier.border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "العمود ${group.groupIndex}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )

            group.weights.forEach { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onWeightClick(w) }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
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
                        text = "${CalculationEngine.formatWeight(w.weightKg)} كجم",
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
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${CalculationEngine.formatWeight(group.groupTotalKg)} كجم",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
    }
}

@Composable
private fun InvoiceSegmentationOverviewCard(
    invoiceNumber: String,
    calc: com.example.domain.InvoiceCalculation,
    currencySymbol: String,
    onSelectSection: (Long) -> Unit,
    onEditSection: (InvoiceSectionEntity) -> Unit,
    onAddSection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "تقسيم الفاتورة رقم #$invoiceNumber وملخص المستحقات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            calc.sectionCalculations.forEachIndexed { idx, secCalc ->
                val sec = secCalc.section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (idx % 2 == 1) EmeraldPrimary else WheatCardBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectSection(sec.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "القسم ${sec.sectionOrder} (${sec.productNameSnapshot})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldLight,
                                modifier = Modifier.clickable { onEditSection(sec) }
                            ) {
                                Text(
                                    text = "سعر ${CalculationEngine.formatNumber(sec.unitPrice)} $currencySymbol/${sec.unitNameSnapshot}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الوزن الإجمالي (${secCalc.bagCount} شكارة):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = "سعر الكيلو:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${CalculationEngine.formatNumber(secCalc.pricePerKg, 3)} $currencySymbol",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = WheatCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إجمالي القسم:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                if (idx < calc.sectionCalculations.lastIndex) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = WheatCardBorder)
                        Text(
                            text = "  ✂ فاصل بين الأقسام  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = WheatCardBorder)
                    }
                }
            }

            OutlinedButton(
                onClick = onAddSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_section_bottom_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة قسم جديد (نوع حبوب جديد أو سعر مختلف)")
            }

            // Product Breakdown if multiple grain types exist
            if (calc.productSummaries.size > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "إجمالي الوزن حسب نوع الحبوب:",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary
                        )
                        calc.productSummaries.forEach { prodSum ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${prodSum.productName} (${prodSum.bagCount} شكارة)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${CalculationEngine.formatWeight(prodSum.totalWeightKg)} كجم — ${CalculationEngine.formatCurrency(prodSum.totalAmount, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = WheatCardBorder)

            // Final Invoice Totals Summary
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
                    text = CalculationEngine.formatCurrency(calc.totalInvoiceAmount, currencySymbol),
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
                    text = CalculationEngine.formatCurrency(calc.netPaidAmount, currencySymbol),
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
                    text = CalculationEngine.formatCurrency(calc.remainingAmount, currencySymbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (calc.remainingAmount > 0) StatusUnpaidRed else StatusPaidGreen
                )
            }
        }
    }
}
