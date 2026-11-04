package com.example.eyesofterapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.eyesofterapp.location.AndroidLocationProvider
import com.example.eyesofterapp.location.ResultadoUbicacion
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

private const val TAG = "GPS_DEBUG"
private const val ZOOM_MINIMO_OPTICAS = 11.0

@Composable
fun PantallaMapa() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Inicialización síncrona de MapLibre antes de cualquier composición de MapView
    remember(context) {
        try {
            MapLibre.getInstance(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando MapLibre: ${e.message}")
        }
    }

    var permisoConcedido by rememberSaveable { mutableStateOf(tienePermiso(context)) }
    var denegadoPermanente by rememberSaveable { mutableStateOf(false) }
    var gpsActivo by rememberSaveable { mutableStateOf(esGpsActivo(context)) }
    var ubicacion by remember { mutableStateOf<LatLng?>(null) }

    val activity = remember(context) { findActivity(context) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        Log.d(TAG, "Resultado launcher permisos -> concedido: $granted")
        permisoConcedido = granted
        gpsActivo = esGpsActivo(context)

        if (!granted && activity != null) {
            denegadoPermanente = !ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) && !ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_COARSE_LOCATION
            )
            Log.d(TAG, "Denegado permanentemente: $denegadoPermanente")
        }

        if (granted && gpsActivo) {
            scope.launch { ubicacion = obtenerUbicacionActual(context) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permisoConcedido = tienePermiso(context)
                gpsActivo = esGpsActivo(context)
                Log.d(TAG, "ON_RESUME - Permiso: $permisoConcedido, GPS: $gpsActivo")
                if (permisoConcedido && gpsActivo && ubicacion == null) {
                    scope.launch { ubicacion = obtenerUbicacionActual(context) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!permisoConcedido) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (gpsActivo) {
            scope.launch { ubicacion = obtenerUbicacionActual(context) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (permisoConcedido && gpsActivo) {
            MapLibreView(ubicacion)
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!permisoConcedido) {
                    Text("Se requiere permiso de ubicación para mostrar el mapa.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (denegadoPermanente) {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Abrir Ajustes de la app")
                        }
                    } else {
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) {
                            Text("Conceder permiso")
                        }
                    }
                } else if (!gpsActivo) {
                    Text("El GPS está apagado.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) {
                        Text("Encender ubicación")
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapLibreView(initialLocation: LatLng?) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    // MapLibre exige llamar onCreate() antes de usar el MapView.
    val mapView = remember(context) { MapView(context).apply { onCreate(null) } }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en ciclo de vida MapView: ${e.message}")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapView.onDestroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destruyendo MapView: ${e.message}")
            }
        }
    }

    // La ubicación suele llegar DESPUÉS de que el mapa se creó: centramos la cámara cuando llegue.
    LaunchedEffect(initialLocation, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val destino = initialLocation ?: return@LaunchedEffect
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(destino).zoom(15.0).tilt(45.0).build()
            )
        )
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            // Mientras llega el GPS, mostramos México en lugar del punto (0,0) en el océano.
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(20.0825, -98.3622))
                .zoom(5.0)
                .build()
            mapLibreMap = map
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                if (tienePermiso(context)) {
                    try {
                        val locationComponent = map.locationComponent
                        locationComponent.activateLocationComponent(
                            LocationComponentActivationOptions.builder(context, style).build()
                        )
                        locationComponent.isLocationComponentEnabled = true
                        locationComponent.cameraMode = CameraMode.TRACKING
                        locationComponent.renderMode = RenderMode.COMPASS
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException activando LocationComponent: ${e.message}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error activando LocationComponent: ${e.message}")
                    }
                }

                initialLocation?.let {
                    try {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(it)
                            .zoom(15.0)
                            .tilt(45.0)
                            .build()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al mover cámara inicial: ${e.message}")
                    }
                }
            }

            map.addOnCameraIdleListener {
                try {
                    // A zoom bajo el área es enorme y Overpass rechaza o tarda la consulta.
                    if (map.cameraPosition.zoom < ZOOM_MINIMO_OPTICAS) return@addOnCameraIdleListener
                    val bounds = map.projection.visibleRegion.latLngBounds
                    scope.launch {
                        cargarOpticas(bounds, map)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en cameraIdleListener: ${e.message}")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                if (tienePermiso(context)) {
                    val loc = try {
                        mapLibreMap?.locationComponent?.lastKnownLocation
                    } catch (e: SecurityException) {
                        null
                    }
                    if (loc != null) {
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        mapLibreMap?.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(latLng)
                                    .zoom(16.0)
                                    .tilt(45.0)
                                    .build()
                            )
                        )
                    } else {
                        scope.launch {
                            val latLng = obtenerUbicacionActual(context)
                            if (latLng != null) {
                                mapLibreMap?.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(latLng)
                                            .zoom(16.0)
                                            .tilt(45.0)
                                            .build()
                                    )
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Mi ubicación")
        }
    }
}

