package com.example.eyesofterapp.composeApp.screens

import com.example.eyesofterapp.data.network.URL_PORTAL

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.navigation.Screen
import com.example.eyesofterapp.composeApp.theme.EyesBotonIcono
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesContador
import com.example.eyesofterapp.composeApp.theme.EyesEtiqueta
import com.example.eyesofterapp.composeApp.theme.EyesIcono
import com.example.eyesofterapp.composeApp.theme.EyesIconos
import com.example.eyesofterapp.composeApp.theme.EyesMarca
import com.example.eyesofterapp.composeApp.theme.EyesMensaje
import com.example.eyesofterapp.composeApp.theme.EyesPildora
import com.example.eyesofterapp.composeApp.theme.EyesSemaforoCirculo
import com.example.eyesofterapp.composeApp.theme.EyesSemaforoPildora
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesTarjetaAcento
import com.example.eyesofterapp.composeApp.theme.EyesVacio
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.composeApp.theme.franjasBarra
import com.example.eyesofterapp.composeApp.theme.infoSemaforo
import com.example.eyesofterapp.composeApp.theme.nombreVisible
import com.example.eyesofterapp.data.model.TamizajeDto
import com.example.eyesofterapp.data.model.fechaLegible
import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.User
import com.example.eyesofterapp.ui.PanelUiState

