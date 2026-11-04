package com.example.eyesofterapp.composeApp.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ===========================================================================
// COMPONENTES DE EYESOFTER
// Piezas pequeñas que usan todas las pantallas, para que el estilo sea el
// mismo en todas partes. Cada una sigue una regla del estilo:
//   - Tarjeta blanca + borde de 1 dp + esquinas de 12 dp, sin sombra.
//   - Un solo botón RELLENO (azul) por pantalla; los demás son "fantasma"
//     (fondo celeste claro) o de solo texto.
//   - Etiquetas de estado en forma de píldora, con un punto + texto
//     (U-02: el estado nunca se comunica solo con el color).
// ===========================================================================

// ---------------------------------------------------------------------------
// Logotipo
// ---------------------------------------------------------------------------

/**
 * El logotipo de EYESOFTER Web (Views/Shared/_Logo.cshtml) convertido a
 * ImageVector: un ojo con mira en el iris y líneas de "circuito" a los lados.
 * Los trazos (pathData) son los mismos del SVG original, con su caja de 64 × 32.
 * Se dibuja en negro y luego se "tiñe" con el color que se pida.
 */
private val LogoEyesofter: ImageVector by lazy {
    // Ayudante: agrega un trazo SVG con el grosor indicado (o relleno si fill = true).
    fun ImageVector.Builder.trazo(d: String, grosor: Float = 2f, fill: Boolean = false) =
        addPath(
            pathData = addPathNodes(d),
            fill = if (fill) SolidColor(Color.Black) else null,
            stroke = if (fill) null else SolidColor(Color.Black),
            strokeLineWidth = grosor,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )

    ImageVector.Builder(
        name = "LogoEyesofter",
        defaultWidth = 64.dp, defaultHeight = 32.dp,
        viewportWidth = 64f, viewportHeight = 32f
    ).apply {
        // Contorno del ojo
        trazo("M13 16c5-7.5 11.5-11 19-11s14 3.5 19 11c-5 7.5-11.5 11-19 11S18 23.5 13 16z")
        // Iris (círculo de radio 6.5) y pupila (círculo relleno de radio 2.4)
        trazo("M25.5 16a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0 -13 0z")
        trazo("M29.6 16a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0z", fill = true)
        // Mira (cuatro marcas alrededor del iris)
        trazo("M32 7.5v3M32 21.5v3M23.5 16h3M37.5 16h3", grosor = 1.6f)
        // Líneas de circuito y sus terminales
        trazo("M13 16H8l-3.5-4M13 16H8l-3.5 4M51 16h5l3.5-4M51 16h5l3.5 4", grosor = 1.6f)
        for (c in listOf("M2.3 11a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0z",
                         "M2.3 21a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0z",
                         "M59.1 11a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0z",
                         "M59.1 21a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0z")) {
            trazo(c, fill = true)
        }
    }.build()
}

/** Dibuja el logotipo. width: ancho deseado (la altura es la mitad). */
@Composable
fun EyesLogo(width: Dp, color: Color = EyesColors.Marino, modifier: Modifier = Modifier) {
    Image(
        imageVector = LogoEyesofter,
        contentDescription = "Logotipo de EYESOFTER",
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.width(width).aspectRatio(2f)
    )
}

