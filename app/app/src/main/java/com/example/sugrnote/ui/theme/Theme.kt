package com.example.sugrnote.ui.theme

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
import com.example.sugrnote.data.settings.DarkThemeStyle
import com.example.sugrnote.data.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Gray10,
    primaryContainer = Blue40,
    secondary = Teal80,
    onSecondary = Gray10,
    tertiary = Amber80,
    background = SurfaceDark,
    surface = Gray20,
    onBackground = Gray90,
    onSurface = Gray90
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Gray99,
    primaryContainer = Blue90,
    secondary = Teal40,
    onSecondary = Gray99,
    tertiary = Amber40,
    background = SurfaceLight,
    surface = Gray95,
    onBackground = Gray10,
    onSurface = Gray10
)

@Composable
fun SugrNoteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkThemeStyle: DarkThemeStyle = DarkThemeStyle.STANDARD,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (isDark && darkThemeStyle == DarkThemeStyle.OLED) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
