package uz.sentinel.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = TealGrey40,
    onSecondary = Color.White,
    secondaryContainer = TealGrey90,
    onSecondaryContainer = TealGrey10,
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVar90,
    onSurfaceVariant = NeutralVar30,
    outline = NeutralVar50,
    outlineVariant = NeutralVar80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral90,
    inversePrimary = Teal80,
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = CyberTeal,
    onPrimary = Color(0xFF00382E),
    primaryContainer = CyberTealDark,
    onPrimaryContainer = Color(0xFF7FF5E7),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    error = NeonCrimson,
    onError = Color(0xFF69001A),
    errorContainer = Color(0xFF930028),
    onErrorContainer = Color(0xFFFFD9DD),
    background = ObsidianBlack,
    onBackground = Color(0xFFE1E3E4),
    surface = ObsidianSurface,
    onSurface = Color(0xFFE1E3E4),
    surfaceVariant = ObsidianSurfaceElev,
    onSurfaceVariant = Color(0xFFC0C8C8),
    outline = GlassBorderTeal,
    outlineVariant = GlassBorderWhite,
    inverseSurface = Color(0xFFE1E3E4),
    inverseOnSurface = ObsidianBlack,
    inversePrimary = Teal40,
    scrim = Color.Black
)

@Composable
fun SentinelShieldTheme(
    darkTheme: Boolean = true, // Default to our signature Cyber-Teal Dark security experience
    dynamicColor: Boolean = false, // Keep consistent cyber branding across devices
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SentinelTypography,
        content = content
    )
}
