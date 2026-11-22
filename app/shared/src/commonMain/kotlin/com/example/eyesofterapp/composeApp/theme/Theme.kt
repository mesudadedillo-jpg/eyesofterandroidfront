package com.example.eyesofterapp.composeApp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eyesofterapp.app.shared.generated.resources.Res
import eyesofterapp.app.shared.generated.resources.montserrat_bold
import eyesofterapp.app.shared.generated.resources.montserrat_extrabold
import eyesofterapp.app.shared.generated.resources.montserrat_medium
import eyesofterapp.app.shared.generated.resources.montserrat_semibold
import org.jetbrains.compose.resources.Font

// ===========================================================================
// TEMA DE EYESOFTER (app)
//
// - COLORES y TIPOGRAFÍA: los mismos de EYESOFTER Web
//   (Content/eyesofter.css: --eyes-marino, --eyes-azul, --eyes-celeste… y Montserrat).
// - ESTILO (formas, bordes, jerarquía): "cuaderno de papel": fondo claro,
//   tarjetas blancas con borde de 1 dp y esquinas de 12 dp, SIN sombras ni
//   degradados, un solo botón relleno por pantalla y etiquetas en píldora.
//
// Si hay que cambiar un color, se cambia SOLO aquí.
// ===========================================================================

/** Paleta de EYESOFTER Web, con los mismos nombres que en eyesofter.css. */
object EyesColors {
    val Marino = Color(0xFF0A1A3F)      // --eyes-marino: títulos y texto principal
    val Marino700 = Color(0xFF102A63)   // --eyes-marino-700
    val Azul = Color(0xFF0B47A8)        // --eyes-azul: el ÚNICO color de botón relleno
    val Azul500 = Color(0xFF2F6FD6)     // --eyes-azul-500
    val Celeste = Color(0xFF9FC0F2)     // --eyes-celeste: acentos y anillos
    val Celeste200 = Color(0xFFC9DCF8)  // --eyes-celeste-200
    val Celeste100 = Color(0xFFE3EDFC)  // --eyes-celeste-100: fondo de botón "fantasma"
    val Celeste050 = Color(0xFFF3F7FE)  // --eyes-celeste-050
    val Gris = Color(0xFF8FA3C4)        // --eyes-gris
    val Fondo = Color(0xFFF6F8FC)       // --eyes-fondo: el "papel" de todas las pantallas
    val Blanco = Color(0xFFFFFFFF)      // superficie de las tarjetas

    // Semáforo de la web (U-02: siempre se acompaña de texto, nunca solo color)
    val VerdeBg = Color(0xFF1D7A3A)
    val VerdeFg = Color(0xFFFFFFFF)
    val AmarilloBg = Color(0xFFF5C542)
    val AmarilloFg = Color(0xFF3B2C00)
    val RojoBg = Color(0xFFB3261E)
    val RojoFg = Color(0xFFFFFFFF)

    // Jerarquía del texto: el MISMO marino con distinta opacidad,
    // en vez de inventar grises nuevos.
    val Texto = Marino
    val TextoSecundario = Marino.copy(alpha = 0.70f)
    val TextoTenue = Marino.copy(alpha = 0.55f)

    /** Borde fino de las tarjetas (marino al 10 %), en lugar de sombras. */
    val Borde = Marino.copy(alpha = 0.10f)
}

/** Esquinas compartidas. */
object EyesShapes {
    val Tarjeta = RoundedCornerShape(12.dp)
    val Boton = RoundedCornerShape(8.dp)
    val Pequena = RoundedCornerShape(4.dp)
    val Pildora = RoundedCornerShape(percent = 50)
}

/**
 * Colores para los componentes de Material 3 que todavía se usan tal cual
 * (campos de texto, chips, indicadores de carga). Así heredan la paleta.
 */
private val EyeSofterColors = lightColorScheme(
    primary = EyesColors.Azul,
    onPrimary = EyesColors.Blanco,
    primaryContainer = EyesColors.Celeste100,
    onPrimaryContainer = EyesColors.Marino,
    secondary = EyesColors.Marino700,
    onSecondary = EyesColors.Blanco,
    secondaryContainer = EyesColors.Celeste200,
    onSecondaryContainer = EyesColors.Marino,
    tertiaryContainer = EyesColors.Celeste050,
    onTertiaryContainer = EyesColors.Marino,
    background = EyesColors.Fondo,
    onBackground = EyesColors.Texto,
    surface = EyesColors.Blanco,
    onSurface = EyesColors.Texto,
    surfaceVariant = EyesColors.Celeste050,
    onSurfaceVariant = EyesColors.TextoSecundario,
    surfaceContainerLowest = EyesColors.Blanco,
    surfaceContainerLow = EyesColors.Blanco,
    surfaceContainer = EyesColors.Blanco,
    surfaceContainerHigh = EyesColors.Blanco,
    surfaceContainerHighest = EyesColors.Celeste050,
    outline = EyesColors.Gris,
    outlineVariant = EyesColors.Borde,
    error = EyesColors.RojoBg,
    onError = EyesColors.RojoFg,
    errorContainer = Color(0xFFFBE7E6),
    onErrorContainer = Color(0xFF5C120E)
)

/**
 * Montserrat, la letra de los títulos de la web. Los archivos .ttf están en
 * composeResources/font/ (licencia OFL en docs/licencias/).
 * Font(...) de Compose Multiplatform es @Composable porque lee el archivo.
 */
@Composable
private fun montserrat() = FontFamily(
    Font(Res.font.montserrat_medium, FontWeight.Medium),
    Font(Res.font.montserrat_semibold, FontWeight.SemiBold),
    Font(Res.font.montserrat_bold, FontWeight.Bold),
    Font(Res.font.montserrat_extrabold, FontWeight.ExtraBold)
)

/**
 * Escala de texto.
 *  - Títulos: Montserrat, con letras un poco más juntas (espaciado negativo)
 *    en los tamaños grandes para que se vean firmes y compactos.
 *  - Texto de lectura: la letra del sistema, igual que el cuerpo de la web
 *    (Bootstrap usa la del sistema). Sin espaciado extra.
 */
@Composable
private fun eyesTypography(): Typography {
    val titulos = montserrat()
    val cuerpo = FontFamily.Default
    return Typography(
        displaySmall = TextStyle(fontFamily = titulos, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em),
        headlineLarge = TextStyle(fontFamily = titulos, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em),
        headlineMedium = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp, letterSpacing = (-0.025).em),
        headlineSmall = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.011).em),
        titleLarge = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
        titleMedium = TextStyle(fontFamily = titulos, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = titulos, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = cuerpo, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = cuerpo, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = cuerpo, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = titulos, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = titulos, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
        // Etiquetas pequeñas en MAYÚSCULAS (encabezados de sección): un poco más separadas.
        labelSmall = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.06.em)
    )
}

/** Esquinas de Material 3: 8 dp en botones y campos, 12 dp en tarjetas. */
private val EyeSofterShapes = Shapes(
    extraSmall = EyesShapes.Pequena,
    small = EyesShapes.Boton,
    medium = EyesShapes.Tarjeta,
    large = EyesShapes.Tarjeta,
    extraLarge = EyesShapes.Tarjeta
)

@Composable
fun EyeSofterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EyeSofterColors,
        typography = eyesTypography(),
        shapes = EyeSofterShapes,
        content = content
    )
}
