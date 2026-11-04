package com.example.eyesofterapp.data.network

import com.example.eyesofterapp.data.cache.DiskOpticasCache
import com.example.eyesofterapp.domain.model.Optica
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class OpticasApi(private val client: HttpClient) {

    private val opticasCatalogo = listOf(
        Optica(1, "Óptica Tulancingo Centro", "Av. Juárez 102, Centro", "Tulancingo", "43600", 20.0825, -98.3622, 1.2, "7757531234", true),
        Optica(2, "Optometría Visión Real", "Calle Morelos 45", "Tulancingo", "43600", 20.0841, -98.3601, 2.5, "7757535678", false),
        Optica(3, "Óptica Devlyn Tulancingo", "Plaza Bella Local 12", "Tulancingo", "43600", 20.0890, -98.3580, 3.1, "7757539999", false),
        Optica(4, "Óptica Visión Central", "Av. Reforma 123", "CDMX", "06600", 19.432608, -99.133209, 1.8, "5555123456", true),
        Optica(5, "Óptica Salud Visual", "Calle Juarez 456", "CDMX", "06600", 19.427015, -99.167671, 3.4, "5555987654", false),
        Optica(6, "Centro Oftálmico del Norte", "Av. Insurgentes Sur 789", "CDMX", "03100", 19.380112, -99.172345, 5.2, "5555443322", false),
        Optica(7, "Óptica Clara Vista", "Hidalgo 101", "Guadalajara", "44100", 20.676667, -103.3475, 2.1, "3336123456", false),
        Optica(8, "Optometría Moderna", "Zaragoza 202", "Monterrey", "64000", 25.686614, -100.316113, 4.0, "8183123456", false)
    )

    suspend fun getOpticasCercanas(lat: Double, lng: Double): ResultadoRed<List<Optica>> {
        val resultado = ejecutarPeticion {
            client.get("/opticas/cercanas") {
                parameter("lat", lat)
                parameter("lng", lng)
            }
        }

        if (resultado is ResultadoRed.Exito && resultado.data.isNotEmpty()) {
            return resultado
        }

        val localList = opticasCatalogo.map { optica ->
            val dist = calcularHaversine(lat, lng, optica.lat, optica.lng)
            optica.copy(distanciaKm = dist)
        }.sortedBy { it.distanciaKm }

        DiskOpticasCache.guardar(localList)
        return ResultadoRed.Exito(localList)
    }

    suspend fun getOpticasPorCiudadOZip(query: String): ResultadoRed<List<Optica>> {
        val resultado = ejecutarPeticion {
            client.get("/opticas/cercanas") {
                parameter("query", query)
                parameter("ciudad", query)
                parameter("cp", query)
            }
        }

        if (resultado is ResultadoRed.Exito && resultado.data.isNotEmpty()) {
            return resultado
        }

        val q = query.lowercase().trim()
        val filtrados = opticasCatalogo.filter {
            it.ciudad.lowercase().contains(q) ||
            it.codigoPostal.lowercase().contains(q) ||
            it.nombre.lowercase().contains(q) ||
            it.direccion.lowercase().contains(q)
        }

        val list = if (filtrados.isNotEmpty()) filtrados else opticasCatalogo
        DiskOpticasCache.guardar(list)
        return ResultadoRed.Exito(list)
    }

    suspend fun verificarSalud(): ResultadoRed<Boolean> {
        val baseUrl = BASE_URL
        return try {
            val response = client.get("/salud")
            if (response.status.isSuccess()) {
                ResultadoRed.Exito(true)
            } else {
                ResultadoRed.ErrorServidor(response.status.value, "Servidor reporta problemas")
            }
        } catch (e: HttpRequestTimeoutException) {
            ResultadoRed.Timeout(baseUrl)
        } catch (e: Exception) {
            ResultadoRed.SinConexion
        }
    }

    private suspend fun ejecutarPeticion(call: suspend () -> io.ktor.client.statement.HttpResponse): ResultadoRed<List<Optica>> {
        val baseUrl = BASE_URL
        return try {
            val response = call()
            when {
                response.status.isSuccess() -> {
                    val list = response.body<List<Optica>>()
                    if (list.isNotEmpty()) {
                        DiskOpticasCache.guardar(list)
                    }
                    ResultadoRed.Exito(list)
                }
                response.status == HttpStatusCode.Unauthorized -> ResultadoRed.ErrorCliente(401, "No autorizado")
                response.status.value in 400..499 -> ResultadoRed.ErrorCliente(response.status.value, "Solicitud inválida")
                response.status.value in 500..599 -> ResultadoRed.ErrorServidor(response.status.value, "Error interno en el servidor")
                else -> ResultadoRed.ErrorServidor(response.status.value, "Error no esperado")
            }
        } catch (e: HttpRequestTimeoutException) {
            ResultadoRed.Timeout(baseUrl)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("Connect", ignoreCase = true) || msg.contains("Host", ignoreCase = true) || msg.contains("Network", ignoreCase = true)) {
                ResultadoRed.SinConexion
            } else {
                ResultadoRed.Desconocido(e)
            }
        }
    }

    private fun calcularHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val pi = kotlin.math.PI
        val dLat = (lat2 - lat1) * pi / 180.0
        val dLon = (lon2 - lon1) * pi / 180.0
        val radLat1 = lat1 * pi / 180.0
        val radLat2 = lat2 * pi / 180.0

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(radLat1) * cos(radLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * acos(min(1.0, sqrt(a)))
        return r * c
    }
}
