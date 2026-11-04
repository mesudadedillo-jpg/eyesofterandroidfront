package com.example.eyesofterapp.composeApp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
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
 * Mapa ilustrado (dibujado con Canvas, sin servicio de mapas). Las posiciones
 * de los marcadores y el zoom funcionan igual que antes; solo cambió el estilo:
 * calles en blanco y celeste sobre el fondo de la paleta, marcadores en
 * píldora y controles redondos con borde fino.
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

        // Marcadores de las ópticas
        Box(modifier = Modifier.fillMaxSize()) {
            opticas.forEachIndexed { index, optica ->
                val baseOffsetX = (0.2f + (index * 0.15f) % 0.65f)
                val baseOffsetY = (0.25f + ((index * 0.22f) + 0.1f) % 0.55f)

                // Ajuste de posición según el zoom
                val offsetX = (0.5f + (baseOffsetX - 0.5f) * zoomLevel).coerceIn(0.05f, 0.85f)
                val offsetY = (0.5f + (baseOffsetY - 0.5f) * zoomLevel).coerceIn(0.05f, 0.80f)

                val isSelected = opticaSeleccionada?.id == optica.id
                // Color del marcador: azul si está seleccionado, verde si es asociada, blanco si es externa.
                val (fondoDestino, textoDestino) = when {
                    isSelected -> EyesColors.Azul to EyesColors.Blanco
                    optica.verificada -> EyesColors.VerdeBg to EyesColors.VerdeFg
                    else -> EyesColors.Blanco to EyesColors.Marino
                }
                // Al seleccionar, el color cambia con una transición corta (indica el estado).
                val fondo by animateColorAsState(fondoDestino, tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "marcador")
                val texto by animateColorAsState(textoDestino, tween(EyesMotion.Color, easing = EyesMotion.Ease), label = "marcadorTexto")
                val esBlanco = fondoDestino == EyesColors.Blanco

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = (offsetX * 300).dp, top = (offsetY * 260).dp)
                        .clickable { onSeleccionarOptica(optica) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = optica.nombre,
                            style = MaterialTheme.typography.labelMedium,
                            color = texto,
                            modifier = Modifier
                                .clip(EyesShapes.Pildora)
                                .background(fondo)
                                .border(1.dp, if (esBlanco) EyesColors.Borde else fondo, EyesShapes.Pildora)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(if (esBlanco) EyesColors.Gris else fondo, CircleShape)
                                .border(2.dp, EyesColors.Blanco, CircleShape)
                        )
                    }
                }
            }

            // Marcador del usuario: anillo marino (como el iris del logotipo) + píldora.
            if (userCoords != null) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(EyesColors.Blanco, CircleShape)
                                .border(6.dp, EyesColors.Marino, CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        if (esUbicacionSimulada) {
                            EyesPildora("Tú (simulado)", EyesColors.AmarilloBg, EyesColors.AmarilloFg)
                        } else {
                            EyesPildora("Tú estás aquí", EyesColors.Marino, EyesColors.Blanco)
                        }
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
