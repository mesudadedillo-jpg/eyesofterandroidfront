package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraSuperior
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesPildora
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesTarjetaAcento
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.domain.model.User

/**
 * Estructura común de los paneles por rol:
 *  - Barra superior con la marca y "Salir".
 *  - Encabezado: píldora con el rol, título grande y saludo.
 *  - Las tarjetas que cada rol pasa en "content".
 */
@Composable
fun RoleScaffold(
    title: String,
    user: User,
    onLogout: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        EyesBarraSuperior(accion = "Salir", onAccion = onLogout)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // El encabezado entra escalonado (rol → título → saludo) al abrir el panel.
            EyesPildora(
                texto = user.role.name.lowercase().replaceFirstChar { it.uppercase() },
                fondo = EyesColors.Celeste100,
                textoColor = EyesColors.Marino,
                conPunto = false,
                modifier = Modifier.aparecer(indice = 0)
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = EyesColors.Texto,
                modifier = Modifier.aparecer(indice = 1)
            )
            Text(
                "Hola, ${user.fullName}",
                style = MaterialTheme.typography.bodyLarge,
                color = EyesColors.TextoSecundario,
                modifier = Modifier.aparecer(indice = 2)
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * Tarjeta de una sección del panel.
 *  - Sin acción: tarjeta blanca con borde fino.
 *  - Con acción (onClick): panel de acento celeste con el botón principal,
 *    porque es lo que el usuario puede hacer en esta pantalla.
 * [orden]: posición de la tarjeta (0, 1, 2...) para que entren una tras otra,
 * después del encabezado (que ocupa los turnos 0 a 2).
 */
@Composable
fun InfoCard(title: String, description: String, onClick: (() -> Unit)? = null, orden: Int = 0) {
    val entrada = Modifier.fillMaxWidth().aparecer(indice = orden + 3)
    if (onClick == null) {
        EyesTarjeta(entrada) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
        }
    } else {
        EyesTarjetaAcento(fondo = EyesColors.Celeste200, modifier = entrada) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = EyesColors.Texto)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = EyesColors.Marino700)
            Spacer(Modifier.height(4.dp))
            EyesBotonPrincipal("Ver ópticas cercanas", onClick = onClick)
        }
    }
}

@Composable
fun AdminScreen(user: User, onLogout: () -> Unit, onVerOpticas: () -> Unit = {}) =
    RoleScaffold("Panel de Administración", user, onLogout) {
        InfoCard("Clínicas y Ópticas", "Directorio de consultorios y ópticas cercanas.", onClick = onVerOpticas, orden = 0)
        InfoCard("Dashboard", "Alumnos evaluados, casos abiertos y tiempo promedio de atención.", orden = 1)
        InfoCard("Usuarios", "Alta, edición y baja de usuarios del sistema.", orden = 2)
    }

@Composable
fun SupervisorScreen(user: User, onLogout: () -> Unit, onVerOpticas: () -> Unit = {}) =
    RoleScaffold("Panel del Supervisor", user, onLogout) {
        InfoCard("Directorio", "Ver ópticas cercanas registradas.", onClick = onVerOpticas, orden = 0)
        InfoCard("Casos abiertos", "Seguimiento de los casos pendientes por grado y grupo.", orden = 1)
        InfoCard("Comprobantes", "Validar o rechazar los comprobantes enviados.", orden = 2)
    }

@Composable
fun ClienteScreen(user: User, onLogout: () -> Unit, onVerOpticas: () -> Unit = {}) =
    RoleScaffold("Mi espacio", user, onLogout) {
        InfoCard("Directorio", "Clínicas y ópticas cercanas.", onClick = onVerOpticas, orden = 0)
        InfoCard("Reporte", "Resultado en formato semáforo, en lenguaje claro.", orden = 1)
        InfoCard("Comprobantes", "Sube la receta o el comprobante de lentes.", orden = 2)
    }

@Composable
fun DoctorScreen(user: User, onLogout: () -> Unit, onVerOpticas: () -> Unit = {}) =
    RoleScaffold("Panel del Doctor", user, onLogout) {
        InfoCard("Ópticas", "Directorio de ópticas asociadas.", onClick = onVerOpticas, orden = 0)
        InfoCard("Escanear pase", "Lee el código QR del pase médico del paciente.", orden = 1)
        InfoCard("Atenciones", "Registro de las atenciones brindadas.", orden = 2)
    }

@Composable
fun AlumnoScreen(user: User, onLogout: () -> Unit, onVerOpticas: () -> Unit = {}) =
    RoleScaffold("Cuida tu vista", user, onLogout) {
        InfoCard("Ópticas Cercanas", "Encuentra ópticas y clínicas de atención visual.", onClick = onVerOpticas, orden = 0)
        InfoCard("Regla 20-20-20", "Cada 20 minutos, mira algo a 20 pies (6 m) durante 20 segundos.", orden = 1)
        InfoCard("Ejercicios", "Parpadeo, enfoque y relajación ocular.", orden = 2)
    }
