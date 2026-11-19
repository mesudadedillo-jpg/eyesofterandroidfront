package com.example.eyesofterapp.data.session

import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.Session
import com.example.eyesofterapp.domain.model.User
import com.russhwolf.settings.Settings

/**
 * Guarda la sesión en el teléfono (SharedPreferences en Android, NSUserDefaults
 * en iOS, localStorage en web) para no pedir usuario y contraseña cada vez que
 * se abre la app. El token dura 30 días; si el servidor lo rechaza (401), la app
 * borra la sesión y vuelve al login.
 */
class SesionGuardada(private val settings: Settings = Settings()) {

    fun leer(): Session? {
        val token = settings.getStringOrNull(TOKEN) ?: return null
        val usuario = settings.getStringOrNull(USUARIO) ?: return null
        return Session(
            token = token,
            user = User(
                username = usuario,
                fullName = settings.getString(NOMBRE, usuario),
                role = Role.fromString(settings.getString(ROL, ""))
            )
        )
    }

    fun guardar(sesion: Session) {
        settings.putString(TOKEN, sesion.token)
        settings.putString(USUARIO, sesion.user.username)
        settings.putString(NOMBRE, sesion.user.fullName)
        settings.putString(ROL, sesion.user.role.name)
    }

    fun borrar() {
        listOf(TOKEN, USUARIO, NOMBRE, ROL).forEach { settings.remove(it) }
    }

    private companion object {
        const val TOKEN = "sesion_token"
        const val USUARIO = "sesion_usuario"
        const val NOMBRE = "sesion_nombre"
        const val ROL = "sesion_rol"
    }
}
