package com.example.eyesofterapp.composeApp.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Íconos de la app, dibujados con los trazos de Material Icons (licencia Apache 2.0)
 * en una caja de 24 × 24. Así no hace falta la librería material-icons-extended,
 * que pesa varios MB, para usar una docena.
 */
object EyesIconos {
    private fun icono(nombre: String, vararg trazos: String): ImageVector =
        ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
            trazos.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()

    val Campana by lazy {
        icono("campana", "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z")
    }
    val Camara by lazy {
        icono("camara", "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0",
            "M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z")
    }
    val Galeria by lazy {
        icono("galeria", "M22,16V4c0,-1.1 -0.9,-2 -2,-2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2zM11,12l2.03,2.71L16,11l4,5H8l3,-4zM2,6v14c0,1.1 0.9,2 2,2h14v-2H4V6H2z")
    }
    val Lugar by lazy {
        icono("lugar", "M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z")
    }
    val MiUbicacion by lazy {
        icono("miUbicacion", "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM20.94,11c-0.46,-4.17 -3.77,-7.48 -7.94,-7.94V1h-2v2.06C6.83,3.52 3.52,6.83 3.06,11H1v2h2.06c0.46,4.17 3.77,7.48 7.94,7.94V23h2v-2.06c4.17,-0.46 7.48,-3.77 7.94,-7.94H23v-2h-2.06zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z")
    }
    val Bien by lazy {
        icono("bien", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z")
    }
    val Atencion by lazy { icono("atencion", "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z") }
    val Urgente by lazy {
        icono("urgente", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-2h2v2zM13,13h-2V7h2v6z")
    }
    val Ojo by lazy {
        icono("ojo", "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z")
    }
    val Reloj by lazy {
        icono("reloj", "M15,1H9v2h6V1zM11,14h2V8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z")
    }
    val Documento by lazy {
        icono("documento", "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6zM16,18H8v-2h8v2zM16,14H8v-2h8v2zM13,9V3.5L18.5,9H13z")
    }
    val Enviar by lazy { icono("enviar", "M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z") }
    val Atras by lazy { icono("atras", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z") }
    val Telefono by lazy {
        icono("telefono", "M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.25,1.02l-2.2,2.2z")
    }
    val ComoLlegar by lazy {
        icono("comoLlegar", "M21.71,11.29l-9,-9c-0.39,-0.39 -1.02,-0.39 -1.41,0l-9,9c-0.39,0.39 -0.39,1.02 0,1.41l9,9c0.39,0.39 1.02,0.39 1.41,0l9,-9c0.39,-0.38 0.39,-1.01 0,-1.41zM14,14.5V12h-4v3H8v-4c0,-0.55 0.45,-1 1,-1h5V7.5l3.5,3.5 -3.5,3.5z")
    }
    val Grupo by lazy {
        icono("grupo", "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0 -3,1.34 -3,3s1.34,3 3,3zM8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3zM8,13c-2.33,0 -7,1.17 -7,3.5V19h14v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5zM16,13c-0.29,0 -0.62,0.02 -0.97,0.05 1.16,0.84 1.97,1.97 1.97,3.45V19h6v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5z")
    }
    val Brujula by lazy {
        icono("brujula", "M12,10.9c-0.61,0 -1.1,0.49 -1.1,1.1s0.49,1.1 1.1,1.1c0.61,0 1.1,-0.49 1.1,-1.1s-0.49,-1.1 -1.1,-1.1zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM14.19,14.19L6,18l3.81,-8.19L18,6l-3.81,8.19z")
    }
    val Buscar by lazy {
        icono("buscar", "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z")
    }
    val Salir by lazy {
        icono("salir", "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5zM4,5h8V3H4c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h8v-2H4V5z")
    }
    val Mas by lazy { icono("mas", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val Menos by lazy { icono("menos", "M19,13H5v-2h14v2z") }
    val Lista by lazy { icono("lista", "M3,13h2v-2H3v2zM3,17h2v-2H3v2zM3,9h2V7H3v2zM7,13h14v-2H7v2zM7,17h14v-2H7v2zM7,7v2h14V7H7z") }
    val Capas by lazy {
        icono("capas", "M11.99,18.54l-7.37,-5.73L3,14.07l9,7 9,-7 -1.63,-1.27 -7.38,5.74zM12,16l7.36,-5.73L21,9l-9,-7 -9,7 1.63,1.27L12,16z")
    }
}

/** Ícono teñido del color que se pida. */
@Composable
fun EyesIcono(icono: ImageVector, descripcion: String?, color: Color = EyesColors.Marino, tamano: Dp = 24.dp, modifier: Modifier = Modifier) {
    Icon(icono, contentDescription = descripcion, tint = color, modifier = modifier.size(tamano))
}
