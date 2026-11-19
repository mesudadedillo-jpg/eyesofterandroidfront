package com.example.eyesofterapp.composeApp.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraPantalla
import com.example.eyesofterapp.composeApp.theme.EyesBotonFantasma
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesEtiqueta
import com.example.eyesofterapp.composeApp.theme.EyesLogo
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesTarjetaAcento
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.plataforma.LocalTelefono
import kotlinx.coroutines.delay

private enum class Fase { Detenido, Pantalla, Descanso }

/** Un ejercicio guiado: pasos que se van mostrando, cada uno con su duración en segundos. */
private class Ejercicio(val nombre: String, val descripcion: String, val pasos: List<Pair<String, Int>>)

private val ejercicios = listOf(
    Ejercicio(
        "Parpadeo", "Humedece tus ojos.",
        List(5) { listOf("Cierra los ojos suavemente" to 2, "Ábrelos y relaja la cara" to 2) }.flatten()
    ),
    Ejercicio(
        "Cerca y lejos", "Entrena el enfoque.",
        List(3) { listOf("Mira la punta de tu dedo, a un palmo de tu nariz" to 5, "Ahora mira lo más lejos que puedas" to 5) }.flatten()
    ),
    Ejercicio(
        "Palmeo", "Descansa los ojos en la oscuridad.",
        listOf("Frota tus manos hasta que estén tibias" to 5, "Cubre tus ojos con las palmas, sin apretar" to 20, "Quita las manos poco a poco y abre los ojos" to 5)
    ),
)

/**
 * Regla 20-20-20 (M-RFN-12): cada 20 minutos de pantalla, 20 segundos mirando
 * a 20 pies (6 m). Al terminar cada tramo el teléfono vibra y suena (M-RI-06).
 * Funciona mientras la app está abierta; la pantalla no se apaga mientras corre.
 */
@Composable
fun PausaVisualScreen(onVolver: () -> Unit) {
    val telefono = LocalTelefono.current
    var fase by remember { mutableStateOf(Fase.Detenido) }
    // Modo demostración: 20 segundos de pantalla en vez de 20 minutos (para probarlo en clase).
    var demo by remember { mutableStateOf(false) }
    val duracionPantalla = if (demo) 20 else 20 * 60
    var restante by remember { mutableIntStateOf(duracionPantalla) }
    var pausas by remember { mutableIntStateOf(0) }
    var ejercicio by remember { mutableStateOf<Ejercicio?>(null) }

    DisposableEffect(fase) {
        telefono?.mantenerPantallaEncendida(fase != Fase.Detenido)
        onDispose { telefono?.mantenerPantallaEncendida(false) }
    }

    LaunchedEffect(fase, demo) {
        if (fase == Fase.Detenido) return@LaunchedEffect
        while (restante > 0) {
            delay(1000)
            restante--
        }
        if (fase == Fase.Pantalla) {
            telefono?.vibrar(longArrayOf(0, 400, 200, 400, 200, 400))
            telefono?.sonar()
            fase = Fase.Descanso
            restante = 20
        } else {
            telefono?.vibrar(longArrayOf(0, 150))
            pausas++
            fase = Fase.Pantalla
            restante = duracionPantalla
        }
    }

    val total = if (fase == Fase.Descanso) 20 else duracionPantalla
    val progreso by animateFloatAsState(
        if (fase == Fase.Detenido) 1f else restante / total.toFloat(), tween(900), label = "anillo"
    )

    Column(Modifier.fillMaxSize().background(if (fase == Fase.Descanso) EyesColors.Celeste200 else EyesColors.Fondo)) {
        EyesBarraPantalla("Cuida tu vista", onVolver)
        val actual = ejercicio
        if (actual != null) {
            EjercicioGuiado(actual) { ejercicio = null }
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                when (fase) {
                    Fase.Detenido -> "Regla 20-20-20"
                    Fase.Pantalla -> "Puedes usar la pantalla"
                    Fase.Descanso -> "¡Mira algo lejano!"
                },
                style = MaterialTheme.typography.headlineMedium, color = EyesColors.Texto, textAlign = TextAlign.Center
            )
            Text(
                when (fase) {
                    Fase.Detenido -> "Cada 20 minutos de pantalla, mira algo a 6 metros durante 20 segundos. Te avisamos con vibración."
                    Fase.Pantalla -> "Te avisamos cuando toque descansar."
                    Fase.Descanso -> "Busca una ventana, un árbol o la pared del fondo del salón y no le quites la vista."
                },
                style = MaterialTheme.typography.bodyLarge, color = EyesColors.TextoSecundario, textAlign = TextAlign.Center
            )

            // Anillo que se va vaciando, con el tiempo en el centro.
            Box(
                Modifier.size(240.dp).semantics { contentDescription = "Faltan ${formato(restante, fase == Fase.Descanso)}" },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val grosor = 18.dp.toPx()
                    val lado = size.minDimension - grosor
                    val esquina = Offset((size.width - lado) / 2, (size.height - lado) / 2)
                    drawArc(EyesColors.Celeste100, 0f, 360f, false, esquina, Size(lado, lado), style = Stroke(grosor))
                    drawArc(
                        if (fase == Fase.Descanso) EyesColors.VerdeBg else EyesColors.Azul,
                        -90f, 360f * progreso, false, esquina, Size(lado, lado),
                        style = Stroke(grosor, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (fase == Fase.Descanso) EyesLogo(width = 72.dp, color = EyesColors.VerdeBg)
                    Text(formato(restante, fase == Fase.Descanso), style = MaterialTheme.typography.displaySmall, color = EyesColors.Texto)
                    Text(if (fase == Fase.Descanso) "segundos" else "minutos", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
                }
            }

            if (fase == Fase.Detenido) {
                EyesBotonPrincipal("Empezar", { restante = duracionPantalla; fase = Fase.Pantalla }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EyesBotonTexto(if (demo) "✓ Modo prueba (20 s)" else "Modo prueba (20 s)", { demo = !demo; restante = if (demo) 20 else 20 * 60 }, color = EyesColors.Azul)
                }
            } else {
                EyesBotonFantasma("Detener", { fase = Fase.Detenido; restante = duracionPantalla }, modifier = Modifier.fillMaxWidth())
                if (pausas > 0) Text("Llevas $pausas ${if (pausas == 1) "pausa" else "pausas"} hoy. ¡Muy bien!", style = MaterialTheme.typography.bodyMedium, color = EyesColors.VerdeBg)
            }
            if (telefono == null) {
                Text("En el teléfono, además, vibra al terminar cada tramo.", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
            }

            EyesEtiqueta("Ejercicios para tus ojos", EyesColors.TextoTenue)
            ejercicios.forEachIndexed { i, e ->
                EyesTarjeta(Modifier.fillMaxWidth().aparecer(i), onClick = { ejercicio = e }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.nombre, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto)
                            Text("${e.descripcion} ${e.pasos.sumOf { it.second }} segundos.", style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario)
                        }
                        Text("Empezar", style = MaterialTheme.typography.labelLarge, color = EyesColors.Azul)
                    }
                }
            }
        }
    }
}

