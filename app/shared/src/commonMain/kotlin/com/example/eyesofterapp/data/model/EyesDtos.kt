package com.example.eyesofterapp.data.model

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// CONTRATO CON EL SERVIDOR (copia del lado de la app) — docs/API.md §5-§13.
// El servidor tiene la suya en eyesofter-backend/src/main/kotlin/.../model/.
// Si cambias un campo aquí, cámbialo también allá y en las dos copias de API.md.
// ---------------------------------------------------------------------------

@Serializable
data class NotificacionDto(
    val id: String,
    /** RESULTADO_TAMIZAJE, RECORDATORIO_CASO, COMPROBANTE_REVISADO, CASO_ACTUALIZADO, AVISO, ALERTA_ENFERMERIA, COMPROBANTE_RECIBIDO, CITA. */
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
    val alumnoId: Int = 0,
    val campana: String? = null,
    /** Seguimiento (solo amarillo o rojo). */
    val caso: CasoDto? = null,
)

@Serializable
data class CasoDto(
    val id: Int,
    val folio: String,
    /** ABIERTO, EN_SEGUIMIENTO o CERRADO. */
    val estado: String,
    val destinoDefinido: Boolean = false,
    val clinicaId: Int? = null,
    val clinicaNombre: String? = null,
    val clinicaAsociada: Boolean = false,
    val pase: PaseDto? = null,
    val fechaAtencion: String? = null,
    val comprobanteAprobado: Boolean = false,
    val citas: List<CitaDto> = emptyList(),
    val eventos: List<EventoDto> = emptyList(),
) {
    val cerrado: Boolean get() = estado == "CERRADO"
}

@Serializable
data class PaseDto(
    val codigo: String,
    val codigoLegible: String = codigo,
    /** VIGENTE, USADO, CANCELADO o EXPIRADO. */
    val estado: String,
    val creadoEn: String,
    val expiraEn: String,
    val usadoEn: String? = null,
    val clinicaId: Int,
    val clinicaNombre: String,
    val clinicaDireccion: String = "",
    val clinicaTelefono: String = "",
    val urlValidacion: String = "",
) {
    val vigente: Boolean get() = estado == "VIGENTE"
}

@Serializable
data class CitaDto(
    val id: Int,
    val clinicaNombre: String? = null,
    val fechaPreferida: String,
    val horario: String? = null,
    val comentario: String? = null,
    /** SOLICITADA, CONFIRMADA, ATENDIDA o CANCELADA. */
    val estado: String,
    val creadaEn: String = "",
)

@Serializable
data class EventoDto(val fecha: String, val tipo: String, val descripcion: String)

@Serializable
data class DestinoEntradaDto(val clinicaId: Int? = null, val nombreManual: String? = null)

@Serializable
data class DestinoDto(val conPase: Boolean, val mensaje: String, val tamizaje: TamizajeDto)

@Serializable
data class CitaEntradaDto(val clinicaId: Int, val fechaPreferida: String, val horario: String? = null, val comentario: String? = null)

@Serializable
data class CitaCreadaDto(val id: Int, val mensaje: String)

/** GET /api/clinicas: todo el directorio, también sin coordenadas. */
@Serializable
data class ClinicaDto(
    val id: Int,
    val nombre: String,
    val tipo: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val codigoPostal: String? = null,
    val telefono: String = "",
    val horario: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val asociada: Boolean = false,
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
    val casoId: Int = 0,
    val alumno: String = "",
)

@Serializable
data class RevisionComprobanteDto(val aprobado: Boolean, val motivo: String? = null)

@Serializable
data class AvisoEntradaDto(
    val titulo: String,
    val mensaje: String,
    /** Correos de las cuentas. */
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

/** Cuerpo de error del servidor: {"error": "mensaje en español", "codigo": "..."}. */
@Serializable
data class ErrorDto(val error: String = "", val codigo: String? = null)

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "2026-11-19T15:04:05.123Z" → "19 nov 2026". Sin librería de fechas, a propósito. */
fun fechaLegible(iso: String): String {
    val partes = iso.take(10).split("-")
    if (partes.size != 3) return iso
    val mes = partes[1].toIntOrNull()?.let { MESES.getOrNull(it - 1) } ?: return iso
    return "${partes[2].trimStart('0')} $mes ${partes[0]}"
}
