package com.example.eyesofterapp.composeApp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.domain.model.distanciaHaversineKm
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.eyesofterapp.composeApp.theme.EyesBotonFantasma
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesMotion
import com.example.eyesofterapp.composeApp.theme.EyesPildora
import com.example.eyesofterapp.composeApp.theme.EyesPildoraOptica
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.Optica

/**
 * Mapa ilustrado (dibujado con Canvas, sin servicio de mapas): calles en
 * blanco y celeste sobre el fondo de la paleta, las ópticas como puntos
 * colocados según su dirección y distancia reales (ver posicionesRadar) y
 * controles redondos con borde fino.
 */
@Composable
fun MapaOpticasView(
    userCoords: Coordenadas?,
    esUbicacionSimulada: Boolean = false,
    opticas: List<Optica>,
    opticaSeleccionada: Optica?,
    onSeleccionarOptica: (Optica) -> Unit,
    onCentrarUbicacion: () -> Unit,
    onSimularUbicacion: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    var zoomLevel by remember { mutableStateOf(1f) }
    var mapaExpandido by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (mapaExpandido) 520.dp else 380.dp)
            .clip(EyesShapes.Tarjeta)
            .background(EyesColors.Celeste050)
            .border(1.dp, EyesColors.Borde, EyesShapes.Tarjeta)
    ) {
        // Fondo: cuadrícula de calles que se escala con el zoom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val strokeWidth = 6f * zoomLevel
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f * zoomLevel, 15f * zoomLevel), 0f)
            val stepX = (canvasWidth / 5f) * zoomLevel
            val stepY = (canvasHeight / 5f) * zoomLevel

            // Calles principales
            var y = 0f
            while (y < canvasHeight) {
                drawLine(EyesColors.Blanco, Offset(0f, y), Offset(canvasWidth, y), strokeWidth = strokeWidth * 2)
                y += stepY.coerceAtLeast(40f)
            }
            var x = 0f
            while (x < canvasWidth) {
                drawLine(EyesColors.Blanco, Offset(x, 0f), Offset(x, canvasHeight), strokeWidth = strokeWidth * 2)
                x += stepX.coerceAtLeast(40f)
            }

            // Calles secundarias discontinuas
            var secY = 0f
            while (secY < canvasHeight) {
                drawLine(
                    color = EyesColors.Celeste200,
                    start = Offset(0f, secY),
                    end = Offset(canvasWidth, secY),
                    strokeWidth = strokeWidth,
                    pathEffect = dashPathEffect
                )
                secY += (stepY / 2f).coerceAtLeast(20f)
            }
        }

        // Marcadores: se colocan como en un RADAR alrededor del usuario.
        //  - Dirección real: el ángulo apunta hacia donde está la óptica.
        //  - Distancia en escala logarítmica: las cercanas (Tulancingo) quedan
        //    separadas entre sí y las lejanas (Monterrey) no se salen del mapa.
        //  - Solo la seleccionada muestra su nombre; las demás son puntos que se
        //    pueden tocar. Así ningún texto se encima.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val densidad = LocalDensity.current
            val anchoPx = constraints.maxWidth.toFloat()
            val altoPx = constraints.maxHeight.toFloat()
            // Se reserva la parte de abajo para la tarjeta de la óptica seleccionada.
            val reservaAbajo = with(densidad) { 176.dp.toPx() }
            val margen = with(densidad) { 30.dp.toPx() }
            val separacion = with(densidad) { 30.dp.toPx() }
            val centro = Offset(anchoPx / 2, ((altoPx - reservaAbajo) / 2 + margen / 2).coerceAtLeast(margen * 2))
            val radioMax = (min(anchoPx / 2 - margen * 1.6f, centro.y - margen)).coerceAtLeast(margen)

            // Sin GPS, el centro es el promedio de las ópticas.
            val origen = userCoords ?: if (opticas.isEmpty()) null
                else Coordenadas(opticas.map { it.lat }.average(), opticas.map { it.lng }.average())

            val posiciones = remember(opticas, origen, zoomLevel, anchoPx, altoPx) {
                if (origen == null) emptyMap()
                else posicionesRadar(opticas, origen, zoomLevel, centro, radioMax, separacion)
            }

            opticas.forEach { optica ->
                val destino = posiciones[optica.id] ?: return@forEach
                key(optica.id) {
                    MarcadorOptica(
                        optica = optica,
                        destino = destino,
                        seleccionada = opticaSeleccionada?.id == optica.id,
                        anchoPx = anchoPx,
                        onClick = { onSeleccionarOptica(optica) }
                    )
                }
            }

            // Marcador del usuario en el centro: anillo marino (como el iris del logotipo).
            if (userCoords != null) {
                Box(
                    Modifier
                        .centradoEn(centro.x, centro.y, anchoPx)
                        .size(22.dp)
                        .background(EyesColors.Blanco, CircleShape)
                        .border(6.dp, EyesColors.Marino, CircleShape)
                )
                val dyPildora = with(densidad) { 26.dp.toPx() }
                Box(Modifier.centradoEn(centro.x, centro.y + dyPildora, anchoPx)) {
                    if (esUbicacionSimulada) {
                        EyesPildora("Tú (simulado)", EyesColors.AmarilloBg, EyesColors.AmarilloFg)
                    } else {
                        EyesPildora("Tú estás aquí", EyesColors.Marino, EyesColors.Blanco)
                    }
                }
            }
        }

        // Controles del mapa: acercar, alejar, agrandar y centrar
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ControlMapa("+", "Acercar") { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f) }
            ControlMapa("−", "Alejar") { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.6f) }
            ControlMapa(if (mapaExpandido) "↥" else "↧", "Cambiar tamaño", activo = mapaExpandido) {
                mapaExpandido = !mapaExpandido
            }
            ControlMapa("◎", "Centrar en mi ubicación") {
                zoomLevel = 1f
                onCentrarUbicacion()
            }
        }

        // Tarjeta de la óptica seleccionada, flotando abajo. Al elegir otra óptica,
        // la nueva sube desde abajo (de donde "sale" la tarjeta) con fundido, y la
        // anterior se desvanece: muestra que el contenido cambió, sin un salto.
        AnimatedContent(
            targetState = opticaSeleccionada,
            contentKey = { it?.id },
            transitionSpec = {
                (fadeIn(tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)) +
                    slideInVertically(tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)) { it / 6 })
                    .togetherWith(fadeOut(tween(EyesMotion.Salida)))
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            label = "opticaSeleccionada"
        ) { seleccion ->
          seleccion?.let { optica ->
            EyesTarjeta(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                padding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(optica.nombre, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                        EyesPildoraOptica(optica.verificada)
                    }
                    if (optica.distanciaKm > 0.0) {
                        Text(optica.distanciaTexto(), style = MaterialTheme.typography.titleSmall, color = EyesColors.Azul)
                    }
                }
                Text(
                    text = "${optica.direccion}, ${optica.ciudad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EyesColors.TextoSecundario
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EyesBotonFantasma(
                        "Cómo llegar",
                        onClick = { try { uriHandler.openUri(optica.urlComoLlegar()) } catch (_: Exception) { } },
                        modifier = Modifier.weight(1f)
                    )
                    EyesBotonTexto(
                        "Llamar",
                        onClick = { try { uriHandler.openUri("tel:${optica.telefono}") } catch (_: Exception) { } },
                        color = EyesColors.Azul
                    )
                }
            }
          }
        }
    }
}

/**
 * Calcula dónde va cada óptica (en píxeles) dentro del mapa ilustrado.
 *
 * 1. Ángulo: dirección real de la óptica vista desde [origen]. La longitud se
 *    multiplica por cos(latitud) porque los meridianos se juntan hacia los polos.
 * 2. Radio: ln(1 + km) normalizado. Con escala logarítmica, 0.3 km y 3 km se
 *    distinguen bien, y 800 km no se sale del mapa. Empieza en 22 % del radio
 *    para no tapar el marcador del usuario. El zoom multiplica el radio.
 * 3. Si un punto queda a menos de [separacion] de otro, se gira un poco
 *    (+0.3, -0.3, +0.6... radianes) y, si no basta, se aleja un paso del
 *    centro, hasta encontrar lugar libre.
 * Las que con el zoom quedan fuera del área no se dibujan.
 */
internal fun posicionesRadar(
    opticas: List<Optica>,
    origen: Coordenadas,
    zoom: Float,
    centro: Offset,
    radioMax: Float,
    separacion: Float
): Map<Int, Offset> {
    if (opticas.isEmpty()) return emptyMap()
    val distancias = opticas.map { distanciaHaversineKm(origen.lat, origen.lng, it.lat, it.lng) }
    val maxKm = max(distancias.max(), 1.0)
    val cosLat = cos(origen.lat * PI / 180.0)
    val ocupados = mutableListOf(centro)           // el usuario también ocupa lugar
    val resultado = mutableMapOf<Int, Offset>()

    opticas.forEachIndexed { i, o ->
        val dx = (o.lng - origen.lng) * cosLat
        val dy = -(o.lat - origen.lat)            // en pantalla, "arriba" es y negativa
        val anguloBase = if (dx == 0.0 && dy == 0.0) i * 0.9 else atan2(dy, dx)
        val fraccion = 0.22 + 0.78 * ln(1 + distancias[i]) / ln(1 + maxKm)
        val r = (fraccion * radioMax * zoom).toFloat()
        if (r > radioMax * 1.05f) return@forEachIndexed

        fun punto(angulo: Double, radio: Float) = Offset(
            centro.x + (cos(angulo) * radio).toFloat(),
            centro.y + (sin(angulo) * radio).toFloat()
        )
        // Prueba el ángulo real y giros alternados; si cerca del centro no alcanza
        // (los giros recorren poca distancia), aleja el punto un paso y vuelve a probar.
        val giros = listOf(0.0, 0.3, -0.3, 0.6, -0.6, 0.9, -0.9, 1.2, -1.2)
        val radios = listOf(r, r + separacion * 0.8f, r + separacion * 1.6f, r + separacion * 2.4f)
            .map { it.coerceAtMost(radioMax) }
        val elegido = radios.asSequence()
            .flatMap { radio -> giros.asSequence().map { punto(anguloBase + it, radio) } }
            .firstOrNull { p -> ocupados.none { (it - p).getDistance() < separacion } }
            ?: punto(anguloBase, r)
        ocupados += elegido
        resultado[o.id] = elegido
    }
    return resultado
}

