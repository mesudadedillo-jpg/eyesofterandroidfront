package com.example.eyesofterapp.domain.model


enum class Role {
    ADMINISTRADOR, SUPERVISOR, CLIENTE, DOCTOR, ALUMNO, DESCONOCIDO;

    companion object {
        fun fromString(value: String): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DESCONOCIDO
    }
}


data class User(
    val username: String,
    val fullName: String,
    val role: Role
)


data class Session(
    val token: String,
    val user: User
)

class InvalidCredentialsException : Exception("Usuario o contraseña incorrectos")