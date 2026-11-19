package com.example.eyesofterapp.data.network

import com.example.eyesofterapp.data.model.AvisoEntradaDto
import com.example.eyesofterapp.data.model.AvisoEnviadoDto
import com.example.eyesofterapp.data.model.BandejaDto
import com.example.eyesofterapp.data.model.ComprobanteDto
import com.example.eyesofterapp.data.model.ErrorDto
import com.example.eyesofterapp.data.model.NotificacionDto
import com.example.eyesofterapp.data.model.RevisionComprobanteDto
import com.example.eyesofterapp.data.model.SeguimientoDto
import com.example.eyesofterapp.data.model.TamizajeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/**
 * Rutas del servidor que piden sesión (docs/API.md §5-§11). Todas mandan
 * "Authorization: Bearer <token>". Si el servidor responde 401 (token vencido),
 * se llama a [alVencerSesion] para que la app regrese al login.
 */
class EyesApi(
    private val client: HttpClient,
    private val token: () -> String?,
    private val alVencerSesion: () -> Unit,
) {
    suspend fun bandeja(): ResultadoRed<BandejaDto> = pedir { get("/api/notificaciones") { con() } }

    suspend fun marcarLeida(id: String): ResultadoRed<NotificacionDto> =
        pedir { post("/api/notificaciones/$id/leida") { con() } }

    suspend fun marcarTodasLeidas(): ResultadoRed<Unit> = pedirSinCuerpo { post("/api/notificaciones/leidas") { con() } }

    suspend fun tamizajes(): ResultadoRed<List<TamizajeDto>> = pedir { get("/api/tamizajes") { con() } }

    suspend fun tamizaje(id: String): ResultadoRed<TamizajeDto> = pedir { get("/api/tamizajes/$id") { con() } }

    suspend fun comprobantes(estado: String? = null): ResultadoRed<List<ComprobanteDto>> =
        pedir { get("/api/comprobantes") { con(); estado?.let { parameter("estado", it) } } }

    /** Sube la foto o PDF. Puede tardar con datos móviles: se da más tiempo que al resto. */
    suspend fun subirComprobante(
        tamizajeId: String,
        tipo: String,
        bytes: ByteArray,
        nombreArchivo: String,
        contentType: String,
    ): ResultadoRed<ComprobanteDto> = pedir {
        submitFormWithBinaryData("/api/comprobantes", formData {
            append("tamizajeId", tamizajeId)
            append("tipo", tipo)
            append("archivo", bytes, Headers.build {
                append(HttpHeaders.ContentType, contentType)
                append(HttpHeaders.ContentDisposition, "filename=\"$nombreArchivo\"")
            })
        }) {
            con()
            timeout { requestTimeoutMillis = 60_000 }
        }
    }

    suspend fun archivoComprobante(id: String): ResultadoRed<ByteArray> = try {
        val r = client.get("/api/comprobantes/$id/archivo") { con() }
        if (r.status.isSuccess()) ResultadoRed.Exito(r.readRawBytes()) else error(r)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        excepcion(e)
    }

    suspend fun revisarComprobante(id: String, aprobado: Boolean, motivo: String?): ResultadoRed<ComprobanteDto> =
        pedir {
            post("/api/comprobantes/$id/revision") {
                con(); contentType(ContentType.Application.Json); setBody(RevisionComprobanteDto(aprobado, motivo))
            }
        }

    suspend fun seguimiento(soloNoLeidas: Boolean = false): ResultadoRed<List<SeguimientoDto>> =
        pedir { get("/api/notificaciones/seguimiento") { con(); if (soloNoLeidas) parameter("soloNoLeidas", true) } }

    suspend fun enviarAviso(aviso: AvisoEntradaDto): ResultadoRed<AvisoEnviadoDto> =
        pedir { post("/api/notificaciones") { con(); contentType(ContentType.Application.Json); setBody(aviso) } }

    // ------------------------------------------------------------------

    private fun HttpRequestBuilder.con() {
        token()?.let { bearerAuth(it) }
    }

    private suspend inline fun <reified T> pedir(crossinline llamada: suspend HttpClient.() -> HttpResponse): ResultadoRed<T> =
        try {
            val r = client.llamada()
            if (r.status.isSuccess()) ResultadoRed.Exito(r.body<T>()) else error(r)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            excepcion(e)
        }

    private suspend inline fun pedirSinCuerpo(crossinline llamada: suspend HttpClient.() -> HttpResponse): ResultadoRed<Unit> =
        try {
            val r = client.llamada()
            if (r.status.isSuccess()) ResultadoRed.Exito(Unit) else error(r)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            excepcion(e)
        }

    private suspend fun error(r: HttpResponse): ResultadoRed<Nothing> {
        val mensaje = try { r.body<ErrorDto>().error } catch (_: Exception) { "" }
        return when (val codigo = r.status.value) {
            401 -> {
                alVencerSesion()
                ResultadoRed.ErrorCliente(401, "Tu sesión venció. Inicia sesión de nuevo.")
            }
            in 400..499 -> ResultadoRed.ErrorCliente(codigo, mensaje.ifBlank { "Solicitud inválida ($codigo)" })
            else -> ResultadoRed.ErrorServidor(codigo, mensaje.ifBlank { "Error en el servidor ($codigo)" })
        }
    }

    private fun excepcion(e: Exception): ResultadoRed<Nothing> = when {
        e is HttpRequestTimeoutException -> ResultadoRed.Timeout(BASE_URL)
        (e.message ?: "").let { m -> listOf("Connect", "Host", "Network", "refused", "Unreachable", "offline").any { m.contains(it, true) } } ->
            ResultadoRed.SinConexion
        else -> ResultadoRed.Desconocido(e)
    }
}
