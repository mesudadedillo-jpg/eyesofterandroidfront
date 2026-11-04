package com.example.eyesofterapp.composeApp.navigation

import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.User

sealed interface Screen {
    data object Login : Screen
    data class Admin(val user: User) : Screen
    data class Supervisor(val user: User) : Screen
    data class Cliente(val user: User) : Screen
    data class Doctor(val user: User) : Screen
    data class Alumno(val user: User) : Screen
    data class Desconocido(val user: User) : Screen
    data class Opticas(val user: User) : Screen
}

fun User.toScreen(): Screen = when (role) {
    Role.ADMINISTRADOR -> Screen.Admin(this)
    Role.SUPERVISOR -> Screen.Supervisor(this)
    Role.CLIENTE -> Screen.Cliente(this)
    Role.DOCTOR -> Screen.Doctor(this)
    Role.ALUMNO -> Screen.Alumno(this)
    Role.DESCONOCIDO -> Screen.Desconocido(this)
}
