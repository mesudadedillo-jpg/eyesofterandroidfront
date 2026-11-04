package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.Optica

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
            .background(Color(0xFFE8F1F5), RoundedCornerShape(16.dp))
    ) {
        // Fondo interactivo de mapa con cuadrícula y calles escaladas por Zoom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Fondo del mapa
            drawRect(color = Color(0xFFE2ECE9))

            // Líneas de cuadrícula ajustadas según zoomLevel
            val strokeWidth = 6f * zoomLevel
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f * zoomLevel, 15f * zoomLevel), 0f)

            // Calles principales
            val stepX = (canvasWidth / 5f) * zoomLevel
            val stepY = (canvasHeight / 5f) * zoomLevel

            var y = 0f
            while (y < canvasHeight) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = strokeWidth * 2
                )
                y += stepY.coerceAtLeast(40f)
            }

            var x = 0f
            while (x < canvasWidth) {
                drawLine(
                    color = Color.White,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = strokeWidth * 2
                )
                x += stepX.coerceAtLeast(40f)
            }

            // Calles secundarias discontinuas
            var secY = 0f
            while (secY < canvasHeight) {
                drawLine(
                    color = Color(0xFFCDE0D8),
                    start = Offset(0f, secY),
                    end = Offset(canvasWidth, secY),
                    strokeWidth = strokeWidth,
                    pathEffect = dashPathEffect
                )
                secY += (stepY / 2f).coerceAtLeast(20f)
            }
        }

        // Marcadores de ópticas proyectados
        Box(modifier = Modifier.fillMaxSize()) {
            opticas.forEachIndexed { index, optica ->
                val baseOffsetX = (0.2f + (index * 0.15f) % 0.65f)
                val baseOffsetY = (0.25f + ((index * 0.22f) + 0.1f) % 0.55f)

                // Ajuste de offset según el zoomLevel
                val offsetX = (0.5f + (baseOffsetX - 0.5f) * zoomLevel).coerceIn(0.05f, 0.85f)
                val offsetY = (0.5f + (baseOffsetY - 0.5f) * zoomLevel).coerceIn(0.05f, 0.80f)

                val isSelected = opticaSeleccionada?.id == optica.id
                val esVerificada = optica.verificada

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = (offsetX * 300).dp,
                            top = (offsetY * 260).dp
                        )
                        .clickable { onSeleccionarOptica(optica) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    esVerificada -> Color(0xFF2E7D32)
                                    else -> Color(0xFFE0E0E0)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
                        ) {
                            Text(
                                text = if (esVerificada) "✅ ${optica.nombre}" else "⚪ ${optica.nombre}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (esVerificada) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected || esVerificada -> Color.White
                                    else -> Color(0xFF424242)
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        esVerificada -> Color(0xFF2E7D32)
                                        else -> Color(0xFF9E9E9E)
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }

            // Marcador GPS del Usuario (📍)
            if (userCoords != null) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (esUbicacionSimulada) Color(0xFFF57C00) else Color(0xFFE53935),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📍", fontSize = 16.sp)
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (esUbicacionSimulada) Color(0xFFF57C00) else Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (esUbicacionSimulada) "Tú estás aquí (GPS Simulado)" else "Tú estás aquí (GPS Real)",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Controles interactivos del Mapa (Zoom / Tamaño / Recenter)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Zoom In
            Card(
                modifier = Modifier.clickable { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f) },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text("➕", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }
            // Zoom Out
            Card(
                modifier = Modifier.clickable { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.6f) },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text("➖", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }
            // Expandir/Agrandar o achicar el Mapa
            Card(
                modifier = Modifier.clickable { mapaExpandido = !mapaExpandido },
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (mapaExpandido) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text("↕️", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }
            // Recenter
            Card(
                modifier = Modifier.clickable {
                    zoomLevel = 1f
                    onCentrarUbicacion()
                },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text("📍", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Tarjeta flotante de la Óptica seleccionada
        opticaSeleccionada?.let { optica ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = optica.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (optica.verificada) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (optica.verificada) "✅ Asociada / Verificada" else "⚪ Opción Externa (Sin Verificar)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (optica.verificada) Color(0xFF2E7D32) else Color(0xFF616161),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (optica.distanciaKm > 0.0) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = "${((optica.distanciaKm * 10).toInt() / 10.0)} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${optica.direccion}, ${optica.ciudad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=${optica.lat},${optica.lng}&destination_place_id=${optica.nombre}"
                                try {
                                    uriHandler.openUri(mapsUrl)
                                } catch (_: Exception) { }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🧭 Cómo llegar")
                        }
                        OutlinedButton(
                            onClick = {
                                val telUrl = "tel:${optica.telefono}"
                                try {
                                    uriHandler.openUri(telUrl)
                                } catch (_: Exception) { }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📞 Llamar")
                        }
                    }
                }
            }
        }
    }
}
