package com.example.eyesofterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eyesofterapp.data.cache.DiskOpticasCache
import com.example.eyesofterapp.data.network.OpticasApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.location.LocationProvider
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
        viewModelScope.launch {
            _state.update {
                it.copy(
                    cargando = true,
                    error = null,
                    sinPermisoUbicacion = false
                )
            }

            val ubicacion = locationProvider?.obtenerUbicacion()
            if (ubicacion == null) {
                // Si el permiso fue denegado o no hay GPS, cargar simulación por defecto (Tulancingo)
                // para que muestre distancias reales en la demostración
                val simCoords = com.example.eyesofterapp.domain.model.Coordenadas(20.0825, -98.3622)
                val resultadoSim = opticasApi.getOpticasCercanas(simCoords.lat, simCoords.lng)
                val opticasData = if (resultadoSim is ResultadoRed.Exito) resultadoSim.data else emptyList()

                _state.update {
                    it.copy(
                        cargando = false,
                        sinPermisoUbicacion = true,
                        coordenadasGps = simCoords,
                        esUbicacionSimulada = true,
                        opticas = opticasData,
                        error = "Permiso GPS denegado o sensor inaccesible. Puedes pedir el permiso de nuevo o usar la ubicación simulada."
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    coordenadasGps = ubicacion,
                    esUbicacionSimulada = false,
                    sinPermisoUbicacion = false
                )
            }

            when (val resultado = opticasApi.getOpticasCercanas(ubicacion.lat, ubicacion.lng)) {
                is ResultadoRed.Exito -> {
                    _state.update {
                        it.copy(
                            cargando = false,
                            opticas = resultado.data,
                            coordenadasGps = ubicacion,
                            esUbicacionSimulada = false,
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

    fun simularUbicacionReal(lat: Double = 20.0825, lng: Double = -98.3622) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    cargando = true,
                    error = null,
                    sinPermisoUbicacion = false
                )
            }

            val ubicacion = com.example.eyesofterapp.domain.model.Coordenadas(lat, lng)
            when (val resultado = opticasApi.getOpticasCercanas(ubicacion.lat, ubicacion.lng)) {
                is ResultadoRed.Exito -> {
                    _state.update {
                        it.copy(
                            cargando = false,
                            opticas = resultado.data,
                            coordenadasGps = ubicacion,
                            esUbicacionSimulada = true,
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
