package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eyesofterapp.domain.model.Optica
import com.example.eyesofterapp.ui.OpticasUiState

@Composable
fun OpticasScreen(
    state: OpticasUiState,
    onSolicitarUbicacion: () -> Unit,
    onSimularUbicacion: () -> Unit = {},
    onBuscarPorCiudad: (String) -> Unit,
    onActualizarBusquedaTexto: (String) -> Unit,
    onVolver: () -> Unit
) {
    var vistaModoMapa by remember { mutableStateOf(true) }
    var opticaSeleccionada by remember { mutableStateOf<Optica?>(null) }
    var filtroDistanciaMaxKm by remember { mutableStateOf<Double?>(null) }
    var filtroVerificadas by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        if (state.opticas.isEmpty() && state.error == null && !state.cargando) {
            onSolicitarUbicacion()
        }
    }

    val opticasFiltradas = remember(state.opticas, filtroDistanciaMaxKm, filtroVerificadas) {
        state.opticas.filter { optica ->
            val cumpleDistancia = filtroDistanciaMaxKm == null || optica.distanciaKm <= filtroDistanciaMaxKm!!
            val cumpleVerificada = filtroVerificadas == null || optica.verificada == filtroVerificadas!!
            cumpleDistancia && cumpleVerificada
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ópticas Cercanas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onClick = onVolver) {
                        Text("Volver")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wrap top controls in a scrollable column that takes only the space it needs
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.mostrandoCache) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Mostrando datos guardados (Modo sin conexión)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            state.coordenadasGps?.let { coords ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.esUbicacionSimulada) MaterialTheme.colorScheme.tertiaryContainer
                                         else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.esUbicacionSimulada)
                                    "🎯 GPS Simulado (Demostración Real): ${coords.lat}, ${coords.lng}"
                                else
                                    "📡 Sensor GPS Real Activo: ${coords.lat}, ${coords.lng}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (state.esUbicacionSimulada) MaterialTheme.colorScheme.onTertiaryContainer
                                       else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (state.esUbicacionSimulada)
                                    "Ubicación demostrativa real (Tulancingo, Hgo) para prueba del sensor."
                                else
                                    "Coordenadas obtenidas directamente del sensor de posicionamiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.esUbicacionSimulada) MaterialTheme.colorScheme.onTertiaryContainer
                                       else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (state.esUbicacionSimulada) {
                            Button(onClick = onSolicitarUbicacion) {
                                Text("📍 GPS Real", fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(onClick = onSimularUbicacion) {
                                Text("🎯 Simular", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Barra de búsqueda por Ciudad o C.P.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.busquedaTexto,
                    onValueChange = onActualizarBusquedaTexto,
                    label = { Text("Buscar por ciudad o C.P.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onBuscarPorCiudad(state.busquedaTexto) },
                    enabled = !state.cargando && state.busquedaTexto.isNotBlank()
                ) {
                    Text("Buscar")
                }
            }

            // Conmutador de Vista (Mapa vs Lista) + Botones de Ubicación GPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = vistaModoMapa,
                        onClick = { vistaModoMapa = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("🗺️ Mapa")
                    }
                    SegmentedButton(
                        selected = !vistaModoMapa,
                        onClick = { vistaModoMapa = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("📋 Lista")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onSolicitarUbicacion,
                        enabled = !state.cargando
                    ) {
                        Text("📍 GPS Real")
                    }
                    TextButton(
                        onClick = onSimularUbicacion,
                        enabled = !state.cargando
                    ) {
                        Text("🎯 Simular")
                    }
                }
            }

            // Filtros de Estado (Asociadas vs Externas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filtroVerificadas == null,
                    onClick = { filtroVerificadas = null },
                    label = { Text("Todas (${state.opticas.size})") }
                )
                FilterChip(
                    selected = filtroVerificadas == true,
                    onClick = { filtroVerificadas = if (filtroVerificadas == true) null else true },
                    label = { Text("✅ Asociadas (${state.opticas.count { it.verificada }})") }
                )
                FilterChip(
                    selected = filtroVerificadas == false,
                    onClick = { filtroVerificadas = if (filtroVerificadas == false) null else false },
                    label = { Text("⚪ Externa/Sin Verificar (${state.opticas.count { !it.verificada }})") }
                )
            }

            // Filtros de Distancia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filtroDistanciaMaxKm == null,
                    onClick = { filtroDistanciaMaxKm = null },
                    label = { Text("Cualquier distancia") }
                )
                FilterChip(
                    selected = filtroDistanciaMaxKm == 5.0,
                    onClick = { filtroDistanciaMaxKm = 5.0 },
                    label = { Text("< 5 km") }
                )
                FilterChip(
                    selected = filtroDistanciaMaxKm == 10.0,
                    onClick = { filtroDistanciaMaxKm = 10.0 },
                    label = { Text("< 10 km") }
                )
            }

            if (state.cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚠️ $errorMsg",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSolicitarUbicacion,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📍 Pedir Permiso de GPS de Nuevo")
                            }
                            OutlinedButton(
                                onClick = onSimularUbicacion,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎯 Simular Ubicación Real (Demostración GPS)")
                            }
                        }
                    }
                }
            }

            } // End of scrollable top column

            if (!state.cargando && opticasFiltradas.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron ópticas con los filtros seleccionados.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (vistaModoMapa) {
                        // VISTA DE MAPA INTERACTIVO DEDICADO
                        MapaOpticasView(
                            userCoords = state.coordenadasGps,
                            esUbicacionSimulada = state.esUbicacionSimulada,
                            opticas = opticasFiltradas,
                            opticaSeleccionada = opticaSeleccionada ?: opticasFiltradas.firstOrNull(),
                            onSeleccionarOptica = { opticaSeleccionada = it },
                            onCentrarUbicacion = onSolicitarUbicacion,
                            onSimularUbicacion = onSimularUbicacion
                        )
                    } else {
                        // VISTA DE LISTA
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(opticasFiltradas) { optica ->
                                OpticaCard(
                                    optica = optica,
                                    onClick = {
                                        opticaSeleccionada = optica
                                        vistaModoMapa = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
            } // End of padding column
        }
    }
}

@Composable
fun OpticaCard(optica: Optica, onClick: (() -> Unit)? = null) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.padding(vertical = 2.dp) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (optica.verificada) MaterialTheme.colorScheme.surface
                            else Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (optica.verificada) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = optica.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (optica.verificada) MaterialTheme.colorScheme.onSurface else Color(0xFF616161)
                    )
                    Spacer(Modifier.height(2.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (optica.verificada) Color(0xFFE8F5E9) else Color(0xFFEEEEEE)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (optica.verificada) "✅ Asociada / Verificada" else "⚪ Opción Externa (Sin Verificar)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (optica.verificada) Color(0xFF2E7D32) else Color(0xFF757575),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (optica.distanciaKm > 0.0) {
                    Text(
                        text = "${((optica.distanciaKm * 10).toInt() / 10.0)} km",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (optica.verificada) MaterialTheme.colorScheme.primary else Color(0xFF757575),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = optica.direccion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (optica.ciudad.isNotBlank() || optica.codigoPostal.isNotBlank()) {
                Text(
                    text = "${optica.ciudad} ${optica.codigoPostal}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(10.dp))
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

                if (onClick != null) {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🗺️ Mapa")
                    }
                }
            }
        }
    }
}
