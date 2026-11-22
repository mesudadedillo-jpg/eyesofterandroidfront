package com.example.eyesofterapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.eyesofterapp.composeApp.navigation.AccionesDirectorio
import com.example.eyesofterapp.composeApp.screens.ordenDirectorio
import com.example.eyesofterapp.composeApp.theme.EyesBotonFantasma
import com.example.eyesofterapp.composeApp.theme.EyesBotonPrincipal
import com.example.eyesofterapp.composeApp.theme.EyesColors
import com.example.eyesofterapp.composeApp.theme.EyesIcono
import com.example.eyesofterapp.composeApp.theme.EyesIconos
import com.example.eyesofterapp.composeApp.theme.EyesOpcion
import com.example.eyesofterapp.composeApp.theme.EyesPildora
import com.example.eyesofterapp.composeApp.theme.EyesSelector
import com.example.eyesofterapp.composeApp.theme.EyesSelectorOpcion
import com.example.eyesofterapp.composeApp.theme.EyesShapes
import com.example.eyesofterapp.composeApp.theme.EyesTarjeta
import com.example.eyesofterapp.domain.model.Coordenadas
import com.example.eyesofterapp.domain.model.FUENTE_OSM
import com.example.eyesofterapp.domain.model.Optica
import com.example.eyesofterapp.domain.model.distanciaHaversineKm
import com.example.eyesofterapp.ui.OpticasUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/** Estilo de OpenFreeMap: gratis, sin API key, con edificios en 3D al inclinar el mapa. */
private const val ESTILO = "https://tiles.openfreemap.org/styles/liberty"
private val TULANCINGO = Coordenadas(20.0825, -98.3622)
private const val INCLINACION_3D = 60.0

private enum class Filtro { Todas, Asociadas, Cerca }

/** Una clínica en el mapa: las del programa (catálogo del servidor) y las de OpenStreetMap. */
private data class Clinica(val clave: String, val optica: Optica, val deOsm: Boolean)

/**
 * Directorio de clínicas con mapa 3D (MapLibre). Reemplaza en Android a
 * OpticasScreen. Ver la explicación en el README ("El mapa del directorio").
 */
