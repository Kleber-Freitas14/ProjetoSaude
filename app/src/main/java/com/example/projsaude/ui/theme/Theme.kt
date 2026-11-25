package com.example.projsaude.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF064FB5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E8FC),
    onPrimaryContainer = Color(0xFF10345E),

    secondary = Color(0xFF355D85),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EEFC),
    onSecondaryContainer = Color(0xFF173654),

    tertiary = Color(0xFF006680),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCBEFFC),
    onTertiaryContainer = Color(0xFF003849),

    background = Color(0xFFF0F6FE),
    onBackground = Color(0xFF142B45),
    surface = Color.White,
    onSurface = Color(0xFF142B45),
    surfaceVariant = Color(0xFFE9F2FF),
    onSurfaceVariant = Color(0xFF35516F),
    surfaceTint = Color(0xFF064FB5),

    outline = Color(0xFF647D98),
    outlineVariant = Color(0xFFC8DBEF),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = Color(0xFF671C16)
)

private val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFFA9C9FF),
    onPrimary = Color(0xFF002F6B),
    primaryContainer = Color(0xFF103C74),
    onPrimaryContainer = Color(0xFFD7E8FC),

    secondary = Color(0xFFA6C7EB),
    onSecondary = Color(0xFF163553),
    secondaryContainer = Color(0xFF243F5E),
    onSecondaryContainer = Color(0xFFD7E8FC),

    tertiary = Color(0xFF8DD3E9),
    onTertiary = Color(0xFF003544),
    tertiaryContainer = Color(0xFF004C60),
    onTertiaryContainer = Color(0xFFCBEFFC),

    background = Color(0xFF0D1726),
    onBackground = Color(0xFFE8F0FC),
    surface = Color(0xFF142236),
    onSurface = Color(0xFFE8F0FC),
    surfaceVariant = Color(0xFF22354B),
    onSurfaceVariant = Color(0xFFC3D3E7),
    surfaceTint = Color(0xFFA9C9FF),

    outline = Color(0xFF8DA4BF),
    outlineVariant = Color(0xFF3C536E),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7C2420),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val FormasProjetoSaude = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ProjSaudeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val esquema = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(contexto)
            else dynamicLightColorScheme(contexto)
        }
        darkTheme -> EsquemaEscuro
        else -> EsquemaClaro
    }

    MaterialTheme(
        colorScheme = esquema,
        typography = Typography,
        shapes = FormasProjetoSaude,
        content = content
    )
}