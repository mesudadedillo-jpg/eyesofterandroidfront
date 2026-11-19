package com.example.eyesofterapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
) {
    EyeSofterTheme {
        CompositionLocalProvider(LocalTelefono provides telefono) {
            val container = remember(locationProvider) { AppContainer(locationProvider) }

            val authViewModel = viewModel<AuthViewModel> {
                AuthViewModel(container.loginUseCase, container.opticasApi, container.sesionGuardada)
            }
            val state by authViewModel.state.collectAsState()

            AppNavHost(
                container = container,
                state = state,
                onLogin = authViewModel::login,
                onLogout = authViewModel::logout,
                token = { authViewModel.token },
                onSesionVencida = authViewModel::sesionVencida,
                directorioNativo = directorioNativo,
            )
        }
    }
}
