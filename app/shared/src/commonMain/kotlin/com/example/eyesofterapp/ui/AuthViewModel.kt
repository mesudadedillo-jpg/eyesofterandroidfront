package com.example.eyesofterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eyesofterapp.data.network.BASE_URL
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val opticasApi: OpticasApi? = null
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        despertarServidor()
    }

    private fun despertarServidor() {
        viewModelScope.launch {
            try {
                opticasApi?.verificarSalud()
            } catch (_: Exception) {
                // Ping silencioso al iniciar
            }
        }
    }

    fun login(username: String, password: String) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Intento 1: Timeout de 3 segundos
            val primerIntento = loginUseCase(username, password, timeoutMs = 3000L)

            if (primerIntento.isSuccess) {
                _state.update { it.copy(isLoading = false, session = primerIntento.getOrNull(), error = null) }
                return@launch
            }

            val errorInicial = primerIntento.exceptionOrNull()?.message ?: ""

            // 401 Credenciales incorrectas: no reintentar
            if (errorInicial.contains("incorrectos", ignoreCase = true) || errorInicial.contains("inválida", ignoreCase = true)) {
                _state.update { it.copy(isLoading = false, error = "Usuario o contraseña incorrectos") }
                return@launch
            }

            // Para timeouts o servidor inalcanzable, consultar /salud con timeout de 3 s
            _state.update { it.copy(error = "Verificando disponibilidad del servidor...") }
            val salud = opticasApi?.verificarSalud() ?: ResultadoRed.SinConexion

            if (salud is ResultadoRed.Exito) {
                // Servidor responde /salud: reintentar login UNA vez con timeout de 8 s
                _state.update { it.copy(error = "Conectando con el servidor...") }
                val segundoIntento = loginUseCase(username, password, timeoutMs = 8000L)

                if (segundoIntento.isSuccess) {
                    _state.update { it.copy(isLoading = false, session = segundoIntento.getOrNull(), error = null) }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "No se puede conectar al servidor ($BASE_URL). Revisa tu conexión."
                        )
                    }
                }
            } else {
                // Servidor no responde /salud
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "No se puede conectar al servidor ($BASE_URL). Presiona 'Iniciar sesión' para reintentar."
                    )
                }
            }
        }
    }

    fun logout() {
        _state.value = AuthUiState()
    }
}
