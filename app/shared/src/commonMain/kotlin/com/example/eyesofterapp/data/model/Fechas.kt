package com.example.eyesofterapp.data.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Ahora en ISO-8601 UTC. */
@OptIn(ExperimentalTime::class)
fun ahoraIso(): String = Clock.System.now().toString()

/**
 * Fecha de calendario en la Ciudad de México (UTC-6, sin horario de verano desde 2022),
 * [masDias] días después de hoy, como "AAAA-MM-DD". Sin librería de fechas, a propósito.
 */
@OptIn(ExperimentalTime::class)
fun fechaMx(masDias: Int = 0): String {
    val segundos = Clock.System.now().epochSeconds - 6 * 3600
    val dias = segundos.floorDiv(86_400L) + masDias
    // Algoritmo de días civiles (Howard Hinnant).
    val z = dias + 719_468
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

/** "2026-11-25" → "mar 25 nov" (día de la semana corto). */
fun fechaCorta(fecha: String): String {
    val p = fecha.split("-").mapNotNull { it.toIntOrNull() }
    if (p.size != 3) return fecha
    val (y0, m, d) = p
    // Zeller simplificado (0 = sábado).
    val y = if (m < 3) y0 - 1 else y0
    val mm = if (m < 3) m + 12 else m
    val h = (d + 13 * (mm + 1) / 5 + y % 100 + (y % 100) / 4 + (y / 100) / 4 + 5 * (y / 100)) % 7
    val dia = listOf("sáb", "dom", "lun", "mar", "mié", "jue", "vie")[h]
    return "$dia ${fechaLegible(fecha).substringBeforeLast(' ')}"
}
