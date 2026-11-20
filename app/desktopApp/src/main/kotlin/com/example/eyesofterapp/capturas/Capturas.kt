package com.example.eyesofterapp.capturas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.example.eyesofterapp.composeApp.navigation.Screen
import com.example.eyesofterapp.composeApp.screens.ComprobantesScreen
import com.example.eyesofterapp.composeApp.screens.ConsentimientoScreen
import com.example.eyesofterapp.composeApp.screens.ElegirDestinoScreen
import com.example.eyesofterapp.composeApp.screens.GuiaScreen
import com.example.eyesofterapp.composeApp.screens.ReportarScreen
import com.example.eyesofterapp.composeApp.screens.SolicitarCitaScreen
import com.example.eyesofterapp.data.model.CasoDto
import com.example.eyesofterapp.data.model.CitaDto
import com.example.eyesofterapp.data.model.ClinicaDto
import com.example.eyesofterapp.data.model.ConsentimientoDto
import com.example.eyesofterapp.data.model.EventoDto
import com.example.eyesofterapp.data.model.PaseDto
import com.example.eyesofterapp.composeApp.screens.InicioScreen
import com.example.eyesofterapp.composeApp.screens.LoginScreen
import com.example.eyesofterapp.composeApp.screens.NotificacionesScreen
import com.example.eyesofterapp.composeApp.screens.NuevoAvisoScreen
import com.example.eyesofterapp.composeApp.screens.OpticasScreen
import com.example.eyesofterapp.composeApp.screens.PausaVisualScreen
import com.example.eyesofterapp.composeApp.screens.SeguimientoScreen
import com.example.eyesofterapp.composeApp.screens.TamizajeScreen
import com.example.eyesofterapp.composeApp.theme.EyeSofterTheme
import com.example.eyesofterapp.data.model.BandejaDto
import com.example.eyesofterapp.data.model.ComprobanteDto
import com.example.eyesofterapp.data.model.NotificacionDto
import com.example.eyesofterapp.data.model.SeguimientoDto
import com.example.eyesofterapp.data.model.TamizajeDto
import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.Optica
import com.example.eyesofterapp.domain.model.Role
import com.example.eyesofterapp.domain.model.User
import com.example.eyesofterapp.plataforma.ArchivoElegido
import com.example.eyesofterapp.plataforma.LocalTelefono
import com.example.eyesofterapp.plataforma.Telefono
import com.example.eyesofterapp.ui.AuthUiState
import com.example.eyesofterapp.ui.OpticasUiState
import com.example.eyesofterapp.ui.PanelUiState
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Genera capturas PNG de las pantallas compartidas con datos de ejemplo, sin
 * abrir ventanas ni emulador:
 *
 *     ./gradlew :app:desktopApp:capturas
 *
 * Quedan en app/desktopApp/build/capturas/. Sirven para revisar el diseño y
 * para la documentación. (El mapa 3D de Android no sale aquí: usa MapLibre.)
 */
