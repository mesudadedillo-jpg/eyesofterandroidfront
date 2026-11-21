package com.example.eyesofterapp.data.network

sealed interface ResultadoRed<out T> {
    data class Exito<out T>(val data: T) : ResultadoRed<T>
    data object SinConexion : ResultadoRed<Nothing>
    data class ServidorInalcanzable(val url: String) : ResultadoRed<Nothing>
    data class Timeout(val url: String) : ResultadoRed<Nothing>
    data class ErrorServidor(val codigo: Int, val mensaje: String = "Error en el servidor ($codigo)") : ResultadoRed<Nothing>
    /** [clave]: el "codigo" que manda el servidor (p. ej. REQUIERE_2FA) para reaccionar sin leer el texto. */
    data class ErrorCliente(val codigo: Int, val mensaje: String = "Credenciales o solicitud inválida ($codigo)", val clave: String? = null) : ResultadoRed<Nothing>
    data class Desconocido(val excepcion: Throwable) : ResultadoRed<Nothing>

    fun aMensajeUsuario(): String = when (this) {
        is Exito -> ""
        is SinConexion -> "Sin conexión a internet. Revisa tu red."
        is ServidorInalcanzable -> "No se puede conectar al servidor ($url). Revisa que esté encendido."
        is Timeout -> "El servidor tardó demasiado en responder ($url)."
        is ErrorServidor -> "Error interno en el servidor ($codigo)."
        is ErrorCliente -> mensaje
        is Desconocido -> "Error inesperado: ${excepcion.message ?: "Sin detalles"}"
    }
}