/** Panel principal según el rol. Se puede jalar hacia abajo para actualizar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    user: User,
    state: PanelUiState,
    onAbrir: (Screen) -> Unit,
    onRefrescar: () -> Unit,
    onLogout: () -> Unit,
    onMensajeVisto: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
            BarraInicio(noLeidas = state.bandeja.noLeidas, onNotificaciones = { onAbrir(Screen.Notificaciones) }, onLogout = onLogout)
            PullToRefreshBox(isRefreshing = state.cargando, onRefresh = onRefrescar, modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val altoPantalla = maxHeight
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    // Al menos el alto de la pantalla: la ayuda y los enlaces legales quedan abajo.
                    Modifier.fillMaxWidth().heightIn(min = altoPantalla).navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EyesPildora(user.role.nombreVisible(), EyesColors.Celeste100, EyesColors.Marino, conPunto = false, modifier = Modifier.aparecer(0))
                    Text(
                        "Hola, ${user.fullName.substringBefore(' ').ifBlank { user.fullName }}",
                        style = MaterialTheme.typography.headlineLarge, color = EyesColors.Texto, modifier = Modifier.aparecer(1)
                    )
                    Text(subtitulo(user.role), style = MaterialTheme.typography.bodyLarge, color = EyesColors.TextoSecundario, modifier = Modifier.aparecer(2))

                    // M-RFNN-04: al iniciar sesión se ven los pendientes, haya o no push.
                    if (state.bandeja.noLeidas > 0) {
                        AvisoPendientes(state.bandeja.noLeidas, Modifier.aparecer(3)) { onAbrir(Screen.Notificaciones) }
                    }
                    BannerSinConexion(state)
                    state.error?.takeIf { !state.sinConexion }?.let {
                        EyesTarjetaAcento(fondo = EyesColors.AmarilloBg, modifier = Modifier.fillMaxWidth()) {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = EyesColors.AmarilloFg)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    when (user.role) {
                        Role.CLIENTE -> PanelTutor(state, onAbrir)
                        Role.SUPERVISOR, Role.ADMINISTRADOR -> PanelPersonal(state, onAbrir)
                        Role.ALUMNO -> PanelAlumno(onAbrir)
                        Role.DOCTOR -> PanelEspecialista(onAbrir)
                        Role.DESCONOCIDO -> EyesVacio(
                            EyesIconos.Atencion, "Tu rol no tiene un panel asignado",
                            "Pide a Dirección que revise tu usuario."
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Ayuda(onAbrir)
                }
                }
                }
            }
        }
        // Avisos tras una acción (p. ej. "Aviso enviado a 12 personas").
        EyesMensaje(state.mensaje, onMensajeVisto)
    }
}

private fun subtitulo(rol: Role) = when (rol) {
    Role.CLIENTE -> "Aquí verás los resultados de la revisión visual de tus hijos y qué hacer después."
    Role.SUPERVISOR -> "Seguimiento de avisos a las familias y comprobantes por revisar."
    Role.ADMINISTRADOR -> "Vista general del programa de tamizaje visual."
    Role.ALUMNO -> "Pequeños hábitos para cuidar tus ojos todos los días."
    Role.DOCTOR -> "Directorio de clínicas del programa."
    Role.DESCONOCIDO -> ""
}

@Composable
private fun BarraInicio(noLeidas: Int, onNotificaciones: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxWidth().franjasBarra().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyesMarca(color = EyesColors.Blanco, modifier = Modifier.weight(1f))
            EyesBotonIcono(EyesIconos.Campana, "Notificaciones", onNotificaciones, contador = noLeidas)
            EyesBotonIcono(EyesIconos.Salir, "Cerrar sesión", onLogout)
        }
    }
}

@Composable
private fun AvisoPendientes(n: Int, modifier: Modifier, onClick: () -> Unit) {
    EyesTarjeta(modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(EyesColors.Celeste100), contentAlignment = Alignment.Center) {
                EyesIcono(EyesIconos.Campana, null, EyesColors.Azul)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(if (n == 1) "Tienes 1 aviso sin leer" else "Tienes $n avisos sin leer", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                Text("Tócalo para verlo", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
            }
            EyesContador(n)
        }
    }
}

// ---------------------------------------------------------------------------
// Tutor
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.PanelTutor(state: PanelUiState, onAbrir: (Screen) -> Unit) {
    val reciente = state.tamizajeReciente
    EyesEtiqueta("Resultado más reciente", EyesColors.TextoTenue)
    if (reciente == null) {
        EyesTarjeta(Modifier.fillMaxWidth().aparecer(4)) {
            EyesVacio(
                EyesIconos.Ojo, "Todavía no hay resultados",
                if (state.cargando) "Buscando resultados..." else "Cuando Enfermería registre la revisión visual, te llegará un aviso y lo verás aquí."
            )
        }
    } else {
        ResultadoDestacado(reciente, Modifier.aparecer(4)) { onAbrir(Screen.Tamizaje(reciente.id)) }
    }

    if (state.tamizajes.size > 1) {
        EyesEtiqueta("Resultados anteriores", EyesColors.TextoTenue)
        state.tamizajes.drop(1).forEachIndexed { i, t ->
            FilaTamizaje(t, Modifier.aparecer(5 + i.coerceAtMost(4))) { onAbrir(Screen.Tamizaje(t.id)) }
        }
    }

    EyesEtiqueta("Accesos rápidos", EyesColors.TextoTenue)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccesoRapido(EyesIconos.Lugar, "Clínicas cercanas", "Mapa y directorio", Modifier.weight(1f)) { onAbrir(Screen.Opticas()) }
        AccesoRapido(
            EyesIconos.Documento, "Mis comprobantes",
            when (val p = state.comprobantesPendientes) { 0 -> "Recetas y lentes"; 1 -> "1 en revisión"; else -> "$p en revisión" },
            Modifier.weight(1f)
        ) { onAbrir(Screen.Comprobantes) }
    }
}

@Composable
private fun ResultadoDestacado(t: TamizajeDto, modifier: Modifier, onClick: () -> Unit) {
    val info = infoSemaforo(t.semaforo)
    Column(
        modifier.fillMaxWidth().clip(EyesShapes.Tarjeta).background(EyesColors.Blanco)
    ) {
        // Franja del color del semáforo arriba: se reconoce de un vistazo.
        Box(Modifier.fillMaxWidth().height(6.dp).background(info.fondo))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyesSemaforoCirculo(t.semaforo)
                Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t.alumnoNombre, style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto)
                    Text(
                        listOfNotNull(fechaLegible(t.fecha), t.grado.takeIf { it.isNotBlank() }?.let { "$it° ${t.grupo}".trim() }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue
                    )
                    EyesSemaforoPildora(t.semaforo)
                }
            }
            Text(info.explicacion, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
            EyesBotonPrincipal("Ver resultado y qué hacer", onClick = onClick, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun FilaTamizaje(t: TamizajeDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    EyesTarjeta(modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.alumnoNombre, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                Text(fechaLegible(t.fecha), style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
            }
            EyesSemaforoPildora(t.semaforo)
        }
    }
}

// ---------------------------------------------------------------------------
// Enfermería y Dirección
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.PanelPersonal(state: PanelUiState, onAbrir: (Screen) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Indicador(
            "${state.avisosSinLeer}", "Familias sin leer su resultado",
            if (state.avisosSinLeer > 0) EyesColors.AmarilloBg else EyesColors.Celeste100,
            if (state.avisosSinLeer > 0) EyesColors.AmarilloFg else EyesColors.Marino,
            Modifier.weight(1f).aparecer(4)
        ) { onAbrir(Screen.Seguimiento) }
        Indicador(
            "${state.comprobantesPendientes}", "Comprobantes por revisar",
            if (state.comprobantesPendientes > 0) EyesColors.Celeste200 else EyesColors.Celeste100, EyesColors.Marino,
            Modifier.weight(1f).aparecer(5)
        ) { onAbrir(Screen.Comprobantes) }
    }
    EyesTarjetaAcento(fondo = EyesColors.Celeste200, modifier = Modifier.fillMaxWidth().aparecer(6)) {
        Text("Avisar a las familias", style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto)
        Text(
            "Manda un aviso a todos los tutores o a algunos en específico. Les llega a la app y queda registro de quién lo leyó.",
            style = MaterialTheme.typography.bodyMedium, color = EyesColors.Marino700
        )
        EyesBotonPrincipal("Escribir aviso", onClick = { onAbrir(Screen.NuevoAviso) })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccesoRapido(EyesIconos.Grupo, "Seguimiento", "${state.seguimiento.size} resultados enviados", Modifier.weight(1f).aparecer(7)) { onAbrir(Screen.Seguimiento) }
        AccesoRapido(EyesIconos.Lugar, "Directorio", "Clínicas y ópticas", Modifier.weight(1f).aparecer(8)) { onAbrir(Screen.Opticas()) }
    }
}

@Composable
private fun Indicador(valor: String, texto: String, fondo: Color, color: Color, modifier: Modifier, onClick: () -> Unit) {
    EyesTarjeta(modifier, onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Box(Modifier.clip(EyesShapes.Boton).background(fondo).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = color)
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
    }
}

// ---------------------------------------------------------------------------
// Alumno y Especialista
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.PanelAlumno(onAbrir: (Screen) -> Unit) {
    EyesTarjetaAcento(fondo = EyesColors.Celeste200, modifier = Modifier.fillMaxWidth().aparecer(4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(EyesColors.Blanco), contentAlignment = Alignment.Center) {
                EyesIcono(EyesIconos.Reloj, null, EyesColors.Azul, 28.dp)
            }
            Text("Pausa 20-20-20", style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto, modifier = Modifier.padding(start = 12.dp))
        }
        Text(
            "Cada 20 minutos de pantalla, mira algo lejano (a 6 metros) durante 20 segundos. El teléfono vibra para recordártelo.",
            style = MaterialTheme.typography.bodyMedium, color = EyesColors.Marino700
        )
        EyesBotonPrincipal("Empezar y ver ejercicios", onClick = { onAbrir(Screen.PausaVisual) })
    }
    EyesTarjeta(Modifier.fillMaxWidth().aparecer(5)) {
        Text("¿Sabías que...?", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
        Text(
            "Cuando miras una pantalla parpadeas casi la mitad de veces que normalmente. Por eso los ojos se sienten secos y cansados. ¡Parpadea seguido!",
            style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario
        )
    }
    AccesoRapido(EyesIconos.Lugar, "Ópticas cercanas", "Dónde revisar tu vista", Modifier.fillMaxWidth().aparecer(6)) { onAbrir(Screen.Opticas()) }
}

@Composable
private fun ColumnScope.PanelEspecialista(onAbrir: (Screen) -> Unit) {
    AccesoRapido(EyesIconos.Lugar, "Directorio de clínicas", "Asociadas al programa primero", Modifier.fillMaxWidth().aparecer(4)) { onAbrir(Screen.Opticas(soloAsociadas = true)) }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    EyesTarjeta(Modifier.fillMaxWidth().aparecer(5)) {
        Text("Leer pase médico", style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
        Text(
            "El QR del pase se valida en el portal con tu cuenta: ahí se revisa que sea para tu clínica, que no haya vencido y que no se haya usado, y registras la atención.",
            style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario
        )
        EyesBotonPrincipal("Abrir el lector en el portal", onClick = { uri.openUri("$URL_PORTAL/Pases/Escanear") })
    }
}

/** M-RS-01 (guía), C-RS-04 (reportar) y C-RS-07 (aviso de privacidad), para todos los roles. */
@Composable
private fun Ayuda(onAbrir: (Screen) -> Unit) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    EyesEtiqueta("Ayuda", EyesColors.TextoTenue)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccesoRapido(EyesIconos.Lista, "Guía rápida", "Cómo usar la app", Modifier.weight(1f)) { onAbrir(Screen.Guia) }
        AccesoRapido(EyesIconos.Atencion, "Reportar", "Algo no funciona o tengo dudas", Modifier.weight(1f)) { onAbrir(Screen.Reportar) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        com.example.eyesofterapp.composeApp.theme.EyesBotonTexto("Aviso de privacidad", onClick = { uri.openUri("$URL_PORTAL/Privacidad/Aviso") }, color = EyesColors.Azul)
        com.example.eyesofterapp.composeApp.theme.EyesBotonTexto("Términos de uso", onClick = { uri.openUri("$URL_PORTAL/Privacidad/Terminos") }, color = EyesColors.Azul)
    }
}

@Composable
internal fun AccesoRapido(icono: ImageVector, titulo: String, subtitulo: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    EyesTarjeta(modifier, onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(EyesColors.Celeste100), contentAlignment = Alignment.Center) {
            EyesIcono(icono, null, EyesColors.Azul)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
        Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
    }
}
