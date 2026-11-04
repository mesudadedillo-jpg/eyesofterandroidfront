package com.example.eyesofterapp.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class Coordenadas(
    val lat: Double,
    val lng: Double
)

/**
 * Fórmula de Haversine: distancia en kilómetros entre dos puntos dados en
 * grados de latitud y longitud, sobre una Tierra esférica de 6371 km de radio.
 *
 *  a = sen²(Δlat/2) + cos(lat1)·cos(lat2)·sen²(Δlng/2)
 *  c = 2·arcsen(√a)
 *  distancia = radio · c
 *
 * Es la misma fórmula del servidor (eyesofter-backend, data/Distancia.kt).
 * Antes OpticasApi usaba arccos en el paso de "c": eso daba ~20 000 km para
 * puntos cercanos y ordenaba las ópticas al revés.
 */
fun distanciaHaversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val aRad = PI / 180.0
    val dLat = (lat2 - lat1) * aRad
    val dLng = (lng2 - lng1) * aRad
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * aRad) * cos(lat2 * aRad) * sin(dLng / 2) * sin(dLng / 2)
    // min(1.0, ...) evita que un redondeo deje √a un poquito arriba de 1.
    return 6371.0 * 2 * asin(min(1.0, sqrt(a)))
}
