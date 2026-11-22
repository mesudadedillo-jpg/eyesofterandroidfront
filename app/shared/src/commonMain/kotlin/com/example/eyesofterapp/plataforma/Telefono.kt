package com.example.eyesofterapp.plataforma

import androidx.compose.runtime.staticCompositionLocalOf

/** Una foto o PDF elegido por el usuario, ya comprimido si era foto (M-RC-05). */
class ArchivoElegido(val bytes: ByteArray, val nombre: String, val contentType: String)

/**
 * Lo que la app necesita del teléfono y que cada plataforma resuelve a su modo.
 * Android lo implementa en androidApp (TelefonoAndroid); en escritorio y web es
 * null y las pantallas ocultan esos botones.
 */
interface Telefono {
    /** Abre la cámara (M-RI-04). null si el usuario canceló. */
    suspend fun tomarFoto(): ArchivoElegido?

    /** Abre la galería o los documentos para elegir una foto o un PDF. */
    suspend fun elegirArchivo(): ArchivoElegido?

    /** Vibración para la pausa 20-20-20 (M-RI-06). Patrón: espera, vibra, espera, vibra... en ms. */
    fun vibrar(patron: LongArray)

    /** Sonido corto de aviso (M-RI-06). */
    fun sonar()

    /** Evita que la pantalla se apague mientras corre un ejercicio. */
    fun mantenerPantallaEncendida(activa: Boolean)
}

val LocalTelefono = staticCompositionLocalOf<Telefono?> { null }
