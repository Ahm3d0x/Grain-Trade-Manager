package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.PaymentRecordType
import com.example.data.local.PaymentStatus
import com.example.data.local.ProductEntity
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.viewmodel.InvoiceWithCalc
import com.example.util.PdfInvoiceExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesScreen(
    invoices: List<InvoiceWithCalc>,
    products: List<ProductEntity>,
    settings: AppSettingsEntity,
    onOpenInWorkspace: (Long) -> Unit,
    onRecordPayment: (Long, Double, String, String) -> Unit,
    onSettleOverpayment: (Long, Double, Boolean, String) -> Unit,
    onDeleteInvoice: (Long) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedPaymentFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var selectedProductFilter by remember { mutableStateOf<String?>(null) }

    var paymentTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var settlementTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var auditTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var deleteTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var expandedInvoiceId by remember { mutableStateOf<Long?>(null) }

    val filteredInvoices = remember(
        invoices,
        searchQuery,
        selectedStatusFilter,
        selectedPaymentFilter,
        selectedProductFilter
    ) {
        invoices.filter { item ->
            val inv = item.details.invoice
            val calc = item.calc
            val matchesQuery = searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.customerNameSnapshot.contains(searchQuery, ignoreCase = true) ||
                inv.customerPhoneSnapshot.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatusFilter == null || inv.status == selectedStatusFilter
            val matchesPayment = selectedPaymentFilter == null || calc.paymentStatus == selectedPaymentFilter
            val matchesProduct = selectedProductFilter == null ||
                calc.productSummaries.any { it.productName == selectedProductFilter }

            matchesQuery && matchesStatus && matchesPayment && matchesProduct
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar-EG")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("invoices_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Filter Card
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
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("بحث برقم الفاتورة، اسم العميل، أو الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("invoices_search_input")
                    )

                    // Status & Payment Filters
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == null && selectedPaymentFilter == null,
                                onClick = {
                                    selectedStatusFilter = null
                                    selectedPaymentFilter = null
                                },
                                label = { Text("الكل (${invoices.size})", softWrap = false, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == InvoiceStatus.APPROVED.name,
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == InvoiceStatus.APPROVED.name) null else InvoiceStatus.APPROVED.name
                                },
                                label = { Text("معتمدة", softWrap = false, maxLines = 1) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == InvoiceStatus.DRAFT.name,
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == InvoiceStatus.DRAFT.name) null else InvoiceStatus.DRAFT.name
                                },
                                label = { Text("مسودة", softWrap = false, maxLines = 1) }
                            )
                        }
                        items(PaymentStatus.entries) { pStatus ->
                            FilterChip(
                                selected = selectedPaymentFilter == pStatus,
                                onClick = {
                                    selectedPaymentFilter = if (selectedPaymentFilter == pStatus) null else pStatus
                                },
                                label = { Text(pStatus.labelAr, softWrap = false, maxLines = 1) }
                            )
                        }
                    }

                    // Grain Type Filter
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedProductFilter == null,
                                onClick = { selectedProductFilter = null },
                                label = { Text("كل الحبوب", softWrap = false, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WheatGold,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(products) { prod ->
                            FilterChip(
                                selected = selectedProductFilter == prod.name,
                                onClick = {
                                    selectedProductFilter = if (selectedProductFilter == prod.name) null else prod.name
                                },
                                label = { Text(prod.name, softWrap = false, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WheatGold,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        if (filteredInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "لا توجد فواتير مطابقة لشروط البحث الحالية.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(filteredInvoices, key = { it.details.invoice.id }) { item ->
                val inv = item.details.invoice
                val calc = item.calc
                val isExpanded = expandedInvoiceId == inv.id

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
                        // Top Row: Invoice Number, Status Badges, and Prominent Delete Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(8.dp), color = EmeraldLight) {
                                    Text(
                                        text = "فاتورة #${inv.invoiceNumber}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldPrimary,
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                InvoiceStatusBadge(status = inv.status)
                                PaymentStatusBadge(paymentStatus = calc.paymentStatus)
                            }
                        }

                        // Customer & Amounts Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inv.customerNameSnapshot,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(inv.invoiceDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val prods = calc.productSummaries.joinToString(" ، ") {
                                    "${it.productName} (${CalculationEngine.formatWeight(it.totalWeightKg)} كجم)"
                                }
                                Text(
                                    text = prods.ifBlank { "بدون أوزان" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CalculationEngine.formatCurrency(calc.totalInvoiceAmount, settings.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "المدفوع: ${CalculationEngine.formatNumber(calc.netPaidAmount)} | المتبقي: ${CalculationEngine.formatNumber(calc.remainingAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (calc.remainingAmount > 0) StatusUnpaidRed else StatusPaidGreen
                                )
                            }
                        }

                        if (isExpanded) {
                            HorizontalDivider(color = WheatCardBorder)
                            calc.sectionCalculations.forEach { secCalc ->
                                Text(
                                    text = "• القسم ${secCalc.section.sectionOrder} (${secCalc.section.productNameSnapshot}): ${secCalc.bagCount} شكارة (${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم) × ${CalculationEngine.formatNumber(secCalc.section.unitPrice)} / ${secCalc.section.unitNameSnapshot} = ${CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (item.details.payments.isNotEmpty()) {
                                Text(
                                    text = "سجل الدفعات والتسويات:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                                item.details.payments.forEach { p ->
                                    val typeLabel = runCatching { PaymentRecordType.valueOf(p.type).labelAr }.getOrDefault(p.type)
                                    Text(
                                        text = "  - $typeLabel: ${CalculationEngine.formatCurrency(p.amount, settings.currencySymbol)} (${p.paymentMethod}) — ${dateFormat.format(Date(p.paymentDate))}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = WheatCardBorder)

                        // Row 1 of Actions: Open & Edit | Record Payment | Delete Invoice (Explicitly visible outside!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onOpenInWorkspace(inv.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1.2f).height(42.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح وتعديل", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { paymentTargetInvoice = item },
                                modifier = Modifier.weight(1f).height(42.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دفعة", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { deleteTargetInvoice = item },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = StatusUnpaidBg.copy(alpha = 0.35f),
                                    contentColor = StatusUnpaidRed
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("delete_invoice_btn_${inv.invoiceNumber}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                            }
                        }

                        // Row 2 of Actions: Share WhatsApp | Export PDF | Print | Audit History
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    PdfInvoiceExporter.shareInvoicePdf(
                                        context = context,
                                        details = item.details,
                                        settings = settings,
                                        whatsAppOnly = true
                                    )
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    PdfInvoiceExporter.saveInvoicePdfToPhone(
                                        context = context,
                                        details = item.details,
                                        settings = settings
                                    )
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WheatGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    PdfInvoiceExporter.printInvoicePdf(
                                        context = context,
                                        details = item.details,
                                        settings = settings
                                    )
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = WheatGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طباعة", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { auditTargetInvoice = item },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("السجل", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                            }
                        }

                        if (calc.overpaidAmount > 0.01) {
                            Button(
                                onClick = { settlementTargetInvoice = item },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSettlementPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تسوية الفائض المالي (${CalculationEngine.formatCurrency(calc.overpaidAmount, settings.currencySymbol)})")
                            }
                        }

                        TextButton(
                            onClick = { expandedInvoiceId = if (isExpanded) null else inv.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isExpanded) "إخفاء تفصيل الأقسام والدفعات ▲" else "عرض تفصيل الأقسام والدفعات ▼")
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    paymentTargetInvoice?.let { target ->
        RecordPaymentDialog(
            invoiceNumber = target.details.invoice.invoiceNumber,
            totalInvoiceAmount = target.calc.totalInvoiceAmount,
            netPaidAmount = target.calc.netPaidAmount,
            remainingAmount = target.calc.remainingAmount,
            currencySymbol = settings.currencySymbol,
            onDismiss = { paymentTargetInvoice = null },
            onRecordPayment = { amt, method, notes ->
                onRecordPayment(target.details.invoice.id, amt, method, notes)
            }
        )
    }

    settlementTargetInvoice?.let { target ->
        OverpaymentSettlementDialog(
            invoiceNumber = target.details.invoice.invoiceNumber,
            overpaidAmount = target.calc.overpaidAmount,
            hasRegisteredCustomer = target.details.invoice.customerId != null,
            customerName = target.details.invoice.customerNameSnapshot,
            currencySymbol = settings.currencySymbol,
            onDismiss = { settlementTargetInvoice = null },
            onSettle = { asCredit, notes ->
                onSettleOverpayment(
                    target.details.invoice.id,
                    target.calc.overpaidAmount,
                    asCredit,
                    notes
                )
            }
        )
    }

    auditTargetInvoice?.let { target ->
        AuditLogsDialog(
            invoiceNumber = target.details.invoice.invoiceNumber,
            logs = target.details.auditLogs.sortedByDescending { it.timestamp },
            onDismiss = { auditTargetInvoice = null }
        )
    }

    deleteTargetInvoice?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTargetInvoice = null },
            title = {
                Text(
                    text = "تأكيد حذف الفاتورة #${target.details.invoice.invoiceNumber}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("هل أنت متأكد من حذف فاتورة العميل «${target.details.invoice.customerNameSnapshot}» وجميع أوزانها نهائيًا؟ لا يمكن التراجع عن هذا الإجراء.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInvoice(target.details.invoice.id)
                        deleteTargetInvoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed)
                ) {
                    Text("نعم، احذف الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetInvoice = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