fun main(args: Array<String>) {
    val salida = File(args.firstOrNull() ?: "build/capturas").apply { mkdirs() }

    val tutor = User("tutor@eyesofter.mx", "María Hernández", Role.CLIENTE)
    val enfermeria = User("enfermeria@eyesofter.mx", "Laura Pérez", Role.SUPERVISOR)
    val alumno = User("alumno@eyesofter.mx", "Diego Ramírez", Role.ALUMNO)
    val especialista = User("especialista@eyesofter.mx", "Dra. Ruiz", Role.DOCTOR)
    val eventos = listOf(
        EventoDto("2026-11-18", "TAMIZAJE", "Tamizaje Snellen (escritorio): OD 20/40, OI 20/30. Semáforo Amarillo."),
        EventoDto("2026-11-18", "AVISO_TUTOR", "Se notificó el resultado al tutor por la app y el portal."),
        EventoDto("2026-11-19", "DESTINO", "Destino: Óptica Tulancingo Centro (asociada). Se generó el pase médico con QR."),
        EventoDto("2026-11-19", "CITA", "El tutor solicitó cita en Óptica Tulancingo Centro para el 2026-11-25 (desde la app)."),
    )
    val pase = PaseDto("K7M2Q9XRT4HW8NBC3PZD6FJYEA", "K7M2 Q9XR T4HW 8NBC 3PZD 6FJY EA", "VIGENTE", "2026-11-19T16:00:00Z", "2027-01-18T16:00:00Z",
        null, 1, "Óptica Tulancingo Centro", "Av. Juárez 102, Centro", "7757531234", "https://portal/Pases/Validar?codigo=K7M2Q9XRT4HW8NBC3PZD6FJYEA")
    val casoConPase = CasoDto(5, "EYS-2026-0003", "EN_SEGUIMIENTO", true, 1, "Óptica Tulancingo Centro", true, pase,
        citas = listOf(CitaDto(3, "Óptica Tulancingo Centro", "2026-11-25", "MANANA", null, "SOLICITADA")), eventos = eventos)
    val casoSinDestino = CasoDto(5, "EYS-2026-0003", "ABIERTO", eventos = eventos.take(2))
    val casoResumen = CasoDto(5, "EYS-2026-0003", "EN_SEGUIMIENTO", true, null, "Óptica Mi Barrio", false, eventos = eventos.take(2))
    val t1 = TamizajeDto("t1", "F-2026-001", "Ana López Hernández", "3", "B", "2026-11-18", "AMARILLO", "En observación", "Entrecierra los ojos para ver el pizarrón.", caso = casoSinDestino)
    val t2 = TamizajeDto("t2", "F-2025-114", "Ana López Hernández", "2", "B", "2025-11-10", "VERDE", "Saludable")
    val notis = listOf(
        NotificacionDto("n1", "RESULTADO_TAMIZAJE", "Resultado del tamizaje visual", "Ya está disponible el resultado del tamizaje visual de Ana López. Tócala para verlo.", mapOf("ruta" to "tamizaje/t1"), "2026-11-18T15:00:00Z"),
        NotificacionDto("n2", "AVISO", "Junta de padres", "El viernes a las 10:00 en el auditorio.", mapOf("ruta" to "notificacion/n2"), "2026-11-15T15:00:00Z", leidaEn = "2026-11-15T18:00:00Z"),
        NotificacionDto("n3", "COMPROBANTE_REVISADO", "Comprobante aceptado", "Enfermería aceptó el comprobante de Ana. ¡Gracias!", mapOf("ruta" to "tamizaje/t2"), "2025-12-01T15:00:00Z", leidaEn = "2025-12-01T18:00:00Z"),
    )
    val comprobantes = listOf(
        ComprobanteDto("c1", "t1", "cliente", "RECETA", "image/jpeg", 312_000, "2026-11-19T10:00:00Z", "PENDIENTE"),
        ComprobanteDto("c2", "t2", "cliente", "COMPROBANTE_LENTES", "application/pdf", 98_000, "2025-12-01T10:00:00Z", "RECHAZADO", motivoRechazo = "La foto está borrosa"),
    )
    val seguimiento = listOf(
        SeguimientoDto("n1", "cliente", "Ana López Hernández", "t1", "2026-11-18T15:00:00Z", null, 1, listOf("push")),
        SeguimientoDto("n4", "tutor7", "Luis Martínez", "t7", "2026-11-14T15:00:00Z", null, 2, emptyList()),
        SeguimientoDto("n5", "tutor9", "Sofía Cruz", "t9", "2026-11-12T15:00:00Z", "2026-11-12T20:00:00Z", 0, listOf("correo")),
    )
    val panelTutor = PanelUiState(bandeja = BandejaDto(1, notis), tamizajes = listOf(t1, t2), comprobantes = comprobantes)
    val qr = System.getenv("QR_PNG")?.let { File(it).takeIf { f -> f.exists() }?.readBytes() }
    val conPase = panelTutor.copy(tamizajes = listOf(t1.copy(caso = casoConPase), t2), pases = qr?.let { mapOf(pase.codigo to it) } ?: emptyMap())
    val conResumen = panelTutor.copy(tamizajes = listOf(t1.copy(caso = casoResumen), t2), sinConexion = true, actualizadoEn = "2026-11-19T18:00:00Z")
    val clinicas = listOf(
        ClinicaDto(1, "Óptica Tulancingo Centro", "Optica", "Av. Juárez 102, Centro", "Tulancingo", "43600", "7757531234", asociada = true),
        ClinicaDto(5, "Centro Oftalmológico Hidalgo", "Oftalmologia", "Blvd. Felipe Ángeles 300", "Tulancingo", asociada = true),
        ClinicaDto(2, "Optometría Visión Real", "Optica", "Calle Morelos 45", "Tulancingo"),
        ClinicaDto(4, "Consultorio Dr. Pérez", "Consultorio", "Calle Hidalgo 9", "Tulancingo"),
    )
    val conDirectorio = panelTutor.copy(clinicas = clinicas, tamizajes = listOf(t1.copy(caso = casoConPase)))
    val panelEnf = PanelUiState(bandeja = BandejaDto(2, notis), seguimiento = seguimiento, comprobantes = comprobantes)
    val opticas = OpticasUiState(
        opticas = listOf(
            Optica(1, "Óptica Tulancingo Centro", "Av. Juárez 102, Centro", "Tulancingo", "43600", 20.0825, -98.3622, 0.4, "7757531234", true),
            Optica(2, "Optometría Visión Real", "Calle Morelos 45", "Tulancingo", "43600", 20.0841, -98.3601, 1.1, "7757535678", false),
            Optica(3, "Óptica Devlyn Tulancingo", "Plaza Bella Local 12", "Tulancingo", "43600", 20.0890, -98.3580, 2.3, "7757539999", false),
        ),
        coordenadasGps = Coordenadas(20.0825, -98.3622),
    )
    val telefonoFalso = object : Telefono {
        override suspend fun tomarFoto(): ArchivoElegido? = null
        override suspend fun elegirArchivo(): ArchivoElegido? = null
        override fun vibrar(patron: LongArray) {}
        override fun sonar() {}
        override fun mantenerPantallaEncendida(activa: Boolean) {}
    }

    val pantallas: List<Pair<String, @Composable () -> Unit>> = listOf(
        "01-login" to { LoginScreen(AuthUiState(), { _, _ -> }) },
        "02-inicio-tutor" to { InicioScreen(tutor, panelTutor, {}, {}, {}) },
        "03-resultado-amarillo" to {
            CompositionLocalProvider(LocalTelefono provides telefonoFalso) {
                TamizajeScreen("t1", panelTutor, { _, _, _ -> }, {}, {}, {}, {}, {})
            }
        },
        "12-pase-medico-qr" to {
            CompositionLocalProvider(LocalTelefono provides telefonoFalso) {
                TamizajeScreen("t1", conPase, { _, _, _ -> }, {}, {}, {}, {}, {})
            }
        },
        "13-resumen-sin-conexion" to { TamizajeScreen("t1", conResumen, { _, _, _ -> }, {}, {}, {}, {}, {}) },
        "14-elegir-clinica" to { ElegirDestinoScreen("t1", conDirectorio, {}, { _, _, _ -> }, {}, {}) },
        "15-pedir-cita" to { SolicitarCitaScreen("t1", conDirectorio, {}, { _, _ -> }, {}, {}) },
        "16-aviso-privacidad" to {
            ConsentimientoScreen(PanelUiState(consentimientoPendiente = true, consentimiento = ConsentimientoDto("2026-11", false)), {}, {}, {})
        },
        "17-guia-rapida" to { GuiaScreen(Role.CLIENTE) {} },
        "18-reportar" to { ReportarScreen(panelTutor, { _, _, _ -> }, {}, {}) },
        "19-inicio-especialista" to { InicioScreen(especialista, PanelUiState(), { _: Screen -> }, {}, {}) },
        "04-notificaciones" to { NotificacionesScreen(panelTutor, {}, {}, {}) },
        "05-inicio-enfermeria" to { InicioScreen(enfermeria, panelEnf, {}, {}, {}) },
        "06-seguimiento" to { SeguimientoScreen(panelEnf, {}, {}) },
        "07-comprobantes-revision" to { ComprobantesScreen(enfermeria, panelEnf, { _, _, _ -> }, { null }, {}, {}, {}) },
        "08-nuevo-aviso" to { NuevoAvisoScreen(panelEnf, { _, _, _, _ -> }, {}) },
        "09-inicio-alumno" to { InicioScreen(alumno, PanelUiState(), { _: Screen -> }, {}, {}) },
        "10-pausa-20-20-20" to { PausaVisualScreen {} },
        "11-directorio-comun" to { OpticasScreen(opticas, {}, {}, {}, {}, {}) },
    )

    for ((nombre, contenido) in pantallas) {
        // 412 × 892 dp a densidad 2.625: un Pixel típico.
        val escena = ImageComposeScene(1080, 2340, Density(2.625f)) { EyeSofterTheme { contenido() } }
        // Varios cuadros con tiempo real de por medio: así terminan las animaciones de entrada.
        var t = 0L
        repeat(12) {
            escena.render(t)
            Thread.sleep(120)
            t += 120_000_000L
        }
        val png = escena.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes
        File(salida, "$nombre.png").writeBytes(png)
        escena.close()
        println("Captura: $nombre.png")
    }
}
