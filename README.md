# EYESOFTER Suite — App (Kotlin Multiplatform)

App móvil de **EYESOFTER Suite**, el proyecto escolar de tamizaje visual para la
Escuela Primaria-Secundaria METANOIA (Tulancingo, Hgo.). Permite iniciar sesión
según el rol del usuario y buscar ópticas cercanas con el GPS del teléfono y un
mapa (MapLibre).

- **Repositorio:** `https://github.com/mesudadedillo-jpg/eyesofterandroidfront`
- **Servidor que usa:** proyecto `eyesofter-backend`, repositorio
  `https://github.com/mesudadedillo-jpg/eyesofterandroid`, desplegado en
  `https://eyesofterandroid-production.up.railway.app`
- **Contrato con el servidor:** [`docs/API.md`](docs/API.md)

Hasta noviembre de 2026 la app y el servidor vivían juntos en un solo proyecto
Gradle (`eyesofterandroid`). Se separaron para que cada uno se compile y
despliegue por su cuenta. Ahora se comunican **solo por HTTP**.

## Tecnologías

Kotlin 2.4.20 · Compose Multiplatform 1.12.1 · Ktor Client 3.6.0 ·
AGP 9.1.1 · MapLibre 11.6.1 · Gradle 9.5.1

Android: `compileSdk 37`, `targetSdk 36`, `minSdk 24`.
**`targetSdk` se dejó en 36 a propósito** por el permiso de red local de
Android 17; no lo subas sin revisar eso.

## Estructura de carpetas

```
eyesofter-app/
├── settings.gradle.kts         Módulos: :app:shared, :app:androidApp, :app:desktopApp, :app:webApp
├── build.gradle.kts            Declara los plugins (sin aplicarlos)
├── gradle/libs.versions.toml   Versiones de todas las librerías (sin nada del servidor)
├── gradlew / gradlew.bat       Ejecutan Gradle (bash / cmd)
├── kotlin-js-store/            Versiones fijas de paquetes npm para la versión web
├── docs/API.md                 Contrato con el servidor
└── app/
    ├── shared/                 Código común: lo usan TODAS las plataformas
    │   └── src/
    │       ├── commonMain/kotlin/com/example/eyesofterapp/
    │       │   ├── App.kt, AppContainer.kt   Pantalla raíz y creación de objetos (API, ViewModels)
    │       │   ├── composeApp/
    │       │   │   ├── navigation/           Rutas entre pantallas (AppNavHost, Screen)
    │       │   │   ├── screens/              LoginScreen, RoleScreens, OpticasScreen, MapaOpticasView
    │       │   │   └── theme/                Colores y tipografía
    │       │   ├── ui/                       ViewModels y estados (AuthViewModel, OpticasViewModel…)
    │       │   ├── domain/                   Reglas del negocio sin red ni pantallas
    │       │   │   ├── model/                Session, User, Role, Optica, Coordenadas
    │       │   │   ├── repository/           Interfaz AuthRepository
    │       │   │   └── usecase/              LoginUseCase
    │       │   ├── data/
    │       │   │   ├── network/              Cliente HTTP y llamadas al servidor
    │       │   │   │   ├── PlatformConfig.kt   ← BASE_URL (URL del servidor)
    │       │   │   │   ├── HttpClientFactory.kt  Ktor Client con JSON, timeouts y logs
    │       │   │   │   ├── AuthApi.kt          POST /api/login
    │       │   │   │   ├── OpticasApi.kt       GET /opticas/cercanas y /salud (+ catálogo de respaldo)
    │       │   │   │   └── ResultadoRed.kt     Resultado tipado: Exito, Timeout, SinConexion…
    │       │   │   ├── model/AuthDtos.kt     Copia del contrato del login
    │       │   │   ├── repository/           AuthRepositoryImpl
    │       │   │   └── cache/                DiskOpticasCache (última lista de ópticas)
    │       │   └── location/LocationProvider.kt  Interfaz común del GPS: obtenerUbicacion()
    │       ├── androidMain/   AndroidLocationProvider (LocationManager, GPS o red)
    │       ├── iosMain/ jsMain/ jvmMain/ wasmJsMain/   Código propio de cada plataforma
    │       └── commonTest/    AuthRepositoryTest
    ├── androidApp/            Entrada Android: MainActivity, PantallaMapa (MapLibre), AndroidManifest
    ├── desktopApp/            Entrada de escritorio (Windows/Mac/Linux)
    ├── webApp/                Entrada web (JS y Wasm)
    └── iosApp/                Proyecto de Xcode (solo se compila en Mac)
```

