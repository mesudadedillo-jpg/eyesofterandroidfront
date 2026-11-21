package com.example.eyesofterapp.data.network

import com.example.eyesofterapp.data.model.ErrorDto
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
                    // 401 también puede ser "falta el código de dos pasos": el servidor lo dice en "codigo".
                    val error = try { response.body<ErrorDto>() } catch (_: Exception) { ErrorDto() }
                    if (error.codigo == "REQUIERE_2FA" || error.codigo == "CODIGO_2FA_INVALIDO")
                        ResultadoRed.ErrorCliente(401, error.error, error.codigo)
                    else ResultadoRed.ErrorCliente(401, "Correo o contraseña incorrectos")
                }
                response.status.value in 400..499 -> {
                    // 429 = cuenta bloqueada por intentos fallidos: el servidor dice cuántos minutos faltan.
                    val mensaje = try { response.body<ErrorDto>().error } catch (_: Exception) { "" }
                    ResultadoRed.ErrorCliente(response.status.value, mensaje.ifBlank { "Datos de solicitud inválidos" })
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
