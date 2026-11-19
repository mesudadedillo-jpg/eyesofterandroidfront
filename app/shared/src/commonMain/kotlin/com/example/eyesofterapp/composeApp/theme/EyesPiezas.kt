package com.example.eyesofterapp.composeApp.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.domain.model.Role
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Barra de una pantalla interna: flecha para regresar + título.
// ---------------------------------------------------------------------------

@Composable
fun EyesBarraPantalla(
    titulo: String,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().franjasBarra().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onVolver),
                contentAlignment = Alignment.Center
            ) {
                EyesIcono(EyesIconos.Atras, "Regresar", EyesColors.Blanco)
            }
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                color = EyesColors.Blanco,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            acciones()
        }
    }
}

/** Botón redondo con ícono (para barras y mapas). Mínimo 48 dp para el dedo. */
@Composable
fun EyesBotonIcono(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    color: Color = EyesColors.Blanco,
    fondo: Color = Color.Transparent,
    contador: Int = 0,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(fondo).clickable(onClick = onClick)
            .semantics { contentDescription = if (contador > 0) "$descripcion, $contador sin leer" else descripcion },
        contentAlignment = Alignment.Center
    ) {
        EyesIcono(icono, null, color)
        if (contador > 0) EyesContador(contador, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp))
    }
}

/** Globito rojo con el número de pendientes. */
@Composable
fun EyesContador(n: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.widthIn(min = 18.dp).height(18.dp).clip(CircleShape).background(EyesColors.RojoBg).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(if (n > 99) "99+" else "$n", style = MaterialTheme.typography.labelSmall, color = EyesColors.RojoFg)
    }
}

// ---------------------------------------------------------------------------
// Semáforo (M-RFN-04). C-RC-10: nunca solo color: siempre color + ícono + texto.
// ---------------------------------------------------------------------------

/** Cómo se ve y qué se le explica al tutor según el resultado. Lenguaje claro, sin diagnósticos (M-RFN-03). */
class InfoSemaforo(
    val fondo: Color,
    val texto: Color,
    val icono: ImageVector,
    val titulo: String,
    val explicacion: String,
    val pasos: List<String>,
)

fun infoSemaforo(semaforo: String): InfoSemaforo = when (semaforo.uppercase()) {
    "VERDE" -> InfoSemaforo(
        EyesColors.VerdeBg, EyesColors.VerdeFg, EyesIconos.Bien, "Saludable",
        "En la revisión no encontramos señales que necesiten atención. ¡Buenas noticias! Sigan cuidando su vista.",
        listOf(
            "Que descanse la vista de las pantallas cada 20 minutos (regla 20-20-20).",
            "Que lea y haga tareas con buena luz.",
            "Revisión de la vista una vez al año.",
        )
    )
    "AMARILLO" -> InfoSemaforo(
        EyesColors.AmarilloBg, EyesColors.AmarilloFg, EyesIconos.Atencion, "En observación",
        "Encontramos algo que conviene revisar con calma. No es una urgencia, pero un especialista debe verlo en las próximas semanas.",
        listOf(
            "Agenda una revisión con un especialista en las próximas semanas.",
            "Si eliges una clínica asociada, te atienden con el pase del programa.",
            "Cuando te den la receta o compres los lentes, sube la foto aquí.",
        )
    )
    else -> InfoSemaforo(
        EyesColors.RojoBg, EyesColors.RojoFg, EyesIconos.Urgente, "Atención urgente",
        "Es importante que un especialista revise su vista pronto. Esto no es un diagnóstico: solo indica que no conviene esperar.",
        listOf(
            "Agenda una cita en los próximos días.",
            "Busca una clínica asociada cerca de ti en el directorio.",
            "Cuando te den la receta o compres los lentes, sube la foto aquí.",
        )
    )
}

/** Píldora pequeña del semáforo, para listas. */
@Composable
fun EyesSemaforoPildora(semaforo: String, modifier: Modifier = Modifier) {
    val info = infoSemaforo(semaforo)
    Row(
        modifier.clip(EyesShapes.Pildora).background(info.fondo).padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EyesIcono(info.icono, null, info.texto, 14.dp)
        Spacer(Modifier.size(4.dp))
        Text(info.titulo, style = MaterialTheme.typography.labelMedium, color = info.texto)
    }
}

/** Círculo grande con el ícono del semáforo, para el detalle del resultado. */
@Composable
fun EyesSemaforoCirculo(semaforo: String, modifier: Modifier = Modifier) {
    val info = infoSemaforo(semaforo)
    Box(modifier.size(72.dp).clip(CircleShape).background(info.fondo), contentAlignment = Alignment.Center) {
        EyesIcono(info.icono, info.titulo, info.texto, 40.dp)
    }
}

// ---------------------------------------------------------------------------
// Mensajes y estados
// ---------------------------------------------------------------------------

/**
 * Aviso corto que sube desde abajo y se va solo a los 4 s (como un Snackbar).
 * Va dentro de un Box que ocupa la pantalla.
 */
@Composable
fun BoxScope.EyesMensaje(mensaje: String?, onVisto: () -> Unit) {
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            delay(4000)
            onVisto()
        }
    }
    AnimatedVisibility(
        visible = mensaje != null,
        enter = fadeIn(tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)) +
            slideInVertically(tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)) { it / 2 },
        exit = fadeOut(tween(EyesMotion.Salida)) + slideOutVertically(tween(EyesMotion.Salida)) { it / 2 },
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().clip(EyesShapes.Tarjeta).background(EyesColors.Marino).clickable(onClick = onVisto)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(mensaje.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = EyesColors.Blanco)
        }
    }
}

/** Pantalla o sección sin datos: ícono, título y explicación. */
@Composable
fun EyesVacio(icono: ImageVector, titulo: String, texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(EyesColors.Celeste100), contentAlignment = Alignment.Center) {
            EyesIcono(icono, null, EyesColors.Azul, 32.dp)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto, textAlign = TextAlign.Center)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario, textAlign = TextAlign.Center)
    }
}

/** Nombre del rol como lo conoce la escuela (los requerimientos hablan de Tutor, Enfermería...). */
fun Role.nombreVisible(): String = when (this) {
    Role.ADMINISTRADOR -> "Dirección"
    Role.SUPERVISOR -> "Enfermería"
    Role.CLIENTE -> "Tutor"
    Role.DOCTOR -> "Especialista"
    Role.ALUMNO -> "Alumno"
    Role.DESCONOCIDO -> "Sin rol"
}
