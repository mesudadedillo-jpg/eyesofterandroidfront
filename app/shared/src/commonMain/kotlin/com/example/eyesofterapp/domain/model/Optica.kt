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
    val verificada: Boolean = false,
    /** Optica, Consultorio, Oftalmologia... */
    val tipo: String = "",
    val horario: String? = null,
    /**
     * Beneficio del convenio entre la escuela de tus hijos y esta clínica (p. ej. "20 % en armazones").
     * Solo llega con la sesión de un tutor; null = sin convenio.
     */
    val convenio: String? = null,
)
