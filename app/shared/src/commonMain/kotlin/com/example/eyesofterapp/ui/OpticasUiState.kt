package com.example.eyesofterapp.ui

import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.Optica

data class OpticasUiState(
    val cargando: Boolean = false,
    val opticas: List<Optica> = emptyList(),
    val coordenadasGps: Coordenadas? = null,
    val esUbicacionSimulada: Boolean = false,
    val mostrandoCache: Boolean = false,
    val sinPermisoUbicacion: Boolean = false,
    val error: String? = null,
    val busquedaTexto: String = ""
)
