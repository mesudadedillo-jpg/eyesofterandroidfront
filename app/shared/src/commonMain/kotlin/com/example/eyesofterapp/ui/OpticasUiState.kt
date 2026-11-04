package com.example.eyesofterapp.ui

import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.Optica

data class OpticasUiState(
    val cargando: Boolean = false,
    val opticas: List<Optica> = emptyList(),
    val coordenadasGps: Coordenadas? = null,
    val esUbicacionSimulada: Boolean = false,
    /** Precisión de la lectura en metros (null si es simulada o desconocida). */
    val precisionMetros: Float? = null,
    /** Proveedor que dio la lectura: gps, network, fused. */
    val proveedorGps: String? = null,
    /** true si no hubo lectura nueva y se mostró la última posición conocida. */
    val esUltimaConocida: Boolean = false,
    val mostrandoCache: Boolean = false,
    val sinPermisoUbicacion: Boolean = false,
    val error: String? = null,
    val busquedaTexto: String = "",
    /** Nombre del lugar buscado por texto (ciudad, C.P.); null si la ubicación viene del GPS. */
    val lugarBuscado: String? = null,
    /** Radio en km en que se buscaron ópticas en OpenStreetMap (3, 10 o 30). */
    val radioBusquedaKm: Int? = null
)
