package com.example.eyesofterapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Coordenadas(
    val lat: Double,
    val lng: Double
)
