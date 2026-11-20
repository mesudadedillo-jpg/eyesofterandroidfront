package com.example.eyesofterapp.composeApp.navigation

/**
 * Pantallas de la app. La navegación es una pila: se agrega al abrir y se
 * quita con "Atrás" (el botón del teléfono o el de la barra).
 */
sealed interface Screen {
    /** Panel del rol (tutor, enfermería, dirección, especialista o alumno). */
    data object Inicio : Screen
    /** Directorio de clínicas y ópticas. soloAsociadas: abrir ya filtrado. */
    data class Opticas(val soloAsociadas: Boolean = false) : Screen
    data object Notificaciones : Screen
    data class Tamizaje(val id: String) : Screen
    data object Comprobantes : Screen
    data object Seguimiento : Screen
    data object NuevoAviso : Screen
    data object PausaVisual : Screen
    /** M-RFN-09: elegir la clínica a la que irá el alumno. */
    data class ElegirDestino(val tamizajeId: String) : Screen
    data class SolicitarCita(val tamizajeId: String) : Screen
    /** M-RS-01: guía rápida dentro de la app. */
    data object Guia : Screen
    data object Reportar : Screen
}

/** Convierte datos.ruta de una notificación en la pantalla a la que lleva (M-RC-02). */
fun rutaAScreen(ruta: String?): Screen? = when {
    ruta == null -> null
    ruta.startsWith("tamizaje/") -> Screen.Tamizaje(ruta.removePrefix("tamizaje/"))
    ruta == "seguimiento" -> Screen.Seguimiento
    ruta == "comprobantes" -> Screen.Comprobantes
    ruta.startsWith("notificacion/") -> Screen.Notificaciones
    else -> null
}
