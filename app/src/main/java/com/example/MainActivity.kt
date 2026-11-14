package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.security.LicenseManager
import com.example.security.LicenseValidationResult
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DefinitionsAndSettingsScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.LicenseActivationScreen
import com.example.ui.screens.PurchaseWorkspaceScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GrainViewModel

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
                            licenseStatus = LicenseValidationResult.Valid(cert, daysRemaining = 365)
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
    val activeInvoice by viewModel.activeInvoiceWithCalc.collectAsStateWithLifecycle()
    val activeSectionId by viewModel.activeSectionId.collectAsStateWithLifecycle()
    val keypadInput by viewModel.keypadInput.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val pricingUnits by viewModel.pricingUnits.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
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
            NavItem(AppScreen.INVOICES, "الفواتير", Icons.Default.Description, "nav_invoices"),
            NavItem(AppScreen.CUSTOMERS, "العملاء", Icons.Default.Group, "nav_customers"),
            NavItem(AppScreen.REPORTS, "التقارير", Icons.Default.Assessment, "nav_reports"),
            NavItem(AppScreen.DEFINITIONS, "التعريفات", Icons.Default.Settings, "nav_definitions")
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
                            val selected = currentScreen == item.screen
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
                                        fontSize = 10.sp,
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
                            val selected = currentScreen == item.screen
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
                                heldDraftInvoices = allInvoices,
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

                        AppScreen.DEFINITIONS -> {
                            DefinitionsAndSettingsScreen(
                                products = products,
                                pricingUnits = pricingUnits,
                                settings = settings,
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