@SuppressLint("MissingPermission")
@Composable
fun DirectorioMapa(
    estado: OpticasUiState,
    acciones: AccionesDirectorio,
    soloAsociadas: Boolean,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    remember(context) { MapLibre.getInstance(context) }

    var filtro by rememberSaveable { mutableStateOf(if (soloAsociadas) Filtro.Asociadas else Filtro.Todas) }
    var verLista by rememberSaveable { mutableStateOf(false) }
    var osm by remember { mutableStateOf<List<Optica>>(emptyList()) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    var errorMapa by remember { mutableStateOf(false) }
    var en3D by rememberSaveable { mutableStateOf(true) }
    var rumbo by remember { mutableFloatStateOf(0f) }
    var irAMiUbicacion by remember { mutableStateOf(false) }

    val usuario = estado.coordenadasGps
    val ubicacionReal = usuario != null && !estado.esUbicacionSimulada

    // Al abrir: pide la ubicación (y el permiso, si falta). Sin permiso, el ViewModel usa Tulancingo.
    LaunchedEffect(Unit) {
        if (estado.opticas.isEmpty() && !estado.cargando) acciones.usarGps()
    }
    // Ópticas reales de OpenStreetMap alrededor del usuario (complementan el catálogo del programa).
    LaunchedEffect(usuario) {
        if (usuario != null) osm = withContext(Dispatchers.IO) { buscarOpticasOsm(usuario) }
    }

    val clinicas = remember(estado.opticas, osm, filtro, usuario) {
        val catalogo = estado.opticas.map { o ->
            val d = if (o.distanciaKm > 0.0 || usuario == null) o.distanciaKm else distanciaHaversineKm(usuario.lat, usuario.lng, o.lat, o.lng)
            o.copy(distanciaKm = d)
        }
        // Si una óptica de OSM está a menos de 80 m de una del catálogo, es la misma.
        val extra = osm.filter { e -> catalogo.none { distanciaHaversineKm(it.lat, it.lng, e.lat, e.lng) < 0.08 } }
        // OpticasApi ya mezcla las del servidor con las de OpenStreetMap (fuente = FUENTE_OSM).
        val todas = catalogo.map { if (it.fuente == FUENTE_OSM) Clinica("s${it.id}", it, true) else Clinica("c${it.id}", it, false) } +
            extra.map { Clinica("o${it.id}", it, true) }
        todas.distinctBy { it.clave }
            .filter {
                when (filtro) {
                    Filtro.Todas -> true
                    Filtro.Asociadas -> it.optica.verificada
                    Filtro.Cerca -> it.optica.distanciaKm in 0.001..5.0
                }
            }.let { lista ->
                val orden = lista.map { it.optica }.ordenDirectorio()
                orden.map { o -> lista.first { it.optica === o } }
            }
    }

    val pager = rememberPagerState { clinicas.size }
    // El listener de toques del mapa se registra una sola vez: lee siempre la lista vigente.
    val clinicasVigentes by rememberUpdatedState(clinicas)
    val seleccion = clinicas.getOrNull(pager.currentPage)

    // Pines: se redibujan al cambiar la lista o la selección.
    LaunchedEffect(clinicas, seleccion?.clave, estiloListo) {
        if (estiloListo) mapa?.style?.let { actualizarPines(it, clinicas, seleccion?.clave) }
    }
    // Al deslizar las tarjetas, la cámara vuela a la clínica elegida.
    LaunchedEffect(pager.settledPage, clinicas, estiloListo) {
        val c = clinicas.getOrNull(pager.settledPage) ?: return@LaunchedEffect
        if (estiloListo && !verLista) volarA(mapa, c.optica.lat, c.optica.lng, 16.5, en3D)
    }
    // "Mi ubicación": cuando llega la ubicación pedida, se centra ahí.
    LaunchedEffect(usuario, irAMiUbicacion, estiloListo) {
        if (irAMiUbicacion && usuario != null && estiloListo) {
            volarA(mapa, usuario.lat, usuario.lng, 15.5, en3D)
            irAMiUbicacion = false
        }
    }
    // El punto azul del usuario, en cuanto haya permiso.
    LaunchedEffect(ubicacionReal, estiloListo) {
        if (ubicacionReal && estiloListo) activarPuntoUsuario(context, mapa)
    }

    Box(Modifier.fillMaxSize().background(EyesColors.Fondo)) {
        MapaLibre(
            centro = usuario ?: TULANCINGO,
            alListo = { m ->
                mapa = m
                m.addOnCameraMoveListener { rumbo = m.cameraPosition.bearing.toFloat() }
                m.addOnMapClickListener { punto ->
                    val pantalla = m.projection.toScreenLocation(punto)
                    val tocado = m.queryRenderedFeatures(pantalla, "pines", "pin-elegido").firstOrNull()
                    val i = clinicasVigentes.indexOfFirst { it.clave == tocado?.getStringProperty("clave") }
                    if (i >= 0) scope.launch { pager.animateScrollToPage(i) }
                    i >= 0
                }
            },
            alCargarEstilo = { estiloListo = true; errorMapa = false },
            alFallar = { errorMapa = true },
            margenInferiorDp = if (verLista) 16 else 230,
        )

        // ---------------- Arriba: regresar, buscar y filtros ----------------
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonFlotante(EyesIconos.Atras, "Regresar", onClick = onVolver)
                CampoBusqueda(
                    texto = estado.busquedaTexto,
                    onTexto = acciones.actualizarTexto,
                    onBuscar = { focus.clearFocus(); acciones.buscar(estado.busquedaTexto) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EyesSelector {
                    EyesSelectorOpcion("Mapa", !verLista) { verLista = false }
                    EyesSelectorOpcion("Lista", verLista) { verLista = true }
                }
                EyesOpcion("Todas", filtro == Filtro.Todas, { filtro = Filtro.Todas })
                EyesOpcion("✓ Asociadas", filtro == Filtro.Asociadas, { filtro = Filtro.Asociadas })
                if (ubicacionReal) EyesOpcion("A menos de 5 km", filtro == Filtro.Cerca, { filtro = Filtro.Cerca })
            }
        }

        if (!verLista) {
            // ---------------- Derecha: controles del mapa ----------------
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotonFlotanteTexto(if (en3D) "2D" else "3D", if (en3D) "Ver plano" else "Ver en 3D") {
                    en3D = !en3D
                    mapa?.let { m ->
                        m.animateCamera(CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder(m.cameraPosition).tilt(if (en3D) INCLINACION_3D else 0.0).build()), 700)
                    }
                }
                BotonFlotante(EyesIconos.Brujula, "Apuntar al norte", modifier = Modifier.rotate(-rumbo - 45f)) {
                    mapa?.let { m ->
                        m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).bearing(0.0).build()), 500)
                    }
                }
                BotonFlotante(EyesIconos.Mas, "Acercar") { mapa?.animateCamera(CameraUpdateFactory.zoomIn(), 300) }
                BotonFlotante(EyesIconos.Menos, "Alejar") { mapa?.animateCamera(CameraUpdateFactory.zoomOut(), 300) }
                BotonFlotante(EyesIconos.MiUbicacion, "Mi ubicación", destacado = ubicacionReal) {
                    irAMiUbicacion = true
                    if (ubicacionReal) {
                        // Ya hay ubicación: se centra de una vez.
                        usuario?.let { volarA(mapa, it.lat, it.lng, 15.5, en3D) }
                        irAMiUbicacion = false
                    } else {
                        acciones.usarGps()
                    }
                }
            }

            // ---------------- Abajo: aviso de ubicación + tarjetas ----------------
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
                if (!ubicacionReal && !estado.cargando) {
                    AvisoUbicacion(
                        sinPermiso = estado.sinPermisoUbicacion,
                        onActivar = acciones.usarGps,
                        onAjustes = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (clinicas.isEmpty()) {
                    TarjetaMensaje(if (estado.cargando) "Buscando clínicas..." else "No hay clínicas con este filtro.")
                } else {
                    HorizontalPager(
                        state = pager,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { pagina ->
                        TarjetaClinica(clinicas[pagina], posicion = "${pagina + 1} de ${clinicas.size}")
                    }
                }
            }
        } else {
            // ---------------- Lista ----------------
            LazyColumn(
                Modifier.fillMaxSize().padding(top = 128.dp).statusBarsPadding().background(EyesColors.Fondo),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(clinicas, key = { _, c -> c.clave }) { i, c ->
                    TarjetaClinica(c, posicion = null, onVerEnMapa = {
                        verLista = false
                        scope.launch { pager.scrollToPage(i) }
                    })
                }
            }
        }

        if (estado.cargando) {
            CircularProgressIndicator(color = EyesColors.Azul, modifier = Modifier.align(Alignment.Center).size(40.dp))
        }
        AnimatedVisibility(errorMapa && !verLista, enter = fadeIn(tween(200)), exit = fadeOut(tween(150)), modifier = Modifier.align(Alignment.Center)) {
            TarjetaMensaje("No se pudo cargar el mapa. Revisa tu conexión; mientras, usa la vista de Lista.")
        }
    }
}

