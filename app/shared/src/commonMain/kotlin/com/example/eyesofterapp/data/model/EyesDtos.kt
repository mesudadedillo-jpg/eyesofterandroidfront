package com.example.eyesofterapp.data.model

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// CONTRATO CON EL SERVIDOR (copia del lado de la app) — docs/API.md §5-§11.
// El servidor tiene la suya en eyesofter-backend/src/main/kotlin/.../model/.
// Si cambias un campo aquí, cámbialo también allá y en las dos copias de API.md.
// ---------------------------------------------------------------------------

@Serializable
data class NotificacionDto(
    val id: String,
    /** RESULTADO_TAMIZAJE, ALERTA_ENFERMERIA, AVISO, COMPROBANTE_RECIBIDO, COMPROBANTE_REVISADO. */
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    /** Siempre trae "ruta": tamizaje/<id>, seguimiento, comprobantes o notificacion/<id>. */
    val datos: Map<String, String> = emptyMap(),
    val creadaEn: String,
    val leidaEn: String? = null,
) {
    val leida: Boolean get() = leidaEn != null
}

@Serializable
data class BandejaDto(
    val noLeidas: Int = 0,
    val notificaciones: List<NotificacionDto> = emptyList(),
)

@Serializable
data class TamizajeDto(
    val id: String,
    val folio: String,
    val alumnoNombre: String,
    val grado: String = "",
    val grupo: String = "",
    val fecha: String,
    /** VERDE, AMARILLO o ROJO. */
    val semaforo: String,
    val semaforoTexto: String = "",
    val observaciones: String = "",
)

@Serializable
data class ComprobanteDto(
    val id: String,
    val tamizajeId: String,
    val tutor: String,
    /** RECETA o COMPROBANTE_LENTES. */
    val tipo: String,
    val contentType: String,
    val tamanoBytes: Int,
    val subidoEn: String,
    /** PENDIENTE, APROBADO o RECHAZADO. */
    val estado: String,
    val revisadoEn: String? = null,
    val motivoRechazo: String? = null,
)

@Serializable
data class RevisionComprobanteDto(val aprobado: Boolean, val motivo: String? = null)

@Serializable
data class AvisoEntradaDto(
    val titulo: String,
    val mensaje: String,
    val usernames: List<String> = emptyList(),
    val rol: String? = null,
)

@Serializable
data class AvisoEnviadoDto(val enviadas: Int, val noEncontrados: List<String> = emptyList())

@Serializable
data class SeguimientoDto(
    val notificacionId: String,
    val tutor: String,
    val alumno: String,
    val tamizajeId: String,
    val creadaEn: String,
    val leidaEn: String? = null,
    val recordatoriosEnviados: Int = 0,
    val entregadaPor: List<String> = emptyList(),
)

/** Cuerpo de error del servidor: {"error": "mensaje en español"}. */
@Serializable
data class ErrorDto(val error: String = "")

/** "2026-11-19T15:04:05.123Z" → "19 nov 2026". Sin librería de fechas, a propósito. */
fun fechaLegible(iso: String): String {
    val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    val partes = iso.take(10).split("-")
    if (partes.size != 3) return iso
    val mes = partes[1].toIntOrNull()?.let { meses.getOrNull(it - 1) } ?: return iso
    return "${partes[2].trimStart('0')} $mes ${partes[0]}"
}
