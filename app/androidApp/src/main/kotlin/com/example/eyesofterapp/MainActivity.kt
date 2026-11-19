package com.example.eyesofterapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.eyesofterapp.location.AndroidLocationProvider

class MainActivity : ComponentActivity() {

    /** Cámara, archivos, vibración y sonido. Debe crearse antes de onStart (registra lanzadores). */
    private val telefono = TelefonoAndroid(this)

    private var alResponderPermiso: ((Boolean) -> Unit)? = null

    private val pedirUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos.values.any { it }
        if (!concedido) {
            // Sin permiso la app sigue funcionando: el mapa usa Tulancingo y ofrece abrir Ajustes.
            Toast.makeText(this, "Sin permiso de ubicación: se muestra Tulancingo.", Toast.LENGTH_SHORT).show()
        }
        avisarResultado(concedido)
    }

    private fun avisarResultado(concedido: Boolean) {
        if (concedido) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!LocationManagerCompat.isLocationEnabled(lm)) {
                Toast.makeText(this, "La ubicación del teléfono está apagada. Actívala desde los ajustes rápidos.", Toast.LENGTH_LONG).show()
            }
        }
        alResponderPermiso?.invoke(concedido)
        alResponderPermiso = null
    }

    /** Lo usa AndroidLocationProvider cuando el ViewModel pide la ubicación. */
    fun solicitarPermisosUbicacion(alResponder: (Boolean) -> Unit) {
        alResponderPermiso = alResponder
        val yaConcedido = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (yaConcedido) avisarResultado(true)
        else pedirUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Íconos BLANCOS en la barra de estado: arriba de cada pantalla va la barra marino.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        setContent {
            val ubicacion = remember { AndroidLocationProvider(context = this, requestPermission = ::solicitarPermisosUbicacion) }
            App(
                locationProvider = ubicacion,
                telefono = telefono,
                // En Android el directorio usa el mapa 3D real (MapLibre).
                directorioNativo = { estado, acciones, soloAsociadas, onVolver ->
                    DirectorioMapa(estado, acciones, soloAsociadas, onVolver)
                },
            )
        }
    }
}
