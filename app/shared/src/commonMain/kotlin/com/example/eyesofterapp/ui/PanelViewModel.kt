package com.example.eyesofterapp.ui

import com.example.eyesofterapp.data.cache.AlmacenLocal
import com.example.eyesofterapp.data.cache.ComprobantePendiente
import com.example.eyesofterapp.data.model.AvisoEntradaDto
import com.example.eyesofterapp.data.model.BandejaDto
import com.example.eyesofterapp.data.model.CitaEntradaDto
import com.example.eyesofterapp.data.model.ClinicaDto
import com.example.eyesofterapp.data.model.ComprobanteDto
import com.example.eyesofterapp.data.model.ConsentimientoDto
import com.example.eyesofterapp.data.model.NotificacionDto
import com.example.eyesofterapp.data.model.ReporteEntradaDto
import com.example.eyesofterapp.data.model.SeguimientoDto
import com.example.eyesofterapp.data.model.TamizajeDto
import com.example.eyesofterapp.data.model.ahoraIso
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
import kotlin.io.encoding.Base64
import kotlin.random.Random

data class PanelUiState(
    val cargando: Boolean = false,
    val bandeja: BandejaDto = BandejaDto(),
    val tamizajes: List<TamizajeDto> = emptyList(),
    val comprobantes: List<ComprobanteDto> = emptyList(),
    val seguimiento: List<SeguimientoDto> = emptyList(),
    /** Directorio completo para elegir destino o cita (también sin coordenadas). */
    val clinicas: List<ClinicaDto> = emptyList(),
    /** PNG del QR por código de pase; vienen del servidor o de lo guardado sin conexión. */
    val pases: Map<String, ByteArray> = emptyMap(),
    /** Comprobantes guardados en el teléfono esperando conexión (M-RFNN-02). */
    val pendientesDeEnvio: List<ComprobantePendiente> = emptyList(),
    /** M-RFNN-01: se está mostrando lo guardado; puede no estar actualizado. */
    val sinConexion: Boolean = false,
    /** Cuándo se descargó lo que se muestra (ISO-8601). */
    val actualizadoEn: String? = null,
    /** C-RC-08: el tutor debe aceptar el aviso de privacidad antes de ver datos. */
    val consentimientoPendiente: Boolean = false,
    val consentimiento: ConsentimientoDto? = null,
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
 *  - Tutor: bandeja, resultados de sus hijos (con caso, pase y línea de tiempo) y sus comprobantes.
 *  - Enfermería / Dirección: bandeja, seguimiento de lectura y comprobantes por revisar.
 *  - Alumno: sus resultados. Otros: solo la bandeja.
 *
 * La bandeja se revisa cada [intervaloRevisionMs] (5 s, C-RC-02) con la app abierta, y
 * siempre al iniciar sesión (M-RFNN-04). Si llega algo nuevo, se recargan los datos.
 * Sin conexión, se muestra lo guardado (M-RFNN-01, M-RFNN-03) y los comprobantes quedan
 * en cola hasta que vuelva la red (M-RFNN-02).
 *
 * No es un androidx ViewModel a propósito: vive lo que dura la sesión. Al cerrar
 * sesión se llama a [cerrar] y deja de consultar al servidor con el token viejo.
 */
class PanelViewModel(
    private val api: EyesApi,
    private val rol: Role,
    private val usuarioId: String,
    private val almacen: AlmacenLocal,
    private val viewModelScope: CoroutineScope,
    requiereConsentimiento: Boolean = false,
    private val intervaloRevisionMs: Long = 5_000,
) {

    private val _state = MutableStateFlow(PanelUiState(consentimientoPendiente = requiereConsentimiento && rol == Role.CLIENTE))
    val state: StateFlow<PanelUiState> = _state.asStateFlow()

    private val esPersonal = rol == Role.ADMINISTRADOR || rol == Role.SUPERVISOR
    private val tieneExpediente = rol == Role.CLIENTE || rol == Role.ALUMNO
    private var revision: Job? = null
    private var enviandoCola = false

    init {
        // Lo guardado se muestra de inmediato; luego se reemplaza con lo del servidor.
        almacen.leerTamizajes(usuarioId)?.let { (lista, cuando) ->
            _state.update { it.copy(tamizajes = lista, actualizadoEn = cuando, pases = pasesGuardados(lista)) }
        }
        _state.update { it.copy(pendientesDeEnvio = almacen.cola(usuarioId)) }
        if (_state.value.consentimientoPendiente) cargarConsentimiento() else refrescar()
        revision = viewModelScope.launch {
            while (isActive) {
                delay(intervaloRevisionMs)
                if (_state.value.consentimientoPendiente) continue
                val antes = _state.value.bandeja.notificaciones.firstOrNull()?.id
                val ok = refrescarBandeja()
                val despues = _state.value.bandeja.notificaciones.firstOrNull()?.id
                if (ok && antes != null && despues != antes) refrescarDatos() // llegó algo nuevo
                if (ok && _state.value.sinConexion) refrescarDatos() // volvió la conexión
                if (ok) enviarCola()
            }
        }
    }

    /** El servidor respondió 403 CONSENTIMIENTO_PENDIENTE (p. ej. cambió la versión del aviso). */
    fun pedirConsentimiento() {
        if (_state.value.consentimientoPendiente) return
        _state.update { it.copy(consentimientoPendiente = true) }
        cargarConsentimiento()
    }

    private fun cargarConsentimiento() {
        viewModelScope.launch {
            when (val r = api.consentimiento()) {
                is ResultadoRed.Exito -> _state.update {
                    it.copy(consentimiento = r.data, consentimientoPendiente = !r.data.aceptado)
                }
                else -> _state.update { it.copy(error = r.aMensajeUsuario()) }
            }
            if (!_state.value.consentimientoPendiente) refrescar()
        }
    }

    fun aceptarConsentimiento(alAceptar: () -> Unit) {
        val version = _state.value.consentimiento?.versionVigente ?: return cargarConsentimiento()
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.aceptarConsentimiento(version)) {
                is ResultadoRed.Exito -> {
                    _state.update { it.copy(enviando = false, consentimientoPendiente = false, mensaje = "Gracias. Ya puedes consultar la información.") }
                    alAceptar()
                    refrescar()
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    fun refrescar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            refrescarBandeja()
            refrescarDatos()
            _state.update { it.copy(cargando = false) }
            enviarCola()
        }
    }

    private suspend fun refrescarDatos() {
        if (tieneExpediente) {
            when (val r = api.tamizajes()) {
                is ResultadoRed.Exito -> {
                    val cuando = ahoraIso()
                    almacen.guardarTamizajes(usuarioId, r.data, cuando)
                    _state.update { it.copy(tamizajes = r.data, sinConexion = false, actualizadoEn = cuando, error = null) }
                    descargarPases(r.data)
                }
                is ResultadoRed.SinConexion, is ResultadoRed.Timeout, is ResultadoRed.ServidorInalcanzable ->
                    _state.update { it.copy(sinConexion = true) }
                else -> _state.update { it.copy(error = r.aMensajeUsuario()) }
            }
        }
        when {
            rol == Role.CLIENTE -> cargar(api.comprobantes()) { s, v -> s.copy(comprobantes = v) }
            esPersonal -> {
                cargar(api.seguimiento()) { s, v -> s.copy(seguimiento = v) }
                cargar(api.comprobantes()) { s, v -> s.copy(comprobantes = v) }
            }
        }
    }

    /** Guarda el QR de cada pase vigente para mostrarlo sin conexión (M-RFNN-03). */
    private suspend fun descargarPases(lista: List<TamizajeDto>) {
        for (t in lista) {
            val pase = t.caso?.pase?.takeIf { it.vigente } ?: continue
            if (_state.value.pases.containsKey(pase.codigo)) continue
            val png = almacen.leerPase(usuarioId, pase.codigo)
                ?: (api.pasePng(t.id) as? ResultadoRed.Exito)?.data?.also { almacen.guardarPase(usuarioId, pase.codigo, it) }
                ?: continue
            _state.update { it.copy(pases = it.pases + (pase.codigo to png)) }
        }
    }

    private fun pasesGuardados(lista: List<TamizajeDto>): Map<String, ByteArray> =
        lista.mapNotNull { t -> t.caso?.pase?.codigo?.let { c -> almacen.leerPase(usuarioId, c)?.let { c to it } } }.toMap()

    private suspend fun refrescarBandeja(): Boolean = when (val r = api.bandeja()) {
        is ResultadoRed.Exito -> { _state.update { it.copy(bandeja = r.data) }; true }
        is ResultadoRed.SinConexion, is ResultadoRed.Timeout, is ResultadoRed.ServidorInalcanzable -> {
            _state.update { it.copy(sinConexion = true) }; false
        }
        else -> false
    }

    private fun <T> cargar(r: ResultadoRed<T>, aplicar: (PanelUiState, T) -> PanelUiState) {
        when (r) {
            is ResultadoRed.Exito -> _state.update { aplicar(it, r.data) }
            is ResultadoRed.SinConexion, is ResultadoRed.Timeout, is ResultadoRed.ServidorInalcanzable -> _state.update { it.copy(sinConexion = true) }
            else -> _state.update { it.copy(error = r.aMensajeUsuario()) }
        }
    }

    /** Confirmación de lectura (C-RFN-07). Se marca al abrirla, antes de navegar. */
    fun abrirNotificacion(n: NotificacionDto) {
        if (n.leida) return
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

    // ------------------------------------------------------------------ destino, pase y citas

    fun cargarClinicas() {
        if (_state.value.clinicas.isNotEmpty()) return
        viewModelScope.launch { cargar(api.clinicas()) { s, v -> s.copy(clinicas = v) } }
    }

    /** M-RFN-09..11. [alTerminar] recibe el mensaje del servidor (pase o resumen). */
    fun elegirDestino(tamizajeId: String, clinicaId: Int?, nombreManual: String?, alTerminar: (String) -> Unit) {
        if (_state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.elegirDestino(tamizajeId, clinicaId, nombreManual)) {
                is ResultadoRed.Exito -> {
                    val nuevos = _state.value.tamizajes.map { if (it.id == tamizajeId) r.data.tamizaje else it }
                    almacen.guardarTamizajes(usuarioId, nuevos, ahoraIso())
                    _state.update { it.copy(enviando = false, tamizajes = nuevos) }
                    descargarPases(listOf(r.data.tamizaje))
                    alTerminar(r.data.mensaje)
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    fun solicitarCita(tamizajeId: String, cita: CitaEntradaDto, alTerminar: () -> Unit) {
        if (_state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.solicitarCita(tamizajeId, cita)) {
                is ResultadoRed.Exito -> {
                    _state.update { it.copy(enviando = false, mensaje = r.data.mensaje) }
                    alTerminar()
                    refrescarDatos()
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    // ------------------------------------------------------------------ comprobantes

    fun subirComprobante(tamizajeId: String, tipo: String, archivo: ArchivoElegido) {
        if (_state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.subirComprobante(tamizajeId, tipo, archivo.bytes, archivo.nombre, archivo.contentType)) {
                is ResultadoRed.Exito -> _state.update {
                    it.copy(enviando = false, comprobantes = listOf(r.data) + it.comprobantes,
                        mensaje = "Listo. Enfermería revisará tu comprobante.")
                }
                is ResultadoRed.SinConexion, is ResultadoRed.Timeout, is ResultadoRed.ServidorInalcanzable -> {
                    // M-RFNN-02: se guarda y se manda solo cuando vuelva la conexión.
                    val alumno = _state.value.tamizajes.firstOrNull { it.id == tamizajeId }?.alumnoNombre.orEmpty()
                    val p = ComprobantePendiente(
                        id = Random.nextLong().toString(16), tamizajeId = tamizajeId, alumno = alumno, tipo = tipo,
                        nombre = archivo.nombre, contentType = archivo.contentType, base64 = Base64.encode(archivo.bytes), guardadoEn = ahoraIso(),
                    )
                    val guardado = almacen.encolar(usuarioId, p)
                    _state.update {
                        it.copy(enviando = false, sinConexion = true, pendientesDeEnvio = almacen.cola(usuarioId),
                            mensaje = if (guardado) "Sin conexión: lo guardamos y se enviará solo al volver la conexión."
                            else "Sin conexión y ya hay varios archivos esperando. Inténtalo cuando tengas red.")
                    }
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    /** Envía la cola de comprobantes guardados sin conexión, del más antiguo al más nuevo. */
    private suspend fun enviarCola() {
        if (enviandoCola) return
        enviandoCola = true
        try {
            for (p in almacen.cola(usuarioId)) {
                when (val r = api.subirComprobante(p.tamizajeId, p.tipo, p.bytes(), p.nombre, p.contentType)) {
                    is ResultadoRed.Exito -> {
                        almacen.quitar(usuarioId, p.id)
                        _state.update {
                            it.copy(comprobantes = listOf(r.data) + it.comprobantes, pendientesDeEnvio = almacen.cola(usuarioId),
                                mensaje = "Se envió el comprobante de ${p.alumno.ifBlank { "tu hijo" }} que estaba guardado.")
                        }
                    }
                    is ResultadoRed.ErrorCliente -> {
                        // El servidor no lo acepta (p. ej. el caso se cerró): no tiene caso reintentar.
                        almacen.quitar(usuarioId, p.id)
                        _state.update { it.copy(pendientesDeEnvio = almacen.cola(usuarioId), mensaje = "No se pudo enviar un comprobante guardado: ${r.mensaje}") }
                    }
                    else -> return // sigue sin conexión: se reintenta en la siguiente vuelta
                }
            }
        } finally {
            enviandoCola = false
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

    /** C-RS-04: falla o duda. */
    fun reportar(tipo: String, mensaje: String, alTerminar: () -> Unit) {
        if (_state.value.enviando) return
        viewModelScope.launch {
            _state.update { it.copy(enviando = true) }
            when (val r = api.reportar(ReporteEntradaDto(tipo, mensaje.trim()))) {
                is ResultadoRed.Exito -> {
                    _state.update { it.copy(enviando = false, mensaje = "Gracias. Dirección revisará tu reporte y te responderá por correo.") }
                    alTerminar()
                }
                else -> _state.update { it.copy(enviando = false, mensaje = r.aMensajeUsuario()) }
            }
        }
    }

    /** M-RI-02: token de Firebase del teléfono. */
    fun registrarPush(tokenPush: String, plataforma: String) {
        viewModelScope.launch { api.registrarDispositivo(tokenPush, plataforma) }
    }

    suspend fun olvidarPush(tokenPush: String) { api.olvidarDispositivo(tokenPush) }

    suspend fun descargarComprobante(id: String): ByteArray? =
        (api.archivoComprobante(id) as? ResultadoRed.Exito)?.data

    fun mensajeVisto() = _state.update { it.copy(mensaje = null) }

    fun cerrar() {
        revision?.cancel()
        viewModelScope.cancel()
    }
}
