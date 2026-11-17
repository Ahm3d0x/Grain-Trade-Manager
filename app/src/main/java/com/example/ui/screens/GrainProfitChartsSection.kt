package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InvoiceStatus
import com.example.data.local.ProfitCalculationMode
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.theme.WheatGoldLight
import com.example.ui.viewmodel.InvoiceWithCalc
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GrainProfitChartPoint(
    val productName: String,
    val saleInvoicesCount: Int,
    val soldWeightKg: Double,
    val totalRevenue: Double,
    val totalCostOfGoods: Double,
    val totalExpenses: Double,
    val directProfit: Double,
    val netProfitAfterExpenses: Double,
    val profitMarginPercent: Double,
    val profitShareRatio: Float
) {
    fun getProfit(mode: ProfitCalculationMode): Double =
        when (mode) {
            ProfitCalculationMode.WITH_EXPENSES -> netProfitAfterExpenses
            ProfitCalculationMode.DIRECT_MARGIN -> directProfit
        }
}

private enum class ProfitChartType(val labelAr: String) {
    PROFIT_BARS("أعمدة صافي الربح"),
    GROUPED_COMPARISON("مقارنة المبيعات والتكلفة والربح"),
    PROFIT_DONUT("نسبة مساهمة الأرباح (%)")
}

