package com.example.eyesofterapp.domain.usecase

import com.example.eyesofterapp.domain.model.Session
import com.example.eyesofterapp.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(username: String, password: String, timeoutMs: Long = 3000L): Result<Session> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Completa usuario y contraseña"))
        }
        return repository.loginConTimeout(username.trim(), password, timeoutMs)
    }
}