/**
 * Un marcador: punto de color (verde = asociada, gris = externa, azul =
 * seleccionada) con un área de toque de 40 dp. La seleccionada muestra su
 * nombre en una píldora arriba del punto.
 * Al hacer zoom, el punto se desliza a su nueva posición (200 ms, ease-in-out:
 * es movimiento dentro de la pantalla) en vez de brincar.
 */
@Composable
private fun MarcadorOptica(optica: Optica, destino: Offset, seleccionada: Boolean, anchoPx: Float, onClick: () -> Unit) {
    val posicion by animateOffsetAsState(destino, tween(EyesMotion.Pantalla, easing = EyesMotion.EaseInOut), label = "posMarcador")
    val colorDestino = when {
        seleccionada -> EyesColors.Azul
        optica.verificada -> EyesColors.VerdeBg
        else -> EyesColors.Gris
    }
    val color by animateColorAsState(colorDestino, tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "colorMarcador")
    val tamano by animateDpAsState(if (seleccionada) 20.dp else 14.dp, tween(EyesMotion.Color, easing = EyesMotion.EaseOut), label = "tamMarcador")

    // Área de toque (invisible) + punto
    Box(
        modifier = Modifier
            .centradoEn(posicion.x, posicion.y, anchoPx)
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Seleccionar ${optica.nombre}", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(tamano)
                .background(color, CircleShape)
                .border(if (seleccionada) 3.dp else 2.dp, EyesColors.Blanco, CircleShape)
        )
    }

    if (seleccionada) {
        val dy = with(LocalDensity.current) { 28.dp.toPx() }
        Text(
            text = optica.nombre,
            style = MaterialTheme.typography.labelMedium,
            color = EyesColors.Blanco,
            maxLines = 1,
            modifier = Modifier
                .centradoEn(posicion.x, posicion.y - dy, anchoPx)
                .aparecer(desplazamiento = 4.dp)
                .clip(EyesShapes.Pildora)
                .background(EyesColors.Azul)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Coloca el elemento con su CENTRO en (x, y), en píxeles dentro del mapa.
 * Si se saldría por la izquierda o la derecha, lo recorre para que quepa
 * (con 8 px de margen). Funciona dentro de un Box que acomoda arriba a la izquierda.
 */
private fun Modifier.centradoEn(x: Float, y: Float, anchoPx: Float): Modifier = layout { medible, restricciones ->
    val p = medible.measure(restricciones.copy(minWidth = 0, minHeight = 0))
    val izquierda = (x - p.width / 2f).coerceIn(8f, max(8f, anchoPx - p.width - 8f))
    layout(p.width, p.height) {
        p.place(izquierda.roundToInt(), (y - p.height / 2f).roundToInt())
    }
}

/** Botón redondo blanco con borde fino para los controles del mapa. */
@Composable
private fun ControlMapa(simbolo: String, descripcion: String, activo: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (activo) EyesColors.Celeste100 else EyesColors.Blanco)
            .border(1.dp, EyesColors.Borde, CircleShape)
            .clickable(onClickLabel = descripcion, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(simbolo, style = MaterialTheme.typography.titleMedium, color = EyesColors.Marino)
    }
}
