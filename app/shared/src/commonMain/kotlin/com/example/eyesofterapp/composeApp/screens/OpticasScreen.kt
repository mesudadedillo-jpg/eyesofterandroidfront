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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.domain.model.FUENTE_OSM
import com.example.eyesofterapp.domain.model.Optica
import com.example.eyesofterapp.ui.OpticasUiState
import kotlin.math.roundToLong

/** Altura mínima recomendada para que un botón sea fácil de tocar (Material / WCAG). */
private val ALTO_TOQUE = 48.dp

private enum class FiltroTipo { TODAS, ASOCIADAS, OTRAS }

@Composable
fun OpticasScreen(
    state: OpticasUiState,
    onSolicitarUbicacion: () -> Unit,
    onSimularUbicacion: () -> Unit = {},
    onBuscarPorCiudad: (String) -> Unit,
    onActualizarBusquedaTexto: (String) -> Unit,
    onVolver: () -> Unit,
    onAbrirMapaEnVivo: (() -> Unit)? = null
) {
    // La lista es la vista inicial: es la más cómoda en teléfono y con lector de pantalla.
    var vistaMapa by rememberSaveable { mutableStateOf(false) }
    var seleccionadaId by remember { mutableStateOf<Int?>(null) }
    var filtroDistanciaKm by rememberSaveable { mutableStateOf<Double?>(null) }
    var filtroTipo by rememberSaveable { mutableStateOf(FiltroTipo.TODAS) }

    LaunchedEffect(Unit) {
        if (state.opticas.isEmpty() && state.error == null && !state.cargando) {
            onSolicitarUbicacion()
        }
    }

    val opticasFiltradas = remember(state.opticas, filtroDistanciaKm, filtroTipo) {
        state.opticas.filter { optica ->
            val cumpleDistancia = filtroDistanciaKm?.let { optica.distanciaKm <= it } ?: true
            val cumpleTipo = when (filtroTipo) {
                FiltroTipo.TODAS -> true
                FiltroTipo.ASOCIADAS -> optica.verificada
                FiltroTipo.OTRAS -> !optica.verificada
            }
            cumpleDistancia && cumpleTipo
        }
    }
    val seleccionada = opticasFiltradas.firstOrNull { it.id == seleccionadaId } ?: opticasFiltradas.firstOrNull()

    Scaffold(topBar = { Encabezado(onVolver) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            // Abajo deja 96 dp libres para que el botón flotante «Mapa» de Android no tape la última tarjeta.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "ubicacion") {
                TarjetaUbicacion(state, onSolicitarUbicacion, onSimularUbicacion)
            }
            item(key = "busqueda") {
                BarraBusqueda(state, onBuscarPorCiudad, onActualizarBusquedaTexto)
            }
            item(key = "vista") {
                SelectorVista(vistaMapa = vistaMapa, onCambiar = { vistaMapa = it })
            }
            item(key = "filtros") {
                Filtros(
                    opticas = state.opticas,
                    filtroTipo = filtroTipo,
                    onTipo = { filtroTipo = it },
                    filtroDistanciaKm = filtroDistanciaKm,
                    onDistancia = { filtroDistanciaKm = it }
                )
            }

            if (state.cargando) {
                item(key = "cargando") { Cargando() }
            }

            state.error?.let { mensaje ->
                item(key = "error") {
                    TarjetaAviso(
                        mensaje = mensaje,
                        mostrandoCache = state.mostrandoCache,
                        onReintentar = onSolicitarUbicacion,
                        onSimular = onSimularUbicacion
                    )
                }

            if (!state.cargando) {
                item(key = "resumen") { Resumen(opticasFiltradas.size, state) }
            }

            if (!state.cargando && opticasFiltradas.isEmpty()) {
                item(key = "vacio") { EstadoVacio(hayFiltros = filtroDistanciaKm != null || filtroTipo != FiltroTipo.TODAS) }
            } else if (vistaMapa) {
                item(key = "mapa") {
                    MapaOpticasView(
                        userCoords = state.coordenadasGps,
                        esUbicacionSimulada = state.esUbicacionSimulada,
                        opticas = opticasFiltradas,
                        opticaSeleccionada = seleccionada,
                        onSeleccionarOptica = { seleccionadaId = it.id },
                        onCentrarUbicacion = onSolicitarUbicacion,
                        etiquetaCentro = when {
                            state.lugarBuscado != null -> "Lugar buscado"
                            state.esUbicacionSimulada -> "Demostración"
                            else -> "Tú estás aquí"
                        }
                    )
                }
                seleccionada?.let { optica ->
                    item(key = "seleccionada") { OpticaCard(optica = optica, resaltada = true) }
                }
            } else {
                items(opticasFiltradas, key = { "${it.fuente}-${it.id}" }) { optica ->
                    OpticaCard(
                        optica = optica,
                        onVerEnMapa = {
                            seleccionadaId = optica.id
                            vistaMapa = true
                        }
                    )
                }
            }

            item(key = "atribucion") { Atribucion() }
        }
    }
}

