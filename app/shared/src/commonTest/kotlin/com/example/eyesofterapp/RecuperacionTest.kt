package com.example.eyesofterapp

import com.example.eyesofterapp.data.network.EyesApi
import com.example.eyesofterapp.ui.PasoRecuperacion
import com.example.eyesofterapp.ui.RecuperacionViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** "¿Olvidaste tu contraseña?" en la app (contrato docs/API.md §3.1). */
@OptIn(ExperimentalCoroutinesApi::class)
class RecuperacionTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val peticiones = mutableListOf<Pair<String, String>>()

    private fun TestScope.vm(restablecer: () -> Pair<HttpStatusCode, String>): RecuperacionViewModel {
        val motor = MockEngine { req ->
            peticiones += req.url.encodedPath to ((req.body as? TextContent)?.text ?: "")
            if (req.url.encodedPath == "/api/recuperar/solicitar")
                respond("""{"mensaje":"Si los datos corresponden a una cuenta, te enviamos un código de 6 dígitos a tu correo.","canal":"correo","minutosVigencia":10}""", HttpStatusCode.Accepted, json)
            else restablecer().let { (estado, cuerpo) -> respond(cuerpo, estado, json) }
        }
        val cliente = HttpClient(motor) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return RecuperacionViewModel(EyesApi(cliente, token = { null }, alVencerSesion = {}), backgroundScope)
    }

    @Test
    fun flujoCompleto() = runTest {
        val vm = vm { HttpStatusCode.NoContent to "" }
        vm.pedirCodigo("  tutor@eyesofter.mx ")
        val enCodigo = vm.state.first { it.paso == PasoRecuperacion.Codigo }
        assertTrue(enCodigo.aviso!!.contains("te enviamos un código"))
        vm.state.first { it.reenviarEn == RecuperacionViewModel.SEGUNDOS_REENVIO }
        // La cuenta atrás llega a 0 y ya se puede pedir otro.
        vm.state.first { it.reenviarEn == 0 }

        vm.restablecer("123 456", "NuevaClave2026", "NuevaClave2026")
        vm.state.first { it.paso == PasoRecuperacion.Listo }
        val (ruta, cuerpo) = peticiones.last()
        assertEquals("/api/recuperar/restablecer", ruta)
        assertTrue(""""cuenta":"tutor@eyesofter.mx"""" in cuerpo && """"codigo":"123456"""" in cuerpo, cuerpo)
    }

    @Test
    fun muestraElErrorDelServidor() = runTest {
        val vm = vm { HttpStatusCode.BadRequest to """{"error":"El código no es correcto. Te quedan 4 intentos.","codigo":"CODIGO_INCORRECTO"}""" }
        vm.pedirCodigo("7751234567")
        vm.state.first { it.paso == PasoRecuperacion.Codigo && !it.enviando }
        vm.restablecer("000000", "NuevaClave2026", "NuevaClave2026")
        val conError = vm.state.first { it.error != null }
        assertEquals(PasoRecuperacion.Codigo, conError.paso)
        assertEquals("El código no es correcto. Te quedan 4 intentos.", conError.error)
    }

    @Test
    fun validaAntesDeLlamarAlServidor() = runTest {
        val vm = vm { HttpStatusCode.NoContent to "" }
        vm.pedirCodigo("hola")
        assertEquals("Escribe tu correo o tu celular de 10 dígitos.", vm.state.value.error)
        vm.pedirCodigo("tutor@eyesofter.mx")
        vm.state.first { it.paso == PasoRecuperacion.Codigo && !it.enviando }
        val antes = peticiones.size
        vm.restablecer("123456", "NuevaClave2026", "Otra")
        assertEquals("Las contraseñas no coinciden.", vm.state.value.error)
        vm.restablecer("123456", "sinnumeros", "sinnumeros")
        assertEquals("La contraseña debe incluir al menos un número (0-9).", vm.state.value.error)
        assertEquals(antes, peticiones.size)
    }
}
