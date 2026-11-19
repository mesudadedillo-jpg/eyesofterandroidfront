package com.example.eyesofterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eyesofterapp.data.network.BASE_URL
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.data.session.SesionGuardada
import com.example.eyesofterapp.domain.model.InvalidCredentialsException
import com.example.eyesofterapp.domain.model.RechazoDelServidorException
import com.example.eyesofterapp.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val opticasApi: OpticasApi? = null,
    private val sesionGuardada: SesionGuardada? = null,
) : ViewModel() {

    // Si hay una sesión guardada, la app abre directo en el panel del usuario.
    private val _state = MutableStateFlow(AuthUiState(session = sesionGuardada?.leer()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** El token que mandan las rutas con sesión. */
    val token: String? get() = _state.value.session?.token

    init {
        despertarServidor()
    }

    // Railway puede tardar en "despertar" el servidor: se le hace un ping al abrir la app.
    private fun despertarServidor() {
        viewModelScope.launch {
            try {
                opticasApi?.verificarSalud()
            } catch (_: Exception) {
            }
        }
    }

    fun login(username: String, password: String) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val primerIntento = loginUseCase(username, password, timeoutMs = 3000L)
            primerIntento.getOrNull()?.let { exito(it); return@launch }

            // Rechazos del servidor (contraseña mala, cuenta bloqueada): no tiene caso reintentar.
            when (val e = primerIntento.exceptionOrNull()) {
                is InvalidCredentialsException -> {
                    _state.update { it.copy(isLoading = false, error = "Usuario o contraseña incorrectos") }
                    return@launch
                }
                is RechazoDelServidorException, is IllegalArgumentException -> {
                    _state.update { it.copy(isLoading = false, error = e.message) }
                    return@launch
                }
            }

            // Timeout o servidor dormido: se pregunta a /salud y se reintenta una vez con más tiempo.
            _state.update { it.copy(error = "Verificando disponibilidad del servidor...") }
            val salud = opticasApi?.verificarSalud() ?: ResultadoRed.SinConexion

            if (salud is ResultadoRed.Exito) {
                _state.update { it.copy(error = "Conectando con el servidor...") }
                val segundo = loginUseCase(username, password, timeoutMs = 8000L)
                segundo.getOrNull()?.let { exito(it); return@launch }
                val mensaje = when (val e = segundo.exceptionOrNull()) {
                    is InvalidCredentialsException -> "Usuario o contraseña incorrectos"
                    is RechazoDelServidorException -> e.message
                    else -> "No se puede conectar al servidor ($BASE_URL). Revisa tu conexión."
                }
                _state.update { it.copy(isLoading = false, error = mensaje) }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "No se puede conectar al servidor ($BASE_URL). Presiona 'Iniciar sesión' para reintentar."
                    )
                }
            }
        }
    }

    private fun exito(sesion: com.example.eyesofterapp.domain.model.Session) {
        sesionGuardada?.guardar(sesion)
        _state.update { it.copy(isLoading = false, session = sesion, error = null) }
    }

    fun logout() {
        sesionGuardada?.borrar()
        _state.value = AuthUiState()
    }

    /** El servidor rechazó el token (401): vuelve al login con un aviso. */
    fun sesionVencida() {
        sesionGuardada?.borrar()
        _state.value = AuthUiState(error = "Tu sesión venció. Inicia sesión de nuevo.")
    }
}
