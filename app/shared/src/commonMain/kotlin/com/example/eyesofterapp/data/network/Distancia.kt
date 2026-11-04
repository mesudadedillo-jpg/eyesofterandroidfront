package com.example.eyesofterapp.data.network

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Fórmula de Haversine: distancia en km entre dos puntos (grados). Igual que en el backend. */
fun distanciaKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val aRad = PI / 180.0
    val dLat = (lat2 - lat1) * aRad
    val dLng = (lng2 - lng1) * aRad
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * aRad) * cos(lat2 * aRad) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * 6371.0 * asin(min(1.0, sqrt(a)))
}
