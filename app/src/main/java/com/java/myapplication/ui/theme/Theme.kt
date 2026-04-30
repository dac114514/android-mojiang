package com.java.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = InkPrimaryDark,
    secondary = InkSecondaryDark,
    tertiary = InkAccentDark,
    background = InkBackgroundDark,
    surface = InkSurfaceDark,
    surfaceVariant = InkSurfaceVariantDark,
    onPrimary = InkText,
    onSecondary = InkText,
    onBackground = InkTextDark,
    onSurface = InkTextDark,
    onSurfaceVariant = InkSubtleTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = InkPrimary,
    secondary = InkSecondary,
    tertiary = InkAccent,
    background = InkBackground,
    surface = InkSurface,
    surfaceVariant = InkSurfaceVariant,
    onPrimary = InkSurface,
    onSecondary = InkSurface,
    onBackground = InkText,
    onSurface = InkText,
    onSurfaceVariant = InkSubtleText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}