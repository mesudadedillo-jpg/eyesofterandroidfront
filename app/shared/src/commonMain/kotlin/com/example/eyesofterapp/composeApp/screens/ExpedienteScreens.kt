package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraPantalla
import com.example.eyesofterapp.composeApp.theme.EyesBotonFantasma
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesEtiqueta
import com.example.eyesofterapp.composeApp.theme.EyesIcono
import com.example.eyesofterapp.composeApp.theme.EyesIconos
import com.example.eyesofterapp.composeApp.theme.EyesMensaje
import com.example.eyesofterapp.composeApp.theme.EyesOpcion
import com.example.eyesofterapp.composeApp.theme.EyesPildora
import com.example.eyesofterapp.composeApp.theme.EyesPildoraOptica
import com.example.eyesofterapp.composeApp.theme.EyesSelector
import com.example.eyesofterapp.composeApp.theme.EyesSelectorOpcion
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesTarjetaAcento
import com.example.eyesofterapp.composeApp.theme.EyesVacio
import com.example.eyesofterapp.composeApp.theme.infoSemaforo
import com.example.eyesofterapp.data.model.CasoDto
import com.example.eyesofterapp.data.model.CitaEntradaDto
import com.example.eyesofterapp.data.model.ClinicaDto
import com.example.eyesofterapp.data.model.PaseDto
import com.example.eyesofterapp.data.model.TamizajeDto
import com.example.eyesofterapp.data.model.fechaCorta
import com.example.eyesofterapp.data.model.fechaLegible
import com.example.eyesofterapp.data.model.fechaMx
import com.example.eyesofterapp.ui.PanelUiState
import org.jetbrains.compose.resources.decodeToImageBitmap

// ---------------------------------------------------------------------------
// Seguimiento del caso dentro del resultado (M-RFN-08..11)
// ---------------------------------------------------------------------------

/**
 * Lo que sigue después de un resultado amarillo o rojo:
 *  1. Elegir a dónde llevará al alumno (directorio o escrita a mano).
 *  2. Clínica asociada → pase médico con QR. No asociada → resumen del tamizaje.
 *  3. Pedir cita, y la línea de tiempo del caso.
 * Todo se ve también sin conexión con lo último descargado (M-RFNN-03).
 */
@Composable
internal fun ColumnScope.SeguimientoDelCaso(
    t: TamizajeDto,
    caso: CasoDto,
    state: PanelUiState,
    onElegirDestino: () -> Unit,
    onSolicitarCita: () -> Unit,
) {
    if (caso.cerrado) {
        EyesTarjetaAcento(EyesColors.VerdeBg, Modifier.fillMaxWidth()) {
            Text("Caso cerrado", style = MaterialTheme.typography.titleLarge, color = EyesColors.VerdeFg)
            Text(
                "Enfermería cerró el seguimiento con la atención o la corrección comprobada. ¡Gracias por cuidar su vista!",
                style = MaterialTheme.typography.bodyMedium, color = EyesColors.VerdeFg
            )
        }
    } else if (!caso.destinoDefinido) {
        EyesTarjetaAcento(EyesColors.Celeste200, Modifier.fillMaxWidth()) {
            Text("¿A dónde llevarás a ${t.alumnoNombre.substringBefore(' ')}?", style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto)
            Text(
                "Si eliges una clínica asociada al programa, te damos un pase médico con código QR. Si no, te damos el resumen del tamizaje para llevarlo a la consulta.",
                style = MaterialTheme.typography.bodyMedium, color = EyesColors.Marino700
            )
            EyesBotonPrincipal("Elegir clínica u óptica", onClick = onElegirDestino, modifier = Modifier.fillMaxWidth())
        }
    } else {
        val pase = caso.pase
        if (pase != null && caso.clinicaAsociada && pase.estado != "CANCELADO") {
            PaseMedico(t, pase, state.pases[pase.codigo])
        } else {
            ResumenTamizaje(t, caso.clinicaNombre)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EyesBotonFantasma("Cambiar clínica", onClick = onElegirDestino, modifier = Modifier.weight(1f))
            EyesBotonFantasma("Pedir cita", onClick = onSolicitarCita, modifier = Modifier.weight(1f))
        }
    }

    if (caso.citas.isNotEmpty()) {
        EyesTarjeta(Modifier.fillMaxWidth()) {
            EyesEtiqueta("Citas solicitadas")
            caso.citas.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.clinicaNombre ?: "Clínica", style = MaterialTheme.typography.bodyLarge, color = EyesColors.Texto)
                        Text(
                            fechaCorta(c.fechaPreferida) + (c.horario?.let { " · " + if (it == "MANANA") "mañana" else if (it == "TARDE") "tarde" else it } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario
                        )
                    }
                    val (texto, fondo, color) = when (c.estado) {
                        "CONFIRMADA" -> Triple("Confirmada", EyesColors.VerdeBg, EyesColors.VerdeFg)
                        "ATENDIDA" -> Triple("Atendida", EyesColors.VerdeBg, EyesColors.VerdeFg)
                        "CANCELADA" -> Triple("Cancelada", EyesColors.RojoBg, EyesColors.RojoFg)
                        else -> Triple("Solicitada", EyesColors.Celeste100, EyesColors.Marino)
                    }
                    EyesPildora(texto, fondo, color)
                }
            }
        }
    }

    if (caso.eventos.isNotEmpty()) LineaDeTiempo(caso)
}

