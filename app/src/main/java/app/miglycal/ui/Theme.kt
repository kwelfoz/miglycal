package app.miglycal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp

/** Paletas selecionáveis nas definições. */
enum class Accent(val key: String, val label: String, val color: Color) {
    GREEN("green", "verde", Color(0xFF55E67A)),
    YELLOW("yellow", "amarelo", Color(0xFFF4D03F)),
    ORANGE("orange", "laranja", Color(0xFFFF9F43)),
    RED("red", "vermelho", Color(0xFFFF5F5F));

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key } ?: GREEN
    }
}

/**
 * Paleta derivada do acento: fundo quase preto com um toque do acento (superfícies
 * tonais ao estilo Material You) e texto/linhas em tons do acento (estilo terminal).
 */
@Immutable
class Pal(val accent: Color) {
    val bg: Color = lerp(Color(0xFF080908), accent, 0.025f)
    val surface: Color = lerp(bg, accent, 0.07f)
    val surfaceHi: Color = lerp(bg, accent, 0.13f)
    val line: Color = lerp(bg, accent, 0.30f)
    val dim: Color = lerp(bg, accent, 0.62f)
    val text: Color = lerp(Color(0xFFEDEDED), accent, 0.16f)
}

val LocalPal = staticCompositionLocalOf { Pal(Accent.GREEN.color) }

@Composable
fun MiglycalTheme(accent: Accent, content: @Composable () -> Unit) {
    val pal = remember(accent) { Pal(accent.color) }
    val scheme = darkColorScheme(
        primary = pal.accent,
        onPrimary = pal.bg,
        secondary = pal.accent,
        onSecondary = pal.bg,
        background = pal.bg,
        onBackground = pal.text,
        surface = pal.surface,
        onSurface = pal.text,
        surfaceVariant = pal.surfaceHi,
        onSurfaceVariant = pal.dim,
        outline = pal.line,
        outlineVariant = pal.line,
        surfaceContainerLowest = pal.bg,
        surfaceContainerLow = pal.surface,
        surfaceContainer = pal.surfaceHi,
        surfaceContainerHigh = pal.surfaceHi,
        surfaceContainerHighest = pal.surfaceHi,
    )
    val sharp = Shapes(
        extraSmall = RectangleShape,
        small = RectangleShape,
        medium = RectangleShape,
        large = RectangleShape,
        extraLarge = RectangleShape,
    )
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = scheme, shapes = sharp, content = content)
    }
}
