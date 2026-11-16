package io.github.estebangocoalt.blocdenotas.ui.theme

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
    primary = Color(0xFF0B6E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE8E3),
    onPrimaryContainer = Color(0xFF00201C),
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEFE9DF),
    onSurfaceVariant = Color(0xFF5C554B),
    outline = Color(0xFF8A8173),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD3C6),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFCFE8E3),
    background = Color(0xFF1F1B16),
    onBackground = Color(0xFFEAE1D6),
    surface = Color(0xFF1F1B16),
    onSurface = Color(0xFFEAE1D6),
    surfaceVariant = Color(0xFF3A342C),
    onSurfaceVariant = Color(0xFFCFC6B6),
    outline = Color(0xFF978F80),
)

/**
 * Colores de las notas (los pasteles de los mockups). Se usan igual en tema claro y oscuro,
 * con texto oscuro encima, para que el contraste sea siempre bueno.
 */
class NoteSwatch(val background: Color, val content: Color, val accent: Color)

object NoteColors {
    val all: List<NoteSwatch> = listOf(
        NoteSwatch(Color(0xFFFFF0B3), Color(0xFF3F3510), Color(0xFF6B5A10)), // 0 amarillo
        NoteSwatch(Color(0xFFCFEBDD), Color(0xFF1F3A2F), Color(0xFF2F5A4B)), // 1 menta
        NoteSwatch(Color(0xFFFFD9C7), Color(0xFF4A2512), Color(0xFF7A3A1E)), // 2 melocotón
        NoteSwatch(Color(0xFFE3DCFA), Color(0xFF2A2548), Color(0xFF4A3F7A)), // 3 lila
        NoteSwatch(Color(0xFFD3E6F8), Color(0xFF14304A), Color(0xFF25496B)), // 4 cielo
        NoteSwatch(Color(0xFFEFE9DF), Color(0xFF1F1B16), Color(0xFF5C554B)), // 5 neutro
    )

    fun at(index: Int): NoteSwatch = all[index.coerceIn(0, all.lastIndex)]
}

@Composable
fun BlocTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
