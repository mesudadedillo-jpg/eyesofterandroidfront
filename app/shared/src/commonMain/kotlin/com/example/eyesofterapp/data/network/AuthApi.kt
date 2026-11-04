package com.example.eyesofterapp.data.network

import com.example.eyesofterapp.data.model.LoginRequestDto
import com.example.eyesofterapp.data.model.LoginResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.TimeSource

class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequestDto, timeoutMs: Long = 3000L): ResultadoRed<LoginResponseDto> {
        val baseUrl = BASE_URL
        val timeMark = TimeSource.Monotonic.markNow()
        println("[Logcat/Client] Iniciando intento de login hacia $baseUrl (timeout: ${timeoutMs}ms)...")

        return try {
            val response = client.post("/api/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
                timeout {
                    requestTimeoutMillis = timeoutMs
                    connectTimeoutMillis = timeoutMs
                }
            }

            val duration = timeMark.elapsedNow().inWholeMilliseconds
            println("[Logcat/Client] Login respuesta recibida en ${duration}ms | HTTP Status: ${response.status.value}")

            when {
                response.status.isSuccess() -> {
                    ResultadoRed.Exito(response.body<LoginResponseDto>())
                }
                response.status == HttpStatusCode.Unauthorized -> {
                    ResultadoRed.ErrorCliente(401, "Usuario o contraseña incorrectos")
                }
                response.status.value in 400..499 -> {
                    ResultadoRed.ErrorCliente(response.status.value, "Datos de solicitud inválidos")
                }
                else -> {
                    ResultadoRed.ErrorServidor(response.status.value, "Error en el servidor de autenticación")
                }
            }
        } catch (e: HttpRequestTimeoutException) {
            val duration = timeMark.elapsedNow().inWholeMilliseconds
            println("[Logcat/Client] Login FALLÓ por Timeout tras ${duration}ms en $baseUrl")
            ResultadoRed.Timeout(baseUrl)
        } catch (e: Exception) {
            val duration = timeMark.elapsedNow().inWholeMilliseconds
            val msg = e.message ?: ""
            println("[Logcat/Client] Login FALLÓ tras ${duration}ms en $baseUrl | Excepcion: ${e::class.simpleName} ($msg)")

            if (msg.contains("Connect", ignoreCase = true) ||
                msg.contains("refused", ignoreCase = true) ||
                msg.contains("Host", ignoreCase = true) ||
                msg.contains("Unreachable", ignoreCase = true)
            ) {
                ResultadoRed.ServidorInalcanzable(baseUrl)
            } else if (msg.contains("Network", ignoreCase = true) || msg.contains("offline", ignoreCase = true)) {
                ResultadoRed.SinConexion
            } else {
                ResultadoRed.Desconocido(e)
            }
        }
    }
}
