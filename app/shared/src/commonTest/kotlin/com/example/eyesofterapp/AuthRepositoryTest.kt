package com.example.eyesofterapp

import com.example.eyesofterapp.data.model.LoginResponseDto
import com.example.eyesofterapp.data.model.UserDto
import com.example.eyesofterapp.data.network.AuthApi
import com.example.eyesofterapp.data.repository.AuthRepositoryImpl
import com.example.eyesofterapp.domain.model.InvalidCredentialsException
import com.example.eyesofterapp.domain.model.Role
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryTest {

    @Test
    fun testSuccessfulLoginReturnsSession() = runTest {
        val mockEngine = MockEngine { request ->
            val responseHeaders = headersOf(HttpHeaders.ContentType, "application/json")
            val mockResponseBody = Json.encodeToString(
                LoginResponseDto(
                    token = "test-token-123",
                    user = UserDto(
                        username = "doctor",
                        fullName = "Dr. Médico Especialista",
                        role = "DOCTOR"
                    )
                )
            )
            respond(mockResponseBody, HttpStatusCode.OK, responseHeaders)
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val repository = AuthRepositoryImpl(AuthApi(client))
        val result = repository.login("doctor", "123456")

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals("test-token-123", session.token)
        assertEquals("doctor", session.user.username)
        assertEquals(Role.DOCTOR, session.user.role)
    }

    @Test
    fun testUnauthorizedLoginReturnsFailure() = runTest {
        val mockEngine = MockEngine {
            respond("", HttpStatusCode.Unauthorized)
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val repository = AuthRepositoryImpl(AuthApi(client))
        val result = repository.login("user", "wrong_pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidCredentialsException)
    }
}
