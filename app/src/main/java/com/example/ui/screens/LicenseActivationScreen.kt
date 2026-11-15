package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.HardwareIdProvider
import com.example.security.LicenseCertificate
import com.example.security.LicenseManager
import com.example.security.LicenseValidationResult
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold

private const val DEVELOPER_WHATSAPP_INTL = "201140409832"

@Composable
fun LicenseActivationScreen(
    initialStatus: LicenseValidationResult = LicenseValidationResult.NotActivated,
    onActivationSuccess: (LicenseCertificate) -> Unit
) {
    val context = LocalContext.current
    val machineId = remember { HardwareIdProvider.getHardwareId(context) }
    var licenseInput by remember { mutableStateOf("") }
    var errorMessage by remember {
        mutableStateOf(
            when (initialStatus) {
                is LicenseValidationResult.Expired -> "انتهت فترة صلاحية الترخيص السابق (${initialStatus.expiredAt}). يرجى تجديد الترخيص."
                is LicenseValidationResult.MachineMismatch -> "الترخيص السابق صادر لجهاز آخر (${initialStatus.expectedMachineId})."
                is LicenseValidationResult.InvalidSignature -> "كود الترخيص السابق غير معتمد أو تم التعديل عليه."
                is LicenseValidationResult.AppMismatch -> "الترخيص السابق خاص بتطبيق آخر (${initialStatus.targetApp})."
                else -> null
            }
        )
    }
    var isVerifying by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Icon with Gold-Emerald Gradient
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(WheatGold, EmeraldPrimary)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "إدارة تجارة الحبوب والغلال",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "نظام الحماية والترخيص الرقمي المعتمد",
                fontSize = 13.sp,
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Hardware Machine ID Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "المعرّف الفريد لهذا الجهاز (Machine ID)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = machineId,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Machine ID", machineId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ معرّف الجهاز بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "نسخ المعرّف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val message = "السلام عليكم باشمهندس أحمد، أود تفعيل وترخيص برنامج إدارة تجارة الحبوب والغلال.\nمعرّف الجهاز الخاص بي هو:\n$machineId"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(
                                        "https://wa.me/$DEVELOPER_WHATSAPP_INTL?text=" + Uri.encode(message)
                                    )
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.25f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "إرسال عبر واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // License Key Input
            OutlinedTextField(
                value = licenseInput,
                onValueChange = {
                    licenseInput = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("أدخل كود الترخيص المعتمد") },
                placeholder = { Text("GTM6-eyJoZWFkZXIi...") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                                if (text.isNotEmpty()) {
                                    licenseInput = text
                                    errorMessage = null
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "لصق من الحافظة",
                            tint = EmeraldPrimary
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    cursorColor = EmeraldPrimary
                ),
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            // Error display if any
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusUnpaidRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = StatusUnpaidRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = StatusUnpaidRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Activation Action Button
            Button(
                onClick = {
                    if (licenseInput.isBlank()) {
                        errorMessage = "يرجى إدخال أو لصق كود الترخيص أولاً"
                        return@Button
                    }
                    isVerifying = true
                    val result = LicenseManager.activateLicense(context, licenseInput.trim())
                    isVerifying = false

                    when (result) {
                        is LicenseValidationResult.Valid -> {
                            Toast.makeText(
                                context,
                                "تم تفعيل البرنامج بنجاح! مرحباً ${result.certificate.payload.clientName}",
                                Toast.LENGTH_LONG
                            ).show()
                            onActivationSuccess(result.certificate)
                        }
                        is LicenseValidationResult.Expired -> {
                            errorMessage = "عذراً، هذا الترخيص منتهي الصلاحية بتاريخ ${result.expiredAt}"
                        }
                        is LicenseValidationResult.MachineMismatch -> {
                            errorMessage = "كود الترخيص هذا مخصص لجهاز آخر (${result.expectedMachineId})"
                        }
                        is LicenseValidationResult.AppMismatch -> {
                            errorMessage = "كود الترخيص هذا مخصص لنظام آخر (${result.targetApp})"
                        }
                        is LicenseValidationResult.InvalidSignature -> {
                            errorMessage = "فشل التحقق الرقمي! كود الترخيص غير سليم أو تم تعديله."
                        }
                        is LicenseValidationResult.MalformedKey -> {
                            errorMessage = result.reason
                        }
                        else -> {
                            errorMessage = "تعذر التحقق من كود الترخيص"
                        }
                    }
                },
                enabled = !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "جارِ التحقق الرقمي...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تفعيل البرنامج والبدء بالعمل", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dedicated Developer Contact Card for First-Time Activation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(EmeraldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "للتواصل وطلب كود التفعيل والترخيص",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Eng. Ahmed Mohamed Attia",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Founder & CEO of UltraSoft",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WheatGold
                            )
                        }
                    }

                    HorizontalDivider(color = WheatCardBorder)

                    // Direct WhatsApp Contact Button ONLY
                    Button(
                        onClick = {
                            val message = "السلام عليكم Eng. Ahmed Mohamed Attia، أود الحصول على كود تفعيل برنامج إدارة تجارة الحبوب والغلال (UltraSoft).\nمعرّف الجهاز (Machine ID):\n$machineId"
                            val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(
                                    "https://wa.me/$DEVELOPER_WHATSAPP_INTL?text=" + Uri.encode(message)
                                )
                            }
                            try {
                                context.startActivity(waIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التواصل مع المطور عبر واتساب",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notice / Guarantee
            Text(
                text = "يتم حفظ الترخيص بشكل دائم ومستمر على هذا الجهاز طوال فترة السريان، ولا يتأثر بالتحديثات أو إعادة التثبيت.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Powered by UltraSoft • Founded by Eng. Ahmed Mohamed Attia",
                fontSize = 10.sp,
                color = WheatGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
