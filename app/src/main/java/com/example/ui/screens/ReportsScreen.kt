package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.PaymentRecordType
import com.example.data.local.PaymentStatus
import com.example.data.local.ProfitCalculationMode
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialBg
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusSettlementBg
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.theme.WheatGoldLight
import com.example.ui.viewmodel.DashboardPeriod
import com.example.ui.viewmodel.InvoiceWithCalc
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ReportTab(val titleAr: String, val icon: ImageVector) {
    PROFIT_CHARTS("رسوم أرباح الحبوب", Icons.Default.BarChart),
    OVERVIEW("الملخص العام", Icons.Default.Assessment),
    PRODUCTS("تقرير المحاصيل", Icons.Default.Grass),
    CUSTOMERS("تقرير العملاء", Icons.Default.Group),
    DAILY_AND_PAYMENTS("الحركة اليومية والمدفوعات", Icons.Default.CalendarMonth)
}

private data class ProductStatRow(
    val productName: String,
    val invoiceCount: Int,
    val sectionCount: Int,
    val bagCount: Int,
    val totalWeightKg: Double,
    val totalAmount: Double,
    val avgPricePerTon: Double,
    val weightShareRatio: Float
)

private data class CustomerStatRow(
    val customerKey: String,
    val customerName: String,
    val customerPhone: String,
    val invoiceCount: Int,
    val bagCount: Int,
    val totalWeightKg: Double,
    val totalAmount: Double,
    val netPaidAmount: Double,
    val remainingAmount: Double,
    val creditBalance: Double,
    val latestInvoiceId: Long?
)

private data class DailyStatRow(
    val dayKey: String,
    val dayDisplay: String,
    val timestamp: Long,
    val invoiceCount: Int,
    val bagCount: Int,
    val totalWeightKg: Double,
    val totalAmount: Double,
    val netPaidAmount: Double,
    val remainingAmount: Double
)

private data class PaymentMethodStatRow(
    val method: String,
    val count: Int,
    val totalAmount: Double
)

