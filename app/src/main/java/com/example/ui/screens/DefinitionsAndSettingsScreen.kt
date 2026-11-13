package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold

@Composable
fun DefinitionsAndSettingsScreen(
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    settings: AppSettingsEntity,
    onSaveProduct: (ProductEntity) -> Unit,
    onSavePricingUnit: (PricingUnitEntity) -> Unit,
    onSaveSettings: (AppSettingsEntity) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onImportBackup: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("أنواع الحبوب", "وحدات التسعير", "الإعدادات والنسخ")

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
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("def_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Grain Products Tab
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
                // Pricing Units Tab
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
                // Settings & Offline Backup Tab
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
                                    text = "بيانات التاجر وإعدادات الفواتير",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = merchantName,
                                    onValueChange = { merchantName = it },
                                    label = { Text("اسم الشركة أو التاجر (يظهر في ترويسة PDF)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = merchantPhone,
                                    onValueChange = { merchantPhone = it },
                                    label = { Text("رقم هاتف التاجر") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = merchantAddress,
                                    onValueChange = { merchantAddress = it },
                                    label = { Text("عنوان الشونة / المضرب") },
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
                                    label = { Text("ملاحظات أسفل الفاتورة المطبوعة") },
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
                                    Text("حفظ الإعدادات")
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
                                    text = "النسخ الاحتياطي واستعادة البيانات (Offline Backup)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "احفظ نسخة احتياطية كاملة من جميع العملاء والفواتير والأوزان وسجل التعديلات على هاتفك، أو استعدها في أي وقت بدون إنترنت.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onExportBackup { json ->
                                                pendingBackupJson = json
                                                createBackupLauncher.launch("grain_trade_backup_${System.currentTimeMillis()}.json")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WheatGold),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Backup, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تصدير نسخة احتياطية")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("استعادة نسخة")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
        var weightText by remember { mutableStateOf(editingUnit?.weightKg?.toString() ?: "150") }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