// ---------------------------------------------------------------------------
// MapView
// ---------------------------------------------------------------------------

@Composable
private fun MapaLibre(
    centro: Coordenadas,
    alListo: (MapLibreMap) -> Unit,
    alCargarEstilo: () -> Unit,
    alFallar: () -> Unit,
    margenInferiorDp: Int,
) {
    val context = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val densidad = LocalDensity.current.density
    val mapView = remember(context) { MapView(context).apply { onCreate(null) } }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(ciclo, mapView) {
        val observador = LifecycleEventObserver { _, evento ->
            runCatching {
                when (evento) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> {}
                }
            }
        }
        ciclo.lifecycle.addObserver(observador)
        // Si la pantalla ya estaba activa al abrir el mapa, arranca de inmediato.
        if (ciclo.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (ciclo.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            ciclo.lifecycle.removeObserver(observador)
            // Primero se apaga el punto del usuario (sus animaciones leían un estilo ya destruido).
            // Sin permiso de ubicación el punto nunca se activó: no hay nada que apagar.
            val conUbicacion = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                .any { mapView.context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            if (conUbicacion) runCatching { mapa?.locationComponent?.let { if (it.isLocationComponentActivated) it.isLocationComponentEnabled = false } }
            runCatching { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
        }
    }

    LaunchedEffect(mapView) {
        mapView.addOnDidFailLoadingMapListener { alFallar() }
        mapView.getMapAsync { m ->
            mapa = m
            m.uiSettings.isCompassEnabled = false // hay brújula propia
            m.uiSettings.isRotateGesturesEnabled = true
            m.uiSettings.isTiltGesturesEnabled = true
            m.cameraPosition = CameraPosition.Builder()
                .target(LatLng(centro.lat, centro.lng)).zoom(15.0).tilt(INCLINACION_3D).bearing(-20.0).build()
            m.setStyle(ESTILO) { estilo ->
                agregarIconos(context, estilo)
                alCargarEstilo()
            }
            alListo(m)
        }
    }

    // Logo y créditos de OpenStreetMap siempre visibles, arriba de las tarjetas.
    LaunchedEffect(mapa, margenInferiorDp) {
        mapa?.uiSettings?.let { ui ->
            val abajo = (margenInferiorDp * densidad).roundToInt()
            val lado = (12 * densidad).roundToInt()
            ui.logoGravity = Gravity.BOTTOM or Gravity.START
            ui.setLogoMargins(lado, 0, 0, abajo)
            ui.attributionGravity = Gravity.BOTTOM or Gravity.START
            ui.setAttributionMargins((100 * densidad).roundToInt(), 0, 0, abajo)
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

private fun volarA(mapa: MapLibreMap?, lat: Double, lng: Double, zoom: Double, en3D: Boolean) {
    val m = mapa ?: return
    // Un giro ligero al llegar hace que se note el relieve de los edificios.
    val rumbo = (m.cameraPosition.bearing + 12.0) % 360.0
    m.animateCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder().target(LatLng(lat, lng)).zoom(zoom)
                .tilt(if (en3D) INCLINACION_3D else 0.0).bearing(if (en3D) rumbo else 0.0).build()
        ),
        1400
    )
}

@SuppressLint("MissingPermission")
private fun activarPuntoUsuario(context: Context, mapa: MapLibreMap?) {
    val m = mapa ?: return
    val estilo = m.style ?: return
    val permiso = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (!permiso) return
    runCatching {
        val lc = m.locationComponent
        if (!lc.isLocationComponentActivated) {
            val opciones = LocationComponentOptions.builder(context).pulseEnabled(true).build()
            lc.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, estilo).locationComponentOptions(opciones).build()
            )
        }
        lc.isLocationComponentEnabled = true
        lc.cameraMode = CameraMode.NONE // el usuario mueve el mapa libremente
        lc.renderMode = RenderMode.COMPASS
    }
}

// ---------------------------------------------------------------------------
// Pines
// ---------------------------------------------------------------------------

private const val PIN_ASOCIADA = "pin-asociada"
private const val PIN_EXTERNA = "pin-externa"
private const val PIN_OSM = "pin-osm"

private fun agregarIconos(context: Context, estilo: Style) {
    val d = context.resources.displayMetrics.density
    estilo.addImage(PIN_ASOCIADA, dibujarPin(d, 0xFF1D7A3A.toInt(), palomita = true))
    estilo.addImage(PIN_EXTERNA, dibujarPin(d, 0xFF0B47A8.toInt(), palomita = false))
    estilo.addImage(PIN_OSM, dibujarPin(d * 0.75f, 0xFF8FA3C4.toInt(), palomita = false))

    val fuente = GeoJsonSource("clinicas", FeatureCollection.fromFeatures(emptyList()))
    estilo.addSource(fuente)

    // Columnas 3D translúcidas sobre cada clínica: se ven de lejos con el mapa inclinado.
    // Más altas y verdes las asociadas. Van debajo de los pines.
    estilo.addSource(GeoJsonSource("columnas", FeatureCollection.fromFeatures(emptyList())))
    estilo.addLayer(
        FillExtrusionLayer("columnas", "columnas").withProperties(
            PropertyFactory.fillExtrusionColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.fillExtrusionHeight(Expression.get("altura")),
            PropertyFactory.fillExtrusionBase(0f),
            PropertyFactory.fillExtrusionOpacity(0.4f),
        )
    )
    fun capa(id: String, elegido: Boolean) = SymbolLayer(id, "clinicas").withProperties(
        PropertyFactory.iconImage(Expression.get("icono")),
        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
        PropertyFactory.iconSize(if (elegido) 1.45f else 1f),
        PropertyFactory.textField(Expression.get("nombre")),
        PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
        PropertyFactory.textSize(if (elegido) 14f else 12f),
        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
        PropertyFactory.textOffset(arrayOf(0f, 0.3f)),
        PropertyFactory.textColor("#0A1A3F"),
        PropertyFactory.textHaloColor("#FFFFFF"),
        PropertyFactory.textHaloWidth(1.6f),
        PropertyFactory.textOptional(true),
        PropertyFactory.textMaxWidth(9f),
        // Los nombres aparecen al acercarse, para no llenar el mapa de texto.
        PropertyFactory.textOpacity(
            if (elegido) Expression.literal(1f)
            else Expression.step(Expression.zoom(), Expression.literal(0f), Expression.stop(13.5, 1f))
        ),
    ).withFilter(Expression.eq(Expression.get("elegido"), Expression.literal(elegido)))
    // El pin elegido va en su propia capa, encima de los demás.
    estilo.addLayer(capa("pines", elegido = false))
    estilo.addLayer(capa("pin-elegido", elegido = true))
}

private fun actualizarPines(estilo: Style, clinicas: List<Clinica>, elegida: String?) {
    estilo.getSourceAs<GeoJsonSource>("columnas")?.setGeoJson(FeatureCollection.fromFeatures(clinicas.map { c ->
        val asociada = c.optica.verificada
        val (altura, radio, color) = when {
            asociada -> Triple(60.0, 32.0, "#1D7A3A")
            c.deOsm -> Triple(18.0, 16.0, "#8FA3C4")
            else -> Triple(30.0, 22.0, "#0B47A8")
        }
        Feature.fromGeometry(circulo(c.optica.lat, c.optica.lng, radio)).apply {
            addNumberProperty("altura", if (c.clave == elegida) altura + 25 else altura)
            addStringProperty("color", color)
        }
    }))
    val fuente = estilo.getSourceAs<GeoJsonSource>("clinicas") ?: return
    val features = clinicas.map { c ->
        Feature.fromGeometry(Point.fromLngLat(c.optica.lng, c.optica.lat)).apply {
            addStringProperty("clave", c.clave)
            addStringProperty("nombre", c.optica.nombre)
            addStringProperty("icono", if (c.deOsm) PIN_OSM else if (c.optica.verificada) PIN_ASOCIADA else PIN_EXTERNA)
            addBooleanProperty("elegido", c.clave == elegida)
        }
    }
    fuente.setGeoJson(FeatureCollection.fromFeatures(features))
}

/** Polígono de 20 lados que aproxima un círculo de [radioM] metros (la base de la columna). */
private fun circulo(lat: Double, lng: Double, radioM: Double): Polygon {
    val puntos = (0..20).map { i ->
        val a = i / 20.0 * 2 * Math.PI
        Point.fromLngLat(
            lng + radioM / (111_320.0 * Math.cos(Math.toRadians(lat))) * Math.cos(a),
            lat + radioM / 110_540.0 * Math.sin(a)
        )
    }
    return Polygon.fromLngLats(listOf(puntos))
}

/** Pin en forma de gota: círculo de color con borde blanco y punta abajo. Asociadas: palomita blanca. */
private fun dibujarPin(densidad: Float, color: Int, palomita: Boolean): Bitmap {
    val w = (34 * densidad).roundToInt()
    val h = (44 * densidad).roundToInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val r = w / 2f - 2 * densidad
    val cx = w / 2f
    val cy = r + 2 * densidad
    val gota = Path().apply {
        addCircle(cx, cy, r, Path.Direction.CW)
        moveTo(cx - r * 0.62f, cy + r * 0.78f)
        lineTo(cx, h - 1.5f * densidad)
        lineTo(cx + r * 0.62f, cy + r * 0.78f)
        close()
    }
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    // Sombra suave para que el pin "flote" sobre el mapa 3D.
    pincel.color = 0x33000000
    c.save(); c.translate(0f, 1.5f * densidad); c.drawPath(gota, pincel); c.restore()
    pincel.color = 0xFFFFFFFF.toInt()
    pincel.style = Paint.Style.STROKE
    pincel.strokeWidth = 3 * densidad
    c.drawPath(gota, pincel)
    pincel.style = Paint.Style.FILL
    pincel.color = color
    c.drawPath(gota, pincel)
    pincel.color = 0xFFFFFFFF.toInt()
    if (palomita) {
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 3.2f * densidad
        pincel.strokeCap = Paint.Cap.ROUND
        pincel.strokeJoin = Paint.Join.ROUND
        c.drawPath(Path().apply {
            moveTo(cx - r * 0.45f, cy + r * 0.02f)
            lineTo(cx - r * 0.1f, cy + r * 0.38f)
            lineTo(cx + r * 0.48f, cy - r * 0.32f)
        }, pincel)
    } else {
        c.drawCircle(cx, cy, r * 0.36f, pincel)
    }
    return bmp
}

// ---------------------------------------------------------------------------
// OpenStreetMap (Overpass): ópticas reales alrededor del usuario
// ---------------------------------------------------------------------------

private fun buscarOpticasOsm(centro: Coordenadas, radioM: Int = 8000): List<Optica> = runCatching {
    val consulta = """
        [out:json][timeout:20];
        (
          nwr["shop"="optician"](around:$radioM,${centro.lat},${centro.lng});
          nwr["healthcare"="optometrist"](around:$radioM,${centro.lat},${centro.lng});
          nwr["amenity"="optician"](around:$radioM,${centro.lat},${centro.lng});
        );
        out center 40;
    """.trimIndent()
    val conexion = URL("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(consulta, "UTF-8"))
        .openConnection() as HttpURLConnection
    conexion.connectTimeout = 10_000
    conexion.readTimeout = 20_000
    conexion.setRequestProperty("User-Agent", "EyesofterApp/1.0")
    val texto = conexion.inputStream.bufferedReader().use { it.readText() }
    val elementos = JSONObject(texto).optJSONArray("elements") ?: return emptyList()
    (0 until elementos.length()).mapNotNull { i ->
        val e = elementos.optJSONObject(i) ?: return@mapNotNull null
        val lat = e.optDouble("lat", e.optJSONObject("center")?.optDouble("lat") ?: Double.NaN)
        val lng = e.optDouble("lon", e.optJSONObject("center")?.optDouble("lon") ?: Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return@mapNotNull null
        val tags = e.optJSONObject("tags")
        val calle = listOfNotNull(tags?.optString("addr:street")?.takeIf { it.isNotBlank() }, tags?.optString("addr:housenumber")?.takeIf { it.isNotBlank() }).joinToString(" ")
        Optica(
            id = i + 1,
            nombre = tags?.optString("name")?.takeIf { it.isNotBlank() } ?: "Óptica",
            direccion = calle.ifBlank { "Dirección no registrada" },
            ciudad = tags?.optString("addr:city").orEmpty(),
            lat = lat,
            lng = lng,
            distanciaKm = distanciaHaversineKm(centro.lat, centro.lng, lat, lng),
            telefono = (tags?.optString("phone")?.takeIf { it.isNotBlank() } ?: tags?.optString("contact:phone")).orEmpty(),
            verificada = false,
        )
    }
}.getOrDefault(emptyList())

// ---------------------------------------------------------------------------
// Piezas de interfaz
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaClinica(c: Clinica, posicion: String?, onVerEnMapa: (() -> Unit)? = null) {
    val context = LocalContext.current
    val o = c.optica
    EyesTarjeta(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(o.nombre, style = MaterialTheme.typography.titleMedium, color = EyesColors.Texto, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when {
                    o.verificada -> EyesPildora("✓ Asociada al programa", EyesColors.VerdeBg, EyesColors.VerdeFg, conPunto = false)
                    c.deOsm -> EyesPildora("OpenStreetMap", EyesColors.Celeste050, EyesColors.TextoSecundario)
                    else -> EyesPildora("Externa", EyesColors.Celeste100, EyesColors.Marino)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (o.distanciaKm > 0.0) Text(distancia(o.distanciaKm), style = MaterialTheme.typography.titleMedium, color = if (o.verificada) EyesColors.VerdeBg else EyesColors.Azul)
                posicion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoTenue) }
            }
        }
        Text(
            listOf(o.direccion, o.ciudad).filter { it.isNotBlank() }.joinToString(", "),
            style = MaterialTheme.typography.bodySmall, color = EyesColors.TextoSecundario, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        if (o.verificada) Text("Te atienden con el pase médico del programa.", style = MaterialTheme.typography.bodySmall, color = EyesColors.VerdeBg)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val comoLlegar = {
                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${o.lat},${o.lng}")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                Unit
            }
            if (o.verificada) EyesBotonPrincipal("Cómo llegar", comoLlegar, modifier = Modifier.weight(1f))
            else EyesBotonFantasma("Cómo llegar", comoLlegar, modifier = Modifier.weight(1f))
            if (o.telefono.isNotBlank()) {
                EyesBotonFantasma("Llamar", {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${o.telefono}"))) }
                })
            }
            onVerEnMapa?.let { EyesBotonFantasma("Mapa", it) }
        }
    }
}

@Composable
private fun AvisoUbicacion(sinPermiso: Boolean, onActivar: () -> Unit, onAjustes: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(EyesShapes.Tarjeta).background(EyesColors.Marino).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EyesIcono(EyesIconos.MiUbicacion, null, EyesColors.Celeste, 22.dp)
        Text(
            "Ubicación aproximada (Tulancingo). Activa la tuya para ver qué clínica te queda más cerca.",
            style = MaterialTheme.typography.bodySmall, color = EyesColors.Blanco,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
        Text(
            if (sinPermiso) "Ajustes" else "Activar",
            style = MaterialTheme.typography.labelLarge, color = EyesColors.Celeste,
            modifier = Modifier.clip(EyesShapes.Boton).clickable { if (sinPermiso) onAjustes() else onActivar() }.padding(8.dp)
        )
    }
}

@Composable
private fun TarjetaMensaje(texto: String) {
    Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(EyesShapes.Tarjeta).background(EyesColors.Blanco).border(1.dp, EyesColors.Borde, EyesShapes.Tarjeta).padding(16.dp)) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoSecundario)
    }
}

