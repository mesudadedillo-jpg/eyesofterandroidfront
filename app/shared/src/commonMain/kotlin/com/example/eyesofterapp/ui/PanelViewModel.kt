package com.example.eyesofterapp.ui

import com.example.eyesofterapp.data.model.AvisoEntradaDto
import com.example.eyesofterapp.data.model.BandejaDto
import com.example.eyesofterapp.data.model.ComprobanteDto
import com.example.eyesofterapp.data.model.NotificacionDto
import com.example.eyesofterapp.data.model.SeguimientoDto
import com.example.eyesofterapp.data.model.TamizajeDto
import com.example.eyesofterapp.data.network.EyesApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.plataforma.ArchivoElegido
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PanelUiState(
    val cargando: Boolean = false,
    val bandeja: BandejaDto = BandejaDto(),
    val tamizajes: List<TamizajeDto> = emptyList(),
    val comprobantes: List<ComprobanteDto> = emptyList(),
    val seguimiento: List<SeguimientoDto> = emptyList(),
    /** Error al cargar (se muestra en la pantalla). */
    val error: String? = null,
    /** Mensaje corto tras una acción ("Comprobante enviado"). Se borra con [PanelViewModel.mensajeVisto]. */
    val mensaje: String? = null,
    val enviando: Boolean = false,
) {
    val tamizajeReciente: TamizajeDto? get() = tamizajes.firstOrNull()
    val comprobantesPendientes: Int get() = comprobantes.count { it.estado == "PENDIENTE" }
    val avisosSinLeer: Int get() = seguimiento.count { it.leidaEn == null }
}

/**
 * Datos del panel del usuario con sesión. Qué se carga depende del rol:
 *  - Tutor: bandeja, resultados de sus hijos y sus comprobantes.
 *  - Enfermería / Dirección: bandeja, seguimiento de lectura y comprobantes por revisar.
 *  - Otros: solo la bandeja.
 *
 * Mientras no haya push (Firebase), la bandeja se revisa cada minuto con la app
 * abierta, y siempre al iniciar sesión (M-RFNN-04).
 *
 * No es un androidx ViewModel a propósito: vive lo que dura la sesión. Al cerrar
 * sesión se llama a [cerrar] y deja de consultar al servidor con el token viejo.
 */
class PanelViewModel(
    private val api: EyesApi,
    private val rol: Role,
    private val viewModelScope: CoroutineScope,
    private val intervaloRevisionMs: Long = 60_000,
) {

    private val _state = MutableStateFlow(PanelUiState())
    val state: StateFlow<PanelUiState> = _state.asStateFlow()

    private val esPersonal = rol == Role.ADMINISTRADOR || rol == Role.SUPERVISOR
    private var revision: Job? = null

    init {
        refrescar()
        revision = viewModelScope.launch {
            while (isActive) {
                delay(intervaloRevisionMs)
                refrescarBandeja()
            }
        }
    }

    fun refrescar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            refrescarBandeja()
            when (rol) {
                Role.CLIENTE -> {
                    cargar(api.tamizajes()) { s, v -> s.copy(tamizajes = v) }
                    cargar(api.comprobantes()) { s, v -> s.copy(comprobantes = v) }
                }
                Role.ADMINISTRADOR, Role.SUPERVISOR -> {
                    cargar(api.seguimiento()) { s, v -> s.copy(seguimiento = v) }
                    cargar(api.comprobantes()) { s, v -> s.copy(comprobantes = v) }
                }
                else -> {}
            }
            _state.update { it.copy(cargando = false) }
        }
    }

    private suspend fun refrescarBandeja() {
        cargar(api.bandeja()) { s, v -> s.copy(bandeja = v) }
    }

    private fun <T> cargar(r: ResultadoRed<T>, aplicar: (PanelUiState, T) -> PanelUiState) {
        when (r) {
            is ResultadoRed.Exito -> _state.update { aplicar(it, r.data) }
            else -> _state.update { it.copy(error = r.aMensajeUsuario()) }
        }
    }

    /** Confirmación de lectura (C-RFN-07). Se marca al abrirla, antes de navegar. */
    fun abrirNotificacion(n: NotificacionDto) {
        if (n.leida) return
        // Se marca en pantalla de inmediato; el servidor confirma después.
        _state.update { s ->
            s.copy(
                bandeja = s.bandeja.copy(
                    noLeidas = (s.bandeja.noLeidas - 1).coerceAtLeast(0),
                    notificaciones = s.bandeja.notificaciones.map { if (it.id == n.id) it.copy(leidaEn = "ahora") else it }
                )
            )
        }
        viewModelScope.launch {
            api.marcarLeida(n.id)
            refrescarBandeja()
        }
    }

    fun marcarTodasLeidas() {
        viewModelScope.launch {
            api.marcarTodasLeidas()
            refrescarBandeja()
        }
    }

    fun subirComprobante(tamizajeId: String, tipo: String, archivo: ArchivoElegido) {
        if (_state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.subirComprobante(tamizajeId, tipo, archivo.bytes, archivo.nombre, archivo.contentType)) {
                is ResultadoRed.Exito -> _state.update {
                    it.copy(enviando = false, comprobantes = listOf(r.data) + it.comprobantes,
                        mensaje = "Listo. Enfermería revisará tu comprobante.")
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    fun revisarComprobante(id: String, aprobado: Boolean, motivo: String?) {
        if (!esPersonal || _state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.revisarComprobante(id, aprobado, motivo)) {
                is ResultadoRed.Exito -> _state.update { s ->
                    s.copy(enviando = false, comprobantes = s.comprobantes.map { if (it.id == id) r.data else it },
                        mensaje = if (aprobado) "Comprobante aprobado. Se avisó al tutor." else "Comprobante rechazado. Se avisó al tutor.")
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    fun enviarAviso(titulo: String, mensaje: String, aTodosLosTutores: Boolean, usernames: List<String>, alTerminar: () -> Unit) {
        if (!esPersonal || _state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            val aviso = AvisoEntradaDto(titulo.trim(), mensaje.trim(), usernames, if (aTodosLosTutores) "CLIENTE" else null)
            when (val r = api.enviarAviso(aviso)) {
                is ResultadoRed.Exito -> {
                    val faltan = r.data.noEncontrados.takeIf { it.isNotEmpty() }?.joinToString(prefix = " No existen: ") ?: ""
                    _state.update { it.copy(enviando = false, mensaje = "Aviso enviado a ${r.data.enviadas} personas.$faltan") }
                    alTerminar()
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    suspend fun descargarComprobante(id: String): ByteArray? =
        (api.archivoComprobante(id) as? ResultadoRed.Exito)?.data

    fun mensajeVisto() = _state.update { it.copy(mensaje = null) }

    fun cerrar() {
        revision?.cancel()
        viewModelScope.cancel()
    }
}
