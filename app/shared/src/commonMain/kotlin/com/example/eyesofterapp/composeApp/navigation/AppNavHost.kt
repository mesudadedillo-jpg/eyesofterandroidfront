package com.example.eyesofterapp.composeApp.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.example.eyesofterapp.AppContainer
import com.example.eyesofterapp.composeApp.screens.ComprobantesScreen
import com.example.eyesofterapp.composeApp.screens.ConsentimientoScreen
import com.example.eyesofterapp.composeApp.screens.ElegirDestinoScreen
import com.example.eyesofterapp.composeApp.screens.GuiaScreen
import com.example.eyesofterapp.composeApp.screens.ReportarScreen
import com.example.eyesofterapp.composeApp.screens.SolicitarCitaScreen
import com.example.eyesofterapp.composeApp.screens.InicioScreen
import com.example.eyesofterapp.composeApp.screens.LoginScreen
import com.example.eyesofterapp.composeApp.screens.NotificacionesScreen
import com.example.eyesofterapp.composeApp.screens.NuevoAvisoScreen
import com.example.eyesofterapp.composeApp.screens.OpticasScreen
import com.example.eyesofterapp.composeApp.screens.PausaVisualScreen
import com.example.eyesofterapp.composeApp.screens.SeguimientoScreen
import com.example.eyesofterapp.composeApp.screens.TamizajeScreen
import com.example.eyesofterapp.composeApp.theme.EyesMotion
import com.example.eyesofterapp.domain.model.Session
import com.example.eyesofterapp.ui.AuthUiState
import com.example.eyesofterapp.ui.OpticasUiState
import com.example.eyesofterapp.ui.PanelViewModel
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.StateFlow

/** Acciones del directorio que comparten la versión común y la nativa (Android). */
class AccionesDirectorio(
    val usarGps: () -> Unit,
    val simular: () -> Unit,
    val buscar: (String) -> Unit,
    val actualizarTexto: (String) -> Unit,
)

/**
 * Directorio con mapa propio de la plataforma. Android pasa uno con MapLibre en 3D;
 * si es null se usa OpticasScreen (mapa ilustrado) en todas las plataformas.
 */
typealias DirectorioNativo = @Composable (
    estado: OpticasUiState,
    acciones: AccionesDirectorio,
    soloAsociadas: Boolean,
    onVolver: () -> Unit,
) -> Unit

