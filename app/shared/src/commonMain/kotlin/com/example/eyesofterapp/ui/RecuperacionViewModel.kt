package com.example.eyesofterapp.ui

import com.example.eyesofterapp.data.network.EyesApi
import com.example.eyesofterapp.data.network.ResultadoRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pasos de "¿Olvidaste tu contraseña?". */
enum class PasoRecuperacion { Cuenta, Codigo, Listo }

data class RecuperacionUiState(
    val paso: PasoRecuperacion = PasoRecuperacion.Cuenta,
    /** Correo o celular que escribió (se manda igual en el paso 2). */
    val cuenta: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
    /** "Te enviamos un código..." (lo dice el servidor). */
    val aviso: String? = null,
    /** Segundos que faltan para poder pedir otro código (el servidor acepta uno por minuto). */
    val reenviarEn: Int = 0,
)

/**
 * "¿Olvidaste tu contraseña?" dentro de la app (C-RFNN-02), sin sesión:
 *  1. Correo o celular → el servidor manda un código de 6 dígitos (correo, o SMS si está activo).
 *  2. Código + contraseña nueva → listo, a iniciar sesión.
 * El código es el mismo sistema del portal: también se puede usar en la web.
 */
class RecuperacionViewModel(
    private val api: EyesApi,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RecuperacionUiState())
    val state: StateFlow<RecuperacionUiState> = _state.asStateFlow()
    private var cuentaAtras: Job? = null

    fun pedirCodigo(cuenta: String) {
        val limpia = cuenta.trim()
        if ('@' !in limpia && limpia.count(Char::isDigit) < 10) {
            _state.update { it.copy(error = "Escribe tu correo o tu celular de 10 dígitos.") }
            return
        }
        enviar(limpia)
    }

    /** "No me llegó": pide otro con la misma cuenta. */
    fun reenviar() {
        if (_state.value.reenviarEn > 0) return
        enviar(_state.value.cuenta)
    }

    private fun enviar(cuenta: String) {
        if (_state.value.enviando) return
        scope.launch {
            _state.update { it.copy(enviando = true, error = null) }
            when (val r = api.solicitarCodigo(cuenta)) {
                is ResultadoRed.Exito -> {
                    _state.update { it.copy(enviando = false, paso = PasoRecuperacion.Codigo, cuenta = cuenta, aviso = r.data.mensaje) }
                    iniciarCuentaAtras()
                }
                else -> _state.update { it.copy(enviando = false, error = r.aMensajeUsuario()) }
            }
        }
    }

    fun restablecer(codigo: String, password: String, confirmar: String) {
        val digitos = codigo.filter(Char::isDigit)
        val problema = when {
            digitos.length != 6 -> "El código son 6 números."
            password != confirmar -> "Las contraseñas no coinciden."
            else -> erroresPassword(password).firstOrNull()
        }
        if (problema != null) {
            _state.update { it.copy(error = problema) }
            return
        }
        if (_state.value.enviando) return
        scope.launch {
            _state.update { it.copy(enviando = true, error = null) }
            when (val r = api.restablecerPassword(_state.value.cuenta, digitos, password)) {
                is ResultadoRed.Exito -> _state.update { it.copy(enviando = false, paso = PasoRecuperacion.Listo, aviso = null) }
                else -> _state.update { it.copy(enviando = false, error = r.aMensajeUsuario()) }
            }
        }
    }

    /** Regresar al paso 1 para escribir otro correo o celular. */
    fun cambiarCuenta() {
        cuentaAtras?.cancel()
        _state.update { RecuperacionUiState(cuenta = it.cuenta) }
    }

    fun errorVisto() = _state.update { it.copy(error = null) }

    private fun iniciarCuentaAtras() {
        cuentaAtras?.cancel()
        cuentaAtras = scope.launch {
            for (s in SEGUNDOS_REENVIO downTo 0) {
                _state.update { it.copy(reenviarEn = s) }
                if (s > 0) delay(1_000)
            }
        }
    }

    fun cerrar() = scope.cancel()

    companion object {
        const val SEGUNDOS_REENVIO = 60

        /** Mismas reglas que el portal y el servidor: 8+ caracteres, mayúscula, minúscula y número. */
        fun erroresPassword(p: String): List<String> = buildList {
            if (p.length < 8) add("La contraseña debe tener al menos 8 caracteres.")
            if (p.length > 64) add("La contraseña no puede pasar de 64 caracteres.")
            if (p.none { it in '0'..'9' }) add("La contraseña debe incluir al menos un número (0-9).")
            if (p.none(Char::isUpperCase)) add("La contraseña debe incluir al menos una letra mayúscula.")
            if (p.none(Char::isLowerCase)) add("La contraseña debe incluir al menos una letra minúscula.")
        }
    }
}
