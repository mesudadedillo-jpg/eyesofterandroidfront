package com.example.eyesofterapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.eyesofterapp.domain.model.Coordenadas
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val context: Context,
    private val requestPermission: ((onResult: (Boolean) -> Unit) -> Unit)? = null
) : LocationProvider {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @SuppressLint("MissingPermission")
    override suspend fun obtenerUbicacion(): Coordenadas? {
        val permisoConcedido = suspendCancellableCoroutine<Boolean> { continuation ->
            val fineGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) {
                continuation.resume(true)
            } else if (requestPermission != null) {
                requestPermission.invoke { resultGranted ->
                    if (continuation.isActive) {
                        continuation.resume(resultGranted)
                    }
                }
            } else {
                continuation.resume(false)
            }
        }

        if (!permisoConcedido) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isGpsEnabled && !isNetworkEnabled) {
                    continuation.resume(Coordenadas(20.0825, -98.3622)) // Default
                    return@suspendCancellableCoroutine
                }

                val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    locationManager.getCurrentLocation(
                        provider,
                        null,
                        context.mainExecutor
                    ) { location ->
                        if (location != null) {
                            continuation.resume(Coordenadas(location.latitude, location.longitude))
                        } else {
                            fallbackToLastKnown(provider, continuation)
                        }
                    }
                } else {
                    fallbackToLastKnown(provider, continuation)
                }

            } catch (e: SecurityException) {
                continuation.resume(null)
            } catch (e: Exception) {
                continuation.resume(Coordenadas(20.0825, -98.3622))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLastKnown(provider: String, continuation: kotlinx.coroutines.CancellableContinuation<Coordenadas?>) {
        try {
            val lastLocation = locationManager.getLastKnownLocation(provider)
            if (lastLocation != null) {
                continuation.resume(Coordenadas(lastLocation.latitude, lastLocation.longitude))
            } else {
                continuation.resume(Coordenadas(20.0825, -98.3622)) // Default fallback
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }
}
