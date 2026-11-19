package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraPantalla
import com.example.eyesofterapp.composeApp.theme.EyesBotonFantasma
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesEtiqueta
import com.example.eyesofterapp.composeApp.theme.EyesIcono
import com.example.eyesofterapp.composeApp.theme.EyesIconos
import com.example.eyesofterapp.composeApp.theme.EyesMensaje
import com.example.eyesofterapp.composeApp.theme.EyesSelector
import com.example.eyesofterapp.composeApp.theme.EyesSelectorOpcion
import com.example.eyesofterapp.composeApp.theme.EyesSemaforoCirculo
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesVacio
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.composeApp.theme.infoSemaforo
import com.example.eyesofterapp.data.model.fechaLegible
import com.example.eyesofterapp.plataforma.ArchivoElegido
import com.example.eyesofterapp.plataforma.LocalTelefono
import com.example.eyesofterapp.ui.PanelUiState
import kotlinx.coroutines.launch

/**
 * Resultado del tamizaje para el tutor: semáforo grande con texto e ícono, explicación
 * en lenguaje claro, qué hacer ahora y la subida de la receta o comprobante.
 * Desde la notificación se llega aquí en un toque (M-RC-02).
 */
@Composable
fun TamizajeScreen(
    tamizajeId: String,
    state: PanelUiState,
    onSubir: (tamizajeId: String, tipo: String, archivo: ArchivoElegido) -> Unit,
    onBuscarClinica: () -> Unit,
    onMensajeVisto: () -> Unit,
    onVolver: () -> Unit,
) {
    val t = state.tamizajes.firstOrNull { it.id == tamizajeId }
    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            EyesBarraPantalla("Resultado de la revisión", onVolver)
            if (t == null) {
                EyesVacio(
                    EyesIconos.Ojo, if (state.cargando) "Cargando resultado..." else "No encontramos este resultado",
                    "Si te llegó un aviso, jala hacia abajo en el inicio para actualizar."
                )
                return@Column
            }
            val info = infoSemaforo(t.semaforo)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Encabezado con el color del resultado.
                Column(
                    Modifier.fillMaxWidth().clip(EyesShapes.Tarjeta).background(info.fondo).padding(24.dp).aparecer(0),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(EyesColors.Blanco.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                        EyesIcono(info.icono, null, info.texto, 48.dp)
                    }
                    Text(info.titulo, style = MaterialTheme.typography.headlineMedium, color = info.texto)
                    Text(t.alumnoNombre, style = MaterialTheme.typography.titleMedium, color = info.texto)
                    Text(
                        "Revisión del ${fechaLegible(t.fecha)}" + if (t.grado.isNotBlank()) " · ${t.grado}° ${t.grupo}".trimEnd() else "",
                        style = MaterialTheme.typography.bodySmall, color = info.texto
                    )
                }

                EyesTarjeta(Modifier.fillMaxWidth().aparecer(1)) {
                    EyesEtiqueta("¿Qué significa?")
                    Text(info.explicacion, style = MaterialTheme.typography.bodyLarge, color = EyesColors.Texto)
                    if (t.observaciones.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        EyesEtiqueta("Observaciones de Enfermería", EyesColors.TextoTenue)
                        Text(t.observaciones, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
                    }
                }

                EyesTarjeta(Modifier.fillMaxWidth().aparecer(2)) {
                    EyesEtiqueta("Qué hacer ahora")
                    info.pasos.forEachIndexed { i, paso ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(EyesColors.Celeste100), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = EyesColors.Azul)
                            }
                            Text(paso, style = MaterialTheme.typography.bodyMedium, color = EyesColors.Texto, modifier = Modifier.padding(start = 12.dp).weight(1f))
                        }
                    }
                    if (t.semaforo.uppercase() != "VERDE") {
                        Spacer(Modifier.height(4.dp))
                        EyesBotonPrincipal("Buscar clínica asociada", onClick = onBuscarClinica, modifier = Modifier.fillMaxWidth())
                    }
                }

                if (t.semaforo.uppercase() != "VERDE") {
                    SubirComprobante(t.id, state, onSubir, Modifier.aparecer(3))
                }
                Text(
                    "Este reporte es preventivo y no sustituye la consulta con un especialista.",
                    style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue
                )
            }
        }
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}

@Composable
private fun SubirComprobante(
    tamizajeId: String,
    state: PanelUiState,
    onSubir: (String, String, ArchivoElegido) -> Unit,
    modifier: Modifier,
) {
    val telefono = LocalTelefono.current
    val scope = rememberCoroutineScope()
    var tipo by remember { mutableStateOf("RECETA") }
    val previos = state.comprobantes.filter { it.tamizajeId == tamizajeId }

    EyesTarjeta(modifier.fillMaxWidth()) {
        EyesEtiqueta("Receta o comprobante de lentes")
        Text(
            "Cuando el especialista te dé la receta o compres los lentes, súbelo aquí para que Enfermería cierre el caso.",
            style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario
        )
        previos.forEach { c -> FilaComprobanteTutor(c) }
        if (telefono == null) {
            Text(
                "Para fotografiar el documento, usa la app en tu teléfono.",
                style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue
            )
            return@EyesTarjeta
        }
        EyesSelector {
            EyesSelectorOpcion("Receta", tipo == "RECETA") { tipo = "RECETA" }
            EyesSelectorOpcion("Compra de lentes", tipo == "COMPROBANTE_LENTES") { tipo = "COMPROBANTE_LENTES" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EyesBotonFantasma(
                if (state.enviando) "Enviando..." else "Tomar foto",
                onClick = { scope.launch { telefono.tomarFoto()?.let { onSubir(tamizajeId, tipo, it) } } },
                enabled = !state.enviando,
                modifier = Modifier.weight(1f)
            )
            EyesBotonFantasma(
                "Elegir archivo",
                onClick = { scope.launch { telefono.elegirArchivo()?.let { onSubir(tamizajeId, tipo, it) } } },
                enabled = !state.enviando,
                modifier = Modifier.weight(1f)
            )
        }
        Text("Foto o PDF de hasta 10 MB. Las fotos se reducen antes de enviarse para ahorrar datos.", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
    }
}

@Composable
internal fun FilaComprobanteTutor(c: com.example.eyesofterapp.data.model.ComprobanteDto) {
    val (texto, fondo, color) = when (c.estado) {
        "APROBADO" -> Triple("Aceptado", EyesColors.VerdeBg, EyesColors.VerdeFg)
        "RECHAZADO" -> Triple("Rechazado", EyesColors.RojoBg, EyesColors.RojoFg)
        else -> Triple("En revisión", EyesColors.Celeste100, EyesColors.Marino)
    }
    Column(
        Modifier.fillMaxWidth().clip(EyesShapes.Boton).background(EyesColors.Celeste050).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EyesIcono(EyesIconos.Documento, null, EyesColors.Azul, 20.dp)
            Text(
                (if (c.tipo == "RECETA") "Receta" else "Compra de lentes") + " · " + fechaLegible(c.subidoEn),
                style = MaterialTheme.typography.bodyMedium, color = EyesColors.Texto,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            com.example.eyesofterapp.composeApp.theme.EyesPildora(texto, fondo, color)
        }
        c.motivoRechazo?.let {
            Text("Motivo: $it", style = MaterialTheme.typography.bodySmall, color = EyesColors.RojoBg)
        }
    }
}
