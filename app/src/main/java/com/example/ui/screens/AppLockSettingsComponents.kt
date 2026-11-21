package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.AppLockConfig
import com.example.security.AppLockManager
import com.example.security.BiometricAvailability
import com.example.security.UnlockMethod
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
import kotlinx.coroutines.delay

/**
 * بطاقة إعدادات قفل وحماية البرنامج داخل تبويب (الترخيص والحماية) في الإعدادات:
 * تتيح تفعيل أو دمج أي طريقة حماية (رقم سري 4 أرقام، نقش 9 نقاط، بصمة الهاتف) معاً أو بشكل مستقل.
 */
@Composable
fun AppLockSettingsCard(
    context: Context,
    onLockAppNow: (() -> Unit)? = null
) {
    var config by remember { mutableStateOf(AppLockManager.getConfig(context)) }
    val biometricStatus = remember { AppLockManager.checkBiometricAvailability(context) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPatternSetupDialog by remember { mutableStateOf(false) }

    // حماية تعديلات أو إلغاء القفل بالتحقق من الهوية الحالية أولاً
    var pendingProtectedAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun refreshConfig() {
        config = AppLockManager.getConfig(context)
    }

    fun runWithVerificationIfNeeded(title: String, action: () -> Unit) {
        if (!config.isLockActive) {
            action()
        } else {
            pendingProtectedAction = title to action
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (config.isLockActive) 1.5.dp else 1.dp,
                color = if (config.isLockActive) EmeraldPrimary else WheatCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("app_lock_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (config.isLockActive) EmeraldLight else WheatGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (config.isLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (config.isLockActive) EmeraldPrimary else WheatGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "قفل وحماية البرنامج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "حماية البيانات من التلاعب بواسطة الأشخاص أو الأطفال",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (config.isLockActive) StatusPaidBg else StatusPartialBg
                ) {
                    Text(
                        text = if (config.isLockActive) "مفعّل (${config.activeMethodCount}) 🔒" else "غير مفعّل",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (config.isLockActive) StatusPaidGreen else StatusPartialAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            // Active methods summary banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (config.isLockActive) EmeraldLight.copy(alpha = 0.7f) else MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (config.isLockActive) EmeraldPrimary.copy(alpha = 0.35f) else WheatCardBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (config.isLockActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (config.isLockActive) {
                            "طرق الفتح المفعّلة حالياً: ${config.activeMethodsSummaryAr()}"
                        } else {
                            "يمكنك تفعيل طريقة واحدة، أو طريقتين معاً، أو الثلاث طرق معاً (PIN + نقش 9 نقاط + بصمة الهاتف)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (config.isLockActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (config.isLockActive) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = WheatCardBorder)

            // 1. 4-Digit PIN Option
            LockMethodOptionRow(
                icon = Icons.Default.Dialpad,
                title = "رقم سري (PIN من 4 أرقام)",
                subtitle = if (config.isPinEnabled) "مفعّل ومشفّر — يمكنك تغييره أو تعطيله" else "إنشاء رمز رقمي سريع مكوّن من 4 أرقام",
                checked = config.isPinEnabled,
                actionLabel = if (config.isPinEnabled) "تغيير الرقم" else null,
                onActionClick = {
                    runWithVerificationIfNeeded("تأكيد الهوية لتغيير الرقم السري") {
                        showPinSetupDialog = true
                    }
                },
                onCheckedChange = { enable ->
                    if (enable) {
                        runWithVerificationIfNeeded("تأكيد الهوية لإضافة رقم سري (PIN)") {
                            showPinSetupDialog = true
                        }
                    } else {
                        runWithVerificationIfNeeded("تأكيد الهوية لإلغاء الرقم السري (PIN)") {
                            AppLockManager.disablePin(context)
                            refreshConfig()
                            Toast.makeText(context, "تم إلغاء قفل الرقم السري (PIN)", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                switchTestTag = "switch_lock_pin"
            )

            HorizontalDivider(color = WheatCardBorder.copy(alpha = 0.55f))

            // 2. 9-Dot Pattern Option
            LockMethodOptionRow(
                icon = Icons.Default.GridOn,
                title = "نقش الأمان (9 نقاط)",
                subtitle = if (config.isPatternEnabled) "مفعّل ومشفّر — يمكنك تعديل النقش أو تعطيله" else "رسم رمز توصيل بين 9 نقاط (4 نقاط على الأقل)",
                checked = config.isPatternEnabled,
                actionLabel = if (config.isPatternEnabled) "تغيير النقش" else null,
                onActionClick = {
                    runWithVerificationIfNeeded("تأكيد الهوية لتغيير نقش الأمان") {
                        showPatternSetupDialog = true
                    }
                },
                onCheckedChange = { enable ->
                    if (enable) {
                        runWithVerificationIfNeeded("تأكيد الهوية لإضافة نقش 9 نقاط") {
                            showPatternSetupDialog = true
                        }
                    } else {
                        runWithVerificationIfNeeded("تأكيد الهوية لإلغاء نقش الأمان") {
                            AppLockManager.disablePattern(context)
                            refreshConfig()
                            Toast.makeText(context, "تم إلغاء قفل نقش 9 نقاط", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                switchTestTag = "switch_lock_pattern"
            )

            HorizontalDivider(color = WheatCardBorder.copy(alpha = 0.55f))

            // 3. Device Fingerprint / Biometric Option
            val bioSubtitle = when {
                config.isBiometricEnabled -> "مفعّلة — تستخدم نفس بصمة فتح وإغلاق الهاتف"
                biometricStatus == BiometricAvailability.AVAILABLE -> "الفتح بنفس بصمة فتح وإغلاق الهاتف المسجلة"
                biometricStatus == BiometricAvailability.NONE_ENROLLED -> "يرجى تسجيل بصمة في إعدادات الهاتف أولاً"
                biometricStatus == BiometricAvailability.NO_HARDWARE -> "هذا الهاتف لا يدعم مستشعر البصمة"
                else -> "مستشعر البصمة غير متاح حالياً"
            }

            LockMethodOptionRow(
                icon = Icons.Default.Fingerprint,
                title = "بصمة الهاتف (بصمة قفل الجهاز)",
                subtitle = bioSubtitle,
                checked = config.isBiometricEnabled,
                enabled = config.isBiometricEnabled || biometricStatus == BiometricAvailability.AVAILABLE,
                onCheckedChange = { enable ->
                    if (enable) {
                        val activity = AppLockManager.findFragmentActivity(context)
                        if (activity == null) {
                            Toast.makeText(context, "تعذر تفعيل مستشعر البصمة", Toast.LENGTH_SHORT).show()
                            return@LockMethodOptionRow
                        }
                        AppLockManager.authenticateWithBiometrics(
                            activity = activity,
                            title = "تفعيل قفل بصمة الهاتف",
                            subtitle = "ضع إصبعك على المستشعر لتأكيد التفعيل",
                            allowDeviceCredentialFallback = false,
                            negativeButtonText = "إلغاء",
                            onSuccess = {
                                AppLockManager.setBiometricEnabled(context, true)
                                refreshConfig()
                                Toast.makeText(context, "تم تفعيل الفتح ببصمة الهاتف بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        runWithVerificationIfNeeded("تأكيد الهوية لإلغاء قفل البصمة") {
                            AppLockManager.setBiometricEnabled(context, false)
                            refreshConfig()
                            Toast.makeText(context, "تم تعطيل الفتح ببصمة الهاتف", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                switchTestTag = "switch_lock_biometric"
            )

            // Additional Controls when Lock is Active
            if (config.isLockActive) {
                HorizontalDivider(color = WheatCardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إعادة القفل تلقائياً عند مغادرة البرنامج",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "يطلب الفتح فور العودة للتطبيق بعد إغلاقه أو تصغيره",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.lockOnBackground,
                        onCheckedChange = { enabled ->
                            AppLockManager.setLockOnBackground(context, enabled)
                            refreshConfig()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onLockAppNow != null) {
                        Button(
                            onClick = onLockAppNow,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lock_app_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قفل وتجربة الآن", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            runWithVerificationIfNeeded("تأكيد الهوية لإلغاء قفل البرنامج بالكامل") {
                                AppLockManager.disableAllLocks(context)
                                refreshConfig()
                                Toast.makeText(context, "تم إلغاء جميع طرق القفل بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusUnpaidRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("disable_all_locks_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إلغاء القفل بالكامل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog 1: 4-Digit PIN Setup (2 Steps: Enter -> Confirm)
    if (showPinSetupDialog) {
        PinSetupDialog(
            isChange = config.isPinEnabled,
            onDismiss = { showPinSetupDialog = false },
            onPinCreated = { newPin ->
                if (AppLockManager.savePin(context, newPin)) {
                    refreshConfig()
                    Toast.makeText(context, "تم حفظ وتفعيل الرقم السري (PIN) بنجاح", Toast.LENGTH_SHORT).show()
                }
                showPinSetupDialog = false
            }
        )
    }

    // Dialog 2: 9-Dot Pattern Setup (2 Steps: Draw -> Confirm)
    if (showPatternSetupDialog) {
        PatternSetupDialog(
            isChange = config.isPatternEnabled,
            onDismiss = { showPatternSetupDialog = false },
            onPatternCreated = { newPattern ->
                if (AppLockManager.savePattern(context, newPattern)) {
                    refreshConfig()
                    Toast.makeText(context, "تم حفظ وتفعيل نقش 9 نقاط بنجاح", Toast.LENGTH_SHORT).show()
                }
                showPatternSetupDialog = false
            }
        )
    }

    // Dialog 3: Verify Current Lock before Modifying or Disabling
    pendingProtectedAction?.let { (dialogTitle, action) ->
        VerifyCurrentLockDialog(
            context = context,
            config = config,
            title = dialogTitle,
            onDismiss = { pendingProtectedAction = null },
            onVerified = {
                pendingProtectedAction = null
                action()
            }
        )
    }
}

@Composable
private fun LockMethodOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Surface(
                    shape = CircleShape,
                    color = if (checked) EmeraldLight else MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (checked) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EmeraldPrimary
                ),
                modifier = Modifier.testTag(switchTestTag)
            )
        }

        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionLabel,
                        color = WheatGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * نافذة إنشاء أو تغيير الرقم السري (4 أرقام) بخطوتين (إدخال ثم تأكيد)
 */
@Composable
fun PinSetupDialog(
    isChange: Boolean,
    onDismiss: () -> Unit,
    onPinCreated: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = Enter new PIN, 2 = Confirm PIN
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(isError) {
        if (isError) {
            delay(750L)
            isError = false
            currentInput = ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (step == 1) {
                        if (isChange) "تغيير الرقم السري (PIN)" else "إنشاء رقم سري جديد (4 أرقام)"
                    } else {
                        "تأكيد الرقم السري"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Text(
                    text = if (step == 1) {
                        "الخطوة 1 من 2: اختر رقماً سرياً من 4 أرقام"
                    } else {
                        "الخطوة 2 من 2: أعد إدخال نفس الـ 4 أرقام للتأكيد"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                errorMsg?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusUnpaidRed,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                PinLockPad(
                    pin = currentInput,
                    isError = isError,
                    onDigit = { d ->
                        if (currentInput.length < AppLockManager.PIN_LENGTH && !isError) {
                            val next = currentInput + d
                            currentInput = next
                            errorMsg = null
                            if (next.length == AppLockManager.PIN_LENGTH) {
                                if (step == 1) {
                                    firstPin = next
                                    currentInput = ""
                                    step = 2
                                } else {
                                    if (next == firstPin) {
                                        onPinCreated(next)
                                    } else {
                                        isError = true
                                        errorMsg = "الرقمان غير متطابقين! حاول مرة أخرى"
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (currentInput.isNotEmpty() && !isError) {
                            currentInput = currentInput.dropLast(1)
                        }
                    },
                    onClear = {
                        currentInput = ""
                        errorMsg = null
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    if (step == 2) {
                        TextButton(
                            onClick = {
                                step = 1
                                firstPin = ""
                                currentInput = ""
                                errorMsg = null
                            }
                        ) {
                            Text("إعادة من البداية", color = WheatGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة إنشاء أو تغيير نقش الأمان (9 نقاط) بخطوتين (رسم ثم إعادة الرسم للتأكيد)
 */
@Composable
fun PatternSetupDialog(
    isChange: Boolean,
    onDismiss: () -> Unit,
    onPatternCreated: (List<Int>) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(isError) {
        if (isError) {
            delay(750L)
            isError = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (step == 1) {
                        if (isChange) "تغيير نقش الأمان (9 نقاط)" else "إنشاء نقش أمان جديد (9 نقاط)"
                    } else {
                        "تأكيد نقش الأمان"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Text(
                    text = if (step == 1) {
                        "الخطوة 1 من 2: اسحب إصبعك لتوصيل 4 نقاط على الأقل"
                    } else {
                        "الخطوة 2 من 2: أعد رسم نفس النقش للتأكيد"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                errorMsg?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusUnpaidRed,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                PatternLockPad(
                    padSize = 248.dp,
                    isError = isError,
                    resetKey = step,
                    onPatternComplete = { dots ->
                        if (dots.size < AppLockManager.MIN_PATTERN_DOTS) {
                            isError = true
                            errorMsg = "يرجى توصيل 4 نقاط على الأقل"
                        } else if (step == 1) {
                            firstPattern = dots
                            errorMsg = null
                            step = 2
                        } else {
                            if (dots == firstPattern) {
                                onPatternCreated(dots)
                            } else {
                                isError = true
                                errorMsg = "النقش غير مطابق! يرجى إعادة الرسم بدقة"
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    if (step == 2) {
                        TextButton(
                            onClick = {
                                step = 1
                                firstPattern = emptyList()
                                errorMsg = null
                            }
                        ) {
                            Text("إعادة رسم النقش", color = WheatGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة التحقق من الهوية قبل تعديل أو إلغاء أي طريقة قفل مفعّلة لمنع الأطفال أو الآخرين من تعطيل الحماية
 */
@Composable
private fun VerifyCurrentLockDialog(
    context: Context,
    config: AppLockConfig,
    title: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val activity = remember(context) { AppLockManager.findFragmentActivity(context) }
    var activeMethod by remember(config) {
        mutableStateOf(
            when {
                config.isPinEnabled -> UnlockMethod.PIN
                config.isPatternEnabled -> UnlockMethod.PATTERN
                else -> UnlockMethod.BIOMETRIC
            }
        )
    }
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(isError) {
        if (isError) {
            delay(700L)
            isError = false
            pinInput = ""
        }
    }

    fun launchBiometricCheck() {
        val frag = activity ?: return
        AppLockManager.authenticateWithBiometrics(
            activity = frag,
            title = title,
            subtitle = "تحقق ببصمة الهاتف للمتابعة",
            allowDeviceCredentialFallback = !config.hasManualFallback,
            negativeButtonText = "إلغاء",
            onSuccess = onVerified,
            onError = { errorMsg = it }
        )
    }

    LaunchedEffect(activeMethod) {
        if (activeMethod == UnlockMethod.BIOMETRIC) {
            delay(150L)
            launchBiometricCheck()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (config.isPinEnabled && config.isPatternEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeMethod = UnlockMethod.PIN; pinInput = ""; errorMsg = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رقم سري", fontWeight = if (activeMethod == UnlockMethod.PIN) FontWeight.Bold else FontWeight.Normal)
                        }
                        OutlinedButton(
                            onClick = { activeMethod = UnlockMethod.PATTERN; errorMsg = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نقش", fontWeight = if (activeMethod == UnlockMethod.PATTERN) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                errorMsg?.let {
                    Text(it, color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                when (activeMethod) {
                    UnlockMethod.PIN -> {
                        PinLockPad(
                            pin = pinInput,
                            isError = isError,
                            onDigit = { d ->
                                if (pinInput.length < AppLockManager.PIN_LENGTH && !isError) {
                                    val next = pinInput + d
                                    pinInput = next
                                    if (next.length == AppLockManager.PIN_LENGTH) {
                                        if (AppLockManager.verifyPin(context, next)) {
                                            onVerified()
                                        } else {
                                            isError = true
                                            errorMsg = "الرقم السري غير صحيح"
                                        }
                                    }
                                }
                            },
                            onBackspace = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
                            onClear = { pinInput = ""; errorMsg = null }
                        )
                    }

                    UnlockMethod.PATTERN -> {
                        PatternLockPad(
                            padSize = 220.dp,
                            isError = isError,
                            onPatternComplete = { dots ->
                                if (AppLockManager.verifyPattern(context, dots)) {
                                    onVerified()
                                } else {
                                    isError = true
                                    errorMsg = "النقش غير صحيح"
                                }
                            }
                        )
                    }

                    UnlockMethod.BIOMETRIC -> {
                        Button(
                            onClick = { launchBiometricCheck() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التحقق ببصمة الهاتف")
                        }
                    }
                }

                if (config.isBiometricEnabled && activeMethod != UnlockMethod.BIOMETRIC) {
                    TextButton(onClick = { launchBiometricCheck() }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("أو التحقق ببصمة الهاتف", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
