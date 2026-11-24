package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.data.network.VersionPublicada

/**
 * Pantalla que bloquea la app hasta instalar la versión más reciente. No tiene "omitir":
 * los cambios de seguridad y de datos deben llegar a todos.
 */
@Composable
fun ActualizarScreen(version: VersionPublicada, versionInstalada: String) {
    val uri = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().background(EyesColors.Fondo).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EyesTarjeta(Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Actualiza la app", style = MaterialTheme.typography.headlineMedium, color = EyesColors.Texto)
            Text(
                "Hay una versión nueva de EYESOFTER (${version.versionName.ifBlank { version.versionCode.toString() }}). " +
                    "Para seguir usándola, instala la actualización. Tus datos y tu sesión se conservan.",
                style = MaterialTheme.typography.bodyLarge, color = EyesColors.TextoSecundario,
            )
            if (version.notas.isNotBlank()) {
                Text("Novedades: ${version.notas}", style = MaterialTheme.typography.bodyMedium, color = EyesColors.Texto)
            }
            EyesBotonPrincipal(
                "Descargar e instalar",
                onClick = { uri.openUri(version.apk.ifBlank { "https://github.com/mesudadedillo-jpg/eyesofterandroidfront/releases/latest/download/eyesofter.apk" }) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Al terminar la descarga, abre el archivo y pulsa «Actualizar». Tienes la versión $versionInstalada.",
                style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
          }
        }
    }
}
