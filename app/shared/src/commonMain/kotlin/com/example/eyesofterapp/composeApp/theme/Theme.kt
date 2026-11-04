package com.example.eyesofterapp.composeApp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta de la app. Si hay que cambiar colores, se cambia solo aqui. */
private val EyeSofterColors = lightColorScheme(
    primary = Color(0xFF0B4DA2),          // azul del logo (aproximado)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6FB),
    onPrimaryContainer = Color(0xFF0A1F5C),
    background = Color(0xFFF5F8FD),
    surface = Color.White,
    error = Color(0xFFB3261E)
)

@Composable
fun EyeSofterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EyeSofterColors,
        content = content
    )
}