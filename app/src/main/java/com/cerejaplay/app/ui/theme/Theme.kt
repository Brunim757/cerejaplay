package com.cerejaplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
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

private fun readableOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF07070B) else Color.White

private val CerejaShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

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
            primaryContainer = CerejaRedDeep,
            onPrimaryContainer = Color.White,
            secondary = CerejaGold,
            onSecondary = Color(0xFF231A00),
            secondaryContainer = Color(0xFF3B2E0C),
            onSecondaryContainer = CerejaGold,
            tertiary = CerejaLeaf,
            onTertiary = Color(0xFF00210B),
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = CerejaSurfaceElevated,
            surfaceContainerHighest = Color(0xFF242430),
            outline = outline,
            outlineVariant = outline,
            error = error,
            onError = onPrimary,
            scrim = Color.Black
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color(0xFFFFDADB),
            onPrimaryContainer = CerejaRedDeep,
            secondary = Color(0xFF8A6400),
            onSecondary = Color.White,
            background = Color(0xFFF8F7F9),
            onBackground = Color(0xFF17171C),
            surface = Color.White,
            onSurface = Color(0xFF17171C),
            surfaceVariant = Color(0xFFEFEDF2),
            onSurfaceVariant = Color(0xFF5C5C66),
            surfaceContainerLow = Color(0xFFF8F7F9),
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color(0xFFEFEDF2),
            surfaceContainerHighest = Color(0xFFE6E3EA),
            outline = Color(0xFFD4D1DA),
            outlineVariant = Color(0xFFE4E1E8),
            error = error,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = CerejaTypography,
        shapes = CerejaShapes,
        content = content
    )
}
