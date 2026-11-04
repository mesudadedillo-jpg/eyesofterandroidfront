package com.example.eyesofterapp.location

import com.example.eyesofterapp.domain.model.Coordenadas

interface LocationProvider {
    suspend fun obtenerUbicacion(): Coordenadas?
}
