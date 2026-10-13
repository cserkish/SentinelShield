package uz.sentinel.presentation.applock

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    packageName: String,
    onUnlocked: () -> Unit,
    viewModel: LockScreenViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(packageName) {
        viewModel.init(packageName)
    }

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.wrongAttempts) {
        if (uiState.wrongAttempts > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            repeat(4) {
                launch {
                    shakeOffset.animateTo(
                        if (it % 2 == 0) 16f else -16f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    )
                }
            }
            shakeOffset.animateTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        ) {
            Text(
                text = "🔒",
                fontSize = 56.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ilova qulflangan",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = packageName.substringAfterLast('.'),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.lockoutRemainingSeconds > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFFFF5252), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏳ Bloklandi: ${uiState.lockoutRemainingSeconds} soniya",
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedContent(
                targetState = uiState.lockMethod,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "lock_method_transition"
            ) { method ->
                when (method) {
                    LockMethod.PIN -> PinPad(
                        digits = uiState.pinDigits,
                        errorMessage = uiState.errorMessage,
                        enabled = uiState.lockoutRemainingSeconds == 0,
                        onDigit = viewModel::onPinDigitAdded,
                        onDelete = viewModel::onPinDigitRemoved
                    )
                    LockMethod.PATTERN -> PatternLockComposable(
                        selectedDots = uiState.patternDots,
                        errorMessage = uiState.errorMessage,
                        enabled = uiState.lockoutRemainingSeconds == 0,
                        onDotSelected = viewModel::onPatternDotAdded,
                        onCompleted = viewModel::onPatternCompleted
                    )
                    LockMethod.BIOMETRIC -> {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        LaunchedEffect(Unit) {
                            activity?.let { viewModel.launchBiometric(it) }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF4FC3F7)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Barmoq izi orqali oching",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = { viewModel.switchToPin() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("PIN kod bilan ochish")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPad(
    digits: List<Int>,
    errorMessage: String?,
    enabled: Boolean = true,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // PIN dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            repeat(6) { index ->
                val filled = index < digits.size
                val dotColor by animateColorAsState(
                    targetValue = if (filled) Color(0xFF4FC3F7) else Color.Transparent,
                    animationSpec = tween(150),
                    label = "dot_color"
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color(0xFFFF5252), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Number grid
        val keys = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2) // -1 = empty, -2 = backspace
        )

        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        -1 -> Spacer(modifier = Modifier.size(64.dp))
                        -2 -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable(enabled = enabled) { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = if (enabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (enabled) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (enabled) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f), CircleShape)
                                .clickable(enabled = enabled) { onDigit(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key.toString(),
                                color = if (enabled) Color.White else Color.White.copy(alpha = 0.25f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternLockComposable(
    selectedDots: List<Int>,
    errorMessage: String?,
    enabled: Boolean = true,
    onDotSelected: (Int) -> Unit,
    onCompleted: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Pattern chizing",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = it, color = Color(0xFFFF5252), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3x3 grid
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            (0..2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    (0..2).forEach { col ->
                        val dotIndex = row * 3 + col
                        val isSelected = selectedDots.contains(dotIndex)
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF4FC3F7) else Color.White.copy(0.3f),
                            animationSpec = tween(150),
                            label = "pattern_dot_$dotIndex"
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(dotColor.copy(alpha = 0.15f))
                                .border(2.dp, dotColor, CircleShape)
                                .clickable(enabled = enabled) {
                                    onDotSelected(dotIndex)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedDots.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (enabled) Color(0xFF4FC3F7) else Color.Gray)
                    .clickable(enabled = enabled) { onCompleted() }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Tasdiqlash",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
