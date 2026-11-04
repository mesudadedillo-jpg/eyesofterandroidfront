package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.data.network.distanciaKm
import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.Optica
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** A zoom 1 el mapa muestra ±5 km alrededor del centro. */
private const val RADIO_BASE_KM = 5.0
private const val ZOOM_MIN = 0.1f   // ±50 km
private const val ZOOM_MAX = 10f    // ±500 m
/** Más marcadores que esto vuelven el mapa ilegible en un teléfono. */
private const val MAX_MARCADORES = 40

private val COLOR_ASOCIADA = Color(0xFF1B7A3A)
private val COLOR_OTRA = Color(0xFF6B7280)

/**
 * Mapa esquemático (sin mosaicos) que funciona en todas las plataformas.
 * Coloca cada óptica según su latitud/longitud respecto al centro (norte arriba).
 * El mapa real con calles está en Android: botón «Mapa» → PantallaMapa (MapLibre).
 */
@Composable
fun MapaOpticasView(
    userCoords: Coordenadas?,
    esUbicacionSimulada: Boolean = false,
    opticas: List<Optica>,
    opticaSeleccionada: Optica?,
    onSeleccionarOptica: (Optica) -> Unit,
    onCentrarUbicacion: () -> Unit,
    onSimularUbicacion: (() -> Unit)? = null,
    etiquetaCentro: String = if (esUbicacionSimulada) "Demostración" else "Tú estás aquí"
) {
    val referencia = userCoords ?: opticas.firstOrNull()?.let { Coordenadas(it.lat, it.lng) }
    val zoomInicial = remember(referencia, opticas) { calcularZoomInicial(referencia, opticas) }
    var zoomLevel by remember(zoomInicial) { mutableStateOf(zoomInicial) }
    var mapaExpandido by remember { mutableStateOf(false) }
    val visibles = remember(opticas) { opticas.sortedBy { it.distanciaKm }.take(MAX_MARCADORES) }
    val radioKm = RADIO_BASE_KM / zoomLevel

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (mapaExpandido) 520.dp else 380.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE2ECE9))
                .semantics {
                    contentDescription = "Mapa esquemático con ${visibles.size} ópticas alrededor del centro. " +
                        "Con lector de pantalla es más cómoda la vista Lista."
                }
        ) {
            // Fondo: cuadrícula tipo calles que se ajusta al zoom
            Canvas(modifier = Modifier.fillMaxSize()) {
                val grosor = (6f * zoomLevel).coerceIn(2f, 14f)
                val paso = (size.width / 5f * zoomLevel).coerceIn(40f, size.width)
                val discontinua = PathEffect.dashPathEffect(floatArrayOf(grosor * 2, grosor * 2), 0f)
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.White, Offset(0f, y), Offset(size.width, y), strokeWidth = grosor * 2)
                    drawLine(Color(0xFFCDE0D8), Offset(0f, y + paso / 2), Offset(size.width, y + paso / 2), strokeWidth = grosor, pathEffect = discontinua)
                    y += paso
                }
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = grosor * 2)
                    x += paso
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (referencia != null) {
                    val kmPorGradoLng = 111.32 * cos(referencia.lat * PI / 180.0)
                    // La seleccionada se dibuja al final para que quede encima.
                    val orden = visibles.sortedBy { it.id == opticaSeleccionada?.id }
                    orden.forEach { optica ->
                        val dxKm = (optica.lng - referencia.lng) * kmPorGradoLng
                        val dyKm = (optica.lat - referencia.lat) * 110.57
                        if (abs(dxKm) > radioKm || abs(dyKm) > radioKm) return@forEach

                        val fx = (0.5 + dxKm / (2 * radioKm)).toFloat()
                        val fy = (0.5 - dyKm / (2 * radioKm)).toFloat()
                        val seleccionada = opticaSeleccionada?.id == optica.id
                        // Solo la seleccionada y las asociadas llevan nombre: así no se enciman.
                        val conEtiqueta = seleccionada || optica.verificada
                        val ajusteY = if (conEtiqueta) 36.dp else 16.dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(
                                    start = (maxWidth * fx - 16.dp).coerceIn(0.dp, (maxWidth - 48.dp).coerceAtLeast(0.dp)),
                                    top = (maxHeight * fy - ajusteY).coerceIn(0.dp, (maxHeight - 48.dp).coerceAtLeast(0.dp))
                                )
                                .clickable(onClickLabel = "ver detalles", role = Role.Button) { onSeleccionarOptica(optica) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${optica.nombre}, a ${distanciaHablada(optica.distanciaKm)}" +
                                        if (optica.verificada) ", asociada EYESOFTER" else ""
                                }
                                .padding(8.dp)
                        ) {
                            if (conEtiqueta) {
                                Surface(
                                    color = if (seleccionada) MaterialTheme.colorScheme.primary else COLOR_ASOCIADA,
                                    shape = RoundedCornerShape(6.dp),
                                    shadowElevation = if (seleccionada) 6.dp else 2.dp
                                ) {
                                    Text(
                                        text = optica.nombre,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp).padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (seleccionada) 18.dp else 14.dp)
                                    .background(
                                        when {
                                            seleccionada -> MaterialTheme.colorScheme.primary
                                            optica.verificada -> COLOR_ASOCIADA
                                            else -> COLOR_OTRA
                                        },
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                // Centro: el usuario (GPS), el lugar buscado o la demostración
                if (userCoords != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center).semantics(mergeDescendants = true) {
                            contentDescription = etiquetaCentro
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(if (esUbicacionSimulada) Color(0xFFB45309) else Color(0xFFC62828), CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                        )
                        Surface(
                            color = if (esUbicacionSimulada) Color(0xFFB45309) else Color(0xFFC62828),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = etiquetaCentro,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Controles (48 dp, con descripción para TalkBack)
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotonMapa("+", "Acercar") { zoomLevel = (zoomLevel * 1.5f).coerceAtMost(ZOOM_MAX) }
                BotonMapa("−", "Alejar") { zoomLevel = (zoomLevel / 1.5f).coerceAtLeast(ZOOM_MIN) }
                BotonMapa(if (mapaExpandido) "▲" else "▼", if (mapaExpandido) "Hacer el mapa más chico" else "Hacer el mapa más grande") {
                    mapaExpandido = !mapaExpandido
                }
                BotonMapa("◎", "Volver a mi ubicación") {
                    zoomLevel = zoomInicial
                    onCentrarUbicacion()
                }
            }
        }

        Text(
            text = "Área mostrada: unos ${formatoDistancia(radioKm * 2)} de lado. Verde = asociada EYESOFTER, gris = otras ópticas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BotonMapa(simbolo: String, descripcion: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.size(48.dp).semantics { contentDescription = descripcion }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(simbolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Elige el zoom para que quepan las ~8 ópticas más cercanas. */
private fun calcularZoomInicial(referencia: Coordenadas?, opticas: List<Optica>): Float {
    if (referencia == null || opticas.isEmpty()) return 1f
    val lejana = opticas
        .map { distanciaKm(referencia.lat, referencia.lng, it.lat, it.lng) }
        .sorted()
        .take(8)
        .last()
        .coerceAtLeast(0.5)
    return (RADIO_BASE_KM / (lejana * 1.2)).toFloat().coerceIn(ZOOM_MIN, ZOOM_MAX)
}
