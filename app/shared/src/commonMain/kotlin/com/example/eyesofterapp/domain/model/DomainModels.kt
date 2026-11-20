package com.example.eyesofterapp.domain.model


enum class Role {
    ADMINISTRADOR, SUPERVISOR, CLIENTE, DOCTOR, ALUMNO, DESCONOCIDO;

    companion object {
        fun fromString(value: String): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DESCONOCIDO
    }
}


data class User(
    /** Correo de la cuenta (las mismas cuentas del portal web). */
    val username: String,
    val fullName: String,
    val role: Role,
    val id: String = "",
    /** Tutor que aún no acepta el aviso de privacidad vigente (C-RC-08). */
    val requiereConsentimiento: Boolean = false,
)


data class Session(
    val token: String,
    val user: User
)

class InvalidCredentialsException : Exception("Correo o contraseña incorrectos")

/** El servidor rechazó la petición con un motivo que se le muestra al usuario tal cual (p. ej. cuenta bloqueada). */
class RechazoDelServidorException(mensaje: String) : Exception(mensaje)