@Composable
fun AppNavHost(
    container: AppContainer,
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    token: () -> String?,
    onSesionVencida: () -> Unit,
    onConsentimientoAceptado: () -> Unit = {},
    directorioNativo: DirectorioNativo? = null,
    /** Ruta pedida desde fuera (p. ej. al tocar una push): "tamizaje/12", "seguimiento"... */
    rutaExterna: StateFlow<String?>? = null,
    onRutaAtendida: () -> Unit = {},
    /** Se llama con el panel de la sesión (Android registra ahí el token de push). */
    alIniciarPanel: (PanelViewModel) -> Unit = {},
) {
    val sesion = state.session
    if (sesion == null) {
        LoginScreen(state, onLogin)
        return
    }
    PanelConSesion(container, sesion, onLogout, token, onSesionVencida, onConsentimientoAceptado, directorioNativo, rutaExterna, onRutaAtendida, alIniciarPanel)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PanelConSesion(
    container: AppContainer,
    sesion: Session,
    onLogout: () -> Unit,
    token: () -> String?,
    onSesionVencida: () -> Unit,
    onConsentimientoAceptado: () -> Unit,
    directorioNativo: DirectorioNativo?,
    rutaExterna: StateFlow<String?>?,
    onRutaAtendida: () -> Unit,
    alIniciarPanel: (PanelViewModel) -> Unit,
) {
    val user = sesion.user
    // Un panel por sesión: al cerrar sesión (o si cambia el token) se detiene y se crea otro.
    val panel = remember(sesion.token) { container.crearPanel(user, token, onSesionVencida) }
    DisposableEffect(panel) { onDispose { panel.cerrar() } }
    LaunchedEffect(panel) { alIniciarPanel(panel) }
    val panelState by panel.state.collectAsState()

    // C-RC-08: sin aviso de privacidad aceptado no se muestran datos.
    if (panelState.consentimientoPendiente) {
        ConsentimientoScreen(panelState, onAceptar = { panel.aceptarConsentimiento(onConsentimientoAceptado) }, onLogout = onLogout, onMensajeVisto = panel::mensajeVisto)
        return
    }

    val opticasViewModel = remember(container) { container.crearOpticasViewModel() }
    val opticasState by opticasViewModel.state.collectAsState()

    var pila by remember(sesion.token) { mutableStateOf(listOf<Screen>(Screen.Inicio)) }
    val actual = pila.last()
    fun abrir(s: Screen) { pila = pila + s }
    fun volver() { if (pila.size > 1) pila = pila.dropLast(1) }

    // Botón o gesto "Atrás" del teléfono: regresa a la pantalla anterior.
    BackHandler(enabled = pila.size > 1) { volver() }

    // M-RC-02: al tocar una push se abre directo la pantalla de su "ruta".
    val ruta = rutaExterna?.collectAsState()?.value
    LaunchedEffect(ruta) {
        if (ruta != null) {
            rutaAScreen(ruta)?.let { if (actual != it) abrir(it) }
            onRutaAtendida()
        }
    }

    val acciones = remember(opticasViewModel) {
        AccionesDirectorio(
            usarGps = opticasViewModel::cargarOpticasPorUbicacion,
            simular = { opticasViewModel.simularUbicacionReal() },
            buscar = opticasViewModel::buscarPorCiudadOZip,
            actualizarTexto = opticasViewModel::actualizarBusquedaTexto,
        )
    }

    AnimatedContent(
        targetState = actual,
        transitionSpec = {
            (fadeIn(tween(EyesMotion.Pantalla, easing = EyesMotion.EaseOut)) +
                slideInVertically(tween(EyesMotion.Pantalla, easing = EyesMotion.EaseOut)) { it / 24 })
                .togetherWith(fadeOut(tween(EyesMotion.Salida)))
        },
        label = "navegacion"
    ) { pantalla ->
        when (pantalla) {
            Screen.Inicio -> InicioScreen(
                user = user,
                state = panelState,
                onAbrir = ::abrir,
                onRefrescar = panel::refrescar,
                onLogout = onLogout,
                onMensajeVisto = panel::mensajeVisto,
            )
            is Screen.Opticas -> if (directorioNativo != null) {
                directorioNativo(opticasState, acciones, pantalla.soloAsociadas, ::volver)
            } else {
                OpticasScreen(
                    state = opticasState,
                    onSolicitarUbicacion = acciones.usarGps,
                    onSimularUbicacion = acciones.simular,
                    onBuscarPorCiudad = acciones.buscar,
                    onActualizarBusquedaTexto = acciones.actualizarTexto,
                    onVolver = ::volver,
                    soloAsociadasInicial = pantalla.soloAsociadas,
                )
            }
            Screen.Notificaciones -> NotificacionesScreen(
                state = panelState,
                onAbrirNotificacion = { n ->
                    panel.abrirNotificacion(n)
                    rutaAScreen(n.datos["ruta"])?.let(::abrir)
                },
                onMarcarTodas = panel::marcarTodasLeidas,
                onVolver = ::volver,
            )
            is Screen.Tamizaje -> TamizajeScreen(
                tamizajeId = pantalla.id,
                state = panelState,
                onSubir = panel::subirComprobante,
                onBuscarClinica = { abrir(Screen.Opticas(soloAsociadas = true)) },
                onElegirDestino = { abrir(Screen.ElegirDestino(it)) },
                onSolicitarCita = { abrir(Screen.SolicitarCita(it)) },
                onMensajeVisto = panel::mensajeVisto,
                onVolver = ::volver,
            )
            is Screen.ElegirDestino -> ElegirDestinoScreen(
                tamizajeId = pantalla.tamizajeId,
                state = panelState,
                onCargarClinicas = panel::cargarClinicas,
                onElegir = { clinica, manual, fin -> panel.elegirDestino(pantalla.tamizajeId, clinica, manual, fin) },
                onMensajeVisto = panel::mensajeVisto,
                onVolver = ::volver,
            )
            is Screen.SolicitarCita -> SolicitarCitaScreen(
                tamizajeId = pantalla.tamizajeId,
                state = panelState,
                onCargarClinicas = panel::cargarClinicas,
                onEnviar = { cita, fin -> panel.solicitarCita(pantalla.tamizajeId, cita, fin) },
                onMensajeVisto = panel::mensajeVisto,
                onVolver = ::volver,
            )
            Screen.Guia -> GuiaScreen(user.role, ::volver)
            Screen.Reportar -> ReportarScreen(
                state = panelState,
                onEnviar = { tipo, mensaje, fin -> panel.reportar(tipo, mensaje, fin) },
                onMensajeVisto = panel::mensajeVisto,
                onVolver = ::volver,
            )
            Screen.Comprobantes -> ComprobantesScreen(
                user = user,
                state = panelState,
                onRevisar = panel::revisarComprobante,
                descargar = panel::descargarComprobante,
                onAbrirTamizaje = { abrir(Screen.Tamizaje(it)) },
                onMensajeVisto = panel::mensajeVisto,
                onVolver = ::volver,
            )
            Screen.Seguimiento -> SeguimientoScreen(state = panelState, onRefrescar = panel::refrescar, onVolver = ::volver)
            Screen.NuevoAviso -> NuevoAvisoScreen(
                state = panelState,
                onEnviar = { titulo, mensaje, todos, usuarios -> panel.enviarAviso(titulo, mensaje, todos, usuarios, ::volver) },
                onVolver = ::volver,
            )
            Screen.PausaVisual -> PausaVisualScreen(onVolver = ::volver)
        }
    }
}
