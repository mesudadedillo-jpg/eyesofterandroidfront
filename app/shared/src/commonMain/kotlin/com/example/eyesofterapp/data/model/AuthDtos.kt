package com.example.eyesofterapp.data.model

import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.Session
import com.example.eyesofterapp.domain.model.User
import kotlinx.serialization.Serializable


@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    /** Código de 6 dígitos de la app autenticadora (verificación en dos pasos). */
    val codigo: String? = null,
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

/** POST /api/recuperar/solicitar (§3.1): "cuenta" es el correo o el celular de 10 dígitos. */
@Serializable
data class SolicitarCodigoDto(val cuenta: String)

/** Respuesta de /api/recuperar/solicitar: la misma exista o no la cuenta. canal: "correo" o "sms". */
@Serializable
data class CodigoSolicitadoDto(val mensaje: String, val canal: String = "correo", val minutosVigencia: Int = 10)

/** POST /api/recuperar/restablecer (§3.1). */
@Serializable
data class RestablecerPasswordDto(val cuenta: String, val codigo: String, val password: String)
