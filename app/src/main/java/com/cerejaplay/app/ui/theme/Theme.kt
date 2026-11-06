package com.cerejaplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.cerejaplay.app.data.model.ThemeConfig

fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val cleaned = hex.trim().removePrefix("#")
        val value = when (cleaned.length) {
            6 -> "FF$cleaned"
            8 -> cleaned
            else -> return fallback
        }
        Color(value.toLong(16))
    }.getOrDefault(fallback)
}

private fun readableOn(background: Color): Color = if (background.luminance() > 0.5f) Color(0xFF0B0B0F) else Color.White

@Composable
fun CerejaPlayTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val primary = parseHexColor(config.primary, CerejaRed)
    val onPrimary = parseHexColor(config.onPrimary, readableOn(primary))
    val secondary = parseHexColor(config.secondary, CerejaRedLight)
    val background = parseHexColor(config.background, CerejaBackground)
    val surface = parseHexColor(config.surface, CerejaSurface)
    val surfaceVariant = parseHexColor(config.surfaceVariant, CerejaSurfaceVariant)
    val onSurface = parseHexColor(config.onSurface, CerejaWhite)
    val muted = parseHexColor(config.muted, CerejaMuted)
    val outline = parseHexColor(config.outline, CerejaOutline)
    val error = parseHexColor(config.error, CerejaError)

    val useDark = if (config.dark) true else isSystemInDarkTheme()
    val scheme = if (useDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onPrimary,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            outline = outline,
            outlineVariant = outline,
            error = error,
            onError = onPrimary,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerLow = surface,
            scrim = Color.Black
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onPrimary,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            outline = outline,
            error = error,
            onError = onPrimary
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = CerejaTypography,
        content = content
    )
}
