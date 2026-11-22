package com.example.eyesofterapp.composeApp.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ===========================================================================
// MOVIMIENTO DE EYESOFTER
//
// Reglas (filosofía de animación de Emil Kowalski, skill "animate"):
//  - Solo se anima lo que tiene un propósito: dar respuesta al toque, mostrar
//    de dónde viene algo, o evitar que el contenido "salte" de golpe.
//  - Solo se animan transformaciones (posición, escala) y opacidad: no
//    obligan a recalcular el diseño, así que van fluidas.
//  - Las entradas usan "ease-out" fuerte: arrancan rápido y frenan suave.
//    Nunca "ease-in" (empieza lento justo cuando el usuario está mirando).
//  - Nada aparece desde escala 0: se empieza en 0.95-0.97 con opacidad 0.
//  - Interfaz por debajo de 300 ms. Los rebotes (spring) solo en la marca.
//  - Si el usuario desactiva las animaciones en Android ("Escala de duración
//    de animación" en Opciones de desarrollador o "Quitar animaciones" en
//    Accesibilidad), Compose respeta esa escala y estas animaciones se acortan.
// ===========================================================================

object EyesMotion {
    /** ease-out fuerte para entradas: cubic-bezier(0.23, 1, 0.32, 1). */
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** ease-in-out fuerte para cosas que se mueven en pantalla: cubic-bezier(0.77, 0, 0.175, 1). */
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /** "ease" de CSS, para cambios de color: cubic-bezier(0.25, 0.1, 0.25, 1). */
    val Ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    const val Presion = 140        // respuesta al presionar un botón o tarjeta
    const val Color = 150          // cambio de color de filtros y selectores
    const val Entrada = 260        // tarjetas y bloques que aparecen
    const val Pantalla = 220       // cambio de pantalla
    const val Salida = 150         // lo que se va, más rápido que lo que llega
    const val Escalon = 60         // retraso entre elementos de una lista (30-80 ms)
}

/**
 * Entrada de un bloque al aparecer por primera vez: sube 12 dp y pasa de
 * transparente a visible. Con [indice] se escalonan los elementos de una
 * lista (cada uno empieza 60 ms después del anterior), para que no entren
 * todos de golpe.
 *
 * Usa graphicsLayer: solo cambia cómo se DIBUJA el elemento, no su tamaño
 * ni su lugar en el diseño, así que no mueve a los demás.
 */
@Composable
fun Modifier.aparecer(indice: Int = 0, desplazamiento: Dp = 12.dp, retrasoExtraMs: Int = 0): Modifier {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((indice * EyesMotion.Escalon + retrasoExtraMs).toLong())
        progreso.animateTo(1f, tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut))
    }
    val px = with(LocalDensity.current) { desplazamiento.toPx() }
    return this.graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * px
    }
}

/**
 * Entrada de la marca (el logotipo del login): crece de 0.9 a 1 con un
 * resorte de rebote bajo. Es el único lugar con rebote: se ve una vez por
 * sesión, así que aquí sí cabe un poco de "vida".
 */
@Composable
fun Modifier.aparecerMarca(retrasoMs: Int = 0): Modifier {
    val escala = remember { Animatable(0.9f) }
    val opacidad = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(retrasoMs.toLong())
        // La opacidad no rebota: sube con ease-out mientras la escala usa el resorte.
        coroutineScope {
            launch { opacidad.animateTo(1f, tween(EyesMotion.Entrada, easing = EyesMotion.EaseOut)) }
            escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    return this.graphicsLayer {
        alpha = opacidad.value
        scaleX = escala.value
        scaleY = escala.value
    }
}

/**
 * Respuesta al toque: mientras el dedo está encima, el elemento se encoge
 * a 97 % en 140 ms, y regresa al soltar. Confirma que la app "escuchó".
 * Se pasa el mismo interactionSource que usa el botón o el clickable.
 */
@Composable
fun Modifier.presionable(interactionSource: InteractionSource, escalaPresionado: Float = 0.97f): Modifier {
    val presionado by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) escalaPresionado else 1f,
        animationSpec = tween(EyesMotion.Presion, easing = EyesMotion.EaseOut),
        label = "presion"
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}
