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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraPantalla
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesEtiqueta
import com.example.eyesofterapp.composeApp.theme.EyesIcono
import com.example.eyesofterapp.composeApp.theme.EyesIconos
import com.example.eyesofterapp.composeApp.theme.EyesMarca
import com.example.eyesofterapp.composeApp.theme.EyesMensaje
import com.example.eyesofterapp.composeApp.theme.EyesSelector
import com.example.eyesofterapp.composeApp.theme.EyesSelectorOpcion
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesTarjetaAcento
import com.example.eyesofterapp.composeApp.theme.franjasBarra
import com.example.eyesofterapp.data.network.URL_PORTAL
import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.ui.PanelUiState

// ---------------------------------------------------------------------------
// Aviso de privacidad (C-RC-08, C-RS-07)
// ---------------------------------------------------------------------------

/**
 * Antes de ver datos de sus hijos, el tutor acepta el aviso de privacidad vigente.
 * Aceptarlo aquí o en el portal cuenta para los dos.
 */
@Composable
fun ConsentimientoScreen(
    state: PanelUiState,
    onAceptar: () -> Unit,
    onLogout: () -> Unit,
    onMensajeVisto: () -> Unit,
) {
    val uri = LocalUriHandler.current
    var acepto by remember { mutableStateOf(false) }
    val c = state.consentimiento
    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().franjasBarra().statusBarsPadding().height(60.dp).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                EyesMarca(color = EyesColors.Blanco)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Antes de empezar", style = MaterialTheme.typography.headlineMedium, color = EyesColors.Texto)
                Text(
                    "Para mostrarte los resultados de la revisión visual de tus hijos necesitamos que conozcas y aceptes cómo cuidamos sus datos.",
                    style = MaterialTheme.typography.bodyLarge, color = EyesColors.TextoSecundario
                )
                EyesTarjeta(Modifier.fillMaxWidth()) {
                    EyesEtiqueta("En resumen")
                    listOf(
                        "Usamos los datos solo para el programa de salud visual de la escuela.",
                        "Los resultados son preventivos, no un diagnóstico médico.",
                        "Los datos de salud se guardan cifrados y solo los ve el personal autorizado.",
                        "Si eliges una clínica asociada, ella recibe el pase con los datos mínimos para atenderlo.",
                        "Puedes pedir acceso, corrección o cancelación de tus datos (derechos ARCO) en la escuela.",
                    ).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = EyesColors.Texto) }
                    Row {
                        EyesBotonTexto("Leer el aviso completo", onClick = { uri.openUri(c?.urlAviso?.ifBlank { null } ?: "$URL_PORTAL/Privacidad/Aviso") }, color = EyesColors.Azul)
                        EyesBotonTexto("Términos de uso", onClick = { uri.openUri(c?.urlTerminos?.ifBlank { null } ?: "$URL_PORTAL/Privacidad/Terminos") }, color = EyesColors.Azul)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(acepto, { acepto = it }, colors = CheckboxDefaults.colors(checkedColor = EyesColors.Azul))
                    Text(
                        "He leído y acepto el aviso de privacidad${c?.let { " (versión ${it.versionVigente})" } ?: ""}.",
                        style = MaterialTheme.typography.bodyMedium, color = EyesColors.Texto, modifier = Modifier.weight(1f)
                    )
                }
                EyesBotonPrincipal("Aceptar y continuar", onClick = onAceptar, enabled = acepto && c != null && !state.enviando, cargando = state.enviando, modifier = Modifier.fillMaxWidth())
                EyesBotonTexto("Ahora no, cerrar sesión", onClick = onLogout, modifier = Modifier.fillMaxWidth())
                state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = EyesColors.RojoBg) }
            }
        }
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}

// ---------------------------------------------------------------------------
// Guía rápida (M-RS-01)
// ---------------------------------------------------------------------------

