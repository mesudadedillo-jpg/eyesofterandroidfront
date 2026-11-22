package com.example.eyesofterapp.domain.model

import kotlinx.serialization.Serializable

/** Óptica que viene del servidor EYESOFTER (catálogo propio, asociadas). */
const val FUENTE_EYESOFTER = "eyesofter"

/** Óptica encontrada en OpenStreetMap (datos públicos y gratuitos). */
const val FUENTE_OSM = "osm"

@Serializable
data class Optica(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val ciudad: String = "",
    val codigoPostal: String = "",
    val lat: Double,
    val lng: Double,
    val distanciaKm: Double = 0.0,
    val telefono: String = "7751234567",
    val verificada: Boolean = false,
    // ---- Campos opcionales (el servidor no los manda; vienen de OpenStreetMap) ----
    /** Horario tal como lo registra OpenStreetMap (opening_hours). Vacío si no se conoce. */
    val horario: String = "",
    val sitioWeb: String = "",
    /** De dónde salió el dato: FUENTE_EYESOFTER o FUENTE_OSM. */
    val fuente: String = FUENTE_EYESOFTER
)
