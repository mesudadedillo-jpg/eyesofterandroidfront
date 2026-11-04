package com.example.eyesofterapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.eyesofterapp.location.AndroidLocationProvider

class MainActivity : ComponentActivity() {

    private var onPermissionResultCallback: ((Boolean) -> Unit)? = null
    private var negacionesSeguidas = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Basta con FINE o COARSE (en Android 12+ el usuario puede elegir "Aproximada").
        val granted = permissions.values.any { it }
        if (granted) {
            negacionesSeguidas = 0
        } else {
            negacionesSeguidas++
            val puedeVolverAPreguntar =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                )
            if (!puedeVolverAPreguntar && negacionesSeguidas >= 2) {
                // Android ya no muestra el diálogo: hay que activarlo a mano en Ajustes.
                Toast.makeText(
                    this,
                    "Permiso de ubicación bloqueado. Actívalo en Ajustes > Permisos > Ubicación.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            } else {
                Toast.makeText(this, "❌ Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
        val callback = onPermissionResultCallback
        onPermissionResultCallback = null
        callback?.invoke(granted)
    }

    /** Lo llama AndroidLocationProvider cuando no hay permiso. */
    fun solicitarPermisosUbicacion(callback: (Boolean) -> Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            callback(true)
            return
        }

        // Si había una solicitud pendiente, la cerramos para que su corrutina no quede colgada.
        onPermissionResultCallback?.invoke(false)
        onPermissionResultCallback = callback
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /** Lo llama AndroidLocationProvider cuando la ubicación del sistema está apagada. */
    private fun abrirAjustesDeUbicacion() {
        Toast.makeText(this, "📍 La ubicación está apagada. Actívala y vuelve a la app.", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val locationProvider = remember {
                AndroidLocationProvider(
                    context = this,
                    requestPermission = ::solicitarPermisosUbicacion,
                    onUbicacionApagada = ::abrirAjustesDeUbicacion
                )
            }
            
            var showMap by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                if (showMap) {
                    PantallaMapa()
                } else {
                    App(locationProvider = locationProvider)
                }

                FloatingActionButton(
                    onClick = { showMap = !showMap },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp, 16.dp, 16.dp, 100.dp)
                ) {
                    Text(if (showMap) "App" else "Mapa")
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
