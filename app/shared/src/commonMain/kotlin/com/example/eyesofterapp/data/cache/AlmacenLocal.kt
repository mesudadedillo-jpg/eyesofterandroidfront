package com.example.eyesofterapp.data.cache

import com.example.eyesofterapp.data.model.TamizajeDto
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/** Un comprobante que no se pudo enviar por falta de conexión (M-RFNN-02). */
@Serializable
data class ComprobantePendiente(
    val id: String,
    val tamizajeId: String,
    val alumno: String,
    val tipo: String,
    val nombre: String,
    val contentType: String,
    val base64: String,
    val guardadoEn: String,
) {
    fun bytes(): ByteArray = Base64.decode(base64)
}

/**
 * Lo que la app guarda en el teléfono para funcionar sin conexión:
 *  - El último expediente descargado (M-RFNN-01) y el QR del pase (M-RFNN-03).
 *  - Los comprobantes por enviar (M-RFNN-02).
 *
 * Todo va por cuenta de usuario y se BORRA al cerrar sesión: si el teléfono es compartido,
 * el siguiente usuario no ve datos de otra familia.
 */
class AlmacenLocal(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun guardarTamizajes(usuarioId: String, lista: List<TamizajeDto>, cuando: String) {
        runCatching {
            settings.putString(clave(usuarioId, "tamizajes"), json.encodeToString(ListSerializer(TamizajeDto.serializer()), lista))
            settings.putString(clave(usuarioId, "tamizajes_fecha"), cuando)
        }
    }

    /** La lista guardada y cuándo se descargó. null si nunca se descargó. */
    fun leerTamizajes(usuarioId: String): Pair<List<TamizajeDto>, String>? = runCatching {
        val texto = settings.getStringOrNull(clave(usuarioId, "tamizajes")) ?: return null
        json.decodeFromString(ListSerializer(TamizajeDto.serializer()), texto) to settings.getString(clave(usuarioId, "tamizajes_fecha"), "")
    }.getOrNull()

    fun guardarPase(usuarioId: String, codigo: String, png: ByteArray) {
        runCatching { settings.putString(clave(usuarioId, "pase_$codigo"), Base64.encode(png)) }
    }

    fun leerPase(usuarioId: String, codigo: String): ByteArray? =
        settings.getStringOrNull(clave(usuarioId, "pase_$codigo"))?.let { runCatching { Base64.decode(it) }.getOrNull() }

    fun cola(usuarioId: String): List<ComprobantePendiente> = runCatching {
        settings.getStringOrNull(clave(usuarioId, "cola"))?.let { json.decodeFromString(ListSerializer(ComprobantePendiente.serializer()), it) }
    }.getOrNull().orEmpty()

    /** false si ya no cabe (máximo 3 archivos u 8 MB en espera). */
    fun encolar(usuarioId: String, p: ComprobantePendiente): Boolean {
        val actual = cola(usuarioId)
        if (actual.size >= 3 || actual.sumOf { it.base64.length } + p.base64.length > 11_000_000) return false
        return guardarCola(usuarioId, actual + p)
    }

    fun quitar(usuarioId: String, id: String) { guardarCola(usuarioId, cola(usuarioId).filterNot { it.id == id }) }

    private fun guardarCola(usuarioId: String, lista: List<ComprobantePendiente>) = runCatching {
        settings.putString(clave(usuarioId, "cola"), json.encodeToString(ListSerializer(ComprobantePendiente.serializer()), lista))
    }.isSuccess

    /** Al cerrar sesión. */
    fun borrar(usuarioId: String) {
        val prefijo = "u_${usuarioId}_"
        settings.keys.filter { it.startsWith(prefijo) }.forEach { settings.remove(it) }
    }

    private fun clave(usuarioId: String, nombre: String) = "u_${usuarioId}_$nombre"
}
