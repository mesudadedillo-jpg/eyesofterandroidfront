package com.example.eyesofterapp.data.model

import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.Session
import com.example.eyesofterapp.domain.model.User
import kotlinx.serialization.Serializable


@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

/** "username" es el correo de la cuenta del portal (docs/API.md §3). */
@Serializable
data class UserDto(
    val username: String,
    val fullName: String,
    val role: String,
    val id: String = "",
    val requiereConsentimiento: Boolean = false,
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val user: UserDto
)


fun LoginResponseDto.toDomain(): Session = Session(
    token = token,
    user = User(
        username = user.username,
        fullName = user.fullName,
        role = Role.fromString(user.role),
        id = user.id,
        requiereConsentimiento = user.requiereConsentimiento,
    )
)

/** GET /api/consentimiento (§4). */
@Serializable
data class ConsentimientoDto(
    val versionVigente: String,
    val aceptado: Boolean,
    val aceptadoEn: String? = null,
    val urlAviso: String = "",
    val urlTerminos: String = "",
)

@Serializable
data class AceptarConsentimientoDto(val version: String, val acepto: Boolean = true)

/** POST /api/reportes (§12). tipo: FALLA o DUDA. */
@Serializable
data class ReporteEntradaDto(val tipo: String, val mensaje: String, val correo: String? = null, val plataforma: String = "ANDROID")

@Serializable
data class RegistroDispositivoDto(val token: String, val plataforma: String)