@Composable
fun GuiaScreen(rol: Role, onVolver: () -> Unit) {
    val uri = LocalUriHandler.current
    val pasos: List<Triple<ImageVector, String, String>> = when (rol) {
        Role.CLIENTE -> listOf(
            Triple(EyesIconos.Campana, "Te llega un aviso", "Cuando Enfermería registra la revisión visual de tu hijo, recibes una notificación. Tócala para ver el resultado."),
            Triple(EyesIconos.Ojo, "Revisa el semáforo", "Verde: todo bien. Amarillo: conviene revisarlo con calma. Rojo: que lo vea un especialista pronto. No es un diagnóstico."),
            Triple(EyesIconos.Lugar, "Elige a dónde llevarlo", "En el resultado, elige una clínica. Las asociadas al programa te dan un pase médico con QR; si eliges otra, te damos el resumen del tamizaje."),
            Triple(EyesIconos.Reloj, "Pide cita", "Desde el resultado puedes pedir cita. Enfermería y la clínica reciben tu solicitud."),
            Triple(EyesIconos.Camara, "Sube la receta o los lentes", "Toma una foto de la receta o del comprobante de lentes. Enfermería lo revisa y cierra el caso."),
            Triple(EyesIconos.Documento, "Sin internet también", "El último resultado, el pase y el resumen se guardan en tu teléfono. Si subes una foto sin señal, se envía sola después."),
        )
        Role.SUPERVISOR, Role.ADMINISTRADOR -> listOf(
            Triple(EyesIconos.Grupo, "Seguimiento", "Ve qué familias ya leyeron su resultado y por dónde les llegó (push o correo). A los 3 días sin leer, el sistema reenvía y te avisa."),
            Triple(EyesIconos.Documento, "Comprobantes", "Revisa las recetas y comprobantes de lentes. Si rechazas uno, escribe el motivo para que la familia sepa qué corregir."),
            Triple(EyesIconos.Enviar, "Avisos", "Manda un aviso a todos los tutores o a algunos en específico."),
            Triple(EyesIconos.Capas, "Portal web", "Los expedientes completos, indicadores, reportes y la bitácora están en el portal web."),
        )
        Role.ALUMNO -> listOf(
            Triple(EyesIconos.Reloj, "Regla 20-20-20", "Cada 20 minutos de pantalla, mira algo a 6 metros durante 20 segundos. El teléfono vibra para recordártelo."),
            Triple(EyesIconos.Ojo, "Ejercicios", "Parpadeo, enfoque y relajación: hazlos cuando sientas los ojos cansados."),
        )
        else -> listOf(
            Triple(EyesIconos.Lugar, "Directorio", "Consulta las clínicas y ópticas del programa; las asociadas aparecen primero."),
            Triple(EyesIconos.Documento, "Pases médicos", "Para validar el QR de un pase y registrar la atención, entra al portal web con tu cuenta (Pases → Leer pase)."),
        )
    }
    Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        EyesBarraPantalla("Guía rápida", onVolver)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pasos.forEachIndexed { i, (icono, titulo, texto) ->
                EyesTarjeta(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(EyesColors.Celeste100), contentAlignment = Alignment.Center) {
                            EyesIcono(icono, null, EyesColors.Azul)
                        }
                        Text("${i + 1}. $titulo", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto, modifier = Modifier.padding(start = 12.dp))
                    }
                    Text(texto, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
                }
            }
            EyesTarjetaAcento(EyesColors.Celeste200, Modifier.fillMaxWidth()) {
                Text("¿Más dudas?", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                Text("La ayuda completa y el aviso de privacidad están en el portal.", style = MaterialTheme.typography.bodyMedium, color = EyesColors.Marino700)
                Row {
                    EyesBotonTexto("Ayuda", onClick = { uri.openUri("$URL_PORTAL/Soporte/Ayuda") }, color = EyesColors.Azul)
                    EyesBotonTexto("Aviso de privacidad", onClick = { uri.openUri("$URL_PORTAL/Privacidad/Aviso") }, color = EyesColors.Azul)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Reportar una falla o duda (C-RS-04)
// ---------------------------------------------------------------------------

@Composable
fun ReportarScreen(
    state: PanelUiState,
    onEnviar: (tipo: String, mensaje: String, alTerminar: () -> Unit) -> Unit,
    onMensajeVisto: () -> Unit,
    onVolver: () -> Unit,
) {
    var tipo by remember { mutableStateOf("FALLA") }
    var mensaje by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        Column(Modifier.fillMaxSize()) {
            EyesBarraPantalla("Reportar un problema", onVolver)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EyesSelector {
                    EyesSelectorOpcion("Algo no funciona", tipo == "FALLA") { tipo = "FALLA" }
                    EyesSelectorOpcion("Tengo una duda", tipo == "DUDA") { tipo = "DUDA" }
                }
                OutlinedTextField(
                    mensaje, { mensaje = it.take(1000) },
                    label = { Text("Cuéntanos qué pasó") },
                    minLines = 5,
                    supportingText = { Text("${mensaje.length}/1000 · No escribas contraseñas.") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Dirección lo revisará y te responderá al correo de tu cuenta.", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
                EyesBotonPrincipal(
                    "Enviar", onClick = { onEnviar(tipo, mensaje, onVolver) },
                    enabled = mensaje.trim().length >= 10 && !state.enviando, cargando = state.enviando, modifier = Modifier.fillMaxWidth()
                )
            }
        }
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}