// --------------------------------------------------------------------------------------
// Partes de la pantalla
// --------------------------------------------------------------------------------------

@Composable
private fun Encabezado(onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ópticas cercanas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        TextButton(onClick = onVolver, modifier = Modifier.heightIn(min = ALTO_TOQUE)) {
            Text("Volver")
        }
    }
}

@Composable
private fun TarjetaUbicacion(
    state: OpticasUiState,
    onSolicitarUbicacion: () -> Unit,
    onSimularUbicacion: () -> Unit
) {
    val coords = state.coordenadasGps
    val (titulo, detalle) = when {
        state.lugarBuscado != null -> "Buscando cerca de:" to state.lugarBuscado.orEmpty()
        coords == null -> "Ubicación" to "Toca «Usar mi ubicación» para encontrar ópticas cerca de ti, o busca una ciudad."
        state.esUbicacionSimulada -> "Ubicación de demostración" to "Tulancingo, Hgo. No viene del sensor de tu teléfono."
        else -> (if (state.esUltimaConocida) "Tu última ubicación conocida" else "Tu ubicación (GPS)") to buildString {
            append("${formatoCoord(coords.lat)}, ${formatoCoord(coords.lng)}")
            state.precisionMetros?.let { append(" · precisión ±${it.toInt()} m") }
            state.proveedorGps?.let { append(" · $it") }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (state.esUbicacionSimulada) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detalle, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSolicitarUbicacion,
                    enabled = !state.cargando,
                    modifier = Modifier.weight(1f).heightIn(min = ALTO_TOQUE)
                ) { Text("Usar mi ubicación") }
                OutlinedButton(
                    onClick = onSimularUbicacion,
                    enabled = !state.cargando,
                    modifier = Modifier.weight(1f).heightIn(min = ALTO_TOQUE)
                ) { Text("Demostración") }
            }
        }
    }
}

@Composable
private fun BarraBusqueda(
    state: OpticasUiState,
    onBuscar: (String) -> Unit,
    onTexto: (String) -> Unit
) {
    val foco = LocalFocusManager.current
    val buscar = {
        foco.clearFocus()
        onBuscar(state.busquedaTexto)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.busquedaTexto,
            onValueChange = onTexto,
            label = { Text("Ciudad, colonia o código postal") },
            placeholder = { Text("Ej. Pachuca, 43600 o Madrid") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (state.busquedaTexto.isNotBlank()) buscar() }),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = buscar,
            enabled = !state.cargando && state.busquedaTexto.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = ALTO_TOQUE)
        ) { Text("Buscar ópticas en ese lugar") }
    }
}

@Composable
private fun SelectorVista(vistaMapa: Boolean, onCambiar: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !vistaMapa,
            onClick = { onCambiar(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.heightIn(min = ALTO_TOQUE)
        ) { Text("Lista") }
        SegmentedButton(
            selected = vistaMapa,
            onClick = { onCambiar(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.heightIn(min = ALTO_TOQUE)
        ) { Text("Mapa") }
    }
}

@Composable
private fun Filtros(
    opticas: List<Optica>,
    filtroTipo: FiltroTipo,
    onTipo: (FiltroTipo) -> Unit,
    filtroDistanciaKm: Double?,
    onDistancia: (Double?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Se desliza hacia los lados: así ningún filtro queda cortado en pantallas angostas.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filtroTipo == FiltroTipo.TODAS,
                onClick = { onTipo(FiltroTipo.TODAS) },
                label = { Text("Todas (${opticas.size})") }
            )
            FilterChip(
                selected = filtroTipo == FiltroTipo.ASOCIADAS,
                onClick = { onTipo(if (filtroTipo == FiltroTipo.ASOCIADAS) FiltroTipo.TODAS else FiltroTipo.ASOCIADAS) },
                label = { Text("Asociadas EYESOFTER (${opticas.count { it.verificada }})") }
            )
            FilterChip(
                selected = filtroTipo == FiltroTipo.OTRAS,
                onClick = { onTipo(if (filtroTipo == FiltroTipo.OTRAS) FiltroTipo.TODAS else FiltroTipo.OTRAS) },
                label = { Text("Otras ópticas (${opticas.count { !it.verificada }})") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf<Pair<Double?, String>>(null to "Cualquier distancia", 1.0 to "Menos de 1 km", 5.0 to "Menos de 5 km", 10.0 to "Menos de 10 km")
                .forEach { (km, texto) ->
                    FilterChip(
                        selected = filtroDistanciaKm == km,
                        onClick = { onDistancia(km) },
                        label = { Text(texto) }
                    )
                }
        }
    }
}

@Composable
private fun Cargando() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text("Buscando ópticas cerca…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TarjetaAviso(
    mensaje: String,
    mostrandoCache: Boolean,
    onReintentar: () -> Unit,
    onSimular: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (mostrandoCache) "Sin conexión: mostrando datos guardados" else "Aviso",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onReintentar, modifier = Modifier.weight(1f).heightIn(min = ALTO_TOQUE)) {
                    Text("Reintentar")
                }
                OutlinedButton(onClick = onSimular, modifier = Modifier.weight(1f).heightIn(min = ALTO_TOQUE)) {
                    Text("Demostración")
                }
            }
        }
    }
}