/** M-RFN-10: pase médico con QR. El QR lo valida el especialista en el portal. */
@Composable
private fun PaseMedico(t: TamizajeDto, pase: PaseDto, png: ByteArray?) {
    val imagen = remember(png) { png?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    val usable = pase.vigente
    EyesTarjeta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EyesEtiqueta("Pase médico", EyesColors.Azul)
            Spacer(Modifier.weight(1f))
            when (pase.estado) {
                "VIGENTE" -> EyesPildora("Vigente", EyesColors.VerdeBg, EyesColors.VerdeFg)
                "USADO" -> EyesPildora("Ya se usó", EyesColors.Celeste100, EyesColors.Marino)
                else -> EyesPildora("Venció", EyesColors.RojoBg, EyesColors.RojoFg)
            }
        }
        Text(pase.clinicaNombre, style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto)
        Text("${pase.clinicaDireccion} · Tel. ${pase.clinicaTelefono}", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
        Box(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(232.dp).clip(EyesShapes.Tarjeta).background(EyesColors.Blanco).border(1.dp, EyesColors.Borde, EyesShapes.Tarjeta)
                    .semantics { contentDescription = "Código QR del pase médico de ${t.alumnoNombre}" },
                contentAlignment = Alignment.Center
            ) {
                when {
                    !usable -> EyesIcono(EyesIconos.Documento, null, EyesColors.TextoTenue, 64.dp)
                    imagen != null -> Image(imagen, null, Modifier.size(220.dp), filterQuality = FilterQuality.None)
                    else -> Text("El QR se descargará al tener conexión.", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue, modifier = Modifier.padding(16.dp))
                }
            }
        }
        Text("Código", style = MaterialTheme.typography.labelMedium, color = EyesColors.TextoTenue)
        Text(pase.codigoLegible, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = EyesColors.Texto)
        Text(
            when (pase.estado) {
                "VIGENTE" -> "Muéstralo en la clínica. Sirve una sola vez y vence el ${fechaLegible(pase.expiraEn)}. Lo puedes abrir sin internet."
                "USADO" -> "La clínica ya registró la atención con este pase."
                else -> "Este pase venció. Vuelve a elegir la clínica para generar uno nuevo."
            },
            style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario
        )
    }
}

/** M-RFN-11 / C-RFN-06: resumen sin QR para una clínica no asociada. Sin diagnóstico. */
@Composable
private fun ResumenTamizaje(t: TamizajeDto, clinica: String?) {
    val info = infoSemaforo(t.semaforo)
    EyesTarjeta(Modifier.fillMaxWidth()) {
        EyesEtiqueta("Resumen del tamizaje para la consulta")
        clinica?.let {
            Text("Para: $it", style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
            Text(
                "Esta clínica no está asociada al programa, por eso no hay pase con QR. Muestra este resumen al especialista.",
                style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(EyesShapes.Boton).background(EyesColors.Celeste050).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DatoResumen("Alumno", t.alumnoNombre)
            DatoResumen("Grado y grupo", "${t.grado}° ${t.grupo}".trim())
            DatoResumen("Fecha del tamizaje", fechaLegible(t.fecha))
            DatoResumen("Resultado", "${info.titulo} (${t.semaforo.lowercase()})")
            if (t.observaciones.isNotBlank()) DatoResumen("Observaciones", t.observaciones)
            DatoResumen("Folio", t.folio)
        }
        Text(
            "Prueba de detección preventiva realizada en la escuela; no es un diagnóstico.",
            style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue
        )
    }
}

@Composable
private fun DatoResumen(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = EyesColors.TextoTenue)
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = EyesColors.Texto)
    }
}