private fun tienePermiso(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

private fun esGpsActivo(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

private fun findActivity(context: Context): ComponentActivity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/** Usa el mismo proveedor robusto que la pantalla de ópticas (gps + network + fused, con timeout). */
private suspend fun obtenerUbicacionActual(context: Context): LatLng? =
    when (val r = AndroidLocationProvider(context).obtenerUbicacion()) {
        is ResultadoUbicacion.Exito -> LatLng(r.coordenadas.lat, r.coordenadas.lng)
        else -> {
            Log.w(TAG, "No se pudo obtener ubicación: $r")
            null
        }
    }

private suspend fun cargarOpticas(bounds: LatLngBounds, map: MapLibreMap) {
    val zona = "(${bounds.southWest.latitude},${bounds.southWest.longitude},${bounds.northEast.latitude},${bounds.northEast.longitude})"
    // Misma búsqueda que OsmApi (app/shared): ópticas, optometristas y oftalmólogos.
    // "out center" da un punto también para edificios (ways/relations).
    val query = """
        [out:json][timeout:25];
        (
          nwr["shop"="optician"]$zona;
          nwr["amenity"="optician"]$zona;
          nwr["healthcare"="optometrist"]$zona;
          nwr["healthcare:speciality"~"ophthalmology|optometry"]$zona;
        );
        out center tags 100;
    """.trimIndent()

    try {
        val client = HttpClient(OkHttp)
        val url = "https://overpass-api.de/api/interpreter?data=${Uri.encode(query)}"
        Log.d(TAG, "Requesting Overpass API...")
        val response: HttpResponse = client.get(url)
        val jsonStr = response.body<String>()
        val json = JSONObject(jsonStr)
        val elements = json.optJSONArray("elements")

        val features = mutableListOf<com.google.gson.JsonObject>()
        if (elements != null) {
            for (i in 0 until elements.length()) {
                val element = elements.optJSONObject(i)
                if (element != null) {
                    val centro = element.optJSONObject("center") ?: element
                    val lat = centro.optDouble("lat", Double.NaN)
                    val lon = centro.optDouble("lon", Double.NaN)
                    if (!lat.isNaN() && !lon.isNaN()) {
                        val tags = element.optJSONObject("tags")
                        val name = tags?.optString("name", "")?.lowercase() ?: ""
                        val isPartner = name.contains("eyesofter")

                        val feature = com.google.gson.JsonObject().apply {
                            addProperty("type", "Feature")
                            val geometry = com.google.gson.JsonObject().apply {
                                addProperty("type", "Point")
                                val coords = com.google.gson.JsonArray()
                                coords.add(lon)
                                coords.add(lat)
                                add("coordinates", coords)
                            }
                            add("geometry", geometry)
                            val properties = com.google.gson.JsonObject().apply {
                                addProperty("isPartner", isPartner)
                                addProperty("name", name)
                            }
                            add("properties", properties)
                        }
                        features.add(feature)
                    }
                }
            }
        }
        
        withContext(Dispatchers.Main) {
            val featureCollection = com.google.gson.JsonObject().apply {
                addProperty("type", "FeatureCollection")
                val featuresArray = com.google.gson.JsonArray()
                features.forEach { featuresArray.add(it) }
                add("features", featuresArray)
            }
            
            val style = map.style
            if (style != null) {
                var source = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("opticas-source")
                if (source == null) {
                    source = org.maplibre.android.style.sources.GeoJsonSource("opticas-source", featureCollection.toString())
                    style.addSource(source)
                    
                    val circleLayer = org.maplibre.android.style.layers.CircleLayer("opticas-layer", "opticas-source")
                        .withProperties(
                            org.maplibre.android.style.layers.PropertyFactory.circleRadius(8f),
                            org.maplibre.android.style.layers.PropertyFactory.circleColor(
                                org.maplibre.android.style.expressions.Expression.switchCase(
                                    org.maplibre.android.style.expressions.Expression.eq(org.maplibre.android.style.expressions.Expression.get("isPartner"), org.maplibre.android.style.expressions.Expression.literal(true)),
                                    org.maplibre.android.style.expressions.Expression.literal("#0000FF"),
                                    org.maplibre.android.style.expressions.Expression.literal("#FF0000")
                                )
                            )
                        )
                    style.addLayer(circleLayer)
                } else {
                    source.setGeoJson(featureCollection.toString())
                }
            }
        }
        client.close()
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching from Overpass: ${e.message}")
    }
}
