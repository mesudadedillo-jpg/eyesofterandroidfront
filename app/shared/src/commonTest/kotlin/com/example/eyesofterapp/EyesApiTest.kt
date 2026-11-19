package com.example.eyesofterapp

import com.example.eyesofterapp.composeApp.navigation.Screen
import com.example.eyesofterapp.composeApp.navigation.rutaAScreen
import com.example.eyesofterapp.composeApp.screens.ordenDirectorio
import com.example.eyesofterapp.data.model.fechaLegible
import com.example.eyesofterapp.data.network.EyesApi
import com.example.eyesofterapp.data.network.ResultadoRed
import com.example.eyesofterapp.domain.model.Optica
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EyesApiTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun cliente(motor: MockEngine) = HttpClient(motor) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun mandaElTokenYLeeLaBandeja() = runTest {
        var autorizacion: String? = null
        val motor = MockEngine { req ->
            autorizacion = req.headers[HttpHeaders.Authorization]
            respond(
                """{"noLeidas":1,"notificaciones":[{"id":"n1","tipo":"RESULTADO_TAMIZAJE","titulo":"T","mensaje":"M",
                   "datos":{"ruta":"tamizaje/t1"},"creadaEn":"2026-11-19T02:24:24.909Z","leidaEn":null,"campoNuevo":1}]}""",
                HttpStatusCode.OK, json
            )
        }
        val api = EyesApi(cliente(motor), token = { "abc.def.ghi" }, alVencerSesion = {})
        val r = api.bandeja()
        assertIs<ResultadoRed.Exito<*>>(r)
        assertEquals("Bearer abc.def.ghi", autorizacion)
        val bandeja = (r as ResultadoRed.Exito).data
        assertEquals(1, bandeja.noLeidas)
        assertEquals(false, bandeja.notificaciones.single().leida)
    }

    @Test
    fun un401CierraLaSesion() = runTest {
        var vencida = false
        val motor = MockEngine { respond("""{"error":"Sesión inválida o vencida."}""", HttpStatusCode.Unauthorized, json) }
        val r = EyesApi(cliente(motor), token = { "viejo" }, alVencerSesion = { vencida = true }).tamizajes()
        assertTrue(vencida)
        assertEquals(401, (r as ResultadoRed.ErrorCliente).codigo)
    }

    @Test
    fun losErroresMuestranElMensajeDelServidor() = runTest {
        val motor = MockEngine { respond("""{"error":"Solo se aceptan fotos (JPG, PNG, WEBP) o PDF"}""", HttpStatusCode.UnsupportedMediaType, json) }
        val r = EyesApi(cliente(motor), token = { "t" }, alVencerSesion = {})
            .subirComprobante("t1", "RECETA", byteArrayOf(1, 2, 3), "x.jpg", "image/jpeg")
        assertEquals("Solo se aceptan fotos (JPG, PNG, WEBP) o PDF", r.aMensajeUsuario())
    }

    @Test
    fun elDirectorioPoneLasAsociadasPrimero() {
        fun o(id: Int, verificada: Boolean, km: Double) = Optica(id, "O$id", "", lat = 0.0, lng = 0.0, distanciaKm = km, verificada = verificada)
        val orden = listOf(o(1, false, 0.5), o(2, true, 9.0), o(3, true, 2.0), o(4, false, 0.0), o(5, false, 1.0)).ordenDirectorio()
        // Asociadas por distancia (3: 2 km, 2: 9 km); luego externas por distancia; sin distancia al final.
        assertEquals(listOf(3, 2, 1, 5, 4), orden.map { it.id })
    }

    @Test
    fun lasRutasDeLasNotificacionesLlevanASuPantalla() {
        assertEquals(Screen.Tamizaje("abc"), rutaAScreen("tamizaje/abc"))
        assertEquals(Screen.Seguimiento, rutaAScreen("seguimiento"))
        assertEquals(Screen.Comprobantes, rutaAScreen("comprobantes"))
        assertEquals(null, rutaAScreen("notificacion/n1"))
    }

    @Test
    fun fechasLegibles() {
        assertEquals("19 nov 2026", fechaLegible("2026-11-19T02:24:24.909Z"))
        assertEquals("5 ene 2027", fechaLegible("2027-01-05"))
    }
}