@Composable
private fun EjercicioGuiado(e: Ejercicio, onTerminar: () -> Unit) {
    val telefono = LocalTelefono.current
    var paso by remember { mutableIntStateOf(0) }
    var segundos by remember { mutableIntStateOf(e.pasos.first().second) }
    var terminado by remember { mutableStateOf(false) }

    LaunchedEffect(e) {
        for ((i, p) in e.pasos.withIndex()) {
            paso = i
            segundos = p.second
            telefono?.vibrar(longArrayOf(0, 60))
            while (segundos > 0) { delay(1000); segundos-- }
        }
        telefono?.vibrar(longArrayOf(0, 300, 150, 300))
        terminado = true
    }
    // El ojo "parpadea": se cierra cuando el paso pide cerrar o cubrir los ojos.
    val texto = e.pasos[paso].first
    val cerrado = listOf("Cierra", "Cubre").any { texto.startsWith(it) }
    val apertura by animateFloatAsState(if (cerrado && !terminado) 0.12f else 1f, tween(350), label = "parpadeo")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Text(e.nombre, style = MaterialTheme.typography.headlineMedium, color = EyesColors.Texto)
        Box(Modifier.size(180.dp).clip(CircleShape).background(EyesColors.Celeste100), contentAlignment = Alignment.Center) {
            EyesLogo(width = 140.dp, color = EyesColors.Azul, modifier = Modifier.graphicsLayerScaleY(apertura))
        }
        EyesTarjetaAcento(fondo = EyesColors.Blanco, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (terminado) "¡Listo! Tus ojos te lo agradecen." else texto,
                style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (!terminado) Text(
                "$segundos s · paso ${paso + 1} de ${e.pasos.size}",
                style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoTenue, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (terminado) EyesBotonPrincipal("Volver", onTerminar, modifier = Modifier.fillMaxWidth())
        else EyesBotonTexto("Salir del ejercicio", onTerminar, color = EyesColors.Azul)
    }
}

private fun Modifier.graphicsLayerScaleY(y: Float) = this.graphicsLayer { scaleY = y }

/** 1200 → "20:00". En descanso solo se muestran los segundos ("15"). */
private fun formato(s: Int, soloSegundos: Boolean = false): String =
    if (soloSegundos) "$s" else "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
