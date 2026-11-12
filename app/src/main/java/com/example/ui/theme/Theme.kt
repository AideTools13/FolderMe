package com.example.ui.theme

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
    primary = EmeraldPrimaryDark,
    secondary = EmeraldSecondaryDark,
    tertiary = EmeraldTertiaryDark,
    background = EmeraldBackgroundDark,
    surface = EmeraldSurfaceDark,
    surfaceVariant = EmeraldSurfaceVariantDark,
    onPrimary = EmeraldOnPrimaryDark,
    onSurface = EmeraldOnSurfaceDark,
    onBackground = EmeraldOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    secondary = EmeraldSecondaryLight,
    tertiary = EmeraldTertiaryLight,
    background = EmeraldBackgroundLight,
    surface = EmeraldSurfaceLight,
    onPrimary = EmeraldOnPrimaryLight,
    onSurface = EmeraldOnSurfaceLight,
    onBackground = EmeraldOnSurfaceLight
)

@Composable
fun FolderMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false by default to showcase custom emerald brand palette
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
