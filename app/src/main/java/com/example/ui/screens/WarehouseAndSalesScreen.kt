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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InventoryAdjustmentEntity
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ProfitCalculationMode
import com.example.data.local.ShrinkageAdjustmentType
import com.example.domain.CalculationEngine
import com.example.domain.ProductWarehouseStock
import com.example.domain.WarehouseSummary
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusPartialBg
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.theme.WheatGoldLight
import com.example.ui.viewmodel.InvoiceWithCalc
import com.example.util.PdfInvoiceExporter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("LongParameterList")
@Composable
fun WarehouseAndSalesScreen(
    warehouseSummary: WarehouseSummary,
    allInvoices: List<InvoiceWithCalc>,
    adjustments: List<InventoryAdjustmentEntity>,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    settings: AppSettingsEntity,
    onCreateBulkSale: (
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
    ) -> Unit,
    onRecordShrinkage: (
        productId: Long?,
        productName: String,
        weightLossKg: Double,
        referencePurchasedKg: Double,
        reason: String
    ) -> Unit,
    onZeroOutBatch: (
        productId: Long?,
        productName: String,
        remainingBookStockKg: Double,
        referencePurchasedKg: Double,
        notes: String
    ) -> Unit,
    onDeleteAdjustment: (InventoryAdjustmentEntity) -> Unit,
    onRecordPayment: (Long, Double, String, String) -> Unit,
    onDeleteInvoice: (Long) -> Unit,
    onToggleProfitMode: (ProfitCalculationMode) -> Unit
) {
    val context = LocalContext.current
    val currency = settings.currencySymbol
    val profitMode = remember(settings.profitCalculationMode) {
        runCatching { ProfitCalculationMode.valueOf(settings.profitCalculationMode) }
            .getOrDefault(ProfitCalculationMode.WITH_EXPENSES)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "أرصدة الشونة" to Icons.Default.Inventory2,
        "البيع والأرباح" to Icons.Default.LocalShipping,
        "الجفاف والعجز" to Icons.Default.Air
    )

    // سلكتور المحصول النشط في صفحة الشونة (تتحدث الصفحة وجميع بياناتها عن نفس نوع الحبوب المختار)
    val availableStocks = warehouseSummary.productStocks
    var selectedProductName by remember(availableStocks) {
        mutableStateOf(
            availableStocks.firstOrNull { it.availableStockKg > 0 }?.productName
                ?: availableStocks.firstOrNull()?.productName
                ?: products.firstOrNull()?.name
                ?: "قمح"
        )
    }

    val selectedStock: ProductWarehouseStock? = remember(availableStocks, selectedProductName) {
        availableStocks.find { it.productName == selectedProductName }
            ?: availableStocks.firstOrNull()
    }

    var showBulkSaleModal by remember { mutableStateOf(false) }
    var preselectedSaleProduct by remember { mutableStateOf<ProductWarehouseStock?>(null) }

    var shrinkageTargetStock by remember { mutableStateOf<ProductWarehouseStock?>(null) }
    var zeroOutTargetStock by remember { mutableStateOf<ProductWarehouseStock?>(null) }
    var paymentTargetSaleInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }
    var deleteTargetSaleInvoice by remember { mutableStateOf<InvoiceWithCalc?>(null) }

    // تصفية فواتير البيع وسجلات الجفاف لتتحدث عن نوع الحبوب المختار فقط
    val filteredSaleInvoices = remember(allInvoices, selectedStock) {
        val targetName = selectedStock?.productName
        allInvoices.filter { item ->
            item.calc.isSaleInvoice && (
                targetName == null ||
                    item.calc.productSummaries.any { it.productName == targetName } ||
                    item.details.sections.any { it.section.productNameSnapshot == targetName }
                )
        }
    }

    val filteredAdjustments = remember(adjustments, selectedStock) {
        val targetName = selectedStock?.productName
        if (targetName == null) adjustments
        else adjustments.filter { it.productNameSnapshot == targetName }
    }

    // بيانات المحصول المختار لعرضها في الكروت العلوية بدون تشتت
    val displayAvailableKg = selectedStock?.availableStockKg ?: warehouseSummary.totalAvailableWeightKg
    val displayValuation = selectedStock?.currentStockValuation ?: warehouseSummary.totalStockValuation
    val displayProfit = selectedStock?.getProfitByMode(profitMode) ?: warehouseSummary.getTotalProfitByMode(profitMode)
    val displaySalesRevenue = selectedStock?.soldTotalRevenue ?: warehouseSummary.totalSalesRevenue
    val displayShrinkageKg = selectedStock?.totalShrinkageKg ?: warehouseSummary.totalShrinkageWeightKg
    val displayShrinkagePct = selectedStock?.shrinkagePercentage ?: warehouseSummary.overallShrinkagePercent
    val activeGrainLabel = selectedStock?.productName ?: selectedProductName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("warehouse_sales_screen")
    ) {
        // Sub-Tab Navigation Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        },
                        modifier = Modifier.testTag("warehouse_tab_$index")
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 0. Grain Type Selector Bar (سلكتور تحديد نوع الحبوب لتركيز صفحة الشونة بالكامل عليه)
            item {
                WarehouseGrainSelectorCard(
                    stocks = availableStocks,
                    selectedStock = selectedStock,
                    currency = currency,
                    onSelectStock = { stock ->
                        selectedProductName = stock.productName
                    }
                )
            }

            // 1. Hero Warehouse & Profit Banner (Focused on Selected Grain)
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
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "شونة ومبيعات وأرباح ($activeGrainLabel)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "نظام الربح النشط: ${profitMode.labelAr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WheatGoldLight
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val nextMode = if (profitMode == ProfitCalculationMode.WITH_EXPENSES) {
                                                ProfitCalculationMode.DIRECT_MARGIN
                                            } else {
                                                ProfitCalculationMode.WITH_EXPENSES
                                            }
                                            onToggleProfitMode(nextMode)
                                        }
                                        .testTag("toggle_profit_mode_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "تبديل نظام الربح",
                                            tint = WheatGoldLight,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "تبديل النظام",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    preselectedSaleProduct = selectedStock
                                        ?: warehouseSummary.productStocks.firstOrNull()
                                    showBulkSaleModal = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("new_bulk_sale_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WheatGold,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "فاتورة بيع وتوريد $activeGrainLabel (وزن مقطوع)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. 4 Key Warehouse & Profit Metrics Grid (Focused on Selected Grain)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WarehouseMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "رصيد $activeGrainLabel بالشونة",
                            value = "${CalculationEngine.formatWeight(displayAvailableKg)} كجم",
                            subtitle = if (selectedStock != null) {
                                "≈ ${CalculationEngine.formatNumber(selectedStock.availableStandardUnits, 2)} ${selectedStock.standardUnitName}"
                            } else {
                                "≈ ${CalculationEngine.formatNumber(displayAvailableKg / 1000.0, 3)} طن متاح"
                            },
                            icon = Icons.Default.Inventory2,
                            accentColor = EmeraldPrimary
                        )
                        WarehouseMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "قيمة مخزون $activeGrainLabel",
                            value = CalculationEngine.formatCurrency(displayValuation, currency),
                            subtitle = "بمتوسط تكلفة الشراء",
                            icon = Icons.Default.Scale,
                            accentColor = WheatGold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WarehouseMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "صافي أرباح $activeGrainLabel",
                            value = CalculationEngine.formatCurrency(displayProfit, currency),
                            subtitle = "مبيعات: ${CalculationEngine.formatCurrency(displaySalesRevenue, currency)}",
                            icon = Icons.Default.TrendingUp,
                            accentColor = if (displayProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                        )
                        WarehouseMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "جفاف وعجز $activeGrainLabel",
                            value = "${CalculationEngine.formatWeight(displayShrinkageKg)} كجم",
                            subtitle = "نسبة الفاقد: ${CalculationEngine.formatNumber(displayShrinkagePct, 2)}%",
                            icon = Icons.Default.Air,
                            accentColor = StatusPartialAmber
                        )
                    }
                }
            }

            // 3. Content per active tab (Filtered by Selected Grain)
            when (selectedTab) {
                0 -> {
                    item {
                        Text(
                            text = "تفاصيل رصيد وتكلفة الشراء لمحصول ($activeGrainLabel)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedStock == null) {
                        item {
                            EmptyWarehouseBox("لا توجد محاصيل مسجلة بعد. ابدأ بإضافة مشتريات لتظهر أرصدة الشونة هنا.")
                        }
                    } else {
                        item(key = selectedStock.productName) {
                            ProductWarehouseCard(
                                stock = selectedStock,
                                currency = currency,
                                profitMode = profitMode,
                                onSellClick = {
                                    preselectedSaleProduct = selectedStock
                                    showBulkSaleModal = true
                                },
                                onManualShrinkageClick = {
                                    shrinkageTargetStock = selectedStock
                                },
                                onZeroOutBatchClick = {
                                    zeroOutTargetStock = selectedStock
                                }
                            )
                        }
                    }
                }

                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "فواتير بيع وتوريد $activeGrainLabel (${filteredSaleInvoices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    preselectedSaleProduct = selectedStock
                                    showBulkSaleModal = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("بيع $activeGrainLabel")
                            }
                        }
                    }

                    if (filteredSaleInvoices.isEmpty()) {
                        item {
                            EmptyWarehouseBox("لا توجد فواتير بيع وتوريد مسجلة لمحصول «$activeGrainLabel» حتى الآن.")
                        }
                    } else {
                        items(filteredSaleInvoices, key = { it.details.invoice.id }) { saleItem ->
                            SaleInvoiceCard(
                                item = saleItem,
                                currency = currency,
                                profitMode = profitMode,
                                onRecordPayment = { paymentTargetSaleInvoice = saleItem },
                                onShareWhatsApp = {
                                    PdfInvoiceExporter.shareInvoicePdf(
                                        context = context,
                                        details = saleItem.details,
                                        settings = settings,
                                        whatsAppOnly = true
                                    )
                                },
                                onSavePdf = {
                                    PdfInvoiceExporter.saveInvoicePdfToPhone(
                                        context = context,
                                        details = saleItem.details,
                                        settings = settings
                                    )
                                },
                                onPrintPdf = {
                                    PdfInvoiceExporter.printInvoicePdf(
                                        context = context,
                                        details = saleItem.details,
                                        settings = settings
                                    )
                                },
                                onDelete = { deleteTargetSaleInvoice = saleItem }
                            )
                        }
                    }
                }

                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سجل الجفاف والعجز لـ $activeGrainLabel (${filteredAdjustments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedStock != null) {
                                OutlinedButton(
                                    onClick = { shrinkageTargetStock = selectedStock }
                                ) {
                                    Icon(Icons.Default.Air, contentDescription = null, tint = StatusPartialAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل جفاف")
                                }
                            }
                        }
                    }

                    if (filteredAdjustments.isEmpty()) {
                        item {
                            EmptyWarehouseBox("لا توجد تسويات جفاف أو عجز مسجلة لمحصول «$activeGrainLabel» حتى الآن.")
                        }
                    } else {
                        items(filteredAdjustments, key = { it.id }) { adj ->
                            ShrinkageAdjustmentRowCard(
                                adjustment = adj,
                                onDelete = { onDeleteAdjustment(adj) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialog 1: Bulk Sale Invoice Modal (وزن كلي مقطوع + معاينة الربح الفورية) ---
    if (showBulkSaleModal) {
        BulkSaleInvoiceModal(
            warehouseSummary = warehouseSummary,
            preselectedStock = preselectedSaleProduct,
            customers = customers,
            products = products,
            pricingUnits = pricingUnits,
            currencySymbol = currency,
            profitMode = profitMode,
            onDismiss = { showBulkSaleModal = false },
            onConfirmSale = { cust, customName, prod, unit, bulkKg, unitPrice, costPerKg, expenses, paidAmt, payMethod, notes ->
                onCreateBulkSale(
                    cust,
                    customName,
                    prod,
                    unit,
                    bulkKg,
                    unitPrice,
                    costPerKg,
                    expenses,
                    paidAmt,
                    payMethod,
                    notes
                )
                showBulkSaleModal = false
            }
        )
    }

    // --- Dialog 2: Manual Shrinkage / Dryness Adjustment Modal ---
    shrinkageTargetStock?.let { stock ->
        ManualShrinkageDialog(
            stock = stock,
            onDismiss = { shrinkageTargetStock = null },
            onConfirm = { lossKg, reason ->
                onRecordShrinkage(
                    stock.productId.takeIf { it > 0 },
                    stock.productName,
                    lossKg,
                    stock.purchasedWeightKg,
                    reason
                )
                shrinkageTargetStock = null
            }
        )
    }

    // --- Dialog 3: Batch Zero-Out Confirmation Modal (تصفير الدفعة عند نفاد المخزن) ---
    zeroOutTargetStock?.let { stock ->
        BatchZeroOutDialog(
            stock = stock,
            onDismiss = { zeroOutTargetStock = null },
            onConfirm = { notes ->
                onZeroOutBatch(
                    stock.productId.takeIf { it > 0 },
                    stock.productName,
                    stock.availableStockKg,
                    stock.purchasedWeightKg,
                    notes
                )
                zeroOutTargetStock = null
            }
        )
    }

    // --- Dialog 4: Record Payment for Sale Invoice ---
    paymentTargetSaleInvoice?.let { target ->
        RecordPaymentDialog(
            invoiceNumber = target.details.invoice.invoiceNumber,
            totalInvoiceAmount = target.calc.totalInvoiceAmount,
            netPaidAmount = target.calc.netPaidAmount,
            remainingAmount = target.calc.remainingAmount,
            currencySymbol = currency,
            onDismiss = { paymentTargetSaleInvoice = null },
            onRecordPayment = { amt, method, notes ->
                onRecordPayment(target.details.invoice.id, amt, method, notes)
            }
        )
    }

    // --- Dialog 5: Delete Sale Invoice Confirmation ---
    deleteTargetSaleInvoice?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTargetSaleInvoice = null },
            title = {
                Text(
                    text = "تأكيد حذف فاتورة البيع #${target.details.invoice.invoiceNumber}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("هل أنت متأكد من حذف فاتورة البيع لـ «${target.details.invoice.customerNameSnapshot}»؟ سيتم إعادة الكمية المباعة (${CalculationEngine.formatWeight(target.calc.totalWeightKg)} كجم) تلقائياً إلى رصيد الشونة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInvoice(target.details.invoice.id)
                        deleteTargetSaleInvoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed)
                ) {
                    Text("نعم، احذف الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetSaleInvoice = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * سلكتور اختيار نوع الحبوب في صفحة الشونة بحيث تعرض الصفحة بالكامل بيانات محصول واحد بدون تشتت
 */
@Composable
private fun WarehouseGrainSelectorCard(
    stocks: List<ProductWarehouseStock>,
    selectedStock: ProductWarehouseStock?,
    currency: String,
    onSelectStock: (ProductWarehouseStock) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentName = selectedStock?.productName ?: "اختر نوع الحبوب"
    val currentAvailKg = selectedStock?.availableStockKg ?: 0.0
    val hasStock = currentAvailKg > 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (stocks.isNotEmpty()) expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("warehouse_grain_selector"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldLight,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Grass,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "نوع الحبوب المعروض في الشونة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (hasStock) EmeraldLight else StatusUnpaidBg
                            ) {
                                Text(
                                    text = "${CalculationEngine.formatWeight(currentAvailKg)} كجم متاح",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasStock) EmeraldPrimary else StatusUnpaidRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "تغيير المحصول",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "اختيار نوع الحبوب",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                stocks.forEach { stock ->
                    val isSelected = stock.productName == selectedStock?.productName
                    val itemHasStock = stock.availableStockKg > 0.0
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stock.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "متوسط التكلفة: ${CalculationEngine.formatCurrency(stock.avgPurchaseCostPerStandardUnit, currency)} / ${stock.standardUnitName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (itemHasStock) EmeraldLight else StatusUnpaidBg
                                ) {
                                    Text(
                                        text = "${CalculationEngine.formatWeight(stock.availableStockKg)} كجم",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (itemHasStock) EmeraldPrimary else StatusUnpaidRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Grass,
                                contentDescription = null,
                                tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                            }
                        },
                        onClick = {
                            onSelectStock(stock)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseMetricCard(
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
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
private fun ProductWarehouseCard(
    stock: ProductWarehouseStock,
    currency: String,
    profitMode: ProfitCalculationMode,
    onSellClick: () -> Unit,
    onManualShrinkageClick: () -> Unit,
    onZeroOutBatchClick: () -> Unit
) {
    val hasStock = stock.availableStockKg > 0.0
    val realizedProfit = stock.getProfitByMode(profitMode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (hasStock) 1.5.dp else 1.dp,
                color = if (hasStock) EmeraldPrimary.copy(alpha = 0.45f) else WheatCardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("warehouse_product_card_${stock.productName}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Product Name + Available Stock Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasStock) EmeraldLight else MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Grass,
                                contentDescription = null,
                                tint = if (hasStock) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stock.productName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "قيمة المخزون الحالي: ${CalculationEngine.formatCurrency(stock.currentStockValuation, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasStock) EmeraldLight else StatusUnpaidBg
                    ) {
                        Text(
                            text = "${CalculationEngine.formatWeight(stock.availableStockKg)} كجم متاح",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasStock) EmeraldPrimary else StatusUnpaidRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${CalculationEngine.formatNumber(stock.availableStockTons, 3)} طن • ${CalculationEngine.formatNumber(stock.availableStandardUnits, 2)} ${stock.standardUnitName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Weighted Average Purchase Cost Strip (Spacious 3-Column Grid without Overlap)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WheatGoldLight.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.25f)) {
                        Text(
                            text = "متوسط الشراء (${stock.standardUnitName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(stock.avgPurchaseCostPerStandardUnit, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "متوسط تكلفة الطن",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(stock.avgPurchaseCostPerTon, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WheatGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "تكلفة الكيلو",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${CalculationEngine.formatNumber(stock.avgPurchaseCostPerKg, 2)} $currency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Movement Summary 2x2 Grid (إجمالي المشترى | إجمالي المباع | الجفاف والعجز | صافي الربح)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إجمالي المشترى",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${CalculationEngine.formatWeight(stock.purchasedWeightKg)} كجم",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إجمالي المباع",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${CalculationEngine.formatWeight(stock.soldWeightKg)} كجم",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
                HorizontalDivider(color = WheatCardBorder.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الجفاف والعجز",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${CalculationEngine.formatWeight(stock.totalShrinkageKg)} كجم (${CalculationEngine.formatNumber(stock.shrinkagePercentage, 1)}%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusPartialAmber
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صافي الربح المحقق",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(realizedProfit, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (realizedProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                        )
                    }
                }
            }

            HorizontalDivider(color = WheatCardBorder)

            // Action Buttons: 1. Sell Crop | 2. Manual Dryness/Shrinkage | 3. Zero-Out Batch on Stock Depletion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(42.dp)
                        .testTag("sell_product_btn_${stock.productName}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بيع وتوريد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onManualShrinkageClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("shrinkage_btn_${stock.productName}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Air, contentDescription = null, tint = StatusPartialAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل جفاف", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onZeroOutBatchClick,
                    enabled = hasStock,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WheatGold
                    ),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(42.dp)
                        .testTag("zero_out_btn_${stock.productName}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصفير الدفعة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SaleInvoiceCard(
    item: InvoiceWithCalc,
    currency: String,
    profitMode: ProfitCalculationMode,
    onRecordPayment: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onSavePdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDelete: () -> Unit
) {
    val inv = item.details.invoice
    val calc = item.calc
    val sec = calc.sectionCalculations.firstOrNull()?.section
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar-EG")) }
    val effectiveProfit = calc.getEffectiveProfit(profitMode)
    val profitPct = if (calc.estimatedCostOfGoods > 0.0) {
        (effectiveProfit / calc.estimatedCostOfGoods) * 100.0
    } else 0.0

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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = WheatGoldLight) {
                        Text(
                            text = "فاتورة بيع #${inv.invoiceNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    PaymentStatusBadge(paymentStatus = calc.paymentStatus)
                }
                Text(
                    text = dateFormat.format(Date(inv.invoiceDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        text = if (sec != null) {
                            "${sec.productNameSnapshot} • وزن مقطوع: ${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم (${CalculationEngine.formatNumber(calc.totalWeightKg / 1000.0, 3)} طن)"
                        } else {
                            "وزن مقطوع: ${CalculationEngine.formatWeight(calc.totalWeightKg)} كجم"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (sec != null) {
                        Text(
                            text = "سعر البيع: ${CalculationEngine.formatCurrency(sec.unitPrice, currency)} / ${sec.unitNameSnapshot}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CalculationEngine.formatCurrency(calc.totalInvoiceAmount, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "المحصّل: ${CalculationEngine.formatNumber(calc.netPaidAmount)} | المتبقي: ${CalculationEngine.formatNumber(calc.remainingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (calc.remainingAmount > 0) StatusUnpaidRed else StatusPaidGreen
                    )
                }
            }

            // Realized Profit Analysis Strip for this Sale Shipment
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (effectiveProfit >= 0) StatusPaidBg else StatusUnpaidBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تكلفة الشراء التقديرية: ${CalculationEngine.formatCurrency(calc.estimatedCostOfGoods, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (profitMode == ProfitCalculationMode.WITH_EXPENSES && calc.saleExpenses > 0) {
                            Text(
                                text = "مصاريف النقلة والمشال: ${CalculationEngine.formatCurrency(calc.saleExpenses, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (effectiveProfit >= 0) {
                                "صافي المكسب: +${CalculationEngine.formatCurrency(effectiveProfit, currency)}"
                            } else {
                                "خسارة: ${CalculationEngine.formatCurrency(effectiveProfit, currency)}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (effectiveProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                        )
                        if (calc.estimatedCostOfGoods > 0) {
                            Text(
                                text = "هامش الربح: ${CalculationEngine.formatNumber(profitPct, 1)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (effectiveProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = WheatCardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("تحصيل", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onShareWhatsApp,
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("واتساب", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onSavePdf,
                    modifier = Modifier.weight(0.9f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = WheatGold, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("حفظ", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onPrintPdf,
                    modifier = Modifier.weight(0.95f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = WheatGold, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("طباعة", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = StatusUnpaidBg.copy(alpha = 0.35f),
                        contentColor = StatusUnpaidRed
                    ),
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف فاتورة البيع", modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun ShrinkageAdjustmentRowCard(
    adjustment: InventoryAdjustmentEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar-EG")) }
    val isBatchZeroOut = adjustment.adjustmentType == ShrinkageAdjustmentType.BATCH_ZERO_OUT.name
    val pct = if (adjustment.referencePurchasedKg > 0.0) {
        (adjustment.weightLossKg / adjustment.referencePurchasedKg) * 100.0
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBatchZeroOut) WheatGoldLight else StatusPartialBg
                    ) {
                        Text(
                            text = if (isBatchZeroOut) "تصفير دفعة (نفاد المخزن)" else "تسوية جفاف / غربلة يدوية",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isBatchZeroOut) EmeraldDark else StatusPartialAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = adjustment.productNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = adjustment.reason.ifBlank { "فروق جفاف طبيعي في الشونة" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(adjustment.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${CalculationEngine.formatWeight(adjustment.weightLossKg)} كجم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusUnpaidRed
                    )
                    if (pct > 0.0) {
                        Text(
                            text = "نسبة الفاقد: ${CalculationEngine.formatNumber(pct, 2)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusPartialAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "إلغاء التسوية",
                        tint = StatusUnpaidRed
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun BulkSaleInvoiceModal(
    warehouseSummary: WarehouseSummary,
    preselectedStock: ProductWarehouseStock?,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    currencySymbol: String,
    profitMode: ProfitCalculationMode,
    onDismiss: () -> Unit,
    onConfirmSale: (
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
    ) -> Unit
) {
    val activeProducts = remember(products) {
        products.filter { it.isActive }.ifEmpty { products }
    }
    var selectedProduct by remember {
        mutableStateOf(
            activeProducts.find { it.name == preselectedStock?.productName }
                ?: activeProducts.firstOrNull()
                ?: ProductEntity(id = 1, name = "قمح")
        )
    }

    val matchingStock = remember(warehouseSummary, selectedProduct) {
        warehouseSummary.productStocks.find { it.productName.trim() == selectedProduct.name.trim() }
    }

    val activeUnits = remember(pricingUnits) {
        pricingUnits.filter { it.isActive }.ifEmpty { pricingUnits }
    }
    var selectedUnit by remember(selectedProduct) {
        mutableStateOf(
            activeUnits.firstOrNull { it.linkedProductId == selectedProduct.id }
                ?: activeUnits.firstOrNull { it.weightKg == 1000.0 }
                ?: activeUnits.firstOrNull()
                ?: PricingUnitEntity(id = 1, name = "طن", weightKg = 1000.0)
        )
    }

    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customBuyerName by remember { mutableStateOf("") }

    var bulkWeightKgText by remember(matchingStock) {
        val defaultKg = matchingStock?.availableStockKg?.takeIf { it > 0.0 }
        mutableStateOf(defaultKg?.let { CalculationEngine.formatPlainInt(it) } ?: "")
    }

    // Default suggested selling price slightly above average purchase cost if available
    var sellingUnitPriceText by remember(selectedProduct, selectedUnit, matchingStock) {
        val avgCostForSelectedUnit = (matchingStock?.avgPurchaseCostPerKg ?: 0.0) * selectedUnit.weightKg
        val suggested = if (avgCostForSelectedUnit > 0.0) {
            CalculationEngine.formatPlainInt(avgCostForSelectedUnit)
        } else {
            ""
        }
        mutableStateOf(suggested)
    }

    var saleExpensesText by remember { mutableStateOf("") }
    var initialPaidText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقدي") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val bulkWeightKg = bulkWeightKgText.toDoubleOrNull() ?: 0.0
    val sellingUnitPrice = sellingUnitPriceText.toDoubleOrNull() ?: 0.0
    val unitWeight = selectedUnit.weightKg.coerceAtLeast(1.0)
    val avgCostPerKg = matchingStock?.avgPurchaseCostPerKg ?: 0.0

    val totalSaleAmount = remember(bulkWeightKg, sellingUnitPrice, unitWeight) {
        if (bulkWeightKg > 0.0 && sellingUnitPrice > 0.0) {
            BigDecimal.valueOf(bulkWeightKg)
                .multiply(BigDecimal.valueOf(sellingUnitPrice))
                .divide(BigDecimal.valueOf(unitWeight), 0, RoundingMode.HALF_UP)
                .toDouble()
        } else 0.0
    }

    val estimatedPurchaseCost = remember(bulkWeightKg, avgCostPerKg) {
        BigDecimal.valueOf(bulkWeightKg)
            .multiply(BigDecimal.valueOf(avgCostPerKg))
            .setScale(0, RoundingMode.HALF_UP)
            .toDouble()
    }

    val saleExpenses = if (profitMode == ProfitCalculationMode.WITH_EXPENSES) {
        saleExpensesText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    } else 0.0

    val previewProfit = totalSaleAmount - estimatedPurchaseCost - saleExpenses
    val previewProfitPct = if (estimatedPurchaseCost > 0.0) {
        (previewProfit / estimatedPurchaseCost) * 100.0
    } else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.5.dp, WheatGold, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إصدار فاتورة بيع وتوريد (وزن مقطوع)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "إدخال الوزن الكلي المقطوع مباشرة مع حساب صافي الربح الفوري",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق")
                    }
                }

                HorizontalDivider(color = WheatCardBorder)

                // 1. Select Grain Product via Dropdown Selector
                GrainProductDropdownSelector(
                    products = activeProducts,
                    selectedProduct = selectedProduct,
                    label = "1. المحصول المباع من الشونة",
                    subtitleFormatter = { prod ->
                        val st = warehouseSummary.productStocks.find { it.productId == prod.id || it.productName == prod.name }
                        val kg = st?.availableStockKg ?: 0.0
                        "الرصيد المتاح: ${CalculationEngine.formatWeight(kg)} كجم"
                    },
                    onProductSelected = { prod ->
                        selectedProduct = prod
                        val linked = activeUnits.firstOrNull { it.linkedProductId == prod.id }
                        if (linked != null) selectedUnit = linked
                    }
                )

                // Live Stock & Purchase Cost Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val availKg = matchingStock?.availableStockKg ?: 0.0
                        val avgUnitCost = avgCostPerKg * selectedUnit.weightKg
                        Text(
                            text = "الرصيد المتاح في الشونة (${selectedProduct.name}): ${CalculationEngine.formatWeight(availKg)} كجم (${CalculationEngine.formatNumber(availKg / 1000.0, 3)} طن)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Text(
                            text = "متوسط تكلفة الشراء عليك: ${CalculationEngine.formatCurrency(avgUnitCost, currencySymbol)} لكل ${selectedUnit.name} (${CalculationEngine.formatNumber(avgCostPerKg, 2)} $currencySymbol/كجم)",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Select Pricing Unit for Sale via Dropdown Selector
                PricingUnitDropdownSelector(
                    units = activeUnits,
                    selectedUnit = selectedUnit,
                    label = "2. وحدة التسعير في البيع (${selectedProduct.name})",
                    onUnitSelected = { unit ->
                        selectedUnit = unit
                    }
                )

                // 3. Bulk Total Weight (Kg) & Selling Unit Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = bulkWeightKgText,
                        onValueChange = { bulkWeightKgText = it; errorMsg = null },
                        label = { Text("الوزن الكلي المقطوع (كجم)") },
                        placeholder = { Text("مثال: 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_sale_weight_input")
                    )

                    OutlinedTextField(
                        value = sellingUnitPriceText,
                        onValueChange = { sellingUnitPriceText = it; errorMsg = null },
                        label = { Text("سعر بيع الـ ${selectedUnit.name}") },
                        placeholder = { Text("سعر البيع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_sale_price_input")
                    )
                }

                if (bulkWeightKg > 0.0) {
                    val eqUnits = bulkWeightKg / unitWeight
                    val availKg = matchingStock?.availableStockKg ?: 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "يعادل: ${CalculationEngine.formatNumber(bulkWeightKg / 1000.0, 3)} طن • ${CalculationEngine.formatNumber(eqUnits, 2)} ${selectedUnit.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (bulkWeightKg > availKg) {
                            Text(
                                text = "تنبيه: الوزن أكبر من رصيد الشونة المسجل",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusPartialAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. Optional Sale Expenses (Shown ONLY when ProfitCalculationMode.WITH_EXPENSES is active)
                if (profitMode == ProfitCalculationMode.WITH_EXPENSES) {
                    OutlinedTextField(
                        value = saleExpensesText,
                        onValueChange = { saleExpensesText = it },
                        label = { Text("مصاريف النقلة (نولون / مشال / غربلة) - اختياري") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_sale_expenses_input")
                    )
                }

                // 5. Buyer / Mill Selection
                Text("3. المشتري (مطحن / تاجر جملة):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (customers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCustomer == null,
                                onClick = { selectedCustomer = null },
                                label = { Text("مشتري جديد / نقدي") }
                            )
                        }
                        items(customers) { cust ->
                            FilterChip(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = {
                                    selectedCustomer = cust
                                    customBuyerName = cust.name
                                },
                                label = { Text(cust.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (selectedCustomer == null) {
                    OutlinedTextField(
                        value = customBuyerName,
                        onValueChange = { customBuyerName = it },
                        label = { Text("اسم المطحن أو تاجر الجملة") },
                        placeholder = { Text("مثال: مطحن السلام / تاجر جملة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 6. Initial Payment Collection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = initialPaidText,
                        onValueChange = { initialPaidText = it },
                        label = { Text("المبلغ المحصّل الآن ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            if (totalSaleAmount > 0.0) {
                                initialPaidText = CalculationEngine.formatPlainInt(totalSaleAmount)
                            }
                        },
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("تحصيل الكل", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات النقلة (رقم السيارة / السائق / تفاصيل)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. Live Profit Preview Card (معاينة صافي الربح الفورية)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (previewProfit >= 0) StatusPaidBg else StatusUnpaidBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (previewProfit >= 0) StatusPaidGreen else StatusUnpaidRed,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "معاينة الحساب وصافي الربح الفوري للنقلة",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي قيمة فاتورة البيع:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = CalculationEngine.formatCurrency(totalSaleAmount, currencySymbol),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تكلفة شراء الكمية المباعة:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = CalculationEngine.formatCurrency(estimatedPurchaseCost, currencySymbol),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (profitMode == ProfitCalculationMode.WITH_EXPENSES) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("مصاريف النقلة والمشال:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = CalculationEngine.formatCurrency(saleExpenses, currencySymbol),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = WheatCardBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صافي ربحك في هذه النقلة:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (previewProfit >= 0) "+" else ""}${CalculationEngine.formatCurrency(previewProfit, currencySymbol)} (${CalculationEngine.formatNumber(previewProfitPct, 1)}%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (previewProfit >= 0) StatusPaidGreen else StatusUnpaidRed
                            )
                        }
                    }
                }

                errorMsg?.let {
                    Text(text = it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (bulkWeightKg <= 0.0) {
                                errorMsg = "يرجى إدخال الوزن الكلي المقطوع بالكيلوجرام"
                                return@Button
                            }
                            if (sellingUnitPrice <= 0.0) {
                                errorMsg = "يرجى إدخال سعر بيع الوحدة"
                                return@Button
                            }
                            val paidAmt = initialPaidText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                            onConfirmSale(
                                selectedCustomer,
                                customBuyerName,
                                selectedProduct,
                                selectedUnit,
                                bulkWeightKg,
                                sellingUnitPrice,
                                avgCostPerKg,
                                saleExpenses,
                                paidAmt,
                                paymentMethod,
                                notes
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp)
                            .testTag("confirm_bulk_sale_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعتماد فاتورة البيع وخصم المخزن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualShrinkageDialog(
    stock: ProductWarehouseStock,
    onDismiss: () -> Unit,
    onConfirm: (weightLossKg: Double, reason: String) -> Unit
) {
    var weightLossText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("جفاف طبيعي في الشونة") }
    var err by remember { mutableStateOf<String?>(null) }

    val reasonsList = listOf(
        "جفاف طبيعي في الشونة",
        "غربلة وتنظيف شوائب",
        "فروق ميزان بسكول",
        "تالف تخزين / سوس"
    )

    val lossKg = weightLossText.toDoubleOrNull() ?: 0.0
    val pctOfPurchased = if (stock.purchasedWeightKg > 0.0 && lossKg > 0.0) {
        (lossKg / stock.purchasedWeightKg) * 100.0
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل عجز / جفاف يدوي (${stock.productName})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "الرصيد الدفتري المتاح حالياً: ${CalculationEngine.formatWeight(stock.availableStockKg)} كجم",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = weightLossText,
                    onValueChange = { weightLossText = it; err = null },
                    label = { Text("وزن العجز / الجفاف المفقود (كجم)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_shrinkage_weight_input")
                )

                if (lossKg > 0.0 && stock.purchasedWeightKg > 0.0) {
                    Text(
                        text = "يمثل هذا العجز نسبة ${CalculationEngine.formatNumber(pctOfPurchased, 2)}% من إجمالي مشتريات ${stock.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusPartialAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text("سبب العجز:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reasonsList) { r ->
                        FilterChip(
                            selected = reason == r,
                            onClick = { reason = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("تفاصيل السبب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                err?.let {
                    Text(it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (lossKg <= 0.0) {
                        err = "يرجى إدخال وزن صحيح أكبر من صفر"
                        return@Button
                    }
                    onConfirm(lossKg, reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تسجيل وخصم من الشونة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun BatchZeroOutDialog(
    stock: ProductWarehouseStock,
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var notes by remember { mutableStateOf("تصفير الدفعة بعد نفاد المحصول فعلياً من الشونة (فروق جفاف طبيعي)") }
    val remainingKg = stock.availableStockKg
    val totalShrinkageAfterZeroOut = stock.totalShrinkageKg + remainingKg
    val batchShrinkagePct = if (stock.purchasedWeightKg > 0.0) {
        (totalShrinkageAfterZeroOut / stock.purchasedWeightKg) * 100.0
    } else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = WheatGold,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = "تصفير دفعة «${stock.productName}» عند نفاد المخزن",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "استخدم هذا الخيار عندما ينتهي المحصول فعلياً من الشونة بعد البيع ويتبقى رصيد دفتري نتيجة الجفاف الطبيعي للغلال:",
                    style = MaterialTheme.typography.bodySmall
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WheatGoldLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• إجمالي المشترى: ${CalculationEngine.formatWeight(stock.purchasedWeightKg)} كجم",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• إجمالي المباع فعلياً: ${CalculationEngine.formatWeight(stock.soldWeightKg)} كجم",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• الرصيد الدفتري المتبقي للتصفير: ${CalculationEngine.formatWeight(remainingKg)} كجم",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusUnpaidRed
                        )
                        Text(
                            text = "• إجمالي نسبة الجفاف والعجز للدفعة: ${CalculationEngine.formatNumber(batchShrinkagePct, 2)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظة التصفير في سجل الجفاف") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = WheatGold)
            ) {
                Text("تأكيد تصفير المخزن وتسجيل الجفاف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun EmptyWarehouseBox(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