### El sensor de ubicación

- `LocationProvider` (común) define **qué** se pide: `obtenerUbicacion(): ResultadoUbicacion`,
  que puede ser `Exito` (con coordenadas, precisión en metros y proveedor), `SinPermiso`,
  `UbicacionApagada` o `SinSenal`. Ya **no** se inventan coordenadas: si el GPS falla, la
  pantalla lo dice y solo usa Tulancingo cuando se marca como *GPS simulado*.
- `AndroidLocationProvider` define **cómo** en Android (sin Google Play Services):
  1. pide el permiso en tiempo de ejecución (basta FINE o COARSE),
  2. revisa que la ubicación del teléfono esté encendida,
  3. escucha **a la vez** los proveedores `fused` (Android 12+), `gps` y `network`;
     gana la primera lectura con precisión ≤ 50 m, o la mejor que llegue en 20 s,
  4. si no llega nada, usa la última ubicación conocida (máx. 1 hora de antigüedad).
- Para depurar en un teléfono real: en Logcat filtra por la etiqueta **`GPS_DEBUG`**;
  verás cada lectura con su proveedor y precisión.
- `AndroidManifest.xml` declara `INTERNET`, `ACCESS_FINE_LOCATION`,
  `ACCESS_COARSE_LOCATION` y el GPS como **no obligatorio**.

## Cómo ejecutarlo en Windows

### Requisito para la terminal

Si `java -version` dice `1.8`, usa el Java de Android Studio en Git Bash:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

También necesitas `local.properties` con la ruta del Android SDK (Android
Studio lo crea solo al abrir el proyecto):

```
sdk.dir=C\:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
```

### Android (recomendado: Android Studio)

1. **File → Open** → carpeta `eyesofter-app` → esperar el *Gradle sync*.
2. Elegir la configuración **androidApp** y el emulador (por ejemplo Pixel).
3. **Run ▶**.

Desde Git Bash:

```bash
./gradlew :app:androidApp:assembleDebug
```

El APK queda en `app/androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

**Probar el GPS en el emulador:** ⋮ (*Extended controls*) → **Location** →
Lat `20.0825`, Lng `-98.3622` → **Set Location**.

### Otras plataformas

| Plataforma | Comando |
|---|---|
| Escritorio | `./gradlew :app:desktopApp:run` |
| Web (Wasm) | `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` |
| Web (JS) | `./gradlew :app:webApp:jsBrowserDevelopmentRun` |
| iOS | Abrir `app/iosApp` en Xcode (requiere Mac) |

### Pruebas

```bash
./gradlew :app:shared:jvmTest
```

## Cómo se conecta con el servidor

Toda la app usa **una sola URL**, definida en
`app/shared/src/commonMain/kotlin/com/example/eyesofterapp/data/network/PlatformConfig.kt`:

```kotlin
const val BASE_URL = "https://eyesofterandroid-production.up.railway.app"
```

| Para probar contra… | Valor de `BASE_URL` |
|---|---|
| Railway (normal) | `https://eyesofterandroid-production.up.railway.app` |
| Tu PC, desde el emulador | `http://10.0.2.2:8080` |
| Tu PC, desde un teléfono físico | `http://<IP-de-tu-PC>:8080` (verla con `ipconfig`) |

`10.0.2.2` es la forma en que el emulador llama a la computadora que lo
ejecuta (`localhost` dentro del emulador es el propio teléfono virtual).
Para correr el servidor local, ve al proyecto `eyesofter-backend` y ejecuta
`./gradlew run`. **Regresa `BASE_URL` a Railway antes de hacer commit.**

La app y el servidor **no comparten código**, solo el contrato de
`docs/API.md`. Si cambia un DTO (`data/model/AuthDtos.kt`,
`domain/model/Optica.kt`) o una ruta, hay que cambiar también el servidor y
las dos copias de `API.md`.

Si el servidor no responde o devuelve una lista vacía, `OpticasApi` muestra un
catálogo local de respaldo con 8 ópticas.

## Pendientes conocidos

- `OpticasApi.calcularHaversine` usa `acos` donde la fórmula lleva `asin`: en
  el catálogo de respaldo las distancias salen mal y el orden queda invertido.
  (El servidor ya lo calcula bien.)
- `PantallaMapa.kt` usa `LocalLifecycleOwner` de un paquete obsoleto (advertencia de compilación).
- `applicationId` es `com.example.eyesofterapp`; Google Play no acepta `com.example`.
- iOS no se ha compilado desde la separación (requiere Mac).

Verificación de subida mediante conexión SSH (2026-11-04 09:14)
