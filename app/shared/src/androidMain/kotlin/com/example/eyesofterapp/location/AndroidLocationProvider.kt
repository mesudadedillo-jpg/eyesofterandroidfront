package com.example.eyesofterapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.example.eyesofterapp.domain.model.Coordenadas
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Lee la ubicación con el LocationManager de Android (sin Google Play Services).
 *
 * Cómo funciona:
 * 1. Pide permiso si hace falta.
 * 2. Revisa que la ubicación del teléfono esté encendida.
 * 3. Escucha a la vez los proveedores fused, gps y network. El primero
 *    que dé una lectura con precisión de 50 m o mejor gana. Si llega una lectura menos precisa,
 *    espera unos segundos más por una mejor (el GPS en interiores tarda o no llega).
 * 4. Si en el tiempo límite no llegó nada, usa la última ubicación conocida del sistema.
 * 5. Si tampoco hay, devuelve SinSenal. Ya no inventa coordenadas.
 *
 * En Logcat filtra por la etiqueta "GPS_DEBUG" para ver cada lectura.
 */
class AndroidLocationProvider(
    context: Context,
    private val requestPermission: ((onResult: (Boolean) -> Unit) -> Unit)? = null,
    private val onUbicacionApagada: (() -> Unit)? = null,
    private val timeoutMs: Long = 20_000L
) : LocationProvider {

    private val appContext = context.applicationContext

    private val locationManager by lazy {
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override suspend fun obtenerUbicacion(): ResultadoUbicacion {
        // 1) Permiso
        if (!tienePermiso()) {
            val pedir = requestPermission
            val concedido = if (pedir != null) pedirPermiso(pedir) else false
            Log.d(TAG, "Resultado de la solicitud de permiso: $concedido")
            if (!concedido || !tienePermiso()) return ResultadoUbicacion.SinPermiso
        }

        // 2) Ubicación del sistema encendida
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            Log.w(TAG, "La ubicación del teléfono está apagada")
            onUbicacionApagada?.invoke()
            return ResultadoUbicacion.UbicacionApagada
        }

        // 3) Lectura nueva del sensor
        val fresca = lecturaFresca()
        if (fresca != null) return fresca.aResultado(esUltimaConocida = false)

        // 4) Última conocida
        val ultima = mejorUltimaConocida()
        if (ultima != null) {
            Log.w(TAG, "Sin lectura nueva; usando última conocida (${ultima.provider})")
            return ultima.aResultado(esUltimaConocida = true)
        }

        Log.w(TAG, "No se obtuvo ninguna lectura del sensor")
        return ResultadoUbicacion.SinSenal
    }

    private suspend fun pedirPermiso(pedir: (onResult: (Boolean) -> Unit) -> Unit): Boolean =
        suspendCancellableCoroutine { cont ->
            pedir { concedido -> if (cont.isActive) cont.resume(concedido) }
        }

    private fun tienePermiso(): Boolean = tieneFine() || tieneCoarse()

    private fun tieneFine() = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun tieneCoarse() = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** Proveedores encendidos que la app puede usar con el permiso que tiene. */
    private fun proveedoresUtilizables(): List<String> {
        val encendidos = try {
            locationManager.getProviders(true)
        } catch (e: Exception) {
            emptyList()
        }
        val lista = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            LocationManager.FUSED_PROVIDER in encendidos
        ) {
            lista += LocationManager.FUSED_PROVIDER
        }
        // Antes de Android 12, el proveedor gps exige permiso FINE (con solo COARSE lanza SecurityException).
        if (LocationManager.GPS_PROVIDER in encendidos &&
            (tieneFine() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        ) {
            lista += LocationManager.GPS_PROVIDER
        }
        if (LocationManager.NETWORK_PROVIDER in encendidos) {
            lista += LocationManager.NETWORK_PROVIDER
        }
        Log.d(TAG, "Proveedores encendidos: $encendidos -> usando: $lista (fine=${tieneFine()})")
        return lista
    }

    @SuppressLint("MissingPermission")
    private suspend fun lecturaFresca(): Location? {
        val proveedores = proveedoresUtilizables()
        if (proveedores.isEmpty()) return null

        val canal = Channel<Location>(Channel.UNLIMITED)
        val listener = LocationListenerCompat { loc -> canal.trySend(loc) }
        val peticion = LocationRequestCompat.Builder(1_000L)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(0L)
            .build()
        val executor = ContextCompat.getMainExecutor(appContext)

        var registrados = 0
        for (proveedor in proveedores) {
            try {
                LocationManagerCompat.requestLocationUpdates(
                    locationManager, proveedor, peticion, executor, listener
                )
                registrados++
            } catch (e: SecurityException) {
                Log.e(TAG, "Sin permiso para el proveedor $proveedor: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Proveedor $proveedor no disponible: ${e.message}")
            }
        }
        if (registrados == 0) {
            canal.close()
            return null
        }

        var mejor: Location? = null
        try {
            var limite = SystemClock.elapsedRealtime() + timeoutMs
            while (true) {
                val restante = limite - SystemClock.elapsedRealtime()
                if (restante <= 0) break
                val loc = withTimeoutOrNull(restante) { canal.receive() } ?: break
                Log.d(
                    TAG,
                    "Lectura ${loc.provider}: ${loc.latitude}, ${loc.longitude} ±${loc.accuracy} m"
                )
                if (esMejor(loc, mejor)) mejor = loc
                if (loc.hasAccuracy() && loc.accuracy <= PRECISION_SUFICIENTE_M) break
                // Ya hay una lectura: esperamos un poco más por una mejor, no el tiempo completo.
                limite = minOf(limite, SystemClock.elapsedRealtime() + ESPERA_EXTRA_MS)
            }
        } finally {
            LocationManagerCompat.removeUpdates(locationManager, listener)
            canal.close()
        }
        return mejor
    }

    @SuppressLint("MissingPermission")
    private fun mejorUltimaConocida(): Location? {
        val ahora = SystemClock.elapsedRealtimeNanos()
        val proveedores = try {
            locationManager.getProviders(true)
        } catch (e: Exception) {
            emptyList()
        }
        return proveedores
            .mapNotNull { proveedor ->
                try {
                    locationManager.getLastKnownLocation(proveedor)
                } catch (e: SecurityException) {
                    null
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .filter { (ahora - it.elapsedRealtimeNanos) / 1_000_000L <= EDAD_MAXIMA_ULTIMA_MS }
            .maxByOrNull { it.elapsedRealtimeNanos }
    }

    private fun esMejor(nueva: Location, actual: Location?): Boolean {
        if (actual == null) return true
        val precisionNueva = if (nueva.hasAccuracy()) nueva.accuracy else Float.MAX_VALUE
        val precisionActual = if (actual.hasAccuracy()) actual.accuracy else Float.MAX_VALUE
        return precisionNueva <= precisionActual
    }

    private fun Location.aResultado(esUltimaConocida: Boolean) = ResultadoUbicacion.Exito(
        coordenadas = Coordenadas(latitude, longitude),
        precisionMetros = if (hasAccuracy()) accuracy else null,
        proveedor = provider ?: "desconocido",
        esUltimaConocida = esUltimaConocida
    )

    private companion object {
        const val TAG = "GPS_DEBUG"
        const val PRECISION_SUFICIENTE_M = 50f
        const val ESPERA_EXTRA_MS = 6_000L
        const val EDAD_MAXIMA_ULTIMA_MS = 60 * 60 * 1000L // 1 hora
    }
}
