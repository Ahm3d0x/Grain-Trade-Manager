package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.LicenseManager
import com.example.security.LicenseValidationResult
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DefinitionsAndSettingsScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.LicenseActivationScreen
import com.example.ui.screens.PurchaseWorkspaceScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.WarehouseAndSalesScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WheatGoldLight
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GrainViewModel
import com.example.util.AppUpdateManager
import com.example.util.UpdateCheckResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var licenseStatus by remember {
                    mutableStateOf(LicenseManager.checkLicenseStatus(context))
                }

                if (licenseStatus is LicenseValidationResult.Valid) {
                    GrainTradeManagerApp()
                } else {
                    LicenseActivationScreen(
                        initialStatus = licenseStatus,
                        onActivationSuccess = { cert ->
                            val refreshed = LicenseManager.checkLicenseStatus(context)
                            licenseStatus = if (refreshed is LicenseValidationResult.Valid) {
                                refreshed
                            } else {
                                LicenseValidationResult.Valid(cert, daysRemaining = 365)
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun GrainTradeManagerApp(
    viewModel: GrainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val dashboardPeriod by viewModel.dashboardPeriod.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoicesWithCalc.collectAsStateWithLifecycle()
    val warehouseSummary by viewModel.warehouseSummary.collectAsStateWithLifecycle()
    val inventoryAdjustments by viewModel.inventoryAdjustments.collectAsStateWithLifecycle()
    val activeInvoice by viewModel.activeInvoiceWithCalc.collectAsStateWithLifecycle()
    val activeSectionId by viewModel.activeSectionId.collectAsStateWithLifecycle()
    val keypadInput by viewModel.keypadInput.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val pricingUnits by viewModel.pricingUnits.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingUpdate by remember {
        mutableStateOf(
            AppUpdateManager.getCachedAvailableUpdate(context)?.takeIf {
                !AppUpdateManager.isUpdateDismissedForSession(context, it.versionCode)
            }
        )
    }

    LaunchedEffect(Unit) {
        val result = AppUpdateManager.checkForUpdates(context)
        if (result is UpdateCheckResult.UpdateAvailable) {
            if (!AppUpdateManager.isUpdateDismissedForSession(context, result.info.versionCode)) {
                pendingUpdate = result.info
            }
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    if (pendingUpdate != null) {
        val update = pendingUpdate!!
        AlertDialog(
            onDismissRequest = {
                if (!update.isMandatory) {
                    AppUpdateManager.dismissUpdatePrompt(context, update.versionCode)
                    pendingUpdate = null
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "تحديث جديد متاح للبرنامج (v${update.versionName})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WheatGoldLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "الإصدار الحالي لديك: v${AppUpdateManager.currentVersionName} (رقم ${AppUpdateManager.currentVersionCode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark
                            )
                            Text(
                                text = "الإصدار الجديد المتاح: v${update.versionName} (رقم ${update.versionCode})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    if (update.releaseNotes.isNotBlank()) {
                        Text(
                            text = "ما الجديد في هذا التحديث:\n${update.releaseNotes}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            AppUpdateManager.openDownloadUrl(context, update.downloadUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تنزيل التحديث الجديد الآن", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            AppUpdateManager.requestUpdateViaWhatsApp(context, update.versionName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("طلب رابط التحديث عبر واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!update.isMandatory) {
                    TextButton(
                        onClick = {
                            AppUpdateManager.dismissUpdatePrompt(context, update.versionCode)
                            pendingUpdate = null
                        }
                    ) {
                        Text("تذكيري لاحقًا")
                    }
                }
            }
        )
    }

    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(AppScreen.DASHBOARD)
        }
    }

    val navItems = remember {
        listOf(
            NavItem(AppScreen.DASHBOARD, "الرئيسية", Icons.Default.Dashboard, "nav_dashboard"),
            NavItem(AppScreen.PURCHASE_WORKSPACE, "شراء ووزن", Icons.Default.Scale, "nav_purchase"),
            NavItem(AppScreen.WAREHOUSE, "الشونة", Icons.Default.Inventory2, "nav_warehouse"),
            NavItem(AppScreen.INVOICES, "الفواتير", Icons.Default.Description, "nav_invoices"),
            NavItem(AppScreen.CUSTOMERS, "العملاء", Icons.Default.Group, "nav_customers"),
            NavItem(AppScreen.REPORTS, "التقارير", Icons.Default.Assessment, "nav_reports"),
            NavItem(AppScreen.SETTINGS, "الإعدادات", Icons.Default.Settings, "nav_definitions")
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentScreen == item.screen ||
                                (item.screen == AppScreen.SETTINGS && currentScreen == AppScreen.DEFINITIONS)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (item.screen == AppScreen.PURCHASE_WORKSPACE) {
                                        viewModel.openPurchaseTabOrResumeDraft()
                                    } else {
                                        viewModel.navigateTo(item.screen)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 9.5.sp,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldLight
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentScreen == item.screen ||
                                (item.screen == AppScreen.SETTINGS && currentScreen == AppScreen.DEFINITIONS)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (item.screen == AppScreen.PURCHASE_WORKSPACE) {
                                        viewModel.openPurchaseTabOrResumeDraft()
                                    } else {
                                        viewModel.navigateTo(item.screen)
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, softWrap = false, maxLines = 1) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                invoices = allInvoices,
                                warehouseSummary = warehouseSummary,
                                selectedPeriod = dashboardPeriod,
                                settings = settings,
                                onSelectPeriod = viewModel::setDashboardPeriod,
                                onStartNewPurchase = { viewModel.startNewPurchase() },
                                onOpenInvoice = { id -> viewModel.openExistingInvoiceInWorkspace(id) },
                                onNavigate = viewModel::navigateTo
                            )
                        }

                        AppScreen.PURCHASE_WORKSPACE -> {
                            PurchaseWorkspaceScreen(
                                invoiceWithCalc = activeInvoice,
                                heldDraftInvoices = allInvoices.filter { !it.calc.isSaleInvoice },
                                activeSectionId = activeSectionId,
                                keypadInput = keypadInput,
                                customers = customers,
                                products = products,
                                pricingUnits = pricingUnits,
                                settings = settings,
                                onStartNewPurchase = { viewModel.startNewPurchase() },
                                onSuspendAndStartNew = { viewModel.suspendCurrentInvoiceAndStartNew() },
                                onSwitchToHeldInvoice = { id -> viewModel.openExistingInvoiceInWorkspace(id) },
                                onSelectSection = viewModel::selectActiveSection,
                                onKeypadDigit = viewModel::onKeypadDigit,
                                onKeypadBackspace = viewModel::onKeypadBackspace,
                                onKeypadClear = viewModel::onKeypadClear,
                                onAddWeightFromKeypad = viewModel::addWeightFromKeypad,
                                onUpdateWeight = viewModel::updateWeightValue,
                                onDeleteWeight = viewModel::deleteWeightItem,
                                onAddSection = viewModel::addSectionToActiveInvoice,
                                onUpdateSection = { sec, prod, unit, price, reason ->
                                    viewModel.updateSectionSettings(
                                        section = sec,
                                        product = prod,
                                        unit = unit,
                                        newPrice = price,
                                        auditReason = reason
                                    )
                                },
                                onDeleteSection = viewModel::deleteSectionFromActiveInvoice,
                                onUpdateHeader = viewModel::updateActiveInvoiceHeader,
                                onQuickCreateCustomer = viewModel::saveCustomer,
                                onApproveInvoice = viewModel::approveActiveInvoice,
                                onRecordPayment = { amt, method, notes ->
                                    activeInvoice?.details?.invoice?.id?.let { id ->
                                        viewModel.recordInvoicePayment(id, amt, method, notes)
                                    }
                                },
                                onSettleOverpayment = { overpaid, asCredit, notes ->
                                    activeInvoice?.details?.invoice?.id?.let { id ->
                                        viewModel.settleOverpaidInvoice(id, overpaid, asCredit, notes)
                                    }
                                },
                                onDeleteInvoice = viewModel::deleteInvoiceById
                            )
                        }

                        AppScreen.WAREHOUSE -> {
                            WarehouseAndSalesScreen(
                                warehouseSummary = warehouseSummary,
                                allInvoices = allInvoices,
                                adjustments = inventoryAdjustments,
                                customers = customers,
                                products = products,
                                pricingUnits = pricingUnits,
                                settings = settings,
                                onCreateBulkSale = { cust, customName, prod, unit, bulkKg, unitPrice, costPerKg, expenses, paidAmt, payMethod, notes ->
                                    viewModel.createBulkSaleInvoice(
                                        customer = cust,
                                        customCustomerName = customName,
                                        product = prod,
                                        unit = unit,
                                        bulkWeightKg = bulkKg,
                                        sellingUnitPrice = unitPrice,
                                        costPerKgSnapshot = costPerKg,
                                        saleExpenses = expenses,
                                        initialPaidAmount = paidAmt,
                                        paymentMethod = payMethod,
                                        notes = notes
                                    )
                                },
                                onRecordShrinkage = viewModel::recordShrinkageAdjustment,
                                onZeroOutBatch = viewModel::zeroOutBatchStock,
                                onDeleteAdjustment = viewModel::deleteInventoryAdjustment,
                                onRecordPayment = viewModel::recordInvoicePayment,
                                onDeleteInvoice = viewModel::deleteInvoiceById,
                                onToggleProfitMode = { newMode ->
                                    viewModel.saveAppSettings(
                                        settings.copy(profitCalculationMode = newMode.name)
                                    )
                                }
                            )
                        }

                        AppScreen.INVOICES -> {
                            InvoicesScreen(
                                invoices = allInvoices,
                                products = products,
                                settings = settings,
                                onOpenInWorkspace = viewModel::openExistingInvoiceInWorkspace,
                                onRecordPayment = viewModel::recordInvoicePayment,
                                onSettleOverpayment = viewModel::settleOverpaidInvoice,
                                onDeleteInvoice = viewModel::deleteInvoiceById
                            )
                        }

                        AppScreen.CUSTOMERS -> {
                            CustomersScreen(
                                customers = customers,
                                invoices = allInvoices,
                                settings = settings,
                                onSaveCustomer = viewModel::saveCustomer,
                                onDeleteCustomer = viewModel::deleteCustomer,
                                onStartPurchaseForCustomer = { cust ->
                                    viewModel.startNewPurchase(customer = cust)
                                },
                                onOpenInvoice = viewModel::openExistingInvoiceInWorkspace,
                                onDeleteInvoice = viewModel::deleteInvoiceById
                            )
                        }

                        AppScreen.REPORTS -> {
                            ReportsScreen(
                                invoices = allInvoices,
                                customers = customers,
                                settings = settings,
                                onOpenInvoice = viewModel::openExistingInvoiceInWorkspace
                            )
                        }

                        AppScreen.DEFINITIONS, AppScreen.SETTINGS -> {
                            DefinitionsAndSettingsScreen(
                                products = products,
                                pricingUnits = pricingUnits,
                                settings = settings,
                                initialMainSection = if (currentScreen == AppScreen.SETTINGS) 1 else 0,
                                onMainSectionChanged = { section ->
                                    viewModel.navigateTo(if (section == 1) AppScreen.SETTINGS else AppScreen.DEFINITIONS)
                                },
                                onSaveProduct = viewModel::saveProduct,
                                onSavePricingUnit = viewModel::savePricingUnit,
                                onSaveSettings = viewModel::saveAppSettings,
                                onExportBackup = viewModel::exportDatabaseBackup,
                                onImportBackup = viewModel::importDatabaseBackup
                            )
                        }
                    }
                }
            }
        }
    }
}
