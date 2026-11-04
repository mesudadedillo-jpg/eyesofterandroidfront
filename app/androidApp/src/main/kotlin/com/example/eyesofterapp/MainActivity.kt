package com.example.eyesofterapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.eyesofterapp.composeApp.theme.EyeSofterTheme
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.location.AndroidLocationProvider

class MainActivity : ComponentActivity() {

    private var onPermissionResultCallback: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            checkGpsAndReturn(true)
        } else {
            // Evaluamos el caso "Denegado Permanentemente"
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (!shouldShowRationale) {
                Toast.makeText(this, "Permiso denegado permanentemente. Actívalo en Ajustes.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
            onPermissionResultCallback?.invoke(false)
            onPermissionResultCallback = null
        }
    }

    private fun checkGpsAndReturn(permissionGranted: Boolean) {
        if (permissionGranted) {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "La ubicación está apagada. Actívala para continuar.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                onPermissionResultCallback?.invoke(false)
            } else {
                Toast.makeText(this, "Ubicación activada", Toast.LENGTH_SHORT).show()
                onPermissionResultCallback?.invoke(true)
            }
        } else {
            onPermissionResultCallback?.invoke(false)
        }
        onPermissionResultCallback = null
    }

    fun solicitarPermisosUbicacion(callback: (Boolean) -> Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        onPermissionResultCallback = callback

        if (fineGranted || coarseGranted) {
            checkGpsAndReturn(true)
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // SystemBarStyle.dark: íconos BLANCOS en la barra de estado, porque arriba
        // de cada pantalla va el marino con las bandas de EYESOFTER.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        setContent {
            val locationProvider = remember {
                AndroidLocationProvider(
                    context = this,
                    requestPermission = ::solicitarPermisosUbicacion
                )
            }
            
            var showMap by remember { mutableStateOf(false) }

            // El tema envuelve TODO, para que la pantalla de MapLibre también use
            // la paleta y la tipografía de EYESOFTER (antes quedaba con el estilo por defecto).
            EyeSofterTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showMap) {
                        PantallaMapa()
                    } else {
                        App(locationProvider = locationProvider)
                    }

                    // Botón flotante para cambiar entre la app y el mapa MapLibre:
                    // píldora marino, plana (sin sombra), como las del resto de la app.
                    FloatingActionButton(
                        onClick = { showMap = !showMap },
                        shape = EyesShapes.Pildora,
                        containerColor = EyesColors.Marino,
                        contentColor = EyesColors.Blanco,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp, 16.dp, 16.dp, 100.dp)
                    ) {
                        Text(
                            if (showMap) "Volver a la app" else "Mapa en vivo",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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