private val ChartPalette = listOf(
    Color(0xFF0F766E), // Deep Teal / Emerald
    Color(0xFFD97706), // Wheat Gold
    Color(0xFF2563EB), // Royal Blue
    Color(0xFF7C3AED), // Violet
    Color(0xFF059669), // Green
    Color(0xFFDC2626), // Crimson
    Color(0xFF0891B2)  // Cyan
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrainProfitChartsSection(
    allInvoices: List<InvoiceWithCalc>,
    periodFilteredInvoices: List<InvoiceWithCalc>,
    periodLabel: String,
    profitMode: ProfitCalculationMode,
    currency: String
) {
    var useCustomDateRange by remember { mutableStateOf(false) }
    var customStartMillis by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        mutableStateOf(cal.timeInMillis)
    }
    var customEndMillis by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var selectedChartType by remember { mutableStateOf(ProfitChartType.PROFIT_BARS) }
    var selectedBarIndex by remember { mutableIntStateOf(0) }

    var animTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(selectedChartType, useCustomDateRange, customStartMillis, customEndMillis, periodFilteredInvoices) {
        animTriggered = false
        animTriggered = true
    }
    val animProgress by animateFloatAsState(
        targetValue = if (animTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "chart_anim_progress"
    )

    val effectiveInvoices = remember(
        allInvoices,
        periodFilteredInvoices,
        useCustomDateRange,
        customStartMillis,
        customEndMillis
    ) {
        if (!useCustomDateRange) {
            periodFilteredInvoices
        } else {
            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = customEndMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
            allInvoices.filter {
                it.details.invoice.invoiceDate in customStartMillis..endOfDay
            }
        }
    }

    // Compute per-grain weighted average purchase cost from all approved purchases so even legacy sales have a fallback cost
    val fallbackAvgCostByGrain = remember(allInvoices) {
        val map = mutableMapOf<String, Pair<Double, Double>>() // weightKg to totalCost
        allInvoices
            .filter { !it.calc.isSaleInvoice && it.details.invoice.status == InvoiceStatus.APPROVED.name }
            .forEach { inv ->
                inv.calc.sectionCalculations.forEach { sec ->
                    val name = sec.section.productNameSnapshot.trim().ifBlank { "غير محدد" }
                    val prev = map[name] ?: (0.0 to 0.0)
                    map[name] = (prev.first + sec.totalWeightKg) to (prev.second + sec.sectionTotalAmount)
                }
            }
        map.mapValues { (_, pair) -> if (pair.first > 0.0) pair.second / pair.first else 0.0 }
    }

    // Aggregate profit per grain type within the selected time window
    val profitPoints = remember(effectiveInvoices, fallbackAvgCostByGrain, profitMode) {
        val saleInvs = effectiveInvoices.filter { it.calc.isSaleInvoice }
        val groupedByGrain = linkedMapOf<String, MutableList<Pair<InvoiceWithCalc, com.example.domain.SectionCalculation>>>()

        // Also include purchased grain names with 0 sales if there are no sales yet, or show all active grains
        saleInvs.forEach { inv ->
            inv.calc.sectionCalculations.forEach { secCalc ->
                val gName = secCalc.section.productNameSnapshot.trim().ifBlank { "غير محدد" }
                groupedByGrain.getOrPut(gName) { mutableListOf() }.add(inv to secCalc)
            }
        }

        val rawPoints = groupedByGrain.map { (grainName, list) ->
            val distinctSaleInvs = list.map { it.first.details.invoice.id }.distinct().size
            var soldKg = 0.0
            var totalRevenue = 0.0
            var totalCost = 0.0
            var totalExpenses = 0.0

            list.forEach { (invWithCalc, secCalc) ->
                val secWeight = secCalc.totalWeightKg
                val secRev = secCalc.sectionTotalAmount
                soldKg += secWeight
                totalRevenue += secRev

                val snapshotCostPerKg = invWithCalc.details.invoice.costPerKgSnapshot
                val effectiveCostPerKg = if (snapshotCostPerKg > 0.0) {
                    snapshotCostPerKg
                } else {
                    fallbackAvgCostByGrain[grainName] ?: 0.0
                }
                totalCost += BigDecimal.valueOf(secWeight)
                    .multiply(BigDecimal.valueOf(effectiveCostPerKg))
                    .setScale(0, RoundingMode.HALF_UP)
                    .toDouble()

                val weightRatio = if (invWithCalc.calc.totalWeightKg > 0.0) {
                    secWeight / invWithCalc.calc.totalWeightKg
                } else 1.0
                totalExpenses += invWithCalc.details.invoice.saleExpenses * weightRatio
            }

            val directProfit = BigDecimal.valueOf(totalRevenue - totalCost)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()
            val netProfit = BigDecimal.valueOf(totalRevenue - totalCost - totalExpenses)
                .setScale(0, RoundingMode.HALF_UP)
                .toDouble()

            val chosenProfit = if (profitMode == ProfitCalculationMode.WITH_EXPENSES) netProfit else directProfit
            val marginPct = if (totalCost > 0.0) (chosenProfit / totalCost) * 100.0 else 0.0

            GrainProfitChartPoint(
                productName = grainName,
                saleInvoicesCount = distinctSaleInvs,
                soldWeightKg = soldKg,
                totalRevenue = totalRevenue,
                totalCostOfGoods = totalCost,
                totalExpenses = totalExpenses,
                directProfit = directProfit,
                netProfitAfterExpenses = netProfit,
                profitMarginPercent = marginPct,
                profitShareRatio = 0f
            )
        }.sortedByDescending { it.getProfit(profitMode) }

        val totalPositiveProfit = rawPoints.sumOf { it.getProfit(profitMode).coerceAtLeast(0.0) }
        rawPoints.map { pt ->
            val share = if (totalPositiveProfit > 0.0 && pt.getProfit(profitMode) > 0.0) {
                (pt.getProfit(profitMode) / totalPositiveProfit).toFloat().coerceIn(0f, 1f)
            } else 0f
            pt.copy(profitShareRatio = share)
        }
    }

    val totalPeriodProfit = profitPoints.sumOf { it.getProfit(profitMode) }
    val totalPeriodRevenue = profitPoints.sumOf { it.totalRevenue }
    val totalPeriodCost = profitPoints.sumOf { it.totalCostOfGoods }
    val totalPeriodExpenses = profitPoints.sumOf { it.totalExpenses }
    val totalPeriodSoldKg = profitPoints.sumOf { it.soldWeightKg }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("ar-EG")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grain_profit_charts_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Custom Date Range & Time Window Selector Card
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary)
                        Column {
                            Text(
                                text = "الفترة الزمنية لتحليل أرباح الحبوب",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (useCustomDateRange) {
                                    "من ${dateFormatter.format(Date(customStartMillis))} إلى ${dateFormatter.format(Date(customEndMillis))}"
                                } else {
                                    "الفترة المحددة: $periodLabel • (${profitMode.labelAr})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    FilterChip(
                        selected = useCustomDateRange,
                        onClick = { useCustomDateRange = !useCustomDateRange },
                        label = {
                            Text(
                                text = if (useCustomDateRange) "فترة مخصصة ✓" else "تحديد تاريخ مخصص",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WheatGold,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("toggle_custom_date_range_chip")
                    )
                }

                if (useCustomDateRange) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "من: ${dateFormatter.format(Date(customStartMillis))}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "إلى: ${dateFormatter.format(Date(customEndMillis))}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Summary KPI Row for the Selected Period
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalPeriodProfit >= 0) StatusPaidBg else StatusUnpaidBg
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "إجمالي صافي الربح في الفترة",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldDark
                    )
                    Text(
                        text = "${if (totalPeriodProfit >= 0) "+" else ""}${CalculationEngine.formatCurrency(totalPeriodProfit, currency)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPeriodProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                    )
                    Text(
                        text = "إجمالي الوزن المباع: ${CalculationEngine.formatWeight(totalPeriodSoldKg)} كجم",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldDark
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "إجمالي المبيعات والتكلفة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CalculationEngine.formatCurrency(totalPeriodRevenue, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = buildString {
                            append("التكلفة: ${CalculationEngine.formatNumber(totalPeriodCost)}")
                            if (profitMode == ProfitCalculationMode.WITH_EXPENSES && totalPeriodExpenses > 0) {
                                append(" | مصاريف: ${CalculationEngine.formatNumber(totalPeriodExpenses)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Chart Type Switcher (Bar Chart | Grouped Revenue vs Cost vs Profit | Donut Share Chart)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ProfitChartType.entries) { chartType ->
                val isSelected = selectedChartType == chartType
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedChartType = chartType },
                    leadingIcon = {
                        val icon = when (chartType) {
                            ProfitChartType.PROFIT_BARS -> Icons.Default.BarChart
                            ProfitChartType.GROUPED_COMPARISON -> Icons.Default.StackedBarChart
                            ProfitChartType.PROFIT_DONUT -> Icons.Default.DonutLarge
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else EmeraldPrimary
                        )
                    },
                    label = {
                        Text(
                            text = chartType.labelAr,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("chart_type_${chartType.name}")
                )
            }
        }

        // 4. Main Interactive Chart Card
        if (profitPoints.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "لا توجد مبيعات مسجلة في هذه الفترة الزمنية لعرض الرسم البياني للأرباح",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "قم بإصدار فاتورة بيع وتوريد من تبويب «الشونة» ليظهر تحليل الأرباح بالرسوم البيانية فوراً لكل محصول.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val safeSelectedIdx = selectedBarIndex.coerceIn(0, profitPoints.lastIndex)
            val selectedPoint = profitPoints[safeSelectedIdx]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (selectedChartType) {
                                    ProfitChartType.PROFIT_BARS -> "الرسم البياني لصافي الربح حسب نوع الحبوب"
                                    ProfitChartType.GROUPED_COMPARISON -> "مقارنة المبيعات والتكلفة وصافي الربح لكل محصول"
                                    ProfitChartType.PROFIT_DONUT -> "التوزيع النسبي لمساهمة كل محصول في الأرباح"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اضغط على أي عمود أو محصول في الرسم البياني لعرض تفاصيله الدقيقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    when (selectedChartType) {
                        ProfitChartType.PROFIT_BARS -> {
                            InteractiveProfitBarChart(
                                points = profitPoints,
                                profitMode = profitMode,
                                currency = currency,
                                animProgress = animProgress,
                                selectedIndex = safeSelectedIdx,
                                onSelectIndex = { selectedBarIndex = it }
                            )
                        }

                        ProfitChartType.GROUPED_COMPARISON -> {
                            InteractiveGroupedComparisonChart(
                                points = profitPoints,
                                profitMode = profitMode,
                                animProgress = animProgress,
                                selectedIndex = safeSelectedIdx,
                                onSelectIndex = { selectedBarIndex = it }
                            )
                        }

                        ProfitChartType.PROFIT_DONUT -> {
                            InteractiveProfitDonutChart(
                                points = profitPoints,
                                profitMode = profitMode,
                                currency = currency,
                                animProgress = animProgress,
                                selectedIndex = safeSelectedIdx,
                                onSelectIndex = { selectedBarIndex = it }
                            )
                        }
                    }

                    // Interactive Selected Grain Tooltip / Inspector Box
                    val selectedProfitVal = selectedPoint.getProfit(profitMode)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldLight.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                                    text = "تفاصيل المحصول المحدد: ${selectedPoint.productName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = "صافي الربح: ${if (selectedProfitVal >= 0) "+" else ""}${CalculationEngine.formatCurrency(selectedProfitVal, currency)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedProfitVal >= 0) StatusPaidGreen else StatusUnpaidRed
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الوزن المباع: ${CalculationEngine.formatWeight(selectedPoint.soldWeightKg)} كجم (${selectedPoint.saleInvoicesCount} نقلة)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = "هامش الربح: ${CalculationEngine.formatNumber(selectedPoint.profitMarginPercent, 1)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المبيعات: ${CalculationEngine.formatCurrency(selectedPoint.totalRevenue, currency)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "تكلفة الشراء: ${CalculationEngine.formatCurrency(selectedPoint.totalCostOfGoods, currency)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (profitMode == ProfitCalculationMode.WITH_EXPENSES) {
                                    Text(
                                        text = "المصاريف: ${CalculationEngine.formatCurrency(selectedPoint.totalExpenses, currency)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Detailed Per-Grain Profit Cards List
            Text(
                text = "جدول تفاصيل الأرباح لكل نوع حبوب (${profitPoints.size} محصول)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            profitPoints.forEachIndexed { idx, pt ->
                val profitVal = pt.getProfit(profitMode)
                val badgeColor = ChartPalette[idx % ChartPalette.size]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (idx == safeSelectedIdx) 1.5.dp else 1.dp,
                            color = if (idx == safeSelectedIdx) EmeraldPrimary else WheatCardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedBarIndex = idx },
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
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Text(
                                    text = pt.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = WheatGoldLight
                                ) {
                                    Text(
                                        text = "${(pt.profitShareRatio * 100).toInt()}% من الأرباح",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldDark,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${if (profitVal >= 0) "+" else ""}${CalculationEngine.formatCurrency(profitVal, currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (profitVal >= 0) StatusPaidGreen else StatusUnpaidRed
                            )
                        }

                        // Profit contribution visual bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((pt.profitShareRatio * animProgress).coerceIn(0.03f, 1f))
                                    .fillMaxHeight()
                                    .background(badgeColor)
                            )
                        }

                        HorizontalDivider(color = WheatCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("الوزن المباع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${CalculationEngine.formatWeight(pt.soldWeightKg)} كجم (${CalculationEngine.formatNumber(pt.soldWeightKg / 1000.0, 2)} طن)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي المبيعات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CalculationEngine.formatCurrency(pt.totalRevenue, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("تكلفة الشراء", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CalculationEngine.formatCurrency(pt.totalCostOfGoods, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DatePicker Dialogs for Custom Date Range
    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = customStartMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selected ->
                            customStartMillis = selected
                            if (customEndMillis < selected) {
                                customEndMillis = selected
                            }
                        }
                        showStartDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تأكيد تاريخ البداية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = customEndMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selected ->
                            customEndMillis = maxOf(selected, customStartMillis)
                        }
                        showEndDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تأكيد تاريخ النهاية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun InteractiveProfitBarChart(
    points: List<GrainProfitChartPoint>,
    profitMode: ProfitCalculationMode,
    currency: String,
    animProgress: Float,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val maxAbsProfit = remember(points, profitMode) {
        points.maxOfOrNull { kotlin.math.abs(it.getProfit(profitMode)) }?.coerceAtLeast(1.0) ?: 1.0
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .pointerInput(points) {
                detectTapGestures { tapOffset ->
                    val chartLeft = 64.dp.toPx()
                    val chartRight = size.width - 12.dp.toPx()
                    val usableWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                    val slotWidth = usableWidth / points.size.coerceAtLeast(1)
                    val tappedIdx = ((tapOffset.x - chartLeft) / slotWidth).toInt()
                    if (tappedIdx in points.indices) {
                        onSelectIndex(tappedIdx)
                    }
                }
            }
            .testTag("interactive_profit_bar_canvas")
    ) {
        val leftPad = 64.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 26.dp.toPx()
        val bottomPad = 42.dp.toPx()

        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
        val baselineY = topPad + chartHeight

        val gridPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#6B7280")
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#111827")
            textSize = 11.sp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val valuePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F766E")
            textSize = 10.sp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Draw 4 Horizontal Grid Lines + Y-Axis Scale Labels
        val steps = 4
        for (i in 0..steps) {
            val ratio = i.toFloat() / steps
            val y = baselineY - (ratio * chartHeight)
            val scaleVal = maxAbsProfit * ratio

            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
            )
            drawContext.canvas.nativeCanvas.drawText(
                CalculationEngine.formatNumber(scaleVal, 0),
                leftPad - 8.dp.toPx(),
                y + 4.dp.toPx(),
                gridPaint
            )
        }

        val count = points.size.coerceAtLeast(1)
        val slotWidth = chartWidth / count
        val barWidth = (slotWidth * 0.56f).coerceAtMost(54.dp.toPx())

        points.forEachIndexed { idx, pt ->
            val profitVal = pt.getProfit(profitMode)
            val normalizedHeight = ((kotlin.math.abs(profitVal) / maxAbsProfit).toFloat() * chartHeight * animProgress)
                .coerceAtLeast(6.dp.toPx())

            val centerX = leftPad + (idx * slotWidth) + (slotWidth / 2f)
            val barLeft = centerX - (barWidth / 2f)
            val barTop = baselineY - normalizedHeight

            val isSelected = idx == selectedIndex
            val baseColor = if (profitVal >= 0) {
                ChartPalette[idx % ChartPalette.size]
            } else {
                StatusUnpaidRed
            }

            // Highlight background pill for selected bar
            if (isSelected) {
                drawRoundRect(
                    color = EmeraldLight.copy(alpha = 0.55f),
                    topLeft = Offset(leftPad + (idx * slotWidth) + 2.dp.toPx(), topPad - 18.dp.toPx()),
                    size = Size(slotWidth - 4.dp.toPx(), chartHeight + bottomPad + 12.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            // Draw Bar with Gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(baseColor, baseColor.copy(alpha = 0.78f)),
                    startY = barTop,
                    endY = baselineY
                ),
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Draw Value Label above Bar
            val formattedVal = CalculationEngine.formatNumber(profitVal, 0)
            valuePaint.color = if (profitVal >= 0) {
                android.graphics.Color.parseColor("#0F766E")
            } else {
                android.graphics.Color.parseColor("#DC2626")
            }
            drawContext.canvas.nativeCanvas.drawText(
                formattedVal,
                centerX,
                (barTop - 6.dp.toPx()).coerceAtLeast(14.dp.toPx()),
                valuePaint
            )

            // Draw Grain Name on X-Axis
            val shortName = if (pt.productName.length > 10) pt.productName.take(9) + "…" else pt.productName
            drawContext.canvas.nativeCanvas.drawText(
                shortName,
                centerX,
                baselineY + 22.dp.toPx(),
                labelPaint
            )
        }
    }
}

@Composable
private fun InteractiveGroupedComparisonChart(
    points: List<GrainProfitChartPoint>,
    profitMode: ProfitCalculationMode,
    animProgress: Float,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val maxVal = remember(points, profitMode) {
        points.maxOfOrNull {
            val totalOut = it.totalCostOfGoods + if (profitMode == ProfitCalculationMode.WITH_EXPENSES) it.totalExpenses else 0.0
            maxOf(it.totalRevenue, totalOut, kotlin.math.abs(it.getProfit(profitMode)))
        }?.coerceAtLeast(1.0) ?: 1.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = WheatGold, label = "إجمالي المبيعات")
            Spacer(modifier = Modifier.width(14.dp))
            LegendDot(color = Color(0xFF64748B), label = "التكلفة والمصاريف")
            Spacer(modifier = Modifier.width(14.dp))
            LegendDot(color = StatusPaidGreen, label = "صافي الربح")
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        val leftPad = 60.dp.toPx()
                        val rightPad = 12.dp.toPx()
                        val slotW = (size.width - leftPad - rightPad).coerceAtLeast(1f) / points.size.coerceAtLeast(1)
                        val idx = ((tapOffset.x - leftPad) / slotW).toInt()
                        if (idx in points.indices) {
                            onSelectIndex(idx)
                        }
                    }
                }
        ) {
            val leftPad = 60.dp.toPx()
            val rightPad = 12.dp.toPx()
            val topPad = 18.dp.toPx()
            val bottomPad = 38.dp.toPx()

            val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val baselineY = topPad + chartHeight

            val gridPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#6B7280")
                textSize = 9.5.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#111827")
                textSize = 10.5.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            for (i in 0..4) {
                val ratio = i / 4f
                val y = baselineY - (ratio * chartHeight)
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    CalculationEngine.formatNumber(maxVal * ratio, 0),
                    leftPad - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    gridPaint
                )
            }

            val slotW = chartWidth / points.size.coerceAtLeast(1)
            val groupW = (slotW * 0.75f).coerceAtMost(66.dp.toPx())
            val singleBarW = groupW / 3.2f

            points.forEachIndexed { idx, pt ->
                val centerX = leftPad + (idx * slotW) + (slotW / 2f)
                val groupStartX = centerX - (groupW / 2f)

                if (idx == selectedIndex) {
                    drawRoundRect(
                        color = EmeraldLight.copy(alpha = 0.5f),
                        topLeft = Offset(leftPad + (idx * slotW) + 2.dp.toPx(), topPad - 8.dp.toPx()),
                        size = Size(slotW - 4.dp.toPx(), chartHeight + bottomPad + 4.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }

                val totalCostAndExp = pt.totalCostOfGoods +
                    if (profitMode == ProfitCalculationMode.WITH_EXPENSES) pt.totalExpenses else 0.0
                val profitVal = pt.getProfit(profitMode).coerceAtLeast(0.0)

                val triples = listOf(
                    pt.totalRevenue to WheatGold,
                    totalCostAndExp to Color(0xFF64748B),
                    profitVal to StatusPaidGreen
                )

                triples.forEachIndexed { bIdx, (v, col) ->
                    val h = ((v / maxVal).toFloat() * chartHeight * animProgress).coerceAtLeast(4.dp.toPx())
                    val bLeft = groupStartX + (bIdx * (singleBarW + 2.dp.toPx()))
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(bLeft, baselineY - h),
                        size = Size(singleBarW, h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                val shortName = if (pt.productName.length > 10) pt.productName.take(9) + "…" else pt.productName
                drawContext.canvas.nativeCanvas.drawText(
                    shortName,
                    centerX,
                    baselineY + 20.dp.toPx(),
                    labelPaint
                )
            }
        }
    }
}

@Composable
private fun InteractiveProfitDonutChart(
    points: List<GrainProfitChartPoint>,
    profitMode: ProfitCalculationMode,
    currency: String,
    animProgress: Float,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val totalPositiveProfit = points.sumOf { it.getProfit(profitMode).coerceAtLeast(0.0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(175.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 28.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                var startAngle = -90f
                points.forEachIndexed { idx, pt ->
                    val sweep = (pt.profitShareRatio * 360f * animProgress)
                    if (sweep > 0.5f) {
                        val isSelected = idx == selectedIndex
                        drawArc(
                            color = ChartPalette[idx % ChartPalette.size],
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = if (isSelected) strokeWidth * 1.18f else strokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "إجمالي الربح",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CalculationEngine.formatCurrency(totalPositiveProfit, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            points.forEachIndexed { idx, pt ->
                val color = ChartPalette[idx % ChartPalette.size]
                val isSelected = idx == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) EmeraldLight else Color.Transparent)
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = pt.productName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${(pt.profitShareRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
