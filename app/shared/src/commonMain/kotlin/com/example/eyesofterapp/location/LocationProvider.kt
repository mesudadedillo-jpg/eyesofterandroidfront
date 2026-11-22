package com.example.eyesofterapp.location

import com.example.eyesofterapp.domain.model.Coordenadas

/**
 * Resultado de pedir la ubicación al sensor.
 * Antes se devolvía Coordenadas? y, cuando el GPS fallaba, se regresaban
 * coordenadas fijas de Tulancingo que la UI mostraba como "GPS real".
 * Ahora cada caso se distingue para poder mostrar el mensaje correcto.
 */
sealed interface ResultadoUbicacion {
    data class Exito(
        val coordenadas: Coordenadas,
        /** Radio de precisión en metros reportado por el sensor (null si no se conoce). */
        val precisionMetros: Float?,
        /** Proveedor que dio la lectura: gps, network, fused... */
        val proveedor: String,
        /** true si no hubo lectura nueva y se usó la última posición guardada por el sistema. */
        val esUltimaConocida: Boolean
    ) : ResultadoUbicacion

    /** El usuario negó el permiso de ubicación. */
    data object SinPermiso : ResultadoUbicacion

    /** El permiso está concedido pero la ubicación del teléfono está apagada. */
    data object UbicacionApagada : ResultadoUbicacion

    /** Todo está activo pero el sensor no dio ninguna lectura a tiempo (interiores, sin señal). */
    data object SinSenal : ResultadoUbicacion
}

interface LocationProvider {
    suspend fun obtenerUbicacion(): ResultadoUbicacion
}
