package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomerEntity
import com.example.domain.CalculationEngine
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusSettlementBg
import com.example.ui.theme.StatusSettlementPurple
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.viewmodel.InvoiceWithCalc
import com.example.util.PdfInvoiceExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(
    customers: List<CustomerEntity>,
    invoices: List<InvoiceWithCalc>,
    settings: AppSettingsEntity,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onStartPurchaseForCustomer: (CustomerEntity) -> Unit,
    onOpenInvoice: (Long) -> Unit,
    onDeleteInvoice: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val currencySymbol = settings.currencySymbol
    var searchQuery by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var historyCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var invoiceToDeleteInHistory by remember { mutableStateOf<InvoiceWithCalc?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery) ||
                it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("customers_screen"),
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
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل الموردين والعملاء (${customers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.testTag("add_customer_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة عميل")
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("بحث بالاسم أو رقم الهاتف أو العنوان") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("customer_search_input")
                    )
                }
            }
        }

        items(filteredCustomers, key = { it.id }) { cust ->
            val customerInvoices = remember(invoices, cust.id) {
                invoices.filter { it.details.invoice.customerId == cust.id }
            }
            val totalWeight = customerInvoices.sumOf { it.calc.totalWeightKg }
            val totalPurchasesAmount = customerInvoices.sumOf { it.calc.totalInvoiceAmount }
            val totalRemaining = customerInvoices.sumOf { it.calc.remainingAmount }

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
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cust.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (cust.phone.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                                    Text(
                                        text = cust.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (cust.address.isNotBlank()) {
                                Text(
                                    text = cust.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (cust.creditBalance > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusSettlementBg
                            ) {
                                Text(
                                    text = "رصيد دائن: ${CalculationEngine.formatCurrency(cust.creditBalance, currencySymbol)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusSettlementPurple,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Customer Summary Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("عدد الفواتير", style = MaterialTheme.typography.bodySmall)
                                Text("${customerInvoices.size} عملية", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("إجمالي التوريد", style = MaterialTheme.typography.bodySmall)
                                Text("${CalculationEngine.formatWeight(totalWeight)} كجم", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("إجمالي القيمة", style = MaterialTheme.typography.bodySmall)
                                Text(CalculationEngine.formatCurrency(totalPurchasesAmount, currencySymbol), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("المتبقي له", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = CalculationEngine.formatCurrency(totalRemaining, currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalRemaining > 0) StatusUnpaidRed else StatusPaidGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onStartPurchaseForCustomer(cust) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("شراء جديد", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { historyCustomer = cust },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سجل المشتريات (${customerInvoices.size})", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row {
                            IconButton(onClick = { editingCustomer = cust }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل العميل", tint = EmeraldPrimary)
                            }
                            IconButton(
                                onClick = { customerToDelete = cust },
                                modifier = Modifier.testTag("delete_customer_${cust.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = StatusUnpaidRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Customer Form Dialog
    if (showAddDialog || editingCustomer != null) {
        CustomerFormDialog(
            initialCustomer = editingCustomer,
            onDismiss = {
                showAddDialog = false
                editingCustomer = null
            },
            onSave = { saved ->
                onSaveCustomer(saved)
                showAddDialog = false
                editingCustomer = null
            }
        )
    }

    // Customer Deletion Confirmation Dialog
    customerToDelete?.let { cust ->
        val linkedCount = invoices.count { it.details.invoice.customerId == cust.id }
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = {
                Text(
                    text = "تأكيد حذف العميل: ${cust.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (linkedCount > 0) {
                    Text("هذا العميل مرتبط بـ $linkedCount فاتورة مسجلة. لحذف العميل يجب أولاً حذف فواتيره من سجل المشتريات حفاظًا على دقة الحسابات.")
                } else {
                    Text("هل أنت متأكد من حذف بيانات العميل «${cust.name}» نهائيًا؟")
                }
            },
            confirmButton = {
                if (linkedCount == 0) {
                    Button(
                        onClick = {
                            onDeleteCustomer(cust)
                            customerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed)
                    ) {
                        Text("نعم، احذف العميل")
                    }
                } else {
                    Button(
                        onClick = {
                            val target = cust
                            customerToDelete = null
                            historyCustomer = target
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("عرض فواتير العميل")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Full Customer Purchase History Dialog with Complete Actions (Open/Edit, Print, Share WhatsApp/PDF, Delete)
    historyCustomer?.let { cust ->
        val custInvoices = invoices.filter { it.details.invoice.customerId == cust.id }
        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar-EG")) }

        Dialog(
            onDismissRequest = { historyCustomer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سجل فواتير العميل: ${cust.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إجمالي الفواتير: ${custInvoices.size} عملية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { historyCustomer = null }) {
                            Text("إغلاق")
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    if (custInvoices.isEmpty()) {
                        Text(
                            text = "لا توجد فواتير مسجلة لهذا العميل حتى الآن.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 460.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(custInvoices, key = { it.details.invoice.id }) { item ->
                                val inv = item.details.invoice
                                val calc = item.calc
                                val prods = calc.productSummaries.joinToString(" ، ") {
                                    "${it.productName} (${CalculationEngine.formatWeight(it.totalWeightKg)} كجم)"
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.background,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp))
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
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = EmeraldLight) {
                                                    Text(
                                                        text = "فاتورة #${inv.invoiceNumber}",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = EmeraldPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                InvoiceStatusBadge(status = inv.status)
                                            }
                                            PaymentStatusBadge(paymentStatus = calc.paymentStatus)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = dateFormat.format(Date(inv.invoiceDate)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = prods.ifBlank { "بدون أوزان" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = EmeraldPrimary,
                                                    fontWeight = FontWeight.Bold
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
                                                    text = "${calc.totalBagCount} شكارة • المتبقي: ${CalculationEngine.formatNumber(calc.remainingAmount)}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = WheatCardBorder)

                                        // Direct Actions for each Invoice in Customer History:
                                        // Open & Edit | WhatsApp Share | PDF | Print | Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    historyCustomer = null
                                                    onOpenInvoice(inv.id)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                                modifier = Modifier.weight(1.2f).height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("فتح وتعديل", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    PdfInvoiceExporter.shareInvoicePdf(
                                                        context = context,
                                                        details = item.details,
                                                        settings = settings,
                                                        whatsAppOnly = true
                                                    )
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إرسال", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    PdfInvoiceExporter.printInvoicePdf(
                                                        context = context,
                                                        details = item.details,
                                                        settings = settings
                                                    )
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = null, tint = WheatGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("طباعة", style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
                                            }

                                            OutlinedButton(
                                                onClick = { invoiceToDeleteInHistory = item },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = StatusUnpaidBg.copy(alpha = 0.3f),
                                                    contentColor = StatusUnpaidRed
                                                ),
                                                modifier = Modifier.height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف الفاتورة", modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog when deleting an invoice from inside the Customer's History
    invoiceToDeleteInHistory?.let { target ->
        AlertDialog(
            onDismissRequest = { invoiceToDeleteInHistory = null },
            title = {
                Text(
                    text = "تأكيد حذف الفاتورة #${target.details.invoice.invoiceNumber}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("هل أنت متأكد من حذف هذه الفاتورة من سجل العميل؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInvoice(target.details.invoice.id)
                        invoiceToDeleteInHistory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed)
                ) {
                    Text("نعم، احذف الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDeleteInHistory = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun CustomerFormDialog(
    initialCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCustomer == null) "إضافة عميل جديد" else "تعديل بيانات العميل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("اسم العميل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
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
                    if (name.isBlank()) {
                        errorMsg = "يرجى إدخال اسم العميل"
                        return@Button
                    }
                    onSave(
                        (initialCustomer ?: CustomerEntity(name = "", phone = "")).copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_customer_dialog_button")
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
