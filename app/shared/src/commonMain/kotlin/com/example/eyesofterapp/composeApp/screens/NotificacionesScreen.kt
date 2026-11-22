package com.example.eyesofterapp.composeApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.eyesofterapp.composeApp.theme.EyesBarraPantalla
import com.example.eyesofterapp.composeApp.theme.EyesBotonTexto
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesIcono
import com.example.eyesofterapp.composeApp.theme.EyesIconos
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.composeApp.theme.EyesVacio
import com.example.eyesofterapp.composeApp.theme.aparecer
import com.example.eyesofterapp.data.model.NotificacionDto
import com.example.eyesofterapp.data.model.fechaLegible
import com.example.eyesofterapp.ui.PanelUiState

/** Bandeja: las no leídas resaltadas; al tocar una se marca como leída y lleva a su pantalla. */
@Composable
fun NotificacionesScreen(
    state: PanelUiState,
    onAbrirNotificacion: (NotificacionDto) -> Unit,
    onMarcarTodas: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        EyesBarraPantalla("Notificaciones", onVolver) {
            if (state.bandeja.noLeidas > 0) EyesBotonTexto("Marcar leídas", onMarcarTodas, color = EyesColors.Blanco)
        }
        val lista = state.bandeja.notificaciones
        if (lista.isEmpty()) {
            EyesVacio(EyesIconos.Campana, "Sin notificaciones", "Aquí aparecerán los resultados de la revisión visual y los avisos de la escuela.")
            return
        }
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(lista, key = { _, n -> n.id }) { i, n ->
                FilaNotificacion(n, Modifier.aparecer(i.coerceAtMost(6))) { onAbrirNotificacion(n) }
            }
        }
    }
}

@Composable
private fun FilaNotificacion(n: NotificacionDto, modifier: Modifier, onClick: () -> Unit) {
    val (icono, color) = when (n.tipo) {
        "RESULTADO_TAMIZAJE" -> EyesIconos.Ojo to EyesColors.Azul
        "ALERTA_ENFERMERIA" -> EyesIconos.Atencion to EyesColors.AmarilloFg
        "COMPROBANTE_RECIBIDO", "COMPROBANTE_REVISADO" -> EyesIconos.Documento to EyesColors.Azul
        else -> EyesIconos.Campana to EyesColors.Azul
    }
    EyesTarjeta(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (n.leida) EyesColors.Celeste050 else EyesColors.Celeste100),
                contentAlignment = Alignment.Center
            ) { EyesIcono(icono, null, color) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    n.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (n.leida) EyesColors.TextoSecundario else EyesColors.Texto
                )
                Text(n.mensaje, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
                Text(fechaLegible(n.creadaEn), style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue)
            }
            // Punto azul = sin leer (además del texto más oscuro: no depende solo del color).
            if (!n.leida) Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(EyesColors.Azul))
        }
    }
}