@Composable
private fun Resumen(cantidad: Int, state: OpticasUiState) {
    val texto = buildString {
        append(
            when (cantidad) {
                0 -> "Sin resultados"
                1 -> "1 óptica"
                else -> "$cantidad ópticas"
            }
        )
        state.radioBusquedaKm?.let { append(" en un radio de $it km") }
        append(", de la más cercana a la más lejana.")
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun EstadoVacio(hayFiltros: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (hayFiltros) "No hay ópticas con estos filtros. Prueba con «Cualquier distancia» o «Todas»."
            else "No encontramos ópticas registradas cerca de este punto. Busca una ciudad cercana.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun Atribucion() {
    val uri = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp)) {
        Text(
            text = "Ópticas y mapa: © colaboradores de OpenStreetMap (ODbL). " +
                "Las ópticas asociadas provienen del servidor EYESOFTER. " +
                "Los datos de OpenStreetMap los aporta la comunidad y pueden estar incompletos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = { abrir(uri, "https://www.openstreetmap.org/copyright") },
            modifier = Modifier.heightIn(min = ALTO_TOQUE)
        ) { Text("Ver licencia de OpenStreetMap") }
    }
}

// --------------------------------------------------------------------------------------
// Tarjeta de una óptica
// --------------------------------------------------------------------------------------

@Composable
fun OpticaCard(
    optica: Optica,
    resaltada: Boolean = false,
    onVerEnMapa: (() -> Unit)? = null
) {
    val uri = LocalUriHandler.current
    val telefono = optica.telefono.filter { it.isDigit() || it == '+' }
    val esOsm = optica.fuente == FUENTE_OSM

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (resaltada) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (optica.verificada) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = optica.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                if (optica.distanciaKm > 0.0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatoDistancia(optica.distanciaKm),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = "a ${distanciaHablada(optica.distanciaKm)}" }
                    )
                }
            }

            Etiqueta(
                texto = when {
                    optica.verificada -> "Asociada EYESOFTER"
                    esOsm -> "Registrada en OpenStreetMap"
                    else -> "Catálogo EYESOFTER"
                },
                fondo = if (optica.verificada) Color(0xFFDDF1E1) else MaterialTheme.colorScheme.surfaceVariant,
                texto2 = if (optica.verificada) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            val lineaDireccion = listOf(optica.direccion, optica.ciudad, optica.codigoPostal)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            Text(lineaDireccion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (optica.horario.isNotBlank()) {
                Text("Horario: ${optica.horario}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // Enlace de Google Maps gratuito (no requiere llave): abre la app de mapas con la ruta.
                        abrir(uri, "https://www.google.com/maps/dir/?api=1&destination=${optica.lat},${optica.lng}")
                    },
                    modifier = Modifier.weight(1f).heightIn(min = ALTO_TOQUE)
                ) { Text("Cómo llegar") }
                if (telefono.length >= 7) {
                    OutlinedButton(
                        onClick = { abrir(uri, "tel:$telefono") },
                        modifier = Modifier.weight(1f).heightIn(min = ALTO_TOQUE)
                    ) { Text("Llamar") }
                }
            }
            if (optica.sitioWeb.isNotBlank() || onVerEnMapa != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (optica.sitioWeb.isNotBlank()) {
                        TextButton(
                            onClick = {
                                val web = optica.sitioWeb.let { if (it.startsWith("http")) it else "https://$it" }
                                abrir(uri, web)
                            },
                            modifier = Modifier.heightIn(min = ALTO_TOQUE)
                        ) { Text("Sitio web") }
                    }
                    if (onVerEnMapa != null) {
                        TextButton(onClick = onVerEnMapa, modifier = Modifier.heightIn(min = ALTO_TOQUE)) {
                            Text("Ver en el mapa")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: Color, texto2: Color) {
    Surface(color = fondo, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = texto2,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// --------------------------------------------------------------------------------------
// Utilidades
// --------------------------------------------------------------------------------------

private fun abrir(uri: androidx.compose.ui.platform.UriHandler, url: String) {
    try {
        uri.openUri(url)
    } catch (_: Exception) {
        // Sin app para abrir el enlace (por ejemplo, "tel:" en una tablet sin teléfono).
    }
}

internal fun formatoDistancia(km: Double): String =
    if (km < 1.0) "${((km * 1000).toInt() / 10) * 10} m"
    else "${(km * 10).toInt() / 10.0} km"

internal fun distanciaHablada(km: Double): String =
    if (km < 1.0) "${((km * 1000).toInt() / 10) * 10} metros"
    else "${(km * 10).toInt() / 10.0} kilómetros"

private fun formatoCoord(valor: Double): String =
    ((valor * 100_000).toLong() / 100_000.0).toString()
