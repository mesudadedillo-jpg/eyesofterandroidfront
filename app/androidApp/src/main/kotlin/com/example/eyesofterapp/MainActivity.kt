package com.example.eyesofterapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.eyesofterapp.location.AndroidLocationProvider
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Cámara, archivos, vibración y sonido. Debe crearse antes de onStart (registra lanzadores). */
    private val telefono = TelefonoAndroid(this)
    private val push = PushAndroid(this)

    /** Ruta de la push que tocó el usuario (M-RC-02): la app abre esa pantalla. */
    private val rutaPush = MutableStateFlow<String?>(null)

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
        val alResponder = alResponderPermiso
        alResponderPermiso = null
        alResponder?.invoke(concedido)
    }

    /** Lo usa AndroidLocationProvider cuando el ViewModel pide la ubicación. */
    fun solicitarPermisosUbicacion(alResponder: (Boolean) -> Unit) {
        // Si había una solicitud pendiente, la cerramos para que su corrutina no quede colgada.
        alResponderPermiso?.invoke(false)
        alResponderPermiso = alResponder
        val yaConcedido = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (yaConcedido) avisarResultado(true)
        else pedirUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    /** Lo llama AndroidLocationProvider cuando la ubicación del sistema está apagada. */
    private fun abrirAjustesDeUbicacion() {
        Toast.makeText(this, "La ubicación del teléfono está apagada. Actívala y vuelve a la app.", Toast.LENGTH_LONG).show()
        runCatching { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Íconos BLANCOS en la barra de estado: arriba de cada pantalla va la barra marino.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        crearCanalAvisos(this)
        rutaPush.value = intent?.getStringExtra(EXTRA_RUTA)

        setContent {
            val ubicacion = remember {
                AndroidLocationProvider(
                    context = this,
                    requestPermission = ::solicitarPermisosUbicacion,
                    onUbicacionApagada = ::abrirAjustesDeUbicacion,
                )
            }
            App(
                locationProvider = ubicacion,
                telefono = telefono,
                // En Android el directorio usa el mapa 3D real (MapLibre).
                directorioNativo = { estado, acciones, soloAsociadas, onVolver ->
                    DirectorioMapa(estado, acciones, soloAsociadas, onVolver)
                },
                push = push,
                rutaExterna = rutaPush,
                onRutaAtendida = { rutaPush.value = null },
            )
        }
    }

    /** La app ya estaba abierta y el usuario tocó una push. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_RUTA)?.let { rutaPush.value = it }
    }
}
