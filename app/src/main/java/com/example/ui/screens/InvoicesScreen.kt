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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grass
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.InvoiceType
import com.example.data.local.PaymentRecordType
import com.example.data.local.PaymentStatus
import com.example.data.local.ProductEntity
import com.example.data.local.ProfitCalculationMode
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidBg
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
    var selectedTypeFilter by remember { mutableStateOf<InvoiceType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedPaymentFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var selectedProductFilter by remember { mutableStateOf<String?>(null) }

    var paymentTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var settlementTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var auditTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var deleteTargetInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var expandedInvoiceId by remember { mutableStateOf<Long?>(null) }

    val profitMode = remember(settings.profitCalculationMode) {
        runCatching { ProfitCalculationMode.valueOf(settings.profitCalculationMode) }
            .getOrDefault(ProfitCalculationMode.WITH_EXPENSES)
    }

    val filteredInvoices = remember(
        invoices,
        searchQuery,
        selectedTypeFilter,
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
                inv.customerPhoneSnapshot.contains(searchQuery, ignoreCase = true) ||
                calc.productSummaries.any { it.productName.contains(searchQuery, ignoreCase = true) }

            val matchesType = selectedTypeFilter == null || inv.invoiceType == selectedTypeFilter?.name
            val matchesStatus = selectedStatusFilter == null || inv.status == selectedStatusFilter
            val matchesPayment = selectedPaymentFilter == null || calc.paymentStatus == selectedPaymentFilter
            val matchesProduct = selectedProductFilter == null ||
                calc.productSummaries.any { it.productName == selectedProductFilter }

            matchesQuery && matchesType && matchesStatus && matchesPayment && matchesProduct
        }
    }

    val hasActiveFilter = searchQuery.isNotBlank() ||
        selectedTypeFilter != null ||
        selectedStatusFilter != null ||
        selectedPaymentFilter != null ||
        selectedProductFilter != null

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar-EG")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("invoices_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Modern Search & Filter Card (بدون تكدس أو قطع للاختيارات)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Clean Single-Line Search Bar with Instant Clear Button
                    EhseblySearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "ابحث برقم الفاتورة أو اسم العميل أو الهاتف...",
                        testTag = "invoices_search_input"
                    )

                    // 2. Full-Width 3-Tab Segmented Row for Invoice Type (كل العمليات | شراء | بيع)
                    val purchaseCount = remember(invoices) { invoices.count { !it.calc.isSaleInvoice } }
                    val saleCount = remember(invoices) { invoices.count { it.calc.isSaleInvoice } }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InvoiceTypeSegmentTab(
                            modifier = Modifier.weight(1f),
                            title = "الكل (${invoices.size})",
                            selected = selectedTypeFilter == null,
                            activeColor = EmeraldPrimary,
                            onClick = { selectedTypeFilter = null }
                        )
                        InvoiceTypeSegmentTab(
                            modifier = Modifier.weight(1f),
                            title = "شراء ($purchaseCount)",
                            selected = selectedTypeFilter == InvoiceType.PURCHASE,
                            activeColor = EmeraldPrimary,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == InvoiceType.PURCHASE) null else InvoiceType.PURCHASE
                            }
                        )
                        InvoiceTypeSegmentTab(
                            modifier = Modifier.weight(1f),
                            title = "بيع ($saleCount)",
                            selected = selectedTypeFilter == InvoiceType.SALE,
                            activeColor = WheatGold,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == InvoiceType.SALE) null else InvoiceType.SALE
                            }
                        )
                    }

                    // 3. Two Side-by-Side Dropdown Selectors: (الحالة والدفع) + (نوع الحبوب)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status & Payment Dropdown Selector
                        var statusMenuExpanded by remember { mutableStateOf(false) }
                        val currentStatusLabel = when {
                            selectedStatusFilter == InvoiceStatus.APPROVED.name -> "معتمدة"
                            selectedStatusFilter == InvoiceStatus.DRAFT.name -> "مسودة"
                            selectedPaymentFilter != null -> selectedPaymentFilter!!.labelAr
                            else -> "كل الحالات"
                        }
                        val isStatusFiltered = selectedStatusFilter != null || selectedPaymentFilter != null

                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isStatusFiltered) EmeraldLight else MaterialTheme.colorScheme.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isStatusFiltered) 1.5.dp else 1.dp,
                                        color = if (isStatusFiltered) EmeraldPrimary else WheatCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { statusMenuExpanded = true }
                                    .testTag("invoices_status_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = if (isStatusFiltered) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "الحالة والدفع",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = currentStatusLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isStatusFiltered) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isStatusFiltered) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل الحالات", fontWeight = if (!isStatusFiltered) FontWeight.Bold else FontWeight.Normal) },
                                    trailingIcon = {
                                        if (!isStatusFiltered) Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                    },
                                    onClick = {
                                        selectedStatusFilter = null
                                        selectedPaymentFilter = null
                                        statusMenuExpanded = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("فواتير معتمدة", fontWeight = if (selectedStatusFilter == InvoiceStatus.APPROVED.name) FontWeight.Bold else FontWeight.Normal) },
                                    trailingIcon = {
                                        if (selectedStatusFilter == InvoiceStatus.APPROVED.name) Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                    },
                                    onClick = {
                                        selectedStatusFilter = InvoiceStatus.APPROVED.name
                                        selectedPaymentFilter = null
                                        statusMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("فواتير مسودة", fontWeight = if (selectedStatusFilter == InvoiceStatus.DRAFT.name) FontWeight.Bold else FontWeight.Normal) },
                                    trailingIcon = {
                                        if (selectedStatusFilter == InvoiceStatus.DRAFT.name) Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                    },
                                    onClick = {
                                        selectedStatusFilter = InvoiceStatus.DRAFT.name
                                        selectedPaymentFilter = null
                                        statusMenuExpanded = false
                                    }
                                )
                                HorizontalDivider()
                                PaymentStatus.entries.forEach { pStatus ->
                                    val isSel = selectedPaymentFilter == pStatus
                                    DropdownMenuItem(
                                        text = { Text(pStatus.labelAr, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                        trailingIcon = {
                                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        onClick = {
                                            selectedPaymentFilter = pStatus
                                            selectedStatusFilter = null
                                            statusMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Grain Type Dropdown Selector
                        var grainMenuExpanded by remember { mutableStateOf(false) }
                        val currentGrainLabel = selectedProductFilter ?: "كل الحبوب"
                        val isGrainFiltered = selectedProductFilter != null

                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isGrainFiltered) WheatGoldLight else MaterialTheme.colorScheme.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isGrainFiltered) 1.5.dp else 1.dp,
                                        color = if (isGrainFiltered) WheatGold else WheatCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { grainMenuExpanded = true }
                                    .testTag("invoices_grain_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Grass,
                                            contentDescription = null,
                                            tint = if (isGrainFiltered) WheatGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "نوع الحبوب",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = currentGrainLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGrainFiltered) EmeraldDark else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isGrainFiltered) WheatGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = grainMenuExpanded,
                                onDismissRequest = { grainMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل الحبوب", fontWeight = if (selectedProductFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                    trailingIcon = {
                                        if (selectedProductFilter == null) Icon(Icons.Default.Check, contentDescription = null, tint = WheatGold)
                                    },
                                    onClick = {
                                        selectedProductFilter = null
                                        grainMenuExpanded = false
                                    }
                                )
                                HorizontalDivider()
                                products.forEach { prod ->
                                    val isSel = selectedProductFilter == prod.name
                                    DropdownMenuItem(
                                        text = { Text(prod.name, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Grass,
                                                contentDescription = null,
                                                tint = if (isSel) WheatGold else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = WheatGold)
                                        },
                                        onClick = {
                                            selectedProductFilter = prod.name
                                            grainMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Results Count & Quick Reset Filters Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نتائج البحث: ${filteredInvoices.size} من أصل ${invoices.size} فاتورة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasActiveFilter) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusUnpaidBg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        searchQuery = ""
                                        selectedTypeFilter = null
                                        selectedStatusFilter = null
                                        selectedPaymentFilter = null
                                        selectedProductFilter = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = StatusUnpaidRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "إعادة ضبط البحث",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusUnpaidRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
                val isSale = calc.isSaleInvoice
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
                        // Top Row: Invoice Number & Status Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSale) WheatGoldLight else EmeraldLight
                                ) {
                                    Text(
                                        text = if (isSale) "فاتورة بيع #${inv.invoiceNumber}" else "فاتورة #${inv.invoiceNumber}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSale) EmeraldDark else EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
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
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
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

                        if (isSale) {
                            val saleProfit = calc.getEffectiveProfit(profitMode)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (saleProfit >= 0) StatusPaidBg else StatusUnpaidBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "وزن مقطوع (${CalculationEngine.formatNumber(calc.totalWeightKg / 1000.0, 3)} طن)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "صافي الربح: ${if (saleProfit >= 0) "+" else ""}${CalculationEngine.formatCurrency(saleProfit, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (saleProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                                    )
                                }
                            }
                        }

                        if (isExpanded) {
                            HorizontalDivider(color = WheatCardBorder)
                            calc.sectionCalculations.forEach { secCalc ->
                                val weightLabel = if (isSale) {
                                    "وزن مقطوع (${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم)"
                                } else {
                                    "${secCalc.bagCount} شكارة (${CalculationEngine.formatWeight(secCalc.totalWeightKg)} كجم)"
                                }
                                Text(
                                    text = "• القسم ${secCalc.section.sectionOrder} (${secCalc.section.productNameSnapshot}): $weightLabel × ${CalculationEngine.formatNumber(secCalc.section.unitPrice)} / ${secCalc.section.unitNameSnapshot} = ${CalculationEngine.formatCurrency(secCalc.sectionTotalAmount, settings.currencySymbol)}",
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

                        // Row 1 of Actions: Open & Edit (for Purchases) | Record Payment | Delete Invoice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isSale) {
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

@Composable
private fun InvoiceTypeSegmentTab(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (selected) activeColor else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

