package com.example.eyesofterapp.data.network

import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.FUENTE_OSM
import com.example.eyesofterapp.domain.model.Optica
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Búsqueda GRATUITA de ópticas reales con OpenStreetMap (funciona en cualquier país).
 *
 * - Overpass API: encuentra ópticas/optometristas alrededor de unas coordenadas.
 * - Nominatim: convierte un texto ("Tulancingo", "43600", "Madrid") en coordenadas.
 *
 * No requieren llave ni cuenta. Reglas de uso justo: identificarse con User-Agent,
 * no más de ~1 búsqueda por segundo y mostrar la atribución "© OpenStreetMap"
 * (la pantalla de ópticas la muestra al final de la lista).
 */
class OsmApi(private val client: HttpClient) {

    data class ResultadoOsm(val opticas: List<Optica>, val radioKm: Int)
    data class LugarEncontrado(val coordenadas: Coordenadas, val nombre: String)

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Busca ópticas ampliando el radio (3 → 10 → 30 km) hasta encontrar al menos 5.
     * Devuelve null solo si NO se pudo consultar ningún servidor (sin internet, caídos).
     */
    suspend fun buscarOpticas(lat: Double, lng: Double): ResultadoOsm? {
        var ultimo: ResultadoOsm? = null
        for (radioKm in RADIOS_KM) {
            // Si un radio mayor falla, nos quedamos con lo que ya se había encontrado.
            val lista = opticasEnRadio(lat, lng, radioKm * 1000) ?: return ultimo
            ultimo = ResultadoOsm(lista, radioKm)
            if (lista.size >= MINIMO_RESULTADOS) return ultimo
        }
        return ultimo
    }

    private suspend fun opticasEnRadio(lat: Double, lng: Double, radioM: Int): List<Optica>? {
        val zona = "(around:$radioM,$lat,$lng)"
        val consulta = """
            [out:json][timeout:20];
            (
              nwr["shop"="optician"]$zona;
              nwr["amenity"="optician"]$zona;
              nwr["healthcare"="optometrist"]$zona;
              nwr["healthcare:speciality"~"ophthalmology|optometry"]$zona;
            );
            out center tags 80;
        """.trimIndent()

        for (servidor in SERVIDORES_OVERPASS) {
            try {
                val respuesta = client.submitForm(
                    url = servidor,
                    formParameters = parameters { append("data", consulta) }
                ) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    timeout {
                        requestTimeoutMillis = 25_000
                        socketTimeoutMillis = 25_000
                    }
                }
                if (!respuesta.status.isSuccess()) {
                    println("[OSM] $servidor respondió ${respuesta.status.value}; probando el siguiente")
                    continue
                }
                return parsearOverpass(respuesta.bodyAsText(), lat, lng)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[OSM] Falló $servidor: ${e.message}")
            }
        }
        return null
    }

    private fun parsearOverpass(texto: String, lat: Double, lng: Double): List<Optica> {
        val elementos = json.parseToJsonElement(texto).jsonObject["elements"]?.jsonArray ?: return emptyList()
        return elementos.mapNotNull { el ->
            val obj = el.jsonObject
            val tipo = obj.texto("type") ?: "node"
            val osmId = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            // Los nodos traen lat/lon; las vías y relaciones (edificios) traen "center".
            val centro = obj["center"]?.jsonObject ?: obj
            val oLat = centro["lat"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val oLng = centro["lon"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val tags = obj["tags"]?.jsonObject

            val calle = listOfNotNull(tags.texto("addr:street"), tags.texto("addr:housenumber")).joinToString(" ")
            val colonia = tags.texto("addr:suburb") ?: tags.texto("addr:neighbourhood")
            val direccion = listOf(calle, colonia.orEmpty()).filter { it.isNotBlank() }.joinToString(", ")

            Optica(
                id = "$tipo/$osmId".hashCode(),
                nombre = tags.texto("name") ?: tags.texto("brand") ?: tags.texto("operator") ?: "Óptica sin nombre",
                direccion = direccion.ifBlank { "Dirección no registrada" },
                ciudad = tags.texto("addr:city") ?: tags.texto("addr:town") ?: tags.texto("addr:village") ?: "",
                codigoPostal = tags.texto("addr:postcode") ?: "",
                lat = oLat,
                lng = oLng,
                distanciaKm = distanciaKm(lat, lng, oLat, oLng),
                telefono = (tags.texto("phone") ?: tags.texto("contact:phone") ?: "").substringBefore(";").trim(),
                verificada = false,
                horario = tags.texto("opening_hours") ?: "",
                sitioWeb = tags.texto("website") ?: tags.texto("contact:website") ?: "",
                fuente = FUENTE_OSM
            )
        }
            .distinctBy { it.id }
            .sortedBy { it.distanciaKm }
    }

    /** Convierte un texto (ciudad, colonia, C.P., país) en coordenadas. null si no lo encuentra. */
    suspend fun geocodificar(texto: String): LugarEncontrado? {
        return try {
            val respuesta = client.get(URL_NOMINATIM) {
                parameter("q", texto)
                parameter("format", "jsonv2")
                parameter("limit", 1)
                parameter("accept-language", "es")
                header(HttpHeaders.UserAgent, USER_AGENT)
                timeout { requestTimeoutMillis = 12_000 }
            }
            if (!respuesta.status.isSuccess()) return null
            val primero = json.parseToJsonElement(respuesta.bodyAsText()).jsonArray.firstOrNull()?.jsonObject
                ?: return null
            val lat = primero.texto("lat")?.toDoubleOrNull() ?: return null
            val lng = primero.texto("lon")?.toDoubleOrNull() ?: return null
            LugarEncontrado(Coordenadas(lat, lng), primero.texto("display_name") ?: texto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[OSM] Nominatim falló: ${e.message}")
            null
        }
    }

    private fun JsonObject?.texto(clave: String): String? =
        this?.get(clave)?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        val RADIOS_KM = listOf(3, 10, 30)
        const val MINIMO_RESULTADOS = 5
        const val USER_AGENT = "EyesofterApp/1.0 (proyecto escolar UPT, Tulancingo)"
        const val URL_NOMINATIM = "https://nominatim.openstreetmap.org/search"
        /** Si el primero falla o está saturado, se intenta con el siguiente. */
        val SERVIDORES_OVERPASS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
        )
    }
}