@Composable
fun ReportsScreen(
    invoices: List<InvoiceWithCalc>,
    customers: List<CustomerEntity>,
    settings: AppSettingsEntity,
    onOpenInvoice: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ReportTab.PROFIT_CHARTS) }
    var selectedPeriod by remember { mutableStateOf(DashboardPeriod.ALL) }
    var includeDrafts by remember { mutableStateOf(true) }
    var onlyUnpaidCustomers by remember { mutableStateOf(false) }

    val profitMode = remember(settings.profitCalculationMode) {
        runCatching { ProfitCalculationMode.valueOf(settings.profitCalculationMode) }
            .getOrDefault(ProfitCalculationMode.WITH_EXPENSES)
    }

    val filteredInvoices = remember(invoices, selectedPeriod, includeDrafts) {
        val periodFiltered = filterInvoicesByPeriodForReports(invoices, selectedPeriod)
        if (includeDrafts) {
            periodFiltered
        } else {
            periodFiltered.filter { it.details.invoice.status == InvoiceStatus.APPROVED.name }
        }
    }

    // Aggregate KPIs
    val totalInvoicesCount = filteredInvoices.size
    val approvedCount = filteredInvoices.count { it.details.invoice.status == InvoiceStatus.APPROVED.name }
    val draftCount = filteredInvoices.count { it.details.invoice.status == InvoiceStatus.DRAFT.name }
    val totalBags = filteredInvoices.sumOf { it.calc.totalBagCount }
    val totalWeightKg = filteredInvoices.sumOf { it.calc.totalWeightKg }
    val totalPurchasesAmount = filteredInvoices.sumOf { it.calc.totalInvoiceAmount }
    val totalNetPaid = filteredInvoices.sumOf { it.calc.netPaidAmount }
    val totalRemaining = filteredInvoices.sumOf { it.calc.remainingAmount }
    val totalOverpaid = filteredInvoices.sumOf { it.calc.overpaidAmount }

    val paidInvoicesCount = filteredInvoices.count { it.calc.paymentStatus == PaymentStatus.PAID }
    val partialInvoicesCount = filteredInvoices.count { it.calc.paymentStatus == PaymentStatus.PARTIAL }
    val unpaidInvoicesCount = filteredInvoices.count { it.calc.paymentStatus == PaymentStatus.UNPAID }

    // 1. Products Statistics
    val productStats = remember(filteredInvoices, totalWeightKg) {
        val map = mutableMapOf<String, MutableList<Pair<Long, com.example.domain.SectionCalculation>>>()
        filteredInvoices.forEach { inv ->
            inv.calc.sectionCalculations.forEach { secCalc ->
                val name = secCalc.section.productNameSnapshot.ifBlank { "غير محدد" }
                map.getOrPut(name) { mutableListOf() }.add(inv.details.invoice.id to secCalc)
            }
        }
        map.map { (productName, pairs) ->
            val distinctInvoices = pairs.map { it.first }.distinct().size
            val bags = pairs.sumOf { it.second.bagCount }
            val weight = pairs.sumOf { it.second.totalWeightKg }
            val amount = pairs.sumOf { it.second.sectionTotalAmount }
            val avgTon = if (weight > 0) (amount / weight) * 1000.0 else 0.0
            val share = if (totalWeightKg > 0) (weight / totalWeightKg).toFloat().coerceIn(0f, 1f) else 0f
            ProductStatRow(
                productName = productName,
                invoiceCount = distinctInvoices,
                sectionCount = pairs.size,
                bagCount = bags,
                totalWeightKg = weight,
                totalAmount = amount,
                avgPricePerTon = avgTon,
                weightShareRatio = share
            )
        }.sortedByDescending { it.totalWeightKg }
    }

    // 2. Customers Statistics
    val customerStats = remember(filteredInvoices, customers, onlyUnpaidCustomers) {
        val customerMapById = customers.associateBy { it.id }
        val grouped = filteredInvoices.groupBy { inv ->
            inv.details.invoice.customerId?.let { "ID_$it" }
                ?: "NAME_${inv.details.invoice.customerNameSnapshot.trim()}"
        }
        val list = grouped.map { (key, invList) ->
            val firstInv = invList.first().details.invoice
            val linkedCust = firstInv.customerId?.let { customerMapById[it] }
            CustomerStatRow(
                customerKey = key,
                customerName = linkedCust?.name ?: firstInv.customerNameSnapshot,
                customerPhone = linkedCust?.phone ?: firstInv.customerPhoneSnapshot,
                invoiceCount = invList.size,
                bagCount = invList.sumOf { it.calc.totalBagCount },
                totalWeightKg = invList.sumOf { it.calc.totalWeightKg },
                totalAmount = invList.sumOf { it.calc.totalInvoiceAmount },
                netPaidAmount = invList.sumOf { it.calc.netPaidAmount },
                remainingAmount = invList.sumOf { it.calc.remainingAmount },
                creditBalance = linkedCust?.creditBalance ?: 0.0,
                latestInvoiceId = invList.maxByOrNull { it.details.invoice.invoiceDate }?.details?.invoice?.id
            )
        }.sortedByDescending { it.totalAmount }

        if (onlyUnpaidCustomers) {
            list.filter { it.remainingAmount > 0 }
        } else {
            list
        }
    }

    // 3. Daily Movement & Payment Methods Statistics
    val dailyStats = remember(filteredInvoices) {
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayDisplayFormat = SimpleDateFormat("EEEE yyyy/MM/dd", Locale.forLanguageTag("ar-EG"))
        filteredInvoices
            .groupBy { dayKeyFormat.format(Date(it.details.invoice.invoiceDate)) }
            .map { (dayKey, invList) ->
                val sampleDate = invList.first().details.invoice.invoiceDate
                DailyStatRow(
                    dayKey = dayKey,
                    dayDisplay = dayDisplayFormat.format(Date(sampleDate)),
                    timestamp = sampleDate,
                    invoiceCount = invList.size,
                    bagCount = invList.sumOf { it.calc.totalBagCount },
                    totalWeightKg = invList.sumOf { it.calc.totalWeightKg },
                    totalAmount = invList.sumOf { it.calc.totalInvoiceAmount },
                    netPaidAmount = invList.sumOf { it.calc.netPaidAmount },
                    remainingAmount = invList.sumOf { it.calc.remainingAmount }
                )
            }
            .sortedByDescending { it.timestamp }
    }

    val paymentMethodStats = remember(filteredInvoices) {
        val allPayments = filteredInvoices.flatMap { it.details.payments }
            .filter { it.type == PaymentRecordType.PAYMENT.name }
        allPayments
            .groupBy { it.paymentMethod.ifBlank { "نقدي" } }
            .map { (method, list) ->
                PaymentMethodStatRow(
                    method = method,
                    count = list.size,
                    totalAmount = list.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen")
    ) {
        // Top Header & Filters Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "التقارير والرسوم البيانية للأرباح",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "رسوم بيانية تفاعلية لأرباح الحبوب وتحليل المشتريات والعملاء",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            val reportText = buildShareableReportText(
                                periodLabel = selectedPeriod.labelAr,
                                currency = settings.currencySymbol,
                                totalInvoicesCount = totalInvoicesCount,
                                totalBags = totalBags,
                                totalWeightKg = totalWeightKg,
                                totalPurchasesAmount = totalPurchasesAmount,
                                totalNetPaid = totalNetPaid,
                                totalRemaining = totalRemaining,
                                productStats = productStats,
                                customerStats = customerStats
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة التقرير الإحصائي"))
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EmeraldLight)
                            .testTag("share_report_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة التقرير",
                            tint = EmeraldPrimary
                        )
                    }
                }

                // Period Filter Row + Include Drafts Toggle
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(DashboardPeriod.entries) { period ->
                        val selected = period == selectedPeriod
                        FilterChip(
                            selected = selected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.labelAr, softWrap = false, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("report_period_${period.name}")
                        )
                    }

                    item {
                        FilterChip(
                            selected = !includeDrafts,
                            onClick = { includeDrafts = !includeDrafts },
                            label = {
                                Text(
                                    text = if (includeDrafts) "شامل المسودات" else "المعتمدة فقط",
                                    softWrap = false,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WheatGold,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Sub-navigation tabs for the Report types
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReportTab.entries) { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary else EmeraldLight.copy(alpha = 0.55f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = tab }
                                .testTag("report_tab_${tab.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else EmeraldDark,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Report Content Area
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                ReportTab.PROFIT_CHARTS -> {
                    item {
                        GrainProfitChartsSection(
                            allInvoices = invoices,
                            periodFilteredInvoices = filteredInvoices,
                            periodLabel = selectedPeriod.labelAr,
                            profitMode = profitMode,
                            currency = settings.currencySymbol
                        )
                    }
                }

                ReportTab.OVERVIEW -> {
                    item {
                        OverviewReportSection(
                            currency = settings.currencySymbol,
                            totalInvoicesCount = totalInvoicesCount,
                            approvedCount = approvedCount,
                            draftCount = draftCount,
                            totalBags = totalBags,
                            totalWeightKg = totalWeightKg,
                            totalPurchasesAmount = totalPurchasesAmount,
                            totalNetPaid = totalNetPaid,
                            totalRemaining = totalRemaining,
                            totalOverpaid = totalOverpaid,
                            paidInvoicesCount = paidInvoicesCount,
                            partialInvoicesCount = partialInvoicesCount,
                            unpaidInvoicesCount = unpaidInvoicesCount,
                            topProducts = productStats.take(4),
                            topCustomers = customerStats.take(4)
                        )
                    }
                }

                ReportTab.PRODUCTS -> {
                    if (productStats.isEmpty()) {
                        item {
                            EmptyReportCard("لا توجد بيانات محاصيل مسجلة في هذه الفترة.")
                        }
                    } else {
                        item {
                            SummaryBannerCard(
                                title = "إجمالي المحاصيل المشتراة (${productStats.size} صنف)",
                                valueRight = "${CalculationEngine.formatWeight(totalWeightKg)} كجم (${totalBags} شكارة)",
                                valueLeft = CalculationEngine.formatCurrency(totalPurchasesAmount, settings.currencySymbol)
                            )
                        }
                        items(productStats, key = { it.productName }) { item ->
                            ProductStatCard(item = item, currency = settings.currencySymbol)
                        }
                    }
                }

                ReportTab.CUSTOMERS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إحصائيات وكشف أرصدة العملاء (${customerStats.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilterChip(
                                selected = onlyUnpaidCustomers,
                                onClick = { onlyUnpaidCustomers = !onlyUnpaidCustomers },
                                label = { Text("لهم متبقي فقط", softWrap = false, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusUnpaidRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (customerStats.isEmpty()) {
                        item {
                            EmptyReportCard("لا توجد بيانات عملاء مطابقة للفلتر المحدد.")
                        }
                    } else {
                        items(customerStats, key = { it.customerKey }) { custRow ->
                            CustomerStatCard(
                                item = custRow,
                                currency = settings.currencySymbol,
                                onOpenLatestInvoice = {
                                    custRow.latestInvoiceId?.let(onOpenInvoice)
                                }
                            )
                        }
                    }
                }

                ReportTab.DAILY_AND_PAYMENTS -> {
                    item {
                        PaymentMethodsBreakdownCard(
                            paymentMethodStats = paymentMethodStats,
                            totalNetPaid = totalNetPaid,
                            currency = settings.currencySymbol
                        )
                    }

                    item {
                        Text(
                            text = "سجل الحركة اليومية (${dailyStats.size} يوم عمل)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (dailyStats.isEmpty()) {
                        item {
                            EmptyReportCard("لا توجد حركات يومية مسجلة في هذه الفترة.")
                        }
                    } else {
                        items(dailyStats, key = { it.dayKey }) { dayRow ->
                            DailyStatCard(item = dayRow, currency = settings.currencySymbol)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewReportSection(
    currency: String,
    totalInvoicesCount: Int,
    approvedCount: Int,
    draftCount: Int,
    totalBags: Int,
    totalWeightKg: Double,
    totalPurchasesAmount: Double,
    totalNetPaid: Double,
    totalRemaining: Double,
    totalOverpaid: Double,
    paidInvoicesCount: Int,
    partialInvoicesCount: Int,
    unpaidInvoicesCount: Int,
    topProducts: List<ProductStatRow>,
    topCustomers: List<CustomerStatRow>
) {
    val paidRatio = if (totalPurchasesAmount > 0) {
        (totalNetPaid / totalPurchasesAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Total Purchases & Total Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportMetricCard(
                modifier = Modifier.weight(1f),
                title = "إجمالي قيمة المشتريات",
                value = CalculationEngine.formatCurrency(totalPurchasesAmount, currency),
                subtitle = "$totalInvoicesCount فاتورة ($approvedCount معتمدة • $draftCount مسودة)",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = EmeraldPrimary,
                bgColor = EmeraldLight
            )
            ReportMetricCard(
                modifier = Modifier.weight(1f),
                title = "إجمالي الوزن المشترى",
                value = "${CalculationEngine.formatWeight(totalWeightKg)} كجم",
                subtitle = "إجمالي الشكاير: $totalBags شكارة",
                icon = Icons.Default.Scale,
                accentColor = WheatGold,
                bgColor = WheatGoldLight
            )
        }

        // Row 2: Net Paid & Remaining Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportMetricCard(
                modifier = Modifier.weight(1f),
                title = "إجمالي المسدد الفعلي",
                value = CalculationEngine.formatCurrency(totalNetPaid, currency),
                subtitle = "نسبة السداد: ${(paidRatio * 100).toInt()}%",
                icon = Icons.Default.CheckCircle,
                accentColor = StatusPaidGreen,
                bgColor = StatusPaidBg
            )
            ReportMetricCard(
                modifier = Modifier.weight(1f),
                title = "المتبقي المستحق للعملاء",
                value = CalculationEngine.formatCurrency(totalRemaining, currency),
                subtitle = if (totalOverpaid > 0) "فائض: ${CalculationEngine.formatCurrency(totalOverpaid, currency)}" else "مستحقات آجلة",
                icon = Icons.Default.WarningAmber,
                accentColor = StatusUnpaidRed,
                bgColor = StatusUnpaidBg
            )
        }

        // Payment Progress & Invoice Status Breakdown Card
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
                        text = "مؤشر السداد وحالات الفواتير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(paidRatio * 100).toInt()}% مسدد",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Visual progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StatusUnpaidBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(paidRatio)
                            .fillMaxHeight()
                            .background(StatusPaidGreen)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "مدفوعة بالكامل",
                        count = paidInvoicesCount,
                        textColor = StatusPaidGreen,
                        bgColor = StatusPaidBg
                    )
                    StatusMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "مدفوعة جزئيًا",
                        count = partialInvoicesCount,
                        textColor = StatusPartialAmber,
                        bgColor = StatusPartialBg
                    )
                    StatusMiniBox(
                        modifier = Modifier.weight(1f),
                        label = "غير مدفوعة",
                        count = unpaidInvoicesCount,
                        textColor = StatusUnpaidRed,
                        bgColor = StatusUnpaidBg
                    )
                }
            }
        }

        // Top Crops Quick Table
        if (topProducts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ملخص الأوزان والقيم حسب الصنف",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    topProducts.forEach { prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.productName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${prod.bagCount} شكارة • ${(prod.weightShareRatio * 100).toInt()}% من الوزن",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${CalculationEngine.formatWeight(prod.totalWeightKg)} كجم",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = CalculationEngine.formatCurrency(prod.totalAmount, currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Customers Quick Table
        if (topCustomers.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "أعلى الموردين / العملاء تعاملًا",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    topCustomers.forEach { cust ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cust.customerName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${cust.invoiceCount} فاتورة • ${CalculationEngine.formatWeight(cust.totalWeightKg)} كجم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CalculationEngine.formatCurrency(cust.totalAmount, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                if (cust.remainingAmount > 0) {
                                    Text(
                                        text = "متبقي: ${CalculationEngine.formatCurrency(cust.remainingAmount, currency)}",
                                        style = MaterialTheme.typography.bodySmall,
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
    }
}

@Composable
private fun ProductStatCard(
    item: ProductStatRow,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Grass, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.invoiceCount} فاتورة (${item.sectionCount} قسم)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WheatGoldLight
                ) {
                    Text(
                        text = "${(item.weightShareRatio * 100).toInt()}% من الوزن الكلي",
                        style = MaterialTheme.typography.labelMedium,
                        color = WheatGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Share bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EmeraldLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.weightShareRatio.coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .background(EmeraldPrimary)
                )
            }

            HorizontalDivider(color = WheatCardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("إجمالي الوزن والشكاير", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${CalculationEngine.formatWeight(item.totalWeightKg)} كجم (${item.bagCount} شكارة)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("متوسط سعر الطن", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CalculationEngine.formatCurrency(item.avgPricePerTon, currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WheatGold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("إجمالي القيمة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CalculationEngine.formatCurrency(item.totalAmount, currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerStatCard(
    item: CustomerStatRow,
    currency: String,
    onOpenLatestInvoice: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp))
            .clickable { onOpenLatestInvoice() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append("${item.invoiceCount} فاتورة • ${item.bagCount} شكارة")
                            if (item.customerPhone.isNotBlank()) append(" • ${item.customerPhone}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.remainingAmount > 0) StatusUnpaidBg else StatusPaidBg
                ) {
                    Text(
                        text = if (item.remainingAmount > 0) {
                            "متبقي: ${CalculationEngine.formatCurrency(item.remainingAmount, currency)}"
                        } else {
                            "خالص السداد"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.remainingAmount > 0) StatusUnpaidRed else StatusPaidGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = WheatCardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("إجمالي التوريد (الوزن)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${CalculationEngine.formatWeight(item.totalWeightKg)} كجم",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي المشتريات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CalculationEngine.formatCurrency(item.totalAmount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("إجمالي المسدد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CalculationEngine.formatCurrency(item.netPaidAmount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusPaidGreen
                    )
                }
            }

            if (item.creditBalance > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusSettlementBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "رصيد دائن مرحّل للعميل: ${CalculationEngine.formatCurrency(item.creditBalance, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusSettlementPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodsBreakdownCard(
    paymentMethodStats: List<PaymentMethodStatRow>,
    totalNetPaid: Double,
    currency: String
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = EmeraldPrimary)
                    Text(
                        text = "توزيع المدفوعات حسب طريقة الدفع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = CalculationEngine.formatCurrency(totalNetPaid, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (paymentMethodStats.isEmpty()) {
                Text(
                    text = "لم يتم تسجيل دفعات سداد خلال هذه الفترة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                paymentMethodStats.forEach { pm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pm.method} (${pm.count} عملية)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(pm.totalAmount, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStatCard(
    item: DailyStatRow,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dayDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Text(
                    text = "${item.invoiceCount} فاتورة • ${item.bagCount} شكارة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = WheatCardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("إجمالي الوزن", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${CalculationEngine.formatWeight(item.totalWeightKg)} كجم",
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي القيمة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CalculationEngine.formatCurrency(item.totalAmount, currency),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المسدد / المتبقي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${CalculationEngine.formatNumber(item.netPaidAmount)} / ${CalculationEngine.formatNumber(item.remainingAmount)} $currency",
                        fontWeight = FontWeight.Bold,
                        color = if (item.remainingAmount > 0) StatusUnpaidRed else StatusPaidGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier.border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusMiniBox(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = textColor,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryBannerCard(
    title: String,
    valueRight: String,
    valueLeft: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = EmeraldLight,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldPrimary, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = EmeraldDark)
                Text(valueRight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            }
            Text(valueLeft, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
        }
    }
}

@Composable
private fun EmptyReportCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun filterInvoicesByPeriodForReports(
    invoices: List<InvoiceWithCalc>,
    period: DashboardPeriod
): List<InvoiceWithCalc> {
    if (period == DashboardPeriod.ALL) return invoices
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val minTimestamp = when (period) {
        DashboardPeriod.TODAY -> cal.timeInMillis
        DashboardPeriod.THIS_WEEK -> {
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.timeInMillis
        }
        DashboardPeriod.THIS_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.timeInMillis
        }
        DashboardPeriod.ALL -> 0L
    }
    return invoices.filter { it.details.invoice.invoiceDate >= minTimestamp }
}

@Suppress("LongParameterList")
private fun buildShareableReportText(
    periodLabel: String,
    currency: String,
    totalInvoicesCount: Int,
    totalBags: Int,
    totalWeightKg: Double,
    totalPurchasesAmount: Double,
    totalNetPaid: Double,
    totalRemaining: Double,
    productStats: List<ProductStatRow>,
    customerStats: List<CustomerStatRow>
): String {
    return buildString {
        appendLine("📊 تقرير المشتريات والإحصائيات ($periodLabel)")
        appendLine("────────────────────────")
        appendLine("• عدد الفواتير: $totalInvoicesCount فاتورة")
        appendLine("• إجمالي الوزن: ${CalculationEngine.formatWeight(totalWeightKg)} كجم ($totalBags شكارة)")
        appendLine("• إجمالي قيمة المشتريات: ${CalculationEngine.formatCurrency(totalPurchasesAmount, currency)}")
        appendLine("• إجمالي المسدد: ${CalculationEngine.formatCurrency(totalNetPaid, currency)}")
        appendLine("• المتبقي المستحق للعملاء: ${CalculationEngine.formatCurrency(totalRemaining, currency)}")
        if (productStats.isNotEmpty()) {
            appendLine("────────────────────────")
            appendLine("🌾 تفصيل الأصناف:")
            productStats.forEach { p ->
                appendLine("- ${p.productName}: ${CalculationEngine.formatWeight(p.totalWeightKg)} كجم (${p.bagCount} شكارة) = ${CalculationEngine.formatCurrency(p.totalAmount, currency)}")
            }
        }
        val unpaidCusts = customerStats.filter { it.remainingAmount > 0 }
        if (unpaidCusts.isNotEmpty()) {
            appendLine("────────────────────────")
            appendLine("👥 العملاء المستحق لهم مبالغ متبقية:")
            unpaidCusts.forEach { c ->
                appendLine("- ${c.customerName}: متبقي ${CalculationEngine.formatCurrency(c.remainingAmount, currency)}")
            }
        }
    }
}
