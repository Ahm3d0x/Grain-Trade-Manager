package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
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
import com.example.util.UpdateCheckResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
    onSavePricingUnit: (PricingUnitEntity) -> Unit,
    onSaveSettings: (AppSettingsEntity) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String) -> Unit
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
                    LicenseDetailsSettingsTab(context = context)
                }

                1 -> {
                    BackupAndRestoreSettingsTab(
                        onExportClick = {
                            onExportBackup { json ->
                                pendingBackupJson = json
                                createBackupLauncher.launch("grain_trade_backup_${System.currentTimeMillis()}.json")
                            }
                        },
                        onImportClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
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
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var desc by remember { mutableStateOf(editingProduct?.description ?: "") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddProduct = false; editingProduct = null },
            title = { Text(if (editingProduct == null) "إضافة نوع حبوب جديد" else "تعديل نوع الحبوب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; err = null },
                        label = { Text("اسم المحصول (مثال: سمسم، كراوية، ذرة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("وصف اختياري") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    err?.let { Text(it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            err = "يرجى إدخال اسم المحصول"
                            return@Button
                        }
                        onSaveProduct(
                            (editingProduct ?: ProductEntity(name = "")).copy(
                                name = name.trim(),
                                description = desc.trim()
                            )
                        )
                        showAddProduct = false
                        editingProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                                    invoiceFooterNotes = footerNotes.trim()
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
private fun LicenseDetailsSettingsTab(context: Context) {
    var showUpdateLicenseDialog by remember { mutableStateOf(false) }
    var licenseResult by remember { mutableStateOf(LicenseManager.checkLicenseStatus(context)) }
    val currentMachineId = remember { HardwareIdProvider.getHardwareId(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(WheatGoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = WheatGold)
                        }
                        Column {
                            Text(
                                text = "النسخ الاحتياطي واستعادة البيانات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "حفظ واسترجاع قاعدة البيانات بالكامل بدون إنترنت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "يمكنك تصدير نسخة احتياطية شاملة تحتوي على جميع العملاء، الفواتير، الأوزان، المدفوعات، أنواع الحبوب، ووحدات التسعير إلى ملف على هاتفك، واستعادتها في أي وقت أو عند نقل التطبيق لهاتف آخر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير نسخة احتياطية", softWrap = false, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onImportClick,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة نسخة", softWrap = false, maxLines = 1)
                        }
                    }
                }
            }
        }
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
        // 1. App Identity Card (Under UltraSoft Umbrella)
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
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                brush = Brush.radialGradient(listOf(WheatGold, EmeraldPrimary)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "برنامج إدارة تجارة الحبوب والغلال",
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
                        text = "أحد الأنظمة الرقمية المتكاملة الصادرة تحت مظلة شركة UltraSoft. صُمم خصيصًا لإدارة عمليات شراء وتوريد ووزن الحبوب والغلال والشُّوَن بدقة وسرعة فائقة، مع التجميع الخماسي الآلي للأوزان، وتقسيم الفاتورة لأكثر من قسم وتسعير، وإصدار فواتير A4 جاهزة للطباعة والمشاركة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
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

        // 3. Developer & UltraSoft Company Card (Eng. Ahmed Mohamed Attia — Founder & CEO of UltraSoft)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EmeraldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Founder, CEO & Lead Software Engineer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Eng. Ahmed Mohamed Attia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "UltraSoft Software & Enterprise Solutions",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = WheatGold
                            )
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    Text(
                        text = "هذا البرنامج أحد منتجات شركة UltraSoft للبرمجيات وحلول الأعمال الرقمية، والتي أسسها ويديرها المهندس أحمد محمد عطية (Eng. Ahmed Mohamed Attia)، حيث تندرج كافة البرامج والأنظمة المحاسبية والإدارية المصممة تحت مظلة UltraSoft.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )

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
                                text = "• جميع حقوق الملكية الفكرية والتطوير محفوظة لشركة UltraSoft.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "• Designed & Engineered by Eng. Ahmed Mohamed Attia (Founder & CEO of UltraSoft).",
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
                            val msg = "السلام عليكم Eng. Ahmed Mohamed Attia، بخصوص برنامج إدارة تجارة الحبوب والغلال (UltraSoft)"
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
                        Text("التواصل مع المطور (UltraSoft) عبر واتساب")
                    }
                }
            }
        }

        // 4. Key System Capabilities Summary
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "أبرز إمكانيات ومميزات النظام",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val features = listOf(
                        "التجميع الخماسي الآلي للأوزان في أعمدة منظمة بدون أخطاء يدوية",
                        "تقسيم الفاتورة الواحدة إلى عدة أقسام مستقلة بنوع حبوب وسعر مختلف",
                        "حفظ تلقائي مستمر للبيانات وميزة تعليق الفواتير واستئنافها",
                        "تصدير وحفظ الفواتير باسم العميل ونوع الحبوب والتاريخ تلقائيًا",
                        "تقارير وإحصائيات شاملة للأصناف والعملاء والحركة اليومية والمدفوعات",
                        "العمل الكامل بدون إنترنت (Offline-First) مع النسخ الاحتياطي والاستعادة"
                    )

                    features.forEach { feat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = feat,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
