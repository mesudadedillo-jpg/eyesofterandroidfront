package com.example.eyesofterapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.eyesofterapp.ui.PanelViewModel
import com.example.eyesofterapp.plataforma.Push
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyesofterapp.composeApp.navigation.AppNavHost
import com.example.eyesofterapp.composeApp.navigation.DirectorioNativo
import com.example.eyesofterapp.composeApp.theme.EyeSofterTheme
import com.example.eyesofterapp.location.LocationProvider
import com.example.eyesofterapp.plataforma.LocalTelefono
import com.example.eyesofterapp.plataforma.Telefono
import com.example.eyesofterapp.ui.AuthViewModel

/**
 * @param telefono cámara, vibración, etc. Solo Android lo da; en escritorio y web
 *   queda en null y las pantallas ocultan esos botones.
 * @param directorioNativo el directorio con el mapa 3D real (MapLibre). Solo Android
 *   lo tiene; las otras plataformas usan el mapa ilustrado de OpticasScreen.
 */
@Composable
fun App(
    locationProvider: LocationProvider? = null,
    telefono: Telefono? = null,
    directorioNativo: DirectorioNativo? = null,
    /** Firebase Cloud Messaging en Android; null en las demás plataformas. */
    push: Push? = null,
    /** Ruta de la push que el usuario tocó ("tamizaje/12"...). */
    rutaExterna: StateFlow<String?>? = null,
    onRutaAtendida: () -> Unit = {},
) {
    EyeSofterTheme {
        CompositionLocalProvider(LocalTelefono provides telefono) {
            val container = remember(locationProvider) { AppContainer(locationProvider) }

            val authViewModel = viewModel<AuthViewModel> {
                AuthViewModel(container.loginUseCase, container.opticasApi, container.sesionGuardada)
            }
            val state by authViewModel.state.collectAsState()
            val scope = rememberCoroutineScope()
            var panelActual by remember { androidx.compose.runtime.mutableStateOf<PanelViewModel?>(null) }

            AppNavHost(
                container = container,
                state = state,
                onLogin = authViewModel::login,
                onLogout = {
                    // El teléfono deja de recibir avisos de esta cuenta y se borra lo guardado (teléfono compartido).
                    val panel = panelActual
                    val usuario = state.session?.user
                    scope.launch {
                        withTimeoutOrNull(3_000) { push?.token()?.let { panel?.olvidarPush(it) } }
                        usuario?.let { container.almacen.borrar(it.id.ifBlank { it.username }) }
                        authViewModel.logout()
                    }
                },
                token = { authViewModel.token },
                onSesionVencida = authViewModel::sesionVencida,
                onConsentimientoAceptado = authViewModel::consentimientoAceptado,
                directorioNativo = directorioNativo,
                rutaExterna = rutaExterna,
                onRutaAtendida = onRutaAtendida,
                alIniciarPanel = { panel ->
                    panelActual = panel
                    if (push != null) scope.launch { push.token()?.let { panel.registrarPush(it, push.plataforma) } }
                },
            )
        }
    }
}