/** Logotipo + nombre "EYESOFTER", como la marca de la barra de la web. */
@Composable
fun EyesMarca(color: Color = EyesColors.Marino, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        EyesLogo(width = 36.dp, color = color)
        Text(
            text = "EYESOFTER",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Bandas y franjas diagonales (las "líneas azules" de EYESOFTER Web)
// ---------------------------------------------------------------------------

/**
 * Fondo de la barra superior, igual que .navbar-eyesofter de la web:
 * marino con dos franjas diagonales (marino-700 y azul).
 *
 * En CSS es linear-gradient(115deg, marino 0 64%, marino-700 64% 72%,
 * azul 72% 78%, marino 78% 100%). Repetir un color en la misma posición
 * (por ejemplo 0.64 y 0.64) hace un corte limpio en vez de un degradado,
 * y así salen franjas.
 */
fun Modifier.franjasBarra(): Modifier = drawBehind {
    // Dirección de 115° de CSS: hacia la derecha y un poco hacia abajo.
    val angulo = 115.0 * PI / 180.0
    val dx = sin(angulo).toFloat()
    val dy = (-cos(angulo)).toFloat()
    // Largo de la línea del degradado, como lo calcula el navegador.
    val largo = abs(size.width * dx) + abs(size.height * dy)
    val centro = Offset(size.width / 2, size.height / 2)
    val inicio = Offset(centro.x - dx * largo / 2, centro.y - dy * largo / 2)
    val fin = Offset(centro.x + dx * largo / 2, centro.y + dy * largo / 2)
    drawRect(
        Brush.linearGradient(
            0f to EyesColors.Marino, 0.64f to EyesColors.Marino,
            0.64f to EyesColors.Marino700, 0.72f to EyesColors.Marino700,
            0.72f to EyesColors.Azul, 0.78f to EyesColors.Azul,
            0.78f to EyesColors.Marino, 1f to EyesColors.Marino,
            start = inicio, end = fin
        )
    )
}

/**
 * Las tres bandas diagonales de la portada y del login de la web
 * (.banda-1, .banda-2 y .banda-3 en eyesofter.css), sobre fondo marino.
 * Cada banda mide 170 % de ancho y 17 % de alto, empieza en -55 % y está
 * girada -45°. Se calculan sobre un cuadrado del lado mayor del área, para
 * que se vean igual en pantallas anchas o angostas.
 */
@Composable
fun EyesBandas(modifier: Modifier = Modifier) {
    // clipToBounds: las bandas giradas son más grandes que el área; esto recorta
    // lo que sobra (como overflow: hidden en el CSS de la web).
    Canvas(modifier.clipToBounds()) {
        drawRect(EyesColors.Marino)
        val lado = max(size.width, size.height)
        val origen = Offset((size.width - lado) / 2, (size.height - lado) / 2)
        // (posición vertical, colores del degradado) de cada banda. La primera usa
        // marino-700 en vez de marino para que se distinga del fondo marino.
        val bandas = listOf(
            0.08f to listOf(EyesColors.Marino700, EyesColors.Azul),
            0.40f to listOf(EyesColors.Marino700, EyesColors.Azul, EyesColors.Celeste),
            0.72f to listOf(EyesColors.Azul, EyesColors.Celeste200)
        )
        for ((arriba, colores) in bandas) {
            val tl = Offset(origen.x - 0.55f * lado, origen.y + arriba * lado)
            val tamano = Size(1.70f * lado, 0.17f * lado)
            val centroBanda = Offset(tl.x + tamano.width / 2, tl.y + tamano.height / 2)
            rotate(degrees = -45f, pivot = centroBanda) {
                drawRect(
                    brush = Brush.horizontalGradient(colores, startX = tl.x, endX = tl.x + tamano.width),
                    topLeft = tl,
                    size = tamano
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Barra superior
// ---------------------------------------------------------------------------

/**
 * Barra superior como la de la web: fondo marino con dos franjas diagonales,
 * la marca en blanco a la izquierda y una acción de texto a la derecha
 * (por ejemplo "Salir" o "Volver"). Pinta también detrás de la barra de estado.
 */
@Composable
fun EyesBarraSuperior(accion: String, onAccion: () -> Unit) {
    Column(Modifier.fillMaxWidth().franjasBarra().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EyesMarca(color = EyesColors.Blanco)
            EyesBotonTexto(accion, onAccion, color = EyesColors.Blanco)
        }
    }
}

// ---------------------------------------------------------------------------
// Tarjetas
// ---------------------------------------------------------------------------

/** Tarjeta blanca estándar: borde de 1 dp, esquinas de 12 dp, sin sombra. */
@Composable
fun EyesTarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            // Si se puede tocar, se encoge un poco al presionarla (respuesta al toque).
            .then(if (onClick != null) Modifier.presionable(interaccion, 0.98f) else Modifier)
            .clip(EyesShapes.Tarjeta)
            .background(EyesColors.Blanco)
            .border(1.dp, EyesColors.Borde, EyesShapes.Tarjeta)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaccion, indication = ripple(), onClick = onClick)
                else Modifier
            )
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/**
 * Tarjeta de acento: un panel pintado con un color de la paleta y SIN borde.
 * Se usa para destacar un bloque, en lugar de añadir sombras.
 */
@Composable
fun EyesTarjetaAcento(
    fondo: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(EyesShapes.Tarjeta)
            .background(fondo)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

// ---------------------------------------------------------------------------
// Píldoras y textos pequeños
// ---------------------------------------------------------------------------

/** Etiqueta en forma de píldora: punto opcional + texto. */
@Composable
fun EyesPildora(
    texto: String,
    fondo: Color,
    textoColor: Color,
    conPunto: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(EyesShapes.Pildora)
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (conPunto) {
            Box(Modifier.size(6.dp).background(textoColor, CircleShape))
            Box(Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = textoColor)
    }
}

/** Estado de una óptica. Verde = asociada al programa; celeste = externa. */
@Composable
fun EyesPildoraOptica(verificada: Boolean) {
    if (verificada) {
        EyesPildora("Asociada", EyesColors.VerdeBg, EyesColors.VerdeFg)
    } else {
        EyesPildora("Externa", EyesColors.Celeste100, EyesColors.Marino)
    }
}

/** Etiqueta pequeña en mayúsculas que va arriba de un título ("eyebrow"). */
@Composable
fun EyesEtiqueta(texto: String, color: Color = EyesColors.Azul) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
}

// ---------------------------------------------------------------------------
// Botones
// ---------------------------------------------------------------------------

/** Botón PRINCIPAL (relleno azul). Úsalo una sola vez por pantalla. */
@Composable
fun EyesBotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false
) {
    val interaccion = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        interactionSource = interaccion,
        shape = EyesShapes.Boton,
        colors = ButtonDefaults.buttonColors(
            containerColor = EyesColors.Azul,
            contentColor = EyesColors.Blanco,
            disabledContainerColor = EyesColors.Celeste200,
            disabledContentColor = EyesColors.TextoTenue
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier.presionable(interaccion).heightIn(min = 44.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(18.dp), color = EyesColors.Azul, strokeWidth = 2.dp)
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** Botón "fantasma": fondo celeste claro y texto azul. Para acciones secundarias. */
@Composable
fun EyesBotonFantasma(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaccion = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaccion,
        shape = EyesShapes.Boton,
        colors = ButtonDefaults.buttonColors(
            containerColor = EyesColors.Celeste100,
            contentColor = EyesColors.Azul,
            disabledContainerColor = EyesColors.Celeste050,
            disabledContentColor = EyesColors.TextoTenue
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier.presionable(interaccion).heightIn(min = 44.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Botón de solo texto (marino), para acciones de menor peso como "Salir". */
@Composable
fun EyesBotonTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = EyesColors.Marino
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = EyesShapes.Boton,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        modifier = modifier
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Opción en forma de píldora para filtros y selectores.
 * Seleccionada: marino con texto blanco. Sin seleccionar: blanca con borde.
 */
@Composable
fun EyesOpcion(texto: String, seleccionada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = EyesShapes.Pildora
    // Los filtros se tocan seguido: solo cambia el color, rápido (150 ms) y sin moverse.
    val fondo by animateColorAsState(
        if (seleccionada) EyesColors.Marino else EyesColors.Blanco,
        tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "fondoOpcion"
    )
    val texto2 by animateColorAsState(
        if (seleccionada) EyesColors.Blanco else EyesColors.Marino,
        tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "textoOpcion"
    )
    val borde by animateColorAsState(
        if (seleccionada) EyesColors.Marino else EyesColors.Borde,
        tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "bordeOpcion"
    )
    Box(
        modifier = modifier
            .clip(forma)
            .background(fondo)
            .border(BorderStroke(1.dp, borde), forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium, color = texto2)
    }
}

/** Fila de opciones que se reparte el ancho (por ejemplo "Mapa | Lista"). */
@Composable
fun EyesSelector(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .clip(EyesShapes.Pildora)
            .background(EyesColors.Celeste100)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

/** Opción dentro de un EyesSelector: la seleccionada queda blanca. */
@Composable
fun RowScope.EyesSelectorOpcion(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    val forma = EyesShapes.Pildora
    // Indica el estado: la opción elegida "se ilumina" en blanco con una transición corta.
    val fondo by animateColorAsState(
        if (seleccionada) EyesColors.Blanco else EyesColors.Celeste100,
        tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "fondoSelector"
    )
    val textoColor by animateColorAsState(
        if (seleccionada) EyesColors.Marino else EyesColors.TextoSecundario,
        tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "textoSelector"
    )
    Box(
        modifier = Modifier
            .clip(forma)
            .background(fondo)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium, color = textoColor)
    }
}

/** Línea fina para separar bloques dentro de una tarjeta. */
@Composable
fun EyesSeparador() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(EyesColors.Borde))
}