@Composable
private fun LineaDeTiempo(caso: CasoDto) {
    EyesTarjeta(Modifier.fillMaxWidth()) {
        EyesEtiqueta("Historial del caso ${caso.folio}")
        caso.eventos.reversed().forEachIndexed { i, e ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                    Box(Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(if (i == 0) EyesColors.Azul else EyesColors.Celeste200))
                }
                Column(Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                    Text(fechaLegible(e.fecha), style = MaterialTheme.typography.labelMedium, color = EyesColors.TextoTenue)
                    Text(e.descripcion, style = MaterialTheme.typography.bodyMedium, color = EyesColors.Texto)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Elegir destino (M-RFN-09, M-RFNN-06, C-RFNN-07)
// ---------------------------------------------------------------------------

@Composable
fun ElegirDestinoScreen(
    tamizajeId: String,
    state: PanelUiState,
    onCargarClinicas: () -> Unit,
    onElegir: (clinicaId: Int?, nombreManual: String?, alTerminar: (String) -> Unit) -> Unit,
    onMensajeVisto: () -> Unit,
    onVolver: () -> Unit,
) {
    LaunchedEffect(Unit) { onCargarClinicas() }
    val t = state.tamizajes.firstOrNull { it.id == tamizajeId }
    var busqueda by remember { mutableStateOf("") }
    var escribir by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf<ClinicaDto?>(null) }
    var confirmarManual by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<String?>(null) }

    val normal = { s: String -> s.lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u") }
    val lista = state.clinicas.filter { busqueda.isBlank() || normal("${it.nombre} ${it.ciudad} ${it.direccion}").contains(normal(busqueda.trim())) }

    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            EyesBarraPantalla("Elegir clínica", onVolver)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                t?.let { Text("Para ${it.alumnoNombre}", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto) }
                EyesSelector {
                    EyesSelectorOpcion("Del directorio", !escribir) { escribir = false }
                    EyesSelectorOpcion("Escribir otra", escribir) { escribir = true }
                }
            }
            if (escribir) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Escribe el nombre de la clínica u óptica. Revisaremos si está en el directorio; si no lo está, no está asociada al programa y te daremos el resumen del tamizaje.",
                        style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario
                    )
                    OutlinedTextField(manual, { manual = it.take(160) }, label = { Text("Nombre de la clínica") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    EyesBotonPrincipal("Continuar", onClick = {
                        // M-RFNN-06: se verifica en el directorio antes de enviar.
                        val encontrada = state.clinicas.firstOrNull { normal(it.nombre) == normal(manual.trim()) }
                        if (encontrada != null) confirmar = encontrada else confirmarManual = true
                    }, enabled = manual.trim().length >= 3 && !state.enviando, modifier = Modifier.fillMaxWidth())
                }
            } else {
                OutlinedTextField(
                    busqueda, { busqueda = it }, label = { Text("Buscar por nombre o colonia") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                if (state.clinicas.isEmpty()) {
                    EyesVacio(EyesIconos.Lugar, if (state.sinConexion) "Sin conexión" else "Cargando directorio...", "Necesitas internet para ver el directorio.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lista, key = { it.id }) { c ->
                            EyesTarjeta(Modifier.fillMaxWidth(), onClick = { confirmar = c }, padding = PaddingValues(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.nombre, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto, modifier = Modifier.weight(1f))
                                    EyesPildoraOptica(c.asociada)
                                }
                                Text(listOf(c.direccion, c.ciudad).filter { it.isNotBlank() }.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
                                if (c.asociada) Text("Con pase médico y código QR", style = MaterialTheme.typography.labelMedium, color = EyesColors.VerdeBg)
                            }
                        }
                    }
                }
            }
        }

        confirmar?.let { c ->
            AlertDialog(
                onDismissRequest = { confirmar = null },
                title = { Text(c.nombre) },
                text = {
                    Text(
                        if (c.asociada) "Es una clínica asociada al programa: se generará un pase médico con código QR para mostrarlo en la consulta."
                        else "Esta clínica no está asociada al programa. No hay pase con QR: se te dará el resumen del tamizaje para llevarlo a la consulta."
                    )
                },
                confirmButton = { EyesBotonTexto("Elegir", onClick = { confirmar = null; onElegir(c.id, null) { resultado = it } }) },
                dismissButton = { EyesBotonTexto("Cancelar", onClick = { confirmar = null }) },
            )
        }
        if (confirmarManual) {
            AlertDialog(
                onDismissRequest = { confirmarManual = false },
                title = { Text("No está en el directorio") },
                text = {
                    Text("\"${manual.trim()}\" no está registrada en EYESOFTER, así que no está asociada al programa y no se puede generar pase con QR. ¿Quieres continuar con el resumen del tamizaje?")
                },
                confirmButton = { EyesBotonTexto("Continuar con el resumen", onClick = { confirmarManual = false; onElegir(null, manual.trim()) { resultado = it } }) },
                dismissButton = { EyesBotonTexto("Cancelar", onClick = { confirmarManual = false }) },
            )
        }
        resultado?.let { r ->
            AlertDialog(
                onDismissRequest = { resultado = null; onVolver() },
                title = { Text("Listo") },
                text = { Text(r) },
                confirmButton = { EyesBotonTexto("Ver", onClick = { resultado = null; onVolver() }) },
            )
        }
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}

// ---------------------------------------------------------------------------
// Solicitar cita (M-RFN-08)
// ---------------------------------------------------------------------------

@Composable
fun SolicitarCitaScreen(
    tamizajeId: String,
    state: PanelUiState,
    onCargarClinicas: () -> Unit,
    onEnviar: (CitaEntradaDto, alTerminar: () -> Unit) -> Unit,
    onMensajeVisto: () -> Unit,
    onVolver: () -> Unit,
) {
    LaunchedEffect(Unit) { onCargarClinicas() }
    val caso = state.tamizajes.firstOrNull { it.id == tamizajeId }?.caso
    var clinicaId by remember { mutableStateOf(caso?.clinicaId) }
    // Días hábiles de las próximas dos semanas.
    val dias = remember { (1..16).map { fechaMx(it) }.filter { !fechaCorta(it).startsWith("sáb") && !fechaCorta(it).startsWith("dom") } }
    var fecha by remember { mutableStateOf(dias.firstOrNull() ?: fechaMx(1)) }
    var horario by remember { mutableStateOf("MANANA") }
    var comentario by remember { mutableStateOf("") }
    val clinica = state.clinicas.firstOrNull { it.id == clinicaId }

    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            EyesBarraPantalla("Pedir cita", onVolver)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EyesEtiqueta("Clínica")
                if (state.clinicas.isEmpty()) Text("Cargando directorio...", color = EyesColors.TextoTenue)
                state.clinicas.take(12).forEach { c ->
                    EyesOpcion("${c.nombre}${if (c.asociada) " · asociada" else ""}", c.id == clinicaId, { clinicaId = c.id }, Modifier.fillMaxWidth())
                }
                EyesEtiqueta("Día que prefieres")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dias.forEach { d -> EyesOpcion(fechaCorta(d), d == fecha, { fecha = d }) }
                }
                EyesEtiqueta("Horario")
                EyesSelector {
                    EyesSelectorOpcion("Mañana", horario == "MANANA") { horario = "MANANA" }
                    EyesSelectorOpcion("Tarde", horario == "TARDE") { horario = "TARDE" }
                }
                OutlinedTextField(comentario, { comentario = it.take(300) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
                Text(
                    "La solicitud llega a Enfermería y a la clínica. Te avisaremos cuando la confirmen." +
                        (clinica?.let { " Si tienes prisa, llama al ${it.telefono}." } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario
                )
                EyesBotonPrincipal(
                    if (state.enviando) "Enviando..." else "Enviar solicitud",
                    onClick = { clinicaId?.let { onEnviar(CitaEntradaDto(it, fecha, horario, comentario.trim().ifBlank { null }), onVolver) } },
                    enabled = clinicaId != null && !state.enviando,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}

/** M-RFNN-01: aviso de que se ve lo guardado. */
@Composable
internal fun BannerSinConexion(state: PanelUiState, modifier: Modifier = Modifier) {
    if (!state.sinConexion && state.pendientesDeEnvio.isEmpty()) return
    EyesTarjetaAcento(EyesColors.AmarilloBg, modifier.fillMaxWidth()) {
        if (state.sinConexion) {
            Text("Sin conexión", style = MaterialTheme.typography.titleMedium, color = EyesColors.AmarilloFg)
            Text(
                "Estás viendo la información guardada" + (state.actualizadoEn?.let { " el ${fechaLegible(it)}" } ?: "") +
                    ". Puede no estar actualizada.",
                style = MaterialTheme.typography.bodySmall, color = EyesColors.AmarilloFg
            )
        }
        if (state.pendientesDeEnvio.isNotEmpty()) {
            Text(
                "${state.pendientesDeEnvio.size} comprobante(s) guardado(s): se enviarán solos al volver la conexión.",
                style = MaterialTheme.typography.bodySmall, color = EyesColors.AmarilloFg
            )
        }
    }
}
