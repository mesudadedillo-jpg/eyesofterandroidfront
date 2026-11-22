package com.example.eyesofterapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eyesofterapp.composeApp.navigation.AppNavHost
import com.example.eyesofterapp.composeApp.theme.EyeSofterTheme
import com.example.eyesofterapp.location.LocationProvider
import com.example.eyesofterapp.ui.AuthViewModel

/**
 * @param onAbrirMapaEnVivo acción para abrir el mapa real (MapLibre). Solo Android
 * lo tiene; en escritorio, web e iOS queda en null y el botón no aparece.
 */
@Composable
fun App(locationProvider: LocationProvider? = null, onAbrirMapaEnVivo: (() -> Unit)? = null) {
    EyeSofterTheme {
        val container = remember(locationProvider) { AppContainer(locationProvider) }

        val authViewModel = viewModel<AuthViewModel> { AuthViewModel(container.loginUseCase, container.opticasApi) }
        val state by authViewModel.state.collectAsState()

        AppNavHost(
            container = container,
            state = state,
            onLogin = authViewModel::login,
            onLogout = authViewModel::logout,
            onAbrirMapaEnVivo = onAbrirMapaEnVivo
        )
    }
}
