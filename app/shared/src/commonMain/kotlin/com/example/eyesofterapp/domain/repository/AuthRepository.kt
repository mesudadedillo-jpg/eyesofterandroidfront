package com.example.eyesofterapp.domain.repository

import com.example.eyesofterapp.domain.model.Session

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Session>
    suspend fun loginConTimeout(username: String, password: String, timeoutMs: Long): Result<Session>
}
