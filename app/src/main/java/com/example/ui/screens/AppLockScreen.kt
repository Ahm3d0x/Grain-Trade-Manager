package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppLockConfig
import com.example.security.AppLockManager
import com.example.security.BiometricAvailability
import com.example.security.UnlockMethod
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WheatCardBorder
import com.example.ui.theme.WheatGold
import com.example.ui.theme.WheatGoldLight
import kotlinx.coroutines.delay
import kotlin.math.hypot

/**
 * شاشة القفل الكاملة للبرنامج: تدعم فتح البرنامج بالرقم السري (4 أرقام)، أو نقش 9 نقاط، أو بصمة الهاتف،
 * مع إمكانية التبديل الفوري بين جميع الطرق المفعّلة في الإعدادات.
 */
@Composable
fun AppLockScreen(
    config: AppLockConfig,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { AppLockManager.findFragmentActivity(context) }
    val haptic = LocalHapticFeedback.current

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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorState by remember { mutableStateOf(false) }
    var lockoutRemainingMs by remember { mutableLongStateOf(AppLockManager.getRemainingLockoutMs(context)) }
    var biometricAutoLaunched by remember { mutableStateOf(false) }

    // منع تخطي شاشة القفل بزر الرجوع - إرسال التطبيق للخلفية بدلاً من ذلك
    BackHandler(enabled = true) {
        activity?.moveTaskToBack(true)
    }

    // مؤقت الانتظار بعد 5 محاولات خاطئة متتالية
    LaunchedEffect(lockoutRemainingMs) {
        if (lockoutRemainingMs > 0L) {
            delay(1000L)
            lockoutRemainingMs = AppLockManager.getRemainingLockoutMs(context)
        }
    }

    fun triggerBiometric() {
        val fragActivity = activity ?: run {
            errorMessage = "تعذر الوصول لمستشعر البصمة حالياً"
            return
        }
        val avail = AppLockManager.checkBiometricAvailability(context)
        if (avail != BiometricAvailability.AVAILABLE && !(!config.hasManualFallback)) {
            errorMessage = when (avail) {
                BiometricAvailability.NONE_ENROLLED -> "لا توجد بصمة مسجلة في إعدادات الهاتف"
                BiometricAvailability.NO_HARDWARE -> "هذا الهاتف لا يحتوي على مستشعر بصمة"
                else -> "مستشعر البصمة غير متاح حالياً"
            }
            return
        }
        errorMessage = null
        val onlyBiometric = !config.hasManualFallback
        AppLockManager.authenticateWithBiometrics(
            activity = fragActivity,
            title = "فتح برنامج احسبلي",
            subtitle = "ضع إصبعك على مستشعر البصمة لفتح البرنامج",
            allowDeviceCredentialFallback = onlyBiometric,
            negativeButtonText = if (config.isPinEnabled) "استخدام الرقم السري" else "استخدام النقش",
            onSuccess = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onUnlocked()
            },
            onError = { msg ->
                errorMessage = msg
            }
        )
    }

    // تشغيل بصمة الهاتف تلقائياً فور فتح شاشة القفل إذا كانت مفعّلة
    LaunchedEffect(config.isBiometricEnabled) {
        if (config.isBiometricEnabled && !biometricAutoLaunched) {
            biometricAutoLaunched = true
            delay(250L)
            triggerBiometric()
        }
    }

    // تصفير حالة الخطأ تلقائياً بعد لحظة قصيرة
    LaunchedEffect(isErrorState) {
        if (isErrorState) {
            delay(850L)
            isErrorState = false
            pinInput = ""
        }
    }

    val isLockedOut = lockoutRemainingMs > 0L
    val lockoutSec = ((lockoutRemainingMs + 999L) / 1000L).coerceAtLeast(0L)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(EmeraldDark, EmeraldPrimary, MaterialTheme.colorScheme.background)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("app_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Shield Header
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier
                    .size(72.dp)
                    .border(2.dp, WheatGoldLight.copy(alpha = 0.7f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل البرنامج",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "برنامج احسبلي محمي بقفل الأمان",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "يرجى التحقق من هويتك للدخول إلى بياناتك وفواتيرك",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            // Main Interactive Lock Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .border(1.dp, WheatCardBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Method Switcher Pills (if PIN and Pattern are both enabled)
                    if (config.isPinEnabled && config.isPatternEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, WheatCardBorder, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LockMethodTab(
                                modifier = Modifier.weight(1f),
                                label = "رقم سري (PIN)",
                                icon = Icons.Default.Dialpad,
                                selected = activeMethod == UnlockMethod.PIN,
                                onClick = {
                                    activeMethod = UnlockMethod.PIN
                                    pinInput = ""
                                    errorMessage = null
                                },
                                testTag = "lock_switch_pin"
                            )
                            LockMethodTab(
                                modifier = Modifier.weight(1f),
                                label = "نقش 9 نقاط",
                                icon = Icons.Default.GridOn,
                                selected = activeMethod == UnlockMethod.PATTERN,
                                onClick = {
                                    activeMethod = UnlockMethod.PATTERN
                                    errorMessage = null
                                },
                                testTag = "lock_switch_pattern"
                            )
                        }
                    }

                    // Lockout Warning or Error Banner
                    if (isLockedOut) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusUnpaidBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تم تجاوز عدد المحاولات الخاطئة. يرجى الانتظار $lockoutSec ثانية أو استخدام بصمة الهاتف.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusUnpaidRed,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusUnpaidBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusUnpaidRed,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Active Unlock UI Body
                    when (activeMethod) {
                        UnlockMethod.PIN -> {
                            Text(
                                text = "أدخل الرقم السري المكوّن من 4 أرقام",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            PinLockPad(
                                pin = pinInput,
                                isError = isErrorState,
                                enabled = !isLockedOut,
                                onDigit = { digit ->
                                    if (pinInput.length < AppLockManager.PIN_LENGTH && !isErrorState) {
                                        val nextPin = pinInput + digit
                                        pinInput = nextPin
                                        errorMessage = null
                                        if (nextPin.length == AppLockManager.PIN_LENGTH) {
                                            if (AppLockManager.verifyPin(context, nextPin)) {
                                                AppLockManager.resetFailedAttempts(context)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onUnlocked()
                                            } else {
                                                val attempts = AppLockManager.recordFailedAttempt(context)
                                                lockoutRemainingMs = AppLockManager.getRemainingLockoutMs(context)
                                                isErrorState = true
                                                errorMessage = if (lockoutRemainingMs > 0L) {
                                                    "رقم سري خاطئ — تم قفل المحاولات مؤقتاً لمدة 30 ثانية"
                                                } else {
                                                    val left = (AppLockManager.MAX_FAILED_ATTEMPTS - attempts).coerceAtLeast(1)
                                                    "الرقم السري غير صحيح (متبقي $left محاولات)"
                                                }
                                            }
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (pinInput.isNotEmpty() && !isErrorState) {
                                        pinInput = pinInput.dropLast(1)
                                    }
                                },
                                onClear = {
                                    pinInput = ""
                                    errorMessage = null
                                }
                            )
                        }

                        UnlockMethod.PATTERN -> {
                            Text(
                                text = "ارسم نقش الأمان المكوّن من 9 نقاط",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            PatternLockPad(
                                padSize = 256.dp,
                                enabled = !isLockedOut,
                                isError = isErrorState,
                                onPatternComplete = { dots ->
                                    if (dots.size < AppLockManager.MIN_PATTERN_DOTS) {
                                        isErrorState = true
                                        errorMessage = "يرجى توصيل 4 نقاط على الأقل"
                                    } else if (AppLockManager.verifyPattern(context, dots)) {
                                        AppLockManager.resetFailedAttempts(context)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onUnlocked()
                                    } else {
                                        val attempts = AppLockManager.recordFailedAttempt(context)
                                        lockoutRemainingMs = AppLockManager.getRemainingLockoutMs(context)
                                        isErrorState = true
                                        errorMessage = if (lockoutRemainingMs > 0L) {
                                            "نقش غير صحيح — تم قفل المحاولات مؤقتاً لمدة 30 ثانية"
                                        } else {
                                            val left = (AppLockManager.MAX_FAILED_ATTEMPTS - attempts).coerceAtLeast(1)
                                            "النقش غير صحيح (متبقي $left محاولات)"
                                        }
                                    }
                                }
                            )
                        }

                        UnlockMethod.BIOMETRIC -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(vertical = 18.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldLight,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, EmeraldPrimary, CircleShape)
                                        .clickable { triggerBiometric() }
                                        .testTag("biometric_big_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "الفتح ببصمة الهاتف",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "اضغط على أيقونة البصمة للفتح ببصمة الهاتف",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { triggerBiometric() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("التحقق ببصمة الهاتف الآن", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Dedicated Fingerprint Quick Button when Biometric is combined with PIN or Pattern
                    if (config.isBiometricEnabled && activeMethod != UnlockMethod.BIOMETRIC) {
                        OutlinedButton(
                            onClick = { triggerBiometric() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("lock_biometric_quick_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أو الفتح السريع ببصمة الهاتف",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Developer Attribution & UltraSoft Protection Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "UltraSoft • Developed by Eng. Ahmed Mohamed Attia",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WheatGold,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "UltraSoft Security • الطرق المفعّلة: ${config.activeMethodsSummaryAr()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LockMethodTab(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) EmeraldPrimary else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * لوحة إدخال الرقم السري المكوّن من 4 أرقام مع مؤشرات دائرية ولوحة أرقام كبيرة مريحة
 */
@Composable
fun PinLockPad(
    pin: String,
    isError: Boolean = false,
    enabled: Boolean = true,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 4 PIN Dots Indicator (LTR order for digits)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(AppLockManager.PIN_LENGTH) { idx ->
                    val filled = idx < pin.length
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isError -> StatusUnpaidRed
                            filled -> EmeraldPrimary
                            else -> Color.Transparent
                        },
                        label = "pin_dot_color"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isError -> StatusUnpaidRed
                            filled -> EmeraldPrimary
                            else -> WheatCardBorder
                        },
                        label = "pin_dot_border"
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(2.dp, borderColor, CircleShape)
                    )
                }
            }

            // Standard 3x4 Numeric Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowDigits ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowDigits.forEach { digit ->
                            PinKeyButton(
                                label = digit,
                                enabled = enabled,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDigit(digit)
                                }
                            )
                        }
                    }
                }

                // Bottom row: Clear | 0 | Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = WheatGoldLight.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClear()
                            }
                            .testTag("pin_key_clear")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "مسح",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }

                    PinKeyButton(
                        label = "0",
                        enabled = enabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigit("0")
                        }
                    )

                    Surface(
                        shape = CircleShape,
                        color = EmeraldLight,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            }
                            .testTag("pin_key_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "حذف رقم",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(1.dp, WheatCardBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("pin_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * لوحة رسم نقش الأمان (9 نقاط 3×3) تفاعلية بالكامل:
 * - تبدأ فور لمس أول نقطة بدون تأخير.
 * - تربط تلقائياً أي نقطة وسطية يتم المرور عليها (مثل 0 → 2 يربط 1 تلقائياً).
 * - تعطي اهتزازًا خفيفًا (Haptic) مع كل نقطة يتم توصيلها.
 */
@Composable
fun PatternLockPad(
    padSize: Dp = 250.dp,
    enabled: Boolean = true,
    isError: Boolean = false,
    resetKey: Any = Unit,
    onPatternComplete: (List<Int>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentPointer by remember { mutableStateOf<Offset?>(null) }
    var dotCenters by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var hitRadiusPx by remember { mutableStateOf(40f) }

    // تصفير النقش تلقائياً عند تغيير الخطوة أو بعد انتهاء عرض حالة الخطأ
    LaunchedEffect(resetKey) {
        selectedDots.clear()
        currentPointer = null
    }
    LaunchedEffect(isError) {
        if (!isError && currentPointer == null && selectedDots.isNotEmpty()) {
            selectedDots.clear()
        }
    }

    val activeColor = if (isError) StatusUnpaidRed else EmeraldPrimary
    val lineColor = if (isError) StatusUnpaidRed.copy(alpha = 0.75f) else WheatGold
    val inactiveRingColor = WheatCardBorder
    val inactiveCoreColor = EmeraldDark.copy(alpha = 0.35f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(padSize)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 1.5.dp,
                color = if (isError) StatusUnpaidRed.copy(alpha = 0.7f) else WheatCardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("pattern_lock_pad")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        selectedDots.clear()
                        currentPointer = down.position

                        fun checkAndAddDot(pos: Offset) {
                            val centers = dotCenters
                            if (centers.size != 9) return
                            for (i in 0..8) {
                                val dist = hypot(pos.x - centers[i].x, pos.y - centers[i].y)
                                if (dist <= hitRadiusPx && !selectedDots.contains(i)) {
                                    val prev = selectedDots.lastOrNull()
                                    if (prev != null) {
                                        val mid = AppLockManager.getIntermediateDot(prev, i)
                                        if (mid != null && !selectedDots.contains(mid)) {
                                            selectedDots.add(mid)
                                        }
                                    }
                                    selectedDots.add(i)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    break
                                }
                            }
                        }

                        checkAndAddDot(down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                currentPointer = null
                                if (selectedDots.isNotEmpty()) {
                                    val snapshot = selectedDots.toList()
                                    onPatternComplete(snapshot)
                                    if (snapshot.size >= AppLockManager.MIN_PATTERN_DOTS) {
                                        // يترك النقاط ظاهرة لحظة ثم تُمسح عند التحديث أو الخطأ
                                    }
                                }
                                break
                            }
                            change.consume()
                            currentPointer = change.position
                            checkAndAddDot(change.position)
                        }
                    }
                }
        ) {
            val cellW = size.width / 3f
            val cellH = size.height / 3f
            val cellMin = minOf(cellW, cellH)
            hitRadiusPx = cellMin * 0.36f

            val centers = List(9) { idx ->
                val row = idx / 3
                val col = idx % 3
                Offset(
                    x = (col + 0.5f) * cellW,
                    y = (row + 0.5f) * cellH
                )
            }
            dotCenters = centers

            // 1. Draw connecting lines between selected dots
            if (selectedDots.size >= 2) {
                for (i in 0 until selectedDots.size - 1) {
                    val start = centers[selectedDots[i]]
                    val end = centers[selectedDots[i + 1]]
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw live line to finger while dragging
            val lastDot = selectedDots.lastOrNull()
            val pointer = currentPointer
            if (lastDot != null && pointer != null) {
                drawLine(
                    color = lineColor.copy(alpha = 0.55f),
                    start = centers[lastDot],
                    end = pointer,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw 9 tactile dots
            val outerR = cellMin * 0.25f
            val innerR = cellMin * 0.095f
            centers.forEachIndexed { idx, center ->
                val isSelected = selectedDots.contains(idx)
                if (isSelected) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.16f),
                        radius = outerR * 1.18f,
                        center = center
                    )
                    drawCircle(
                        color = activeColor,
                        radius = outerR,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = activeColor,
                        radius = innerR * 1.25f,
                        center = center
                    )
                } else {
                    drawCircle(
                        color = inactiveRingColor,
                        radius = outerR * 0.85f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = inactiveCoreColor,
                        radius = innerR,
                        center = center
                    )
                }
            }
        }
    }
}
