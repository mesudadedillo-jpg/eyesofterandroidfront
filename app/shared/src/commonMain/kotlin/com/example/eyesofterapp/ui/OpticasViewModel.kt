package com.example.eyesofterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eyesofterapp.data.cache.DiskOpticasCache
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.location.LocationProvider
import com.example.eyesofterapp.location.ResultadoUbicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OpticasViewModel(
    private val opticasApi: OpticasApi,
    private val locationProvider: LocationProvider?
) : ViewModel() {

    private val _state = MutableStateFlow(OpticasUiState())
    val state: StateFlow<OpticasUiState> = _state.asStateFlow()

    fun cargarOpticasPorUbicacion() {
        if (_state.value.cargando) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    cargando = true,
                    error = null,
                    sinPermisoUbicacion = false
                )
            }

            val resultadoGps = locationProvider?.obtenerUbicacion()
            if (resultadoGps !is ResultadoUbicacion.Exito) {
                val mensaje = when (resultadoGps) {
                    ResultadoUbicacion.SinPermiso ->
                        "No diste permiso de ubicación. Vuelve a pedirlo o actívalo en Ajustes > Apps > Eyesofter > Permisos."
                    ResultadoUbicacion.UbicacionApagada ->
                        "La ubicación del teléfono está apagada. Enciéndela y vuelve a tocar \"GPS Real\"."
                    ResultadoUbicacion.SinSenal ->
                        "El GPS no respondió a tiempo. Sal a un lugar abierto o activa Wi-Fi para mejorar la precisión, y vuelve a intentarlo."
                    null ->
                        "Esta plataforma no tiene sensor de ubicación disponible."
                    is ResultadoUbicacion.Exito -> null // no ocurre
                }
                // Mostramos la ubicación de demostración, pero marcada claramente como SIMULADA.
                val simCoords = Coordenadas(20.0825, -98.3622)
                val resultadoSim = opticasApi.getOpticasCercanas(simCoords.lat, simCoords.lng)
                val opticasData = if (resultadoSim is ResultadoRed.Exito) resultadoSim.data else emptyList()

                _state.update {
                    it.copy(
                        cargando = false,
                        sinPermisoUbicacion = resultadoGps == ResultadoUbicacion.SinPermiso,
                        coordenadasGps = simCoords,
                        esUbicacionSimulada = true,
                        precisionMetros = null,
                        proveedorGps = null,
                        esUltimaConocida = false,
                        opticas = opticasData,
                        error = mensaje
                    )
                }
                return@launch
            }

            val ubicacion = resultadoGps.coordenadas
            _state.update {
                it.copy(
                    coordenadasGps = ubicacion,
                    esUbicacionSimulada = false,
                    precisionMetros = resultadoGps.precisionMetros,
                    proveedorGps = resultadoGps.proveedor,
                    esUltimaConocida = resultadoGps.esUltimaConocida,
                    sinPermisoUbicacion = false
                )
            }

            when (val resultado = opticasApi.getOpticasCercanas(ubicacion.lat, ubicacion.lng)) {
                is ResultadoRed.Exito -> {
                    _state.update {
                        it.copy(
                            cargando = false,
                            opticas = resultado.data,
                            mostrandoCache = false,
                            error = null
                        )
                    }
                }
                else -> {
                    procesarErrorRed(resultado)
                }
            }
        }
    }

    fun simularUbicacionReal(lat: Double = 20.0825, lng: Double = -98.3622) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    cargando = true,
                    error = null,
                    sinPermisoUbicacion = false
                )
            }

            val ubicacion = Coordenadas(lat, lng)
            when (val resultado = opticasApi.getOpticasCercanas(ubicacion.lat, ubicacion.lng)) {
                is ResultadoRed.Exito -> {
                    _state.update {
                        it.copy(
                            cargando = false,
                            opticas = resultado.data,
                            coordenadasGps = ubicacion,
                            esUbicacionSimulada = true,
                            precisionMetros = null,
                            proveedorGps = null,
                            esUltimaConocida = false,
                            mostrandoCache = false,
                            sinPermisoUbicacion = false,
                            error = null
                        )
                    }
                }
                else -> {
                    procesarErrorRed(resultado)
                }
            }
        }
    }

    fun buscarPorCiudadOZip(query: String) {
        val busqueda = query.ifBlank { _state.value.busquedaTexto }
        if (busqueda.isBlank()) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    cargando = true,
                    error = null,
                    sinPermisoUbicacion = false,
                    busquedaTexto = busqueda
                )
            }

            when (val resultado = opticasApi.getOpticasPorCiudadOZip(busqueda)) {
                is ResultadoRed.Exito -> {
                    _state.update {
                        it.copy(
                            cargando = false,
                            opticas = resultado.data,
                            coordenadasGps = null,
                            mostrandoCache = false,
                            sinPermisoUbicacion = false,
                            error = null
                        )
                    }
                }
                else -> {
                    procesarErrorRed(resultado)
                }
            }
        }
    }

    fun actualizarBusquedaTexto(texto: String) {
        _state.update { it.copy(busquedaTexto = texto) }
    }

    private suspend fun procesarErrorRed(resultado: ResultadoRed<*>) {
        val diskCache = DiskOpticasCache.obtener()
        val salud = opticasApi.verificarSalud()

        val mensajeError = when {
            salud is ResultadoRed.SinConexion || resultado is ResultadoRed.SinConexion ->
                "Sin conexión a internet. Revisa tu red."
            salud is ResultadoRed.ErrorServidor ->
                "El servidor de ópticas está temporalmente caído."
            resultado is ResultadoRed.Timeout ->
                "La consulta tardó demasiado en responder."
            else ->
                resultado.aMensajeUsuario()
        }

        if (!diskCache.isNullOrEmpty()) {
            _state.update {
                it.copy(
                    cargando = false,
                    opticas = diskCache,
                    mostrandoCache = true,
                    error = "$mensajeError - Mostrando datos guardados."
                )
            }
        } else {
            _state.update {
                it.copy(
                    cargando = false,
                    opticas = emptyList(),
                    mostrandoCache = false,
                    error = mensajeError
                )
            }
        }
    }
}
