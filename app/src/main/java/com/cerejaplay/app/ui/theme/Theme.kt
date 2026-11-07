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
    appearance: String = "dark",
    content: @Composable () -> Unit
) {
    val primary = parseHexColor(config.primary, CerejaRed)
    val onPrimary = parseHexColor(config.onPrimary, readableOn(primary))
    val secondary = parseHexColor(config.secondary, CerejaRedLight)
    val amoled = appearance == "amoled"
    val light = appearance == "light"
    val background = when {
        amoled -> Color.Black
        light -> Color(0xFFF8F7F9)
        else -> parseHexColor(config.background, CerejaBackground)
    }
    val surface = when {
        amoled -> Color(0xFF060608)
        light -> Color.White
        else -> parseHexColor(config.surface, CerejaSurface)
    }
    val surfaceVariant = when {
        amoled -> Color(0xFF101014)
        light -> Color(0xFFEFEDF2)
        else -> parseHexColor(config.surfaceVariant, CerejaSurfaceVariant)
    }
    val onSurface = when {
        light -> Color(0xFF17171C)
        else -> parseHexColor(config.onSurface, CerejaWhite)
    }
    val muted = when {
        light -> Color(0xFF5C5C66)
        else -> parseHexColor(config.muted, CerejaMuted)
    }
    val outline = when {
        light -> Color(0xFFD4D1DA)
        amoled -> Color(0xFF1E1E24)
        else -> parseHexColor(config.outline, CerejaOutline)
    }
    val error = parseHexColor(config.error, CerejaError)

    // O modo claro/escuro é uma preferência do usuário; "amoled" e "dark" são
    // escuros, "light" força o esquema claro.
    val useDark = !light && (amoled || config.dark || isSystemInDarkTheme())
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
            surfaceContainerHigh = if (amoled) Color(0xFF141418) else CerejaSurfaceElevated,
            surfaceContainerHighest = if (amoled) Color(0xFF1B1B20) else Color(0xFF242430),
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
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            surfaceContainerLow = background,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = Color(0xFFE6E3EA),
            outline = outline,
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
