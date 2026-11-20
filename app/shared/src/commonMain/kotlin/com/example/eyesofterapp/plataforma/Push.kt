package com.example.eyesofterapp.plataforma

/**
 * Notificaciones push del teléfono (M-RI-02). Android la implementa con Firebase Cloud
 * Messaging; si el proyecto no tiene google-services.json o el usuario no dio permiso,
 * [token] devuelve null y la app sigue funcionando con la bandeja y el correo (M-RFNN-04).
 */
interface Push {
    /** ANDROID o IOS. */
    val plataforma: String

    /** Pide permiso si hace falta y devuelve el token de FCM, o null. */
    suspend fun token(): String?
}