@Composable
private fun CampoBusqueda(texto: String, onTexto: (String) -> Unit, onBuscar: () -> Unit, modifier: Modifier) {
    Row(
        modifier.height(48.dp).clip(EyesShapes.Pildora).background(EyesColors.Blanco).border(1.dp, EyesColors.Borde, EyesShapes.Pildora).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EyesIcono(EyesIconos.Buscar, null, EyesColors.TextoTenue, 20.dp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (texto.isEmpty()) Text("Ciudad, colonia o C.P.", style = MaterialTheme.typography.bodyMedium, color = EyesColors.TextoTenue)
            BasicTextField(
                value = texto,
                onValueChange = onTexto,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = EyesColors.Texto),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (texto.isNotBlank()) onBuscar() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BotonFlotante(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    descripcion: String,
    destacado: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(if (destacado) EyesColors.Azul else EyesColors.Blanco)
            .border(1.dp, EyesColors.Borde, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        EyesIcono(icono, descripcion, if (destacado) EyesColors.Blanco else EyesColors.Marino, modifier = modifier)
    }
}

@Composable
private fun BotonFlotanteTexto(texto: String, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(EyesColors.Marino).clickable(onClickLabel = descripcion, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

private fun distancia(km: Double): String =
    if (km < 1.0) "${(km * 1000).roundToInt()} m" else "${((km * 10).roundToInt() / 10.0)} km"
