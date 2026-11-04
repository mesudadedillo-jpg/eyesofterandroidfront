package com.example.eyesofterapp.composeApp.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.eyesofterapp.AppContainer
import com.example.eyesofterapp.composeApp.screens.AdminScreen
import com.example.eyesofterapp.composeApp.screens.AlumnoScreen
import com.example.eyesofterapp.composeApp.screens.ClienteScreen
import com.example.eyesofterapp.composeApp.screens.DoctorScreen
import com.example.eyesofterapp.composeApp.screens.LoginScreen
import com.example.eyesofterapp.composeApp.screens.OpticasScreen
import com.example.eyesofterapp.composeApp.screens.RoleScaffold
import com.example.eyesofterapp.composeApp.screens.SupervisorScreen
import com.example.eyesofterapp.ui.AuthUiState

@Composable
fun AppNavHost(
    container: AppContainer,
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val screenPorRol: Screen = state.session?.user?.toScreen() ?: Screen.Login
    var currentScreen by remember(screenPorRol) { mutableStateOf(screenPorRol) }

    val opticasViewModel = remember(container) { container.crearOpticasViewModel() }
    val opticasState by opticasViewModel.state.collectAsState()

    Crossfade(targetState = currentScreen, label = "navegacion") { current ->
        when (current) {
            Screen.Login -> LoginScreen(state, onLogin)
            is Screen.Admin -> AdminScreen(current.user, onLogout = onLogout, onVerOpticas = { currentScreen = Screen.Opticas(current.user) })
            is Screen.Supervisor -> SupervisorScreen(current.user, onLogout = onLogout, onVerOpticas = { currentScreen = Screen.Opticas(current.user) })
            is Screen.Cliente -> ClienteScreen(current.user, onLogout = onLogout, onVerOpticas = { currentScreen = Screen.Opticas(current.user) })
            is Screen.Doctor -> DoctorScreen(current.user, onLogout = onLogout, onVerOpticas = { currentScreen = Screen.Opticas(current.user) })
            is Screen.Alumno -> AlumnoScreen(current.user, onLogout = onLogout, onVerOpticas = { currentScreen = Screen.Opticas(current.user) })
            is Screen.Desconocido -> RoleScaffold("Rol no reconocido", current.user, onLogout) {
                Text("Tu rol no tiene una vista asignada. Contacta al administrador.")
            }
            is Screen.Opticas -> OpticasScreen(
                state = opticasState,
                onSolicitarUbicacion = { opticasViewModel.cargarOpticasPorUbicacion() },
                onSimularUbicacion = { opticasViewModel.simularUbicacionReal() },
                onBuscarPorCiudad = { query -> opticasViewModel.buscarPorCiudadOZip(query) },
                onActualizarBusquedaTexto = { texto -> opticasViewModel.actualizarBusquedaTexto(texto) },
                onVolver = { currentScreen = current.user.toScreen() }
            )
        }
    }
}
