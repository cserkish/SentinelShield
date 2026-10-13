package uz.sentinel.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.sentinel.presentation.theme.GlassBorderWhite

/**
 * Zamonaviy shishasimon (Frosted Glass) karta komponenti.
 * Cyber-Teal Dark mavzusida mayin neon hoshiya va silliq burchaklarga ega.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    borderColor: Color = GlassBorderWhite,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
