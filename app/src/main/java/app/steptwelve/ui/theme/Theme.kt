package app.steptwelve.ui.theme

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

// Calm green fallback palette for devices without dynamic color (Android 11 and lower).
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E5E4E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4EFD8),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4C6359),
    secondaryContainer = Color(0xFFCEE9DB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98D3BC),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF13513F),
    onPrimaryContainer = Color(0xFFB4EFD8),
    secondary = Color(0xFFB3CCC0),
    secondaryContainer = Color(0xFF354B42),
)

@Composable
fun StepTwelveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
