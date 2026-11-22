package com.example.eyesofterapp.domain.repository

import com.example.eyesofterapp.domain.model.Session

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Session>
    /** [codigo]: el de la app autenticadora si la cuenta tiene verificación en dos pasos. */
    suspend fun loginConTimeout(username: String, password: String, timeoutMs: Long, codigo: String? = null): Result<Session>
}
