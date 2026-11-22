package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.example.eyesofterapp.composeApp.theme.EyesSelector
import com.example.eyesofterapp.composeApp.theme.EyesSelectorOpcion
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesVacio
import com.example.eyesofterapp.data.model.ComprobanteDto
import com.example.eyesofterapp.data.model.SeguimientoDto
import com.example.eyesofterapp.data.model.fechaLegible
import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.User
import com.example.eyesofterapp.ui.PanelUiState
import org.jetbrains.compose.resources.decodeToImageBitmap

// ---------------------------------------------------------------------------
// Comprobantes: el tutor ve los suyos; Enfermería y Dirección los revisan.
// ---------------------------------------------------------------------------

@Composable
fun ComprobantesScreen(
    user: User,
    state: PanelUiState,
    onRevisar: (id: String, aprobado: Boolean, motivo: String?) -> Unit,
    descargar: suspend (id: String) -> ByteArray?,
    onAbrirTamizaje: (String) -> Unit,
    onMensajeVisto: () -> Unit,
    onVolver: () -> Unit,
) {
    val esPersonal = user.role == Role.SUPERVISOR || user.role == Role.ADMINISTRADOR
    var verPendientes by remember { mutableStateOf(true) }
    val lista = if (!esPersonal) state.comprobantes
    else state.comprobantes.filter { (it.estado == "PENDIENTE") == verPendientes }

    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            EyesBarraPantalla(if (esPersonal) "Comprobantes" else "Mis comprobantes", onVolver)
            if (esPersonal) {
                Box(Modifier.padding(start = 16.dp, top = 16.dp)) {
                    EyesSelector {
                        EyesSelectorOpcion("Por revisar (${state.comprobantesPendientes})", verPendientes) { verPendientes = true }
                        EyesSelectorOpcion("Revisados", !verPendientes) { verPendientes = false }
                    }
                }
            }
            if (lista.isEmpty()) {
                EyesVacio(
                    EyesIconos.Documento,
                    if (esPersonal && verPendientes) "No hay comprobantes por revisar" else "Sin comprobantes",
                    if (esPersonal) "Cuando un tutor suba una receta o comprobante de lentes, aparecerá aquí."
                    else "Abre el resultado de tu hijo para subir la receta o el comprobante de lentes."
                )
                if (!esPersonal) state.tamizajeReciente?.let { t ->
                    Box(Modifier.padding(horizontal = 16.dp)) { FilaTamizaje(t) { onAbrirTamizaje(t.id) } }
                }
                return@Column
            }
            LazyColumn(
                Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lista, key = { it.id }) { c ->
                    if (esPersonal) TarjetaRevision(c, state, onRevisar, descargar)
                    else EyesTarjeta(Modifier.fillMaxWidth(), onClick = { onAbrirTamizaje(c.tamizajeId) }) { FilaComprobanteTutor(c) }
                }
            }
        }
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}

@Composable
private fun TarjetaRevision(
    c: ComprobanteDto,
    state: PanelUiState,
    onRevisar: (String, Boolean, String?) -> Unit,
    descargar: suspend (String) -> ByteArray?,
) {
    val alumno = state.seguimiento.firstOrNull { it.tamizajeId == c.tamizajeId }?.alumno
    var imagen by remember(c.id) { mutableStateOf<ImageBitmap?>(null) }
    var rechazando by remember(c.id) { mutableStateOf(false) }
    var motivo by remember(c.id) { mutableStateOf("") }

    // Vista previa: se descarga solo si es imagen (los PDF se identifican con su ícono).
    LaunchedEffect(c.id) {
        if (c.contentType.startsWith("image/")) {
            imagen = descargar(c.id)?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
        }
    }

    EyesTarjeta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(alumno ?: "Tutor: ${c.tutor}", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                Text(
                    (if (c.tipo == "RECETA") "Receta" else "Compra de lentes") + " · ${fechaLegible(c.subidoEn)} · ${c.tamanoBytes / 1024} KB",
                    style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue
                )
            }
            when (c.estado) {
                "APROBADO" -> EyesPildora("Aceptado", EyesColors.VerdeBg, EyesColors.VerdeFg)
                "RECHAZADO" -> EyesPildora("Rechazado", EyesColors.RojoBg, EyesColors.RojoFg)
                else -> EyesPildora("Pendiente", EyesColors.AmarilloBg, EyesColors.AmarilloFg)
            }
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(EyesShapes.Boton).background(EyesColors.Celeste050),
            contentAlignment = Alignment.Center
        ) {
            val img = imagen
            if (img != null) {
                Image(img, "Foto del comprobante", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(260.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    EyesIcono(EyesIconos.Documento, null, EyesColors.Azul, 36.dp)
                    Text(if (c.contentType == "application/pdf") "Documento PDF" else "Cargando vista previa...", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
                }
            }
        }
        c.motivoRechazo?.let { Text("Motivo: $it", style = MaterialTheme.typography.bodySmall, color = EyesColors.RojoBg) }

        if (c.estado == "PENDIENTE") {
            if (rechazando) {
                CampoTexto(motivo, { motivo = it }, "¿Por qué se rechaza? (lo verá el tutor)", lineas = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EyesBotonTexto("Cancelar", { rechazando = false })
                    EyesBotonFantasma(
                        "Confirmar rechazo", { onRevisar(c.id, false, motivo) },
                        enabled = motivo.isNotBlank() && !state.enviando, modifier = Modifier.weight(1f)
                    )
                }
                // Motivos frecuentes: un toque y listo.
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("La foto está borrosa", "No se ve el nombre del alumno", "No es una receta ni un comprobante").forEach {
                        EyesOpcion(it, motivo == it, { motivo = it })
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EyesBotonFantasma("Rechazar", { rechazando = true }, enabled = !state.enviando, modifier = Modifier.weight(1f))
                    EyesBotonPrincipal("Aprobar", { onRevisar(c.id, true, null) }, enabled = !state.enviando, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Seguimiento: quién leyó el resultado (C-RFN-07) y cuántos recordatorios se enviaron.
// ---------------------------------------------------------------------------

@Composable
fun SeguimientoScreen(state: PanelUiState, onRefrescar: () -> Unit, onVolver: () -> Unit) {
    var soloSinLeer by remember { mutableStateOf(true) }
    val lista = state.seguimiento.filter { !soloSinLeer || it.leidaEn == null }
    Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        EyesBarraPantalla("Seguimiento de avisos", onVolver) {
            EyesBotonTexto("Actualizar", onRefrescar, color = EyesColors.Blanco)
        }
        Box(Modifier.padding(start = 16.dp, top = 16.dp)) {
            EyesSelector {
                EyesSelectorOpcion("Sin leer (${state.avisosSinLeer})", soloSinLeer) { soloSinLeer = true }
                EyesSelectorOpcion("Todos (${state.seguimiento.size})", !soloSinLeer) { soloSinLeer = false }
            }
        }
        if (lista.isEmpty()) {
            EyesVacio(
                EyesIconos.Grupo,
                if (soloSinLeer) "Todas las familias ya leyeron su resultado" else "Todavía no se han enviado resultados",
                "A los 3 días sin leer, el sistema reenvía el aviso al tutor y te avisa."
            )
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lista, key = { it.notificacionId }) { FilaSeguimiento(it) }
        }
    }
}

@Composable
private fun FilaSeguimiento(s: SeguimientoDto) {
    EyesTarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.alumno.ifBlank { "Alumno" }, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                Text("Tutor: ${s.tutor} · enviado ${fechaLegible(s.creadaEn)}", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
            }
            if (s.leidaEn != null) EyesPildora("Leído ${fechaLegible(s.leidaEn)}", EyesColors.VerdeBg, EyesColors.VerdeFg)
            else EyesPildora("Sin leer", EyesColors.AmarilloBg, EyesColors.AmarilloFg)
        }
        val detalle = buildList {
            if (s.recordatoriosEnviados > 0) add(if (s.recordatoriosEnviados == 1) "1 recordatorio enviado" else "${s.recordatoriosEnviados} recordatorios enviados")
            add(
                when {
                    "push" in s.entregadaPor -> "Llegó al teléfono"
                    "correo" in s.entregadaPor -> "Llegó por correo"
                    else -> "Solo en la app (sin push ni correo)"
                }
            )
        }
        Text(detalle.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
    }
}

// ---------------------------------------------------------------------------
// Nuevo aviso
// ---------------------------------------------------------------------------

@Composable
fun NuevoAvisoScreen(
    state: PanelUiState,
    onEnviar: (titulo: String, mensaje: String, aTodos: Boolean, usuarios: List<String>) -> Unit,
    onVolver: () -> Unit,
) {
    var titulo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var aTodos by remember { mutableStateOf(true) }
    var usuarios by remember { mutableStateOf("") }
    val listaUsuarios = usuarios.split(',', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }
    val listo = titulo.isNotBlank() && mensaje.isNotBlank() && (aTodos || listaUsuarios.isNotEmpty())

    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            EyesBarraPantalla("Nuevo aviso", onVolver)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EyesEtiqueta("Para")
                EyesSelector {
                    EyesSelectorOpcion("Todos los tutores", aTodos) { aTodos = true }
                    EyesSelectorOpcion("Usuarios específicos", !aTodos) { aTodos = false }
                }
                if (!aTodos) CampoTexto(usuarios, { usuarios = it }, "Usuarios separados por coma (p. ej. tutor1, tutor2)")
                EyesEtiqueta("Aviso")
                CampoTexto(titulo, { if (it.length <= 80) titulo = it }, "Título (p. ej. Junta de padres)")
                CampoTexto(mensaje, { if (it.length <= 500) mensaje = it }, "Mensaje", lineas = 5)
                Text("${mensaje.length}/500", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
                EyesBotonPrincipal(
                    "Enviar aviso", { onEnviar(titulo, mensaje, aTodos, if (aTodos) emptyList() else listaUsuarios) },
                    enabled = listo, cargando = state.enviando, modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // El mensaje de éxito se muestra en el inicio (al enviar se regresa ahí).
        if (state.mensaje != null && !state.enviando) EyesMensaje(state.mensaje) {}
    }
}

@Composable
internal fun CampoTexto(valor: String, onCambio: (String) -> Unit, etiqueta: String, lineas: Int = 1) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = lineas == 1,
        minLines = lineas,
        shape = EyesShapes.Boton,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EyesColors.Azul,
            unfocusedBorderColor = EyesColors.Borde,
            focusedContainerColor = EyesColors.Blanco,
            unfocusedContainerColor = EyesColors.Blanco,
            focusedLabelColor = EyesColors.Azul,
            cursorColor = EyesColors.Azul
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
