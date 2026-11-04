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

@Serializable
data class UserDto(
    val username: String,
    val fullName: String,
    val role: String
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
        role = Role.fromString(user.role)
    )
)