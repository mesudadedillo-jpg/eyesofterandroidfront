package com.example.eyesofterapp

import com.example.eyesofterapp.domain.model.distanciaHaversineKm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Comprueba la fórmula de Haversine (antes usaba acos y daba ~20 000 km para puntos cercanos). */
class DistanciaTest {

    @Test
    fun mismoPuntoEstaACeroKm() {
        assertEquals(0.0, distanciaHaversineKm(20.0825, -98.3622, 20.0825, -98.3622), 0.0001)
    }

    @Test
    fun tulancingoACdmxSonUnos108Km() {
        val km = distanciaHaversineKm(20.0825, -98.3622, 19.432608, -99.133209)
        assertTrue(km in 100.0..115.0, "Distancia calculada: $km")
    }

    @Test
    fun lugaresCercanosQuedanCerca() {
        // Dos ópticas del centro de Tulancingo, a pocas cuadras.
        val km = distanciaHaversineKm(20.0825, -98.3622, 20.0841, -98.3601)
        assertTrue(km < 1.0, "Distancia calculada: $km")
    }
}
