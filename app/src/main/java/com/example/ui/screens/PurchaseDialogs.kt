package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AuditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceSectionEntity
import com.example.data.local.PricingUnitEntity
import com.example.data.local.ProductEntity
import com.example.data.local.WeightEntity
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusSettlementBg
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddOrEditSectionDialog(
    isEdit: Boolean,
    existingSection: InvoiceSectionEntity? = null,
    products: List<ProductEntity>,
    pricingUnits: List<PricingUnitEntity>,
    isApprovedInvoice: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (ProductEntity, PricingUnitEntity, Double, String?) -> Unit,
    onDeleteSection: ((String?) -> Unit)? = null
) {
    val activeProducts = remember(products) { products.filter { it.isActive }.ifEmpty { products } }
    val activeUnits = remember(pricingUnits) { pricingUnits.filter { it.isActive }.ifEmpty { pricingUnits } }

    var selectedProduct by remember {
        mutableStateOf(
            activeProducts.find { it.id == existingSection?.productId }
                ?: activeProducts.firstOrNull()
                ?: ProductEntity(id = 1, name = "قمح")
        )
    }
    var selectedUnit by remember {
        mutableStateOf(
            activeUnits.find { it.id == existingSection?.unitId }
                ?: activeUnits.find { it.linkedProductId == selectedProduct.id }
                ?: activeUnits.firstOrNull()
                ?: PricingUnitEntity(id = 1, name = "طن", weightKg = 1000.0)
        )
    }
    var priceText by remember {
        mutableStateOf(existingSection?.unitPrice?.let { CalculationEngine.formatPlainInt(it) } ?: "4000")
    }
    var auditReason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "إعدادات وتسعير القسم ${existingSection?.sectionOrder ?: ""}" else "إضافة قسم جديد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "نوع الحبوب:",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeProducts) { prod ->
                        FilterChip(
                            selected = selectedProduct.id == prod.id,
                            onClick = {
                                selectedProduct = prod
                                activeUnits.find { it.linkedProductId == prod.id }?.let { linked ->
                                    selectedUnit = linked
                                }
                            },
                            label = { Text(prod.name, softWrap = false, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text(
                    text = "وحدة التسعير:",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeUnits) { unit ->
                        FilterChip(
                            selected = selectedUnit.id == unit.id,
                            onClick = { selectedUnit = unit },
                            label = {
                                Text(
                                    text = "${unit.name} (${CalculationEngine.formatWeight(unit.weightKg)} كجم)",
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

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; errorMsg = null },
                    label = { Text("سعر ${selectedUnit.name} ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("section_unit_price_input")
                )

                val previewPrice = priceText.toDoubleOrNull() ?: 0.0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "التسعير: ${CalculationEngine.formatCurrency(previewPrice, currencySymbol)} لكل ${selectedUnit.name} (${CalculationEngine.formatWeight(selectedUnit.weightKg)} كجم)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (isApprovedInvoice) {
                    OutlinedTextField(
                        value = auditReason,
                        onValueChange = { auditReason = it },
                        label = { Text("سبب التعديل (اختياري)") },
                        placeholder = { Text("يمكن تركه فارغًا") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("section_audit_reason_input")
                    )
                }

                errorMsg?.let {
                    Text(
                        text = it,
                        color = StatusUnpaidRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isEdit && onDeleteSection != null) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = {
                            val reason = if (isApprovedInvoice) {
                                auditReason.ifBlank { "حذف قسم" }
                            } else null
                            onDeleteSection(reason)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusUnpaidRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف هذا القسم")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (price == null || price <= 0.0) {
                        errorMsg = "يرجى إدخال سعر صحيح أكبر من صفر"
                        return@Button
                    }
                    val reason = if (isApprovedInvoice) {
                        auditReason.ifBlank { "تعديل إعدادات القسم" }
                    } else null
                    onConfirm(selectedProduct, selectedUnit, price, reason)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_section_dialog_button")
            ) {
                Text(if (isEdit) "حفظ" else "إضافة القسم")
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
fun EditOrDeleteWeightDialog(
    weight: WeightEntity,
    sectionOrder: Int,
    isApprovedInvoice: Boolean,
    onDismiss: () -> Unit,
    onUpdateWeight: (Double, String?) -> Unit,
    onDeleteWeight: (String?) -> Unit
) {
    var weightText by remember { mutableStateOf(CalculationEngine.formatPlainInt(weight.weightKg)) }
    var auditReason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "شكارة #${weight.sequenceNumber} (القسم $sectionOrder)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it; errorMsg = null },
                    label = { Text("الوزن الصافي بالكيلوجرام") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_weight_input")
                )

                if (isApprovedInvoice) {
                    OutlinedTextField(
                        value = auditReason,
                        onValueChange = { auditReason = it },
                        label = { Text("سبب التعديل أو الحذف (اختياري)") },
                        placeholder = { Text("يمكن تركه فارغًا") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("weight_audit_reason_input")
                    )
                }

                errorMsg?.let {
                    Text(text = it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = {
                        val reason = if (isApprovedInvoice) {
                            auditReason.ifBlank { "حذف وزن" }
                        } else null
                        onDeleteWeight(reason)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusUnpaidRed),
                    modifier = Modifier.fillMaxWidth().testTag("delete_weight_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف الشكارة #${weight.sequenceNumber}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newW = weightText.toDoubleOrNull()
                    if (newW == null || newW <= 0.0) {
                        errorMsg = "يرجى إدخال وزن صحيح أكبر من صفر"
                        return@Button
                    }
                    val reason = if (isApprovedInvoice) {
                        auditReason.ifBlank { "تعديل وزن" }
                    } else null
                    onUpdateWeight(newW, reason)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_weight_edit_button")
            ) {
                Text("حفظ التعديل")
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
fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    currentCustomerId: Long?,
    currentNotes: String,
    isApprovedInvoice: Boolean,
    onDismiss: () -> Unit,
    onSelectCustomerAndNotes: (CustomerEntity?, String, String?) -> Unit,
    onQuickCreateCustomer: (CustomerEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember {
        mutableStateOf(customers.find { it.id == currentCustomerId })
    }
    var notesText by remember { mutableStateOf(currentNotes) }
    var auditReason by remember { mutableStateOf("") }
    var showNewCustomerForm by remember { mutableStateOf(false) }
    var newCustName by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }

    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختيار العميل وملاحظات الفاتورة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!showNewCustomerForm) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("بحث باسم العميل أو رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            val isAnonymous = selectedCustomer == null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAnonymous) EmeraldLight else Color.Transparent)
                                    .border(1.dp, WheatCardBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedCustomer = null }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "عميل نقدي (بدون تسجيل)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isAnonymous) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        items(filtered, key = { it.id }) { cust ->
                            val isSelected = selectedCustomer?.id == cust.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) EmeraldLight else Color.Transparent)
                                    .border(1.dp, WheatCardBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedCustomer = cust }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = cust.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (cust.phone.isNotBlank()) {
                                        Text(
                                            text = cust.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (cust.creditBalance > 0) {
                                    Text(
                                        text = "دائن: ${CalculationEngine.formatNumber(cust.creditBalance)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusSettlementPurple
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showNewCustomerForm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة عميل جديد")
                    }
                } else {
                    Text(
                        text = "تسجيل عميل جديد:",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text("اسم العميل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (newCustName.isNotBlank()) {
                                    val c = CustomerEntity(name = newCustName.trim(), phone = newCustPhone.trim())
                                    onQuickCreateCustomer(c)
                                    showNewCustomerForm = false
                                    newCustName = ""
                                    newCustPhone = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("حفظ العميل")
                        }
                        TextButton(onClick = { showNewCustomerForm = false }) {
                            Text("رجوع")
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات الفاتورة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isApprovedInvoice) {
                    OutlinedTextField(
                        value = auditReason,
                        onValueChange = { auditReason = it },
                        label = { Text("سبب التعديل (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason = if (isApprovedInvoice) auditReason.ifBlank { null } else null
                    onSelectCustomerAndNotes(selectedCustomer, notesText, reason)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("حفظ")
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
fun RecordPaymentDialog(
    invoiceNumber: String,
    totalInvoiceAmount: Double,
    netPaidAmount: Double,
    remainingAmount: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onRecordPayment: (Double, String, String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (remainingAmount > 0) CalculationEngine.formatPlainInt(remainingAmount) else "")
    }
    var method by remember { mutableStateOf("نقدي") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val methods = listOf("نقدي", "تحويل بنكي", "فودافون كاش / محفظة", "شيك")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل دفعة سداد — فاتورة #$invoiceNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("إجمالي الفاتورة: ${CalculationEngine.formatCurrency(totalInvoiceAmount, currencySymbol)}")
                        Text("المدفوع سابقًا: ${CalculationEngine.formatCurrency(netPaidAmount, currencySymbol)}")
                        Text(
                            text = "المتبقي المستحق: ${CalculationEngine.formatCurrency(remainingAmount, currencySymbol)}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("المبلغ المدفوع ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text("طريقة الدفع:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(methods) { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m, softWrap = false, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الدفعة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(text = it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    onRecordPayment(amt, method, notes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("تسجيل الدفعة")
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
fun OverpaymentSettlementDialog(
    invoiceNumber: String,
    overpaidAmount: Double,
    hasRegisteredCustomer: Boolean,
    customerName: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSettle: (Boolean, String) -> Unit
) {
    var notes by remember { mutableStateOf("تسوية فرق الفاتورة بعد التعديل") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusSettlementPurple)
                Text(
                    text = "تسوية فائض الفاتورة #$invoiceNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusSettlementBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "المبلغ الزائد عن قيمة الفاتورة الحالية:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = CalculationEngine.formatCurrency(overpaidAmount, currencySymbol),
                            style = MaterialTheme.typography.headlineMedium,
                            color = StatusSettlementPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("بيان التسوية (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSettle(false, notes)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("settle_refund_cash_button")
                ) {
                    Text("1. تسجيل عملية رد نقدي للفائض")
                }

                if (hasRegisteredCustomer) {
                    Button(
                        onClick = {
                            onSettle(true, notes)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSettlementPurple),
                        modifier = Modifier.fillMaxWidth().testTag("settle_customer_credit_button")
                    ) {
                        Text("2. ترحيل الفائض كرصيد دائن للعميل ($customerName)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لاحقًا")
            }
        }
    )
}

@Composable
fun AuditLogsDialog(
    invoiceNumber: String,
    logs: List<AuditLogEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.forLanguageTag("ar-EG")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = "سجل التعديلات — #$invoiceNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (logs.isEmpty()) {
                Text("لا توجد تعديلات مسجلة على هذه الفاتورة حتى الآن.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.actionType,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "الموضع: ${log.targetSection}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "قبل: ${log.oldValue}  ←  بعد: ${log.newValue}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (log.reason.isNotBlank()) {
                                    Text(
                                        text = "السبب: ${log.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WheatGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
