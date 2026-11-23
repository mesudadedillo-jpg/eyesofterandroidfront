package com.example.eyesofterapp

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Aviso de nueva versión. El flujo "Publicar APK" sube a cada release un `version.json`
 * con el versionCode publicado; si es mayor que el instalado se muestra el botón "Actualizar",
 * que descarga el APK nuevo (Android lo instala encima porque va firmado con la misma llave).
 */
private const val REPO = "https://github.com/mesudadedillo-jpg/eyesofterandroidfront/releases/latest/download"
private const val URL_VERSION = "$REPO/version.json"
private const val URL_APK = "$REPO/eyesofter.apk"

data class VersionPublicada(
    val versionCode: Long,
    val versionName: String,
    val notas: String = "",
)

private fun versionInstalada(context: Context): Long {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
    else @Suppress("DEPRECATION") info.versionCode.toLong()
}

/** Devuelve la versión publicada solo si es más nueva que la instalada; null si no hay o falla la red. */
suspend fun buscarActualizacion(context: Context): VersionPublicada? = withContext(Dispatchers.IO) {
    // En compilaciones de prueba (versionCode 1) no tiene sentido avisar.
    if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return@withContext null
    runCatching {
        val conexion = (URL(URL_VERSION).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            useCaches = false
        }
        try {
            if (conexion.responseCode != 200) return@runCatching null
            val o = JSONObject(conexion.inputStream.bufferedReader().readText())
            val publicada = VersionPublicada(o.getLong("versionCode"), o.getString("versionName"), o.optString("notas"))
            publicada.takeIf { it.versionCode > versionInstalada(context) }
        } finally {
            conexion.disconnect()
        }
    }.getOrNull()
}

@Composable
fun AvisoActualizacion(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nueva by remember { mutableStateOf<VersionPublicada?>(null) }
    LaunchedEffect(Unit) { nueva = buscarActualizacion(context) }
    val version = nueva ?: return

    Card(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2545), contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Hay una nueva versión (${version.versionName})", fontWeight = FontWeight.Bold)
            if (version.notas.isNotBlank()) Text(version.notas, Modifier.padding(top = 4.dp))
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                TextButton(onClick = { nueva = null }) { Text("Más tarde", color = Color.White) }
                Button(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_APK)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text("Actualizar") }
            }
        }
    }
}
