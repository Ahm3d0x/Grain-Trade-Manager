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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.InvoiceStatus
import com.example.data.local.PaymentStatus
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusPartialBg
import com.example.ui.theme.StatusSettlementBg
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.theme.WheatGoldLight
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DashboardPeriod
import com.example.ui.viewmodel.InvoiceWithCalc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    invoices: List<InvoiceWithCalc>,
    selectedPeriod: DashboardPeriod,
    settings: AppSettingsEntity,
    onSelectPeriod: (DashboardPeriod) -> Unit,
    onStartNewPurchase: () -> Unit,
    onOpenInvoice: (Long) -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val filteredInvoices = remember(invoices, selectedPeriod) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val cutoff = when (selectedPeriod) {
            DashboardPeriod.TODAY -> cal.timeInMillis
            DashboardPeriod.THIS_WEEK -> cal.apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            DashboardPeriod.THIS_MONTH -> cal.apply { add(java.util.Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            DashboardPeriod.ALL -> 0L
        }
        invoices.filter { it.details.invoice.invoiceDate >= cutoff }
    }

    val totalPurchases = filteredInvoices.size
    val totalWeightKg = filteredInvoices.sumOf { it.calc.totalWeightKg }
    val totalBags = filteredInvoices.sumOf { it.calc.totalBagCount }
    val totalAmount = filteredInvoices.sumOf { it.calc.totalInvoiceAmount }
    val totalPaid = filteredInvoices.sumOf { it.calc.netPaidAmount }
    val totalRemaining = filteredInvoices.sumOf { it.calc.remainingAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Header Banner + Quick New Purchase Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldDark, EmeraldPrimary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Grain Trade Manager",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WheatGoldLight
                                )
                                Text(
                                    text = settings.merchantName.ifBlank { "إدارة تجارة الحبوب والغلال" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "النظام المتكامل لشراء ووزن الحبوب وتقسيم الفواتير",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Balance,
                                        contentDescription = "شعار الميزان",
                                        tint = WheatGoldLight,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onStartNewPurchase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("new_purchase_hero_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WheatGold,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "عملية شراء جديدة (تسجيل أوزان)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Time Period Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "إحصائيات العمليات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DashboardPeriod.entries) { period ->
                        val selected = period == selectedPeriod
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPeriod(period) },
                            label = {
                                Text(
                                    text = period.labelAr,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("period_chip_${period.name}")
                        )
                    }
                }
            }
        }

        // 3. Key Metrics Grid (Purchases Count, Total Weight, Total Value, Remaining Balance)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "عمليات الشراء",
                        value = "$totalPurchases فاتورة",
                        subtitle = "$totalBags شكارة مسجلة",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = EmeraldPrimary
                    )
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "إجمالي الأوزان",
                        value = "${CalculationEngine.formatWeight(totalWeightKg)} كجم",
                        subtitle = "≈ ${CalculationEngine.formatNumber(totalWeightKg / 1000.0, 3)} طن",
                        icon = Icons.Default.Scale,
                        accentColor = WheatGold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "إجمالي المشتريات",
                        value = CalculationEngine.formatCurrency(totalAmount, settings.currencySymbol),
                        subtitle = "المدفوع: ${CalculationEngine.formatNumber(totalPaid, 0)} ${settings.currencySymbol}",
                        icon = Icons.Default.Payments,
                        accentColor = StatusPaidGreen
                    )
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "المتبقي للعملاء",
                        value = CalculationEngine.formatCurrency(totalRemaining, settings.currencySymbol),
                        subtitle = if (totalRemaining > 0) "مستحق السداد" else "خالص بالكامل",
                        icon = Icons.Default.WarningAmber,
                        accentColor = if (totalRemaining > 0) StatusPartialAmber else StatusPaidGreen
                    )
                }
            }
        }

        // 4. Application Structure Quick Grid (Matches User Attachment #2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "الأقسام الرئيسية للتطبيق",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        title = "عملية شراء جديدة",
                        subtitle = "إدخال الأوزان والأقسام",
                        icon = Icons.Default.Scale,
                        onClick = onStartNewPurchase,
                        testTag = "quick_nav_new_purchase"
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        title = "الفواتير والعمليات",
                        subtitle = "البحث والمدفوعات وPDF",
                        icon = Icons.Default.Description,
                        onClick = { onNavigate(AppScreen.INVOICES) },
                        testTag = "quick_nav_invoices"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        title = "إدارة العملاء",
                        subtitle = "سجل الموردين والحسابات",
                        icon = Icons.Default.Group,
                        onClick = { onNavigate(AppScreen.CUSTOMERS) },
                        testTag = "quick_nav_customers"
                    )
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        title = "التقارير والإحصائيات",
                        subtitle = "الأصناف والعملاء والمدفوعات",
                        icon = Icons.Default.Payments,
                        onClick = { onNavigate(AppScreen.REPORTS) },
                        testTag = "quick_nav_reports"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavTile(
                        modifier = Modifier.weight(1f),
                        title = "التعريفات والإعدادات",
                        subtitle = "الحبوب والوحدات والنسخ",
                        icon = Icons.Default.Settings,
                        onClick = { onNavigate(AppScreen.DEFINITIONS) },
                        testTag = "quick_nav_definitions"
                    )
                }
            }
        }

        // 5. Recent Operations List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر العمليات المسجلة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "عرض الكل",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(AppScreen.INVOICES) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (filteredInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "لا توجد عمليات مسجلة في هذه الفترة",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "اضغط على «عملية شراء جديدة» لبدء تسجيل أوزان الشكاير وإصدار الفاتورة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredInvoices.take(8), key = { it.details.invoice.id }) { item ->
                RecentInvoiceRowCard(
                    item = item,
                    currencySymbol = settings.currencySymbol,
                    onClick = { onOpenInvoice(item.details.invoice.id) }
                )
            }
        }
    }
}

@Composable
private fun StatSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier.border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickNavTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldLight,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentInvoiceRowCard(
    item: InvoiceWithCalc,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val inv = item.details.invoice
    val calc = item.calc
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar", "EG")) }
    val productsText = calc.productSummaries.joinToString(" ، ") { it.productName }.ifBlank { "بدون محصول" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("invoice_card_${inv.invoiceNumber}"),
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldLight
                    ) {
                        Text(
                            text = "فاتورة #${inv.invoiceNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    InvoiceStatusBadge(status = inv.status)
                }
                PaymentStatusBadge(paymentStatus = calc.paymentStatus)
            }

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
                        text = "$productsText • ${calc.sectionCalculations.size} قسم • ${dateFormat.format(Date(inv.invoiceDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CalculationEngine.formatCurrency(calc.totalInvoiceAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم (${calc.totalBagCount} شكارة)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceStatusBadge(status: String) {
    val isApproved = status == InvoiceStatus.APPROVED.name
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isApproved) EmeraldLight else WheatGoldLight
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isApproved) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = if (isApproved) "معتمدة" else "مسودة",
                style = MaterialTheme.typography.labelMedium,
                color = if (isApproved) EmeraldPrimary else WheatGold,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PaymentStatusBadge(paymentStatus: PaymentStatus) {
    val (bg, fg) = when (paymentStatus) {
        PaymentStatus.PAID -> StatusPaidBg to StatusPaidGreen
        PaymentStatus.PARTIAL -> StatusPartialBg to StatusPartialAmber
        PaymentStatus.UNPAID -> StatusUnpaidBg to StatusUnpaidRed
        PaymentStatus.OVERPAID_NEEDS_SETTLEMENT -> StatusSettlementBg to StatusSettlementPurple
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = paymentStatus.labelAr,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
