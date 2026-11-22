package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.core.content.FileProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ProfitCalculationMode
import com.example.domain.CalculationEngine
import com.example.security.HardwareIdProvider
import com.example.security.LicenseManager
import com.example.security.LicenseValidationResult
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
import com.example.util.AppUpdateInfo
import com.example.util.AppUpdateManager
import com.example.util.AutoBackupEntry
import com.example.util.AutoBackupManager
import com.example.util.AutoBackupResult
import com.example.util.UpdateCheckResult
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Provides two clearly separated main sections:
 * - Main Section 0: التعريفات (Definitions: انواع الحبوب، وحدات التسعير، بيانات المكتب والفاتورة)
 * - Main Section 1: الإعدادات (Settings: الترخيص والحماية، النسخ الاحتياطي والاستعادة، حول البرنامج والمطور)
 */
@Composable
fun DefinitionsAndSettingsScreen(
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    settings: AppSettingsEntity,
    initialMainSection: Int = 0, // 0 = التعريفات, 1 = الإعدادات
    onMainSectionChanged: ((Int) -> Unit)? = null,
    onSaveProduct: (ProductEntity) -> Unit,
    onSaveProductWithPricingUnit: (ProductEntity, Long?, String?, Double?) -> Unit = { p, _, _, _ -> onSaveProduct(p) },
    onSavePricingUnit: (PricingUnitEntity) -> Unit,
    onSaveSettings: (AppSettingsEntity) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String) -> Unit,
    onLockAppNow: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var mainSection by remember { mutableIntStateOf(initialMainSection) }
    LaunchedEffect(initialMainSection) {
        mainSection = initialMainSection
    }

    // Sub-tabs inside "التعريفات" (0 = أنواع الحبوب, 1 = وحدات التسعير, 2 = بيانات المكتب والفاتورة)
    var defSubTab by remember { mutableIntStateOf(0) }
    val defTabs = listOf(
        "أنواع الحبوب" to Icons.Default.Grass,
        "وحدات التسعير" to Icons.Default.Scale,
        "بيانات المكتب" to Icons.Default.Storefront
    )

    // Sub-tabs inside "الإعدادات" (0 = الترخيص والحماية, 1 = النسخ الاحتياطي, 2 = حول البرنامج والمطور)
    var settingsSubTab by remember { mutableIntStateOf(0) }
    val settingsTabs = listOf(
        "الترخيص والحماية" to Icons.Default.VerifiedUser,
        "النسخ الاحتياطي" to Icons.Default.Backup,
        "حول البرنامج والمطور" to Icons.Default.Info
    )

    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddProduct by remember { mutableStateOf(false) }

    var editingUnit by remember { mutableStateOf<PricingUnitEntity?>(null) }
    var showAddUnit by remember { mutableStateOf(false) }

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                onImportBackup(content)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("definitions_settings_screen")
    ) {
        // Top Header with Separate Main Switcher: [التعريفات والوحدات] vs [الإعدادات والترخيص]
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
                // Main Section Switcher (Definitions vs Settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MainSectionPill(
                        modifier = Modifier.weight(1f),
                        title = "التعريفات والوحدات",
                        icon = Icons.Default.Grass,
                        selected = mainSection == 0,
                        onClick = {
                            mainSection = 0
                            onMainSectionChanged?.invoke(0)
                        },
                        testTag = "main_tab_definitions"
                    )
                    MainSectionPill(
                        modifier = Modifier.weight(1f),
                        title = "الإعدادات والترخيص",
                        icon = Icons.Default.Settings,
                        selected = mainSection == 1,
                        onClick = {
                            mainSection = 1
                            onMainSectionChanged?.invoke(1)
                        },
                        testTag = "main_tab_settings"
                    )
                }

                // Sub-Tab Row for the active main section
                if (mainSection == 0) {
                    TabRow(
                        selectedTabIndex = defSubTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = EmeraldPrimary
                    ) {
                        defTabs.forEachIndexed { index, (title, icon) ->
                            Tab(
                                selected = defSubTab == index,
                                onClick = { defSubTab = index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (defSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("def_tab_$index")
                            )
                        }
                    }
                } else {
                    TabRow(
                        selectedTabIndex = settingsSubTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = EmeraldPrimary
                    ) {
                        settingsTabs.forEachIndexed { index, (title, icon) ->
                            Tab(
                                selected = settingsSubTab == index,
                                onClick = { settingsSubTab = index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (settingsSubTab == index) FontWeight.Bold else FontWeight.Normal,
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("settings_tab_$index")
                            )
                        }
                    }
                }
            }
        }

        // Content Area
        if (mainSection == 0) {
            // =========================================================================
            // MAIN SECTION 0: التعريفات والوحدات (DEFINITIONS)
            // =========================================================================
            when (defSubTab) {
                0 -> {
                    // 1. Grain Products Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تعريف أنواع الحبوب والغلال (${products.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showAddProduct = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.testTag("add_product_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة محصول")
                                }
                            }
                        }

                        items(products, key = { it.id }) { prod ->
                            val linkedUnit = remember(prod, pricingUnits) {
                                CalculationEngine.resolveDefaultUnitForProduct(prod, pricingUnits)
                            }
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
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (prod.description.isNotBlank()) {
                                            Text(
                                                text = prod.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (linkedUnit != null) WheatGoldLight else MaterialTheme.colorScheme.background
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Scale,
                                                    contentDescription = null,
                                                    tint = if (linkedUnit != null) WheatGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (linkedUnit != null) {
                                                        "وحدة التسعير: ${linkedUnit.name} (${CalculationEngine.formatWeight(linkedUnit.weightKg)} كجم)"
                                                    } else {
                                                        "بدون وحدة تسعير مرتبطة"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (linkedUnit != null) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = prod.isActive,
                                            onCheckedChange = { onSaveProduct(prod.copy(isActive = it)) }
                                        )
                                        IconButton(onClick = { editingProduct = prod }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = EmeraldPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // 2. Pricing Units Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary)
                                    Text(
                                        text = "حماية السجل التاريخي: تعديل وزن أي وحدة هنا يطبق على العمليات الجديدة فقط ولا يغير حسابات الفواتير القديمة.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وحدات التسعير والأوزان القياسية (${pricingUnits.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showAddUnit = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = WheatGold),
                                    modifier = Modifier.testTag("add_pricing_unit_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة وحدة")
                                }
                            }
                        }

                        items(pricingUnits, key = { it.id }) { unit ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${unit.name} — ${CalculationEngine.formatWeight(unit.weightKg)} كجم",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "المحصول المرتبط: ${unit.linkedProductName?.ifBlank { "عام لجميع الحبوب" } ?: "عام لجميع الحبوب"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = unit.isActive,
                                            onCheckedChange = { onSavePricingUnit(unit.copy(isActive = it)) }
                                        )
                                        IconButton(onClick = { editingUnit = unit }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل الوحدة", tint = EmeraldPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // 3. Merchant / Office Definitions Tab
                    MerchantDefinitionsTabContent(
                        settings = settings,
                        onSaveSettings = onSaveSettings
                    )
                }
            }
        } else {
            // =========================================================================
            // MAIN SECTION 1: الإعدادات (SETTINGS: LICENSE, BACKUP, ABOUT DEVELOPER)
            // =========================================================================
            when (settingsSubTab) {
                0 -> {
                    LicenseDetailsSettingsTab(
                        context = context,
                        onLockAppNow = onLockAppNow
                    )
                }

                1 -> {
                    BackupAndRestoreSettingsTab(
                        context = context,
                        onExportClick = {
                            onExportBackup { json ->
                                pendingBackupJson = json
                                createBackupLauncher.launch("grain_trade_backup_${System.currentTimeMillis()}.json")
                            }
                        },
                        onImportClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onRestoreJson = { json ->
                            onImportBackup(json)
                        }
                    )
                }

                2 -> {
                    AboutAppAndDeveloperTab(context = context)
                }
            }
        }
    }

    // --- Dialogs for Products & Pricing Units ---
    if (showAddProduct || editingProduct != null) {
        val initialLinkedUnit = remember(editingProduct, pricingUnits) {
            editingProduct?.let { CalculationEngine.resolveDefaultUnitForProduct(it, pricingUnits) }
                ?: pricingUnits.firstOrNull { it.isActive }
                ?: pricingUnits.firstOrNull()
        }
        var name by remember(editingProduct?.id) { mutableStateOf(editingProduct?.name ?: "") }
        var desc by remember(editingProduct?.id) { mutableStateOf(editingProduct?.description ?: "") }
        var createNewUnitMode by remember(editingProduct?.id) {
            mutableStateOf(pricingUnits.isEmpty())
        }
        var selectedUnit by remember(editingProduct?.id, initialLinkedUnit) {
            mutableStateOf(initialLinkedUnit)
        }
        var newUnitName by remember(editingProduct?.id) { mutableStateOf("") }
        var newUnitWeightText by remember(editingProduct?.id) { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        val activeUnitsList = remember(pricingUnits) {
            pricingUnits.filter { it.isActive }.ifEmpty { pricingUnits }
        }

        AlertDialog(
            onDismissRequest = { showAddProduct = false; editingProduct = null },
            title = {
                Text(
                    text = if (editingProduct == null) "إضافة نوع حبوب وربط وحدة التسعير" else "تعديل نوع الحبوب ووحدة التسعير",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; err = null },
                        label = { Text("اسم المحصول (مثال: قمح، سمسم، ذرة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("وصف اختياري") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = WheatCardBorder)

                    Text(
                        text = "وحدة التسعير المرتبطة بهذا المحصول:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    if (activeUnitsList.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !createNewUnitMode,
                                onClick = { createNewUnitMode = false; err = null },
                                label = { Text("اختيار وحدة موجودة") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = createNewUnitMode,
                                onClick = { createNewUnitMode = true; err = null },
                                label = { Text("تعريف وحدة جديدة") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WheatGold,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!createNewUnitMode && activeUnitsList.isNotEmpty()) {
                        val effectiveSelected = selectedUnit ?: activeUnitsList.first()
                        PricingUnitDropdownSelector(
                            units = activeUnitsList,
                            selectedUnit = effectiveSelected,
                            label = "وحدة التسعير الافتراضية",
                            onUnitSelected = { unit ->
                                selectedUnit = unit
                                err = null
                            }
                        )
                    } else {
                        OutlinedTextField(
                            value = newUnitName,
                            onValueChange = { newUnitName = it; err = null },
                            label = { Text("اسم وحدة التسعير (مثال: أردب قمح، طن)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("product_new_unit_name_input")
                        )
                        OutlinedTextField(
                            value = newUnitWeightText,
                            onValueChange = { newUnitWeightText = it; err = null },
                            label = { Text("وزن الوحدة بالكيلوجرام (مثال: 150)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("product_new_unit_weight_input")
                        )
                    }

                    err?.let {
                        Text(it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            err = "يرجى إدخال اسم المحصول"
                            return@Button
                        }
                        if (createNewUnitMode) {
                            val w = newUnitWeightText.toDoubleOrNull()
                            if (newUnitName.isBlank() || w == null || w <= 0.0) {
                                err = "يرجى إدخال اسم وحدة التسعير ووزنها بالكيلوجرام"
                                return@Button
                            }
                            onSaveProductWithPricingUnit(
                                (editingProduct ?: ProductEntity(name = "")).copy(
                                    name = name.trim(),
                                    description = desc.trim()
                                ),
                                null,
                                newUnitName.trim(),
                                w
                            )
                        } else {
                            val chosenId = selectedUnit?.id ?: activeUnitsList.firstOrNull()?.id
                            onSaveProductWithPricingUnit(
                                (editingProduct ?: ProductEntity(name = "")).copy(
                                    name = name.trim(),
                                    description = desc.trim(),
                                    defaultUnitId = chosenId
                                ),
                                chosenId,
                                null,
                                null
                            )
                        }
                        showAddProduct = false
                        editingProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("save_product_dialog_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProduct = false; editingProduct = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showAddUnit || editingUnit != null) {
        var unitName by remember { mutableStateOf(editingUnit?.name ?: "") }
        var weightText by remember {
            mutableStateOf(editingUnit?.weightKg?.let { CalculationEngine.formatPlainInt(it) } ?: "150")
        }
        var linkedProd by remember {
            mutableStateOf(products.find { it.id == editingUnit?.linkedProductId })
        }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddUnit = false; editingUnit = null },
            title = { Text(if (editingUnit == null) "إضافة وحدة تسعير جديدة" else "تعديل وحدة التسعير") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it; err = null },
                        label = { Text("اسم الوحدة (مثال: أردب قمح، طن، قنطار)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it; err = null },
                        label = { Text("وزن الوحدة بالكيلوجرام") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("ربط بمحصول محدد (اختياري):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = linkedProd == null,
                                onClick = { linkedProd = null },
                                label = { Text("عام لكل الحبوب") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(products) { p ->
                            FilterChip(
                                selected = linkedProd?.id == p.id,
                                onClick = { linkedProd = p },
                                label = { Text(p.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    err?.let { Text(it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightText.toDoubleOrNull()
                        if (unitName.isBlank() || w == null || w <= 0.0) {
                            err = "يرجى إدخال اسم الوحدة ووزن صحيح أكبر من صفر"
                            return@Button
                        }
                        onSavePricingUnit(
                            (editingUnit ?: PricingUnitEntity(name = "", weightKg = 1000.0)).copy(
                                name = unitName.trim(),
                                weightKg = w,
                                linkedProductId = linkedProd?.id,
                                linkedProductName = linkedProd?.name ?: "جميع المحاصيل"
                            )
                        )
                        showAddUnit = false
                        editingUnit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ الوحدة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnit = false; editingUnit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun MainSectionPill(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) EmeraldPrimary else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MerchantDefinitionsTabContent(
    settings: AppSettingsEntity,
    onSaveSettings: (AppSettingsEntity) -> Unit
) {
    var merchantName by remember(settings) { mutableStateOf(settings.merchantName) }
    var merchantPhone by remember(settings) { mutableStateOf(settings.merchantPhone) }
    var merchantAddress by remember(settings) { mutableStateOf(settings.merchantAddress) }
    var currencySymbol by remember(settings) { mutableStateOf(settings.currencySymbol) }
    var startingInvNum by remember(settings) { mutableStateOf(settings.startingInvoiceNumber.toString()) }
    var footerNotes by remember(settings) { mutableStateOf(settings.invoiceFooterNotes) }
    var profitMode by remember(settings.profitCalculationMode) {
        mutableStateOf(
            runCatching { ProfitCalculationMode.valueOf(settings.profitCalculationMode) }
                .getOrDefault(ProfitCalculationMode.WITH_EXPENSES)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "نظام حساب صافي الربح في الشونة والمبيعات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "اختر طريقة حساب صافي الربح المعتمدة في فواتير البيع والتوريد وتقارير الشونة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ProfitCalculationMode.entries.forEach { mode ->
                        val isSelected = profitMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) EmeraldPrimary else WheatCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    profitMode = mode
                                    onSaveSettings(
                                        settings.copy(
                                            merchantName = merchantName.trim(),
                                            merchantPhone = merchantPhone.trim(),
                                            merchantAddress = merchantAddress.trim(),
                                            currencySymbol = currencySymbol.trim().ifBlank { "ج.م" },
                                            startingInvoiceNumber = startingInvNum.toIntOrNull() ?: settings.startingInvoiceNumber,
                                            invoiceFooterNotes = footerNotes.trim(),
                                            profitCalculationMode = mode.name
                                        )
                                    )
                                }
                                .testTag("profit_mode_option_${mode.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.labelAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.descriptionAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "بيانات التاجر والمكتب وإعدادات الفواتير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        label = { Text("اسم المكتب أو التاجر (يظهر في ترويسة PDF)") },
                        placeholder = { Text("اكتب اسم المكتب أو التاجر هنا...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = merchantPhone,
                        onValueChange = { merchantPhone = it },
                        label = { Text("رقم هاتف المكتب / التاجر") },
                        placeholder = { Text("اكتب رقم الهاتف هنا...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = merchantAddress,
                        onValueChange = { merchantAddress = it },
                        label = { Text("العنوان / المكان") },
                        placeholder = { Text("اكتب عنوان المكتب أو الشونة هنا...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("العملة الافتراضية") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startingInvNum,
                            onValueChange = { startingInvNum = it },
                            label = { Text("رقم الفاتورة القادم") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = footerNotes,
                        onValueChange = { footerNotes = it },
                        label = { Text("ملاحظات أسفل الفاتورة المطبوعة (اختياري)") },
                        placeholder = { Text("اكتب أي ملاحظات تود ظهورها أسفل الفاتورة...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    merchantName = merchantName.trim(),
                                    merchantPhone = merchantPhone.trim(),
                                    merchantAddress = merchantAddress.trim(),
                                    currencySymbol = currencySymbol.trim().ifBlank { "ج.م" },
                                    startingInvoiceNumber = startingInvNum.toIntOrNull() ?: settings.startingInvoiceNumber,
                                    invoiceFooterNotes = footerNotes.trim(),
                                    profitCalculationMode = profitMode.name
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ بيانات المكتب والفواتير")
                    }
                }
            }
        }
    }
}

/**
 * Settings Sub-Tab 0: Comprehensive License & Digital Protection Details
 */
@Composable
private fun LicenseDetailsSettingsTab(
    context: Context,
    onLockAppNow: (() -> Unit)? = null
) {
    var showUpdateLicenseDialog by remember { mutableStateOf(false) }
    var licenseResult by remember { mutableStateOf(LicenseManager.checkLicenseStatus(context)) }
    val currentMachineId = remember { HardwareIdProvider.getHardwareId(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. App Lock & Protection Card (PIN 4 أرقام + نقش 9 نقاط + بصمة الهاتف)
        item {
            AppLockSettingsCard(
                context = context,
                onLockAppNow = onLockAppNow
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, if (licenseResult is LicenseValidationResult.Valid) EmeraldPrimary else StatusUnpaidRed, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Banner
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (licenseResult is LicenseValidationResult.Valid) EmeraldLight else StatusUnpaidBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (licenseResult is LicenseValidationResult.Valid) EmeraldPrimary else StatusUnpaidRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "شهادة الترخيص والحماية الرقمية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "UltraSoft • Eng. Ahmed Mohamed Attia",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (licenseResult is LicenseValidationResult.Valid) StatusPaidBg else StatusUnpaidBg
                        ) {
                            Text(
                                text = if (licenseResult is LicenseValidationResult.Valid) "مرخص رسمي 🟢" else "غير مفعل 🔴",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (licenseResult is LicenseValidationResult.Valid) StatusPaidGreen else StatusUnpaidRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    if (licenseResult is LicenseValidationResult.Valid) {
                        val validResult = licenseResult as LicenseValidationResult.Valid
                        val cert = validResult.certificate
                        val payload = cert.payload

                        // Calculate total license duration & progress ratio
                        val totalDurationDays = remember(payload.issuedAt, payload.expiresAt, payload.isLifetime) {
                            if (payload.isLifetime) 365L
                            else calculateTotalDurationDays(payload.issuedAt, payload.expiresAt)
                        }
                        val remainingRatio = if (payload.isLifetime) {
                            1f
                        } else if (totalDurationDays > 0) {
                            (validResult.daysRemaining.toFloat() / totalDurationDays.toFloat()).coerceIn(0f, 1f)
                        } else 1f

                        // Remaining Duration Visual Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (payload.isLifetime || validResult.daysRemaining > 30) EmeraldLight else StatusPartialBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "صلاحية ومدة الترخيص المتبقية:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = if (payload.isLifetime) {
                                            "مدى الحياة (Lifetime ♾️)"
                                        } else {
                                            "متبقي ${validResult.daysRemaining} يوم"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (payload.isLifetime || validResult.daysRemaining > 30) EmeraldPrimary else StatusPartialAmber
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(remainingRatio.coerceAtLeast(0.04f))
                                            .fillMaxHeight()
                                            .background(if (payload.isLifetime || validResult.daysRemaining > 30) StatusPaidGreen else StatusPartialAmber)
                                    )
                                }

                                if (!payload.isLifetime) {
                                    Text(
                                        text = "إجمالي مدة الترخيص: $totalDurationDays يوم  |  المتبقي الفعلي: ${validResult.daysRemaining} يوم",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }

                        // Detailed License Rows
                        LicenseInfoRow(label = "اسم العميل / الجهة المرخصة:", value = payload.clientName, highlight = true)
                        LicenseInfoRow(label = "رقم شهادة الترخيص (License ID):", value = payload.licenseId.ifBlank { "GTM-OFFICIAL" })
                        LicenseInfoRow(label = "نوع / فئة الترخيص:", value = "${payload.tier} Edition")
                        LicenseInfoRow(
                            label = "نظام الصلاحية:",
                            value = if (payload.isLifetime) "ترخيص دائم مدى الحياة (Lifetime)" else "ترخيص دوري محدد المدة"
                        )
                        LicenseInfoRow(
                            label = "تاريخ إصدار الترخيص:",
                            value = payload.issuedAt.take(10).ifBlank { "غير محدد" }
                        )
                        LicenseInfoRow(
                            label = "تاريخ انتهاء الترخيص:",
                            value = if (payload.isLifetime) "لا ينتهي (مدى الحياة)" else payload.expiresAt.take(10),
                            highlight = !payload.isLifetime
                        )
                        LicenseInfoRow(
                            label = "المدة المتبقية:",
                            value = if (payload.isLifetime) "مفتوح مدى الحياة ♾️" else "${validResult.daysRemaining} يوم متبقي",
                            highlight = true
                        )
                        LicenseInfoRow(
                            label = "المميزات المفعلة:",
                            value = if (payload.features.contains("all")) "جميع المميزات والتقارير والـ PDF مفعلة بالكامل" else payload.features.joinToString("، ")
                        )
                        LicenseInfoRow(
                            label = "الشركة المالكة والمطورة للنظام:",
                            value = "UltraSoft (Eng. Ahmed Mohamed Attia)",
                            highlight = true
                        )
                        LicenseInfoRow(
                            label = "التوقيع الرقمي وإصدار الحماية:",
                            value = "${cert.header.alg} (${cert.header.type} v${cert.header.ver})"
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusUnpaidBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "البرنامج غير مفعل حاليًا أو انتهت صلاحية الترخيص. يرجى إدخال كود ترخيص صالح صادر لهذا الجهاز.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusUnpaidRed,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    // Device Machine ID Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "معرّف الجهاز المرتبط بالترخيص (Machine ID):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentMachineId,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Machine ID", currentMachineId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ معرّف الجهاز", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ معرّف الجهاز",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showUpdateLicenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحديث أو تجديد كود الترخيص")
                    }
                }
            }
        }
    }

    if (showUpdateLicenseDialog) {
        var newKeyInput by remember { mutableStateOf("") }
        var updateErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showUpdateLicenseDialog = false },
            title = { Text("تحديث أو تجديد كود الترخيص") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الصق كود الترخيص الصادر من سلطة إصدار التراخيص:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newKeyInput,
                        onValueChange = { newKeyInput = it; updateErr = null },
                        placeholder = { Text("GTM6-eyJoZWFkZXIi...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    updateErr?.let {
                        Text(it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = LicenseManager.activateLicense(context, newKeyInput.trim())
                        if (res is LicenseValidationResult.Valid) {
                            Toast.makeText(context, "تم تحديث الترخيص بنجاح!", Toast.LENGTH_LONG).show()
                            licenseResult = res
                            showUpdateLicenseDialog = false
                        } else {
                            updateErr = "كود الترخيص غير صالح أو غير مخصص لهذا الجهاز"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateLicenseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun LicenseInfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Settings Sub-Tab 1: Backup & Restore ONLY (Merchant info is in Definitions tab)
 */
@Composable
private fun BackupAndRestoreSettingsTab(
    context: Context,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onRestoreJson: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var statusSummary by remember { mutableStateOf(AutoBackupManager.getStatusSummary(context)) }
    var isRunningBackupNow by remember { mutableStateOf(false) }
    var backupActionMessage by remember { mutableStateOf<String?>(null) }
    var entryPendingRestore by remember { mutableStateOf<AutoBackupEntry?>(null) }
    var entryPendingDelete by remember { mutableStateOf<AutoBackupEntry?>(null) }

    val dateTimeFormatter = remember {
        SimpleDateFormat("yyyy/MM/dd • hh:mm:ss a", Locale.forLanguageTag("ar-EG"))
    }
    val timeOnlyFormatter = remember {
        SimpleDateFormat("hh:mm:ss a", Locale.forLanguageTag("ar-EG"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Smart Automatic Backup on App Close Card (30-Day Policy)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = EmeraldPrimary)
                            }
                            Column {
                                Text(
                                    text = "النسخ الاحتياطي التلقائي الذكي (30 يومًا)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "يحفظ تلقائيًا عند إغلاق التطبيق في حال حدوث تغيير بالبيانات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = statusSummary.isEnabled,
                            onCheckedChange = { enabled ->
                                AutoBackupManager.setAutoBackupEnabled(context, enabled)
                                statusSummary = AutoBackupManager.getStatusSummary(context)
                            },
                            modifier = Modifier.testTag("auto_backup_switch")
                        )
                    }

                    // Explanation of the 2-day + 28-day = 30-day smart policy
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldLight.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "آلية الاحتفاظ الذكي بالنسخ (توفير المساحة + أقصى حماية):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "• آخر يومين (2 يوم): الاحتفاظ بجميع ملفات النسخ الاحتياطي لكل عملية فتح وإغلاق حدث فيها تغيير (سجل التغيرات الكامل).",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark
                            )
                            Text(
                                text = "• الـ 28 يومًا السابقة: عند دخول اليوم الثالث يتم تلقائيًا اعتماد آخر نسخة احتياطية فقط من ذلك اليوم وحذف باقي سجلات اليوم نفسه، ليكون هناك 28 ملفًا لـ 28 يومًا (ملف واحد شامل لآخر تغييرات كل يوم).",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark
                            )
                        }
                    }

                    // Folder Path & Storage Stats
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, WheatCardBorder, RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = WheatGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "مجلد النسخ على الجهاز: ${AutoBackupManager.BACKUP_FOLDER_NAME}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = statusSummary.folderPath,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(color = WheatCardBorder.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الأيام المحفوظة: ${statusSummary.totalDaysCount} / 30 يوم",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "المساحة المستخدمة: ${AutoBackupManager.formatFileSize(statusSummary.totalStorageBytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WheatGold
                                )
                            }
                            if (statusSummary.lastBackupTimestamp > 0L) {
                                Text(
                                    text = "آخر نسخ تلقائي: ${dateTimeFormatter.format(Date(statusSummary.lastBackupTimestamp))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    backupActionMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Button(
                        onClick = {
                            isRunningBackupNow = true
                            backupActionMessage = null
                            coroutineScope.launch {
                                val res = AutoBackupManager.performAutoBackupIfChanged(
                                    context = context,
                                    force = true,
                                    triggerReason = "نسخ فوري من الإعدادات"
                                )
                                isRunningBackupNow = false
                                statusSummary = AutoBackupManager.getStatusSummary(context)
                                backupActionMessage = when (res) {
                                    is AutoBackupResult.Created ->
                                        "تم حفظ نسخة احتياطية جديدة بنجاح وتحديث سجل الـ 30 يومًا ✓"
                                    is AutoBackupResult.NoChanges ->
                                        "لا توجد تغييرات جديدة في البيانات منذ آخر نسخة محفوظة."
                                    is AutoBackupResult.Disabled ->
                                        "النسخ الاحتياطي التلقائي متوقف حاليًا."
                                    is AutoBackupResult.Error ->
                                        res.message
                                }
                            }
                        },
                        enabled = !isRunningBackupNow,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_smart_auto_backup_now_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunningBackupNow) "جارِ حفظ النسخة الاحتياطية..." else "حفظ نسخة الآن في فولدر الـ 30 يومًا",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Manual Export / Import Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WheatGoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = WheatGold)
                        }
                        Column {
                            Text(
                                text = "تصدير واستيراد يدوي لملف خارجي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لحفظ نسخة خارجية أو نقل البيانات إلى هاتف آخر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير ملف خارجي", softWrap = false, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onImportClick,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة من ملف", softWrap = false, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 3. Detailed Last 2 Days Section (All open/close session changes)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سجل التغيرات الكامل لآخر يومين (جميع الجلسات)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "يحتفظ بملف مستقل لكل عملية فتح وإغلاق حدث فيها تعديل خلال آخر يومين",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = "${statusSummary.detailedTwoDaysEntries.size} نسخة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (statusSummary.detailedTwoDaysEntries.isEmpty()) {
                        Text(
                            text = "لا توجد نسخ مسجلة بعد؛ سيتم الحفظ تلقائيًا فور إجراء أي تعديل وإغلاق التطبيق.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val groupedByDay = statusSummary.detailedTwoDaysEntries.groupBy { it.dayKey }
                        groupedByDay.forEach { (dayKey, dayEntries) ->
                            Text(
                                text = "📅 يوم $dayKey (${dayEntries.size} جلسة تعديل محفوظة)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            dayEntries.forEach { entry ->
                                AutoBackupEntryRow(
                                    entry = entry,
                                    subtitleTime = timeOnlyFormatter.format(Date(entry.timestamp)),
                                    badgeText = "جلسة تعديل",
                                    badgeBg = EmeraldLight,
                                    badgeColor = EmeraldPrimary,
                                    onRestoreClick = { entryPendingRestore = entry },
                                    onShareClick = { shareAutoBackupFile(context, entry) },
                                    onDeleteClick = { entryPendingDelete = entry }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Consolidated 28 Days Archive Section (1 Final File Per Day)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أرشيف الـ 28 يومًا السابقة (نسخة يومية موحدة)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WheatGold
                            )
                            Text(
                                text = "ملف واحد لكل يوم يحتوي على آخر تغييرات اليوم بعد دمج وحذف الجلسات الأقدم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WheatGoldLight
                        ) {
                            Text(
                                text = "${statusSummary.consolidatedDailyEntries.size} / 28 يوم",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WheatGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (statusSummary.consolidatedDailyEntries.isEmpty()) {
                        Text(
                            text = "عند مرور يومين على أي يوم عمل، يتم تلقائيًا اعتماد آخر نسخة في ذلك اليوم ونقلها إلى هذا الأرشيف اليومي (حتى 28 يومًا).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        statusSummary.consolidatedDailyEntries.forEach { entry ->
                            AutoBackupEntryRow(
                                entry = entry,
                                subtitleTime = "آخر تحديث في اليوم: ${timeOnlyFormatter.format(Date(entry.timestamp))}",
                                badgeText = "نسخة اليوم النهائية",
                                badgeBg = WheatGoldLight,
                                badgeColor = WheatGold,
                                onRestoreClick = { entryPendingRestore = entry },
                                onShareClick = { shareAutoBackupFile(context, entry) },
                                onDeleteClick = { entryPendingDelete = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (entryPendingRestore != null) {
        val target = entryPendingRestore!!
        AlertDialog(
            onDismissRequest = { entryPendingRestore = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل تريد استعادة البيانات من نسخة يوم (${target.dayKey})؟\n" +
                        "التفاصيل: ${target.changeSummary}\n\n" +
                        "سيتم استبدال البيانات الحالية بالبيانات المحفوظة في هذه النسخة."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = AutoBackupManager.readBackupFileJson(target)
                        entryPendingRestore = null
                        if (!json.isNullOrBlank()) {
                            onRestoreJson(json)
                            backupActionMessage = "تم استعادة النسخة الاحتياطية ليوم ${target.dayKey} بنجاح ✓"
                        } else {
                            backupActionMessage = "تعذر قراءة ملف النسخة الاحتياطية."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (entryPendingDelete != null) {
        val target = entryPendingDelete!!
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("حذف ملف النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف ملف النسخة الاحتياطية (${target.fileName})؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        AutoBackupManager.deleteBackupEntry(context, target)
                        entryPendingDelete = null
                        statusSummary = AutoBackupManager.getStatusSummary(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun AutoBackupEntryRow(
    entry: AutoBackupEntry,
    subtitleTime: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    onRestoreClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${entry.dayKey} • $subtitleTime",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.changeSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحجم: ${AutoBackupManager.formatFileSize(entry.fileSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onRestoreClick) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استعادة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onShareClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة ملف النسخة",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف النسخة",
                            tint = StatusUnpaidRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun shareAutoBackupFile(context: Context, entry: AutoBackupEntry) {
    try {
        val file = File(entry.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية احسبلي - ${entry.dayKey}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة أو حفظ ملف النسخة الاحتياطية"))
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر مشاركة ملف النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Settings Sub-Tab 2: Dedicated About App & Developer Page (حول البرنامج والمطور)
 */
@Composable
private fun AboutAppAndDeveloperTab(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    var availableUpdate by remember { mutableStateOf(AppUpdateManager.getCachedAvailableUpdate(context)) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Developer & UltraSoft Company Card (Always First at the Top)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ultrasoft_logo),
                                contentDescription = "UltraSoft Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "UltraSoft",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WheatGold
                            )
                            Text(
                                text = "تطوير: Eng. Ahmed Mohamed Attia",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "هذا البرنامج تحت مظلة شركة UltraSoft",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldLight.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• هذا البرنامج تحت مظلة شركة UltraSoft.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "• Developed by Eng. Ahmed Mohamed Attia (01140409832).",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "• محمي بنظام التوقيع الرقمي المشفر (UltraSoft Authority Engine - Ed25519).",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val msg = "السلام عليكم Eng. Ahmed Mohamed Attia، بخصوص برنامج احسبلي (UltraSoft)"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/201140409832?text=" + Uri.encode(msg))
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التواصل مع المطور عبر واتساب")
                    }
                }
            }
        }

        // 2. App Update Notification & Download System Card (مرتبط تلقائيًا بمستودع Ahm3d0x/Grain-Trade-Manager)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (availableUpdate != null) 2.dp else 1.dp,
                        color = if (availableUpdate != null) WheatGold else WheatCardBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (availableUpdate != null) WheatGoldLight else EmeraldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdateAlt,
                                    contentDescription = null,
                                    tint = if (availableUpdate != null) WheatGold else EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تحديثات البرنامج والإصدارات",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "الإصدار المثبت حاليًا: v${AppUpdateManager.currentVersionName} (Version ${AppUpdateManager.currentVersionCode})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (availableUpdate != null) StatusPartialBg else StatusPaidBg
                        ) {
                            Text(
                                text = if (availableUpdate != null) {
                                    "تحديث v${availableUpdate!!.versionName} متاح 🔔"
                                } else {
                                    "محدّث لأحدث إصدار ✓"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (availableUpdate != null) StatusPartialAmber else StatusPaidGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (availableUpdate != null) {
                        val upd = availableUpdate!!
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WheatGoldLight.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WheatGold, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎉 يتوفر إصدار جديد للبرنامج: الإصدار ${upd.versionName} (رقم ${upd.versionCode})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                if (upd.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = "ما الجديد في التحديث:\n${upd.releaseNotes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = {
                                        AppUpdateManager.openDownloadUrl(context, upd.downloadUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تنزيل التحديث الجديد الآن (v${upd.versionName})",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    updateStatusText?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }

                    Button(
                        onClick = {
                            isCheckingUpdate = true
                            updateStatusText = null
                            coroutineScope.launch {
                                val result = AppUpdateManager.checkForUpdates(context)
                                isCheckingUpdate = false
                                when (result) {
                                    is UpdateCheckResult.UpdateAvailable -> {
                                        availableUpdate = result.info
                                        updateStatusText = "تم العثور على تحديث جديد: الإصدار ${result.info.versionName}!"
                                    }
                                    is UpdateCheckResult.UpToDate -> {
                                        availableUpdate = null
                                        updateStatusText = "أنت تستخدم أحدث إصدار حاليًا (v${result.currentVersionName})."
                                    }
                                    is UpdateCheckResult.Error -> {
                                        updateStatusText = result.message
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارِ التحقق من وجود تحديثات...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فحص وجود تحديثات جديدة الآن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. What's New in Version 3.3 Documentation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, WheatGold, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "توثيق مميزات الإصدار الجديد (v3.3)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = WheatGoldLight
                        ) {
                            Text(
                                text = "الإصدار 3.3",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WheatGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val v3ReleaseHighlights = listOf(
                        "ربط نوع الحبوب بوحدة التسعير تلقائيًا (جديد v3.3): عند تعريف أو تعديل أي نوع حبوب يمكنك تحديد وحدة التسعير المرتبطة به ليتم اختيارها تلقائيًا فور اختيار المحصول في الفواتير.",
                        "حقل سعر الوحدة فارغ دائمًا وواضح فوق الكيبورد (جديد v3.3): إلغاء السعر الثابت (4000) نهائيًا ليبدأ حقل السعر فارغًا دائمًا مع تحسين استجابة الشاشة للوحة المفاتيح.",
                        "إخفاء قائمة العملاء تلقائيًا بعد الاختيار (جديد v3.3): عند اختيار العميل تُطوى القائمة تلقائيًا في بطاقة أنيقة ومضغوطة لتوفير مساحة الشاشة مع زر لتغيير العميل بضغطة واحدة.",
                        "توثيق بيانات المطور في شاشة الدخول والفواتير المطبوعة (جديد v3.3): عرض بيانات الشركة والمطور أعلى صفحة معلومات البرنامج، وتذييل أنيق باسم المطور ورقم التواصل أسفل جميع فواتير الـ PDF المطبوعة وشاشة تسجيل الدخول.",
                        "نظام قفل وحماية البرنامج المتكامل (v3.2): حماية بياناتك وفواتيرك عبر 3 طرق مرنة (PIN أو نقش أمان أو بصمة الهاتف) مع إمكانية دمج أكثر من طريقة معًا.",
                        "سلكتور المحصول والبحث المتطور والشونة والرسوم البيانية (v3.1): متابعة لحظية للمخزن الحي والأرباح والنسخ الاحتياطي التلقائي (30 يومًا)."
                    )

                    v3ReleaseHighlights.forEach { itemText ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WheatGold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Text(
                                text = itemText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. App Identity Card (Under UltraSoft Umbrella)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(84.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF083926),
                        shadowElevation = 4.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ehsebly_app_icon_fg_1794758461550),
                            contentDescription = "أيقونة برنامج احسبلي",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "برنامج احسبلي",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EmeraldLight
                    ) {
                        Text(
                            text = "UltraSoft ERP • الإصدار ${AppUpdateManager.currentVersionName} (بناء رقم ${AppUpdateManager.currentVersionCode})",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "أحد الأنظمة الرقمية المتكاملة الصادرة تحت مظلة شركة UltraSoft. صُمم خصيصًا لإدارة الدورة الكاملة لتجارة الحبوب والغلال والشُّوَن: بدءًا من الشراء والوزن بالتجميع الخماسي الآلي، مرورًا بإدارة المخزن الحي (الشونة) وتسويات الجفاف والغربلة، وصولًا إلى فواتير البيع والتوريد للمطاحن وحساب صافي الربح بالرسوم البيانية التفاعلية وإصدار فواتير A4 جاهزة للطباعة والمشاركة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // 4. Comprehensive, Organized System Capabilities & Features
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مميزات وإمكانيات البرنامج بالتفصيل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    val featureGroups = listOf(
                        "1. نظام الوزن والتجميع الخماسي الآلي" to listOf(
                            "لوحة أرقام سريعة مصممة لإدخال الأوزان المتتالية بيد واحدة مع وضع زر (+ إضافة) في أقرب مكان للأصابع.",
                            "تجميع تلقائي لكل 5 أوزان (5 شكاير) في عمود مستقل مع حساب مجموع كل عمود والوزن الصافي الكلي بالكيلوجرام فورًا.",
                            "إمكانية تعديل أو حذف أي وزنة داخل أي عمود في أي وقت مع إعادة الحساب الآلي لكافة المجاميع."
                        ),
                        "2. إدارة الفواتير والأقسام المتعددة والتعليق" to listOf(
                            "تقسيم الفاتورة الواحدة إلى عدة أقسام مستقلة (مثل قسم قمح، قسم ذرة، قسم فول) لكل قسم وحدة تسعير وسعر خاص به.",
                            "حفظ تلقائي لحظي للفاتورة والأوزان أثناء التسجيل بحيث لا تضيع أي بيانات في حال الخروج من البرنامج أو انغلاقه.",
                            "ميزة تعليق الفاتورة الحالية وبدء فاتورة جديدة لعميل آخر، مع إمكانية التبديل بين الفواتير المعلقة واستكمالها.",
                            "تفريغ تلقائي لشاشة الوزن بعد الضغط على حفظ واعتماد الفاتورة لتكون جاهزة لاستقبال فاتورة جديدة فورًا."
                        ),
                        "3. شاشة المخزن الحي (الشونة) وفواتير البيع والتوريد" to listOf(
                            "متابعة لحظية لرصيد الشونة من كل محصول بالكيلوجرام والطن والوحدة القياسية (الأردب) مع حساب متوسط تكلفة الشراء المرجح.",
                            "إصدار فواتير بيع وتوريد للمطاحن وتجار الجملة بنظام الوزن الكلي المقطوع دون الحاجة لإدخال شكاير فردية.",
                            "خصم آلي للكميات المباعة من رصيد الشونة وحساب فوري لصافي مكسب التاجر في كل نقلة بيع مع تسجيل الدفعة المحصلة."
                        ),
                        "4. معالجة عجز الجفاف والغربلة ونظامي حساب الأرباح" to listOf(
                            "تسوية جفاف يدوية في أي وقت لخصم وزن الرطوبة أو الغربلة من رصيد المحصول مع توثيق السبب والتاريخ.",
                            "زر تصفير الدفعة عند نفاد المخزن فعليًا لتحويل الرصيد المتبقي دفتريًا إلى عجز جفاف وحساب نسبة الهالك المئوية بدقة.",
                            "نظامان مرنان لحساب صافي الربح يحددهما التاجر من الإعدادات: إما (شامل مصاريف النقلة من نقل ومشال وعمولة) أو (فرق السعر المباشر)."
                        ),
                        "5. الرسوم البيانية التفاعلية لأرباح الحبوب والتقارير" to listOf(
                            "رسوم بيانية تفاعلية توضح إجمالي صافي الربح لكل نوع من أنواع الحبوب (أعمدة الأرباح، مقارنة الإيراد والتكلفة، ونسب المساهمة الدائرية).",
                            "تحديد الفترة الزمنية بمرونة عالية عبر الفلاتر السريعة أو اختيار فترة مخصصة بالتاريخ (من تاريخ – إلى تاريخ).",
                            "إمكانية الضغط على أي محصول في الرسم البياني لعرض بطاقة فحص تفصيلية (الوزن المباع، الإيراد، التكلفة، المصاريف، متوسط الربح للطن، وهامش الربح)."
                        ),
                        "6. الطباعة الاحترافية وتصدير الـ PDF والواتساب" to listOf(
                            "تصميم فاتورة A4 رسمية (للشراء أو البيع/التوريد) تستوعب حتى 10 أعمدة أوزان جنبًا إلى جنب مع توزيع متساوٍ لعرض الأعمدة.",
                            "ترويسة فاتحة متوافقة تمامًا مع طابعات الأبيض والأسود والليزر، مع حماية الجداول من الانقسام بين الصفحات.",
                            "تسمية ملف الفاتورة تلقائيًا باسم: (اسم العميل - نوع الحبوب - التاريخ).",
                            "حفظ الفاتورة مباشرة على الهاتف في مجلد التنزيلات أو إرسالها مباشرة عبر واتساب أو طباعتها بضغطة واحدة."
                        ),
                        "7. إدارة العملاء وكشوف الحساب والمدفوعات" to listOf(
                            "حسابات دقيقة بالأرقام الصحيحة (بالكيلوجرام الصافي وبدون قروش أو كسور عشرية مزعجة).",
                            "سجل متكامل لكل عميل أو مطحن يعرض جميع الفواتير، وإجمالي الأوزان، والمبالغ المستحقة، والمدفوعات، والمتبقي.",
                            "القدرة على فتح أي فاتورة من سجل العميل لتعديلها أو طباعتها أو إرسالها عبر واتساب أو حذفها مع تأكيد الحذف.",
                            "تسجيل دفعات السداد الكلي أو الجزئي (نقدي، تحويل بنكي، محفظة إلكترونية، شيك) وتسوية الفروق."
                        ),
                        "8. النسخ الاحتياطي التلقائي الذكي (30 يومًا) والتحديثات" to listOf(
                            "نسخ احتياطي تلقائي عند إغلاق التطبيق في حال حدوث أي تغيير في البيانات داخل مجلد مخصص على الجهاز (Ehsebly_AutoBackups).",
                            "الاحتفاظ بجميع نسخ وجلسات التعديل الكاملة لآخر يومين، وعند اليوم الثالث يتم اعتماد آخر نسخة فقط من اليوم وحذف الجلسات الأقدم ليكون هناك 28 ملفًا لـ 28 يومًا توفيرًا للمساحة.",
                            "العمل الكامل بدون إنترنت (Offline-First) مع إمكانية استعادة أو مشاركة أي نسخة من سجل الـ 30 يومًا بضغطة واحدة، ونظام إشعارات تلقائي للتحديثات."
                        )
                    )

                    featureGroups.forEachIndexed { idx, (groupTitle, itemsList) ->
                        if (idx > 0) {
                            HorizontalDivider(color = WheatCardBorder.copy(alpha = 0.6f))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = groupTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            itemsList.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(15.dp)
                                    )
                                    Text(
                                        text = feat,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
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

private fun calculateTotalDurationDays(issuedAt: String, expiresAt: String): Long {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )
    fun parse(str: String): Long {
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ROOT)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val d = sdf.parse(str)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }
    val start = parse(issuedAt)
    val end = parse(expiresAt)
    if (start <= 0L || end <= start) return 365L
    return ((end - start) / (1000L * 60 * 60 * 24)).coerceAtLeast(1L)
}
