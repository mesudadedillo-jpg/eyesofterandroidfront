package com.example.eyesofterapp.domain.model

import kotlinx.serialization.Serializable

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
    val verificada: Boolean = false
)
