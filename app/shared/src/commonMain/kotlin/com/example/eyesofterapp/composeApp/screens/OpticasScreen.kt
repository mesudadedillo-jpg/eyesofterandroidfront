package com.example.eyesofterapp.composeApp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraSuperior
import com.example.eyesofterapp.composeApp.theme.EyesBotonFantasma
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesEtiqueta
import com.example.eyesofterapp.composeApp.theme.EyesMotion
import com.example.eyesofterapp.composeApp.theme.EyesOpcion
import com.example.eyesofterapp.composeApp.theme.EyesPildora
import com.example.eyesofterapp.composeApp.theme.EyesPildoraOptica
import com.example.eyesofterapp.composeApp.theme.EyesSelector
import com.example.eyesofterapp.composeApp.theme.EyesSelectorOpcion
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesTarjetaAcento
import com.example.eyesofterapp.domain.model.Optica
import com.example.eyesofterapp.ui.OpticasUiState
import kotlin.math.roundToLong

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

    Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        EyesBarraSuperior(accion = "Volver", onAccion = onVolver)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Parte de arriba con controles: se desplaza si no cabe y solo ocupa lo necesario
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ópticas cercanas", style = MaterialTheme.typography.headlineMedium, color = EyesColors.Texto)

                if (state.mostrandoCache) {
                    EyesTarjetaAcento(fondo = EyesColors.AmarilloBg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sin conexión: se muestran los datos guardados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EyesColors.AmarilloFg
                        )
                    }
                }

                // Estado del GPS: píldora (real o simulado) + coordenadas + acción para cambiar.
                state.coordenadasGps?.let { coords ->
                    EyesTarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (state.esUbicacionSimulada) {
                                    EyesPildora("Ubicación simulada", EyesColors.AmarilloBg, EyesColors.AmarilloFg)
                                } else {
                                    EyesPildora("GPS real activo", EyesColors.VerdeBg, EyesColors.VerdeFg)
                                }
                                Text(
                                    text = "${coords.lat.cuatroDecimales()}, ${coords.lng.cuatroDecimales()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = EyesColors.TextoSecundario
                                )
                                Text(
                                    text = if (state.esUbicacionSimulada)
                                        "Punto de demostración en Tulancingo, Hgo., para probar el sensor."
                                    else
                                        "Coordenadas obtenidas del sensor de posicionamiento.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EyesColors.TextoTenue
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            if (state.esUbicacionSimulada) {
                                EyesBotonFantasma("Usar GPS", onClick = onSolicitarUbicacion)
                            } else {
                                EyesBotonFantasma("Simular", onClick = onSimularUbicacion)
                            }
                        }
                    }
                }

                // Búsqueda por ciudad o C.P.: el único botón relleno de la pantalla.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.busquedaTexto,
                        onValueChange = onActualizarBusquedaTexto,
                        label = { Text("Buscar por ciudad o C.P.") },
                        singleLine = true,
                        shape = EyesShapes.Boton,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EyesColors.Azul,
                            unfocusedBorderColor = EyesColors.Borde,
                            focusedContainerColor = EyesColors.Blanco,
                            unfocusedContainerColor = EyesColors.Blanco,
                            focusedLabelColor = EyesColors.Azul,
                            cursorColor = EyesColors.Azul
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    EyesBotonPrincipal(
                        texto = "Buscar",
                        onClick = { onBuscarPorCiudad(state.busquedaTexto) },
                        enabled = !state.cargando && state.busquedaTexto.isNotBlank()
                    )
                }

                // Selector Mapa | Lista + acciones de ubicación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EyesSelector {
                        EyesSelectorOpcion("Mapa", seleccionada = vistaModoMapa, onClick = { vistaModoMapa = true })
                        EyesSelectorOpcion("Lista", seleccionada = !vistaModoMapa, onClick = { vistaModoMapa = false })
                    }
                    Row {
                        EyesBotonTexto("GPS real", onClick = onSolicitarUbicacion, enabled = !state.cargando, color = EyesColors.Azul)
                        EyesBotonTexto("Simular", onClick = onSimularUbicacion, enabled = !state.cargando, color = EyesColors.Azul)
                    }
                }

                // Filtros: se desplazan de lado si no caben en pantallas angostas.
                EyesEtiqueta("Tipo", color = EyesColors.TextoTenue)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EyesOpcion("Todas (${state.opticas.size})", filtroVerificadas == null, { filtroVerificadas = null })
                    EyesOpcion(
                        "Asociadas (${state.opticas.count { it.verificada }})",
                        filtroVerificadas == true,
                        { filtroVerificadas = if (filtroVerificadas == true) null else true }
                    )
                    EyesOpcion(
                        "Externas (${state.opticas.count { !it.verificada }})",
                        filtroVerificadas == false,
                        { filtroVerificadas = if (filtroVerificadas == false) null else false }
                    )
                }

                EyesEtiqueta("Distancia", color = EyesColors.TextoTenue)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EyesOpcion("Cualquiera", filtroDistanciaMaxKm == null, { filtroDistanciaMaxKm = null })
                    EyesOpcion("Menos de 5 km", filtroDistanciaMaxKm == 5.0, { filtroDistanciaMaxKm = 5.0 })
                    EyesOpcion("Menos de 10 km", filtroDistanciaMaxKm == 10.0, { filtroDistanciaMaxKm = 10.0 })
                }

                if (state.cargando) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EyesColors.Azul)
                    }
                }

                state.error?.let { errorMsg ->
                    EyesTarjeta(Modifier.fillMaxWidth()) {
                        EyesPildora("Ubicación no disponible", EyesColors.RojoBg, EyesColors.RojoFg)
                        Text(
                            text = errorMsg,
                            color = EyesColors.Texto,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        EyesBotonFantasma(
                            "Pedir permiso de GPS de nuevo",
                            onClick = onSolicitarUbicacion,
                            modifier = Modifier.fillMaxWidth()
                        )
                        EyesBotonTexto(
                            "Simular ubicación (demostración)",
                            onClick = onSimularUbicacion,
                            color = EyesColors.Azul,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } // Fin de la parte de arriba

            if (!state.cargando && opticasFiltradas.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No se encontraron ópticas con los filtros seleccionados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EyesColors.TextoSecundario
                    )
                }
            } else {
                // Mapa ⇄ Lista: fundido corto entre las dos vistas, para que el cambio no
                // sea un salto brusco. Sin desplazamiento: ocupan el mismo lugar.
                AnimatedContent(
                    targetState = vistaModoMapa,
                    transitionSpec = {
                        fadeIn(tween(EyesMotion.Pantalla, easing = EyesMotion.EaseOut))
                            .togetherWith(fadeOut(tween(EyesMotion.Salida)))
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    label = "mapaLista"
                ) { mostrarMapa ->
                    if (mostrarMapa) {
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
        }
    }
}

/** Coordenada con 4 decimales (unos 11 m de precisión), por ejemplo 20.0825. */
private fun Double.cuatroDecimales(): String = ((this * 10_000).roundToLong() / 10_000.0).toString()

/** Distancia con un decimal, por ejemplo "2.5 km". */
internal fun Optica.distanciaTexto(): String = "${((distanciaKm * 10).toInt() / 10.0)} km"

/** Abre Google Maps con la ruta a la óptica. */
internal fun Optica.urlComoLlegar(): String =
    "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&destination_place_id=$nombre"

@Composable
fun OpticaCard(optica: Optica, onClick: (() -> Unit)? = null) {
    val uriHandler = LocalUriHandler.current

    EyesTarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
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
                Text(
                    text = optica.distanciaTexto(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (optica.verificada) EyesColors.Azul else EyesColors.TextoSecundario
                )
            }
        }

        Text(optica.direccion, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
        if (optica.ciudad.isNotBlank() || optica.codigoPostal.isNotBlank()) {
            Text(
                text = "${optica.ciudad} ${optica.codigoPostal}".trim(),
                style = MaterialTheme.typography.bodySmall,
                color = EyesColors.TextoTenue
            )
        }

        // En una lista hay muchas tarjetas: sus acciones son "fantasma" y de texto,
        // para no llenar la pantalla de botones azules.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
            if (onClick != null) {
                EyesBotonTexto("Ver en mapa", onClick = onClick, color = EyesColors.Azul)
            }
        }
    }
}
