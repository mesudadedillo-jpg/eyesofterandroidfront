# EYESOFTER Suite — App (Kotlin Multiplatform)

App móvil de **EYESOFTER Suite**, el proyecto escolar de tamizaje visual para la
Escuela Primaria-Secundaria METANOIA (Tulancingo, Hgo.).

| Rol | Qué puede hacer |
|---|---|
| **Tutor** (`CLIENTE`) | Ver el resultado de la revisión visual de sus hijos (semáforo con color, ícono y texto, en lenguaje claro), saber qué hacer, buscar una clínica asociada en el mapa 3D y subir la foto de la receta o del comprobante de lentes |
| **Enfermería** (`SUPERVISOR`) y **Dirección** (`ADMINISTRADOR`) | Ver qué familias ya leyeron su resultado, revisar (aprobar/rechazar) comprobantes y mandar avisos a los tutores |
| **Alumno** | Pausa 20-20-20 con vibración y sonido, y ejercicios guiados para los ojos |
| **Especialista** (`DOCTOR`) | Directorio de clínicas (el escaneo del pase QR está pendiente) |

Todos tienen la bandeja de notificaciones (con globito de pendientes) y el directorio de clínicas.
La sesión se guarda en el teléfono: no hay que escribir la contraseña cada vez.

- **Repositorio:** `https://github.com/mesudadedillo-jpg/eyesofterandroidfront`
- **Servidor que usa:** proyecto `eyesofter-backend`, repositorio
  `https://github.com/mesudadedillo-jpg/eyesofterandroid`, desplegado en
  `https://eyesofter-api.up.railway.app`
- **Contrato con el servidor:** [`docs/API.md`](docs/API.md)

Hasta noviembre de 2026 la app y el servidor vivían juntos en un solo proyecto
Gradle (`eyesofterandroid`). Se separaron para que cada uno se compile y
despliegue por su cuenta. Ahora se comunican **solo por HTTP**.

## Tecnologías

Kotlin 2.4.20 · Compose Multiplatform 1.12.1 · Ktor Client 3.6.0 ·
AGP 9.1.1 · MapLibre 11.6.1 (mapas de OpenFreeMap) · Gradle 9.5.1

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
    │       │   │   ├── navigation/           Pila de pantallas (AppNavHost) y Screen; "Atrás" regresa
    │       │   │   ├── screens/              Login, Inicio (un panel por rol), Notificaciones, Tamizaje,
    │       │   │   │                         PersonalScreens (comprobantes, seguimiento, avisos),
    │       │   │   │                         PausaVisual (20-20-20), Opticas + MapaOpticasView (mapa ilustrado)
    │       │   │   └── theme/                Colores, tipografía, componentes, íconos (EyesIconos), semáforo (EyesPiezas)
    │       │   ├── ui/                       ViewModels: AuthViewModel (sesión), PanelViewModel (datos del rol), OpticasViewModel
    │       │   ├── plataforma/Telefono.kt    Lo que se pide al teléfono: cámara, archivos, vibración, sonido
    │       │   ├── domain/                   Reglas del negocio sin red ni pantallas
    │       │   │   ├── model/                Session, User, Role, Optica, Coordenadas
    │       │   │   ├── repository/           Interfaz AuthRepository
    │       │   │   └── usecase/              LoginUseCase
    │       │   ├── data/
    │       │   │   ├── network/              Cliente HTTP y llamadas al servidor
    │       │   │   │   ├── PlatformConfig.kt   ← BASE_URL (URL del servidor)
    │       │   │   │   ├── HttpClientFactory.kt  Ktor Client con JSON, timeouts y logs
    │       │   │   │   ├── AuthApi.kt          POST /api/login
    │       │   │   │   ├── EyesApi.kt          Rutas con sesión (Bearer): notificaciones, tamizajes, comprobantes…
    │       │   │   │   ├── OpticasApi.kt       GET /opticas/cercanas y /salud (+ catálogo de respaldo)
    │       │   │   │   └── ResultadoRed.kt     Resultado tipado: Exito, Timeout, SinConexion…
    │       │   │   ├── model/AuthDtos.kt     Copia del contrato del login
    │       │   │   ├── model/EyesDtos.kt     Copia del contrato de notificaciones, tamizajes y comprobantes
    │       │   │   ├── session/              SesionGuardada: token y usuario en el teléfono
    │       │   │   ├── repository/           AuthRepositoryImpl
    │       │   │   └── cache/                DiskOpticasCache (última lista de ópticas)
    │       │   └── location/LocationProvider.kt  Interfaz común del GPS: obtenerUbicacion()
    │       ├── androidMain/   AndroidLocationProvider (LocationManager, GPS o red)
    │       ├── iosMain/ jsMain/ jvmMain/ wasmJsMain/   Código propio de cada plataforma
    │       └── commonTest/    AuthRepositoryTest
    ├── androidApp/            Entrada Android: MainActivity, DirectorioMapa (mapa 3D), TelefonoAndroid, AndroidManifest
    ├── desktopApp/            Entrada de escritorio + capturas/Capturas.kt (genera PNG de las pantallas)
    ├── webApp/                Entrada web (JS y Wasm)
    └── iosApp/                Proyecto de Xcode (solo se compila en Mac)
```

### El mapa del directorio (Android)

`androidApp/.../DirectorioMapa.kt`, con MapLibre y el estilo gratuito de
OpenFreeMap (no necesita API key):

- **3D:** arranca inclinado a 60° y girado. Con dos dedos se gira (rotar) y se
  inclina (deslizar hacia arriba o abajo). Los edificios que tiene OpenStreetMap
  salen en relieve, y cada clínica tiene una **columna 3D translúcida**: alta y
  verde si está asociada al programa, más baja y azul si es externa.
- **Pines propios:** verde con palomita = asociada; azul = externa; gris =
  óptica encontrada en OpenStreetMap cerca de ti. El pin elegido crece. Los
  nombres aparecen al acercarse.
- **Tarjetas abajo:** se deslizan de lado; al cambiar de tarjeta la cámara
  "vuela" a esa clínica. Tocar un pin lleva a su tarjeta. Cada tarjeta tiene
  distancia, dirección, **Cómo llegar** (Google Maps) y **Llamar**.
- **Orden:** primero las asociadas y luego por cercanía (M-RFN-07).
- **Controles:** 2D/3D, brújula (vuelve al norte), acercar, alejar y mi ubicación.
- **Filtros y búsqueda:** Todas / Asociadas / A menos de 5 km, y búsqueda por
  ciudad, colonia o C.P. También hay vista de **Lista**.
- **Sin permiso de ubicación** el mapa funciona igual, centrado en Tulancingo,
  con un aviso para activarla (M-RFNN-05).

En escritorio y web se usa el mapa ilustrado de `MapaOpticasView` (sin MapLibre).

### Acceso al teléfono

| Qué | Para qué | Cómo (Android) | Permiso |
|---|---|---|---|
| Ubicación | Clínicas cercanas | `AndroidLocationProvider` | Se pide al abrir el directorio |
| Cámara | Fotografiar la receta o el comprobante (M-RI-04) | La app de cámara del teléfono (`TakePicture` + FileProvider) | Ninguno |
| Archivos | Elegir una foto o PDF ya guardado | Selector del sistema (`OpenDocument`) | Ninguno |
| Vibración y sonido | Aviso de la pausa 20-20-20 (M-RI-06) | `Vibrator`, `RingtoneManager` | `VIBRATE` (no se pregunta) |
| Pantalla encendida | Mientras corre la pausa o un ejercicio | `FLAG_KEEP_SCREEN_ON` | Ninguno |

Las fotos se reducen a 1600 px y JPEG 80 % antes de subirse (M-RC-05). Las
notificaciones push (Firebase) están pendientes; mientras, la app revisa la
bandeja al iniciar sesión y cada minuto con la app abierta.

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

### Capturas de las pantallas

Genera un PNG de cada pantalla con datos de ejemplo (sin emulador) en
`app/desktopApp/build/capturas/`:

```bash
./gradlew :app:desktopApp:capturas
```

## Cómo se conecta con el servidor

Toda la app usa **una sola URL**, definida en
`app/shared/src/commonMain/kotlin/com/example/eyesofterapp/data/network/PlatformConfig.kt`:

```kotlin
const val BASE_URL = "https://eyesofter-api.up.railway.app"
```

| Para probar contra… | Valor de `BASE_URL` |
|---|---|
| Railway (normal) | `https://eyesofter-api.up.railway.app` |
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

Las cuentas son las del **portal web** (`eyesofterweb`): la app inicia sesión con el correo y la
contraseña que Dirección dio de alta allá. El portal y el servidor comparten la base de datos.

Si el servidor no responde, el directorio muestra lo último guardado.

## Qué hace la app frente a la especificación

| Requisito | Dónde |
|---|---|
| M-RFN-01/02 Inicio de sesión con las cuentas del portal (correo) y aviso de resultado | `LoginScreen.kt`, push `PushAndroid.kt` |
| M-RFN-03/04 Resultado con semáforo (color + texto + icono) | `TamizajeScreen.kt` |
| M-RFN-05 / M-RI-04 Foto de la receta o comprobante | `TamizajeScreen.kt`, `TelefonoAndroid.kt` |
| M-RFN-06/07 / M-RI-03/05 Mapa 3D con asociadas primero e insignia | `DirectorioMapa.kt` |
| M-RFN-08 Pedir cita | `ExpedienteScreens.kt` → `SolicitarCitaScreen` |
| M-RFN-09..11 Destino: pase con QR o resumen | `ExpedienteScreens.kt` → `ElegirDestinoScreen`, `PaseMedico`, `ResumenTamizaje` |
| M-RFN-12/13 / M-RI-06 Pausa 20-20-20 con vibración y sonido, ejercicios | `PausaVisualScreen.kt` |
| M-RFNN-01/03 Sin conexión: último reporte, pase y resumen guardados | `data/cache/AlmacenLocal.kt`, `BannerSinConexion` |
| M-RFNN-02 Comprobante en cola sin red; se envía solo | `PanelViewModel.subirComprobante/enviarCola` |
| M-RFNN-04 Pendientes al iniciar sesión; el servidor manda correo si no hay push | `InicioScreen.kt`, servidor |
| M-RFNN-05 Directorio como lista sin ubicación | `DirectorioMapa.kt` (modo lista) |
| M-RFNN-06 Clínica escrita a mano: se verifica y se ofrece el resumen | `ElegirDestinoScreen` |
| M-RI-02 Push Android/iOS (FCM) | `PushAndroid.kt` (se activa con `google-services.json`) |
| M-RC-02 La notificación abre su pantalla | `rutaAScreen`, `MainActivity.onNewIntent` |
| C-RC-02 Cambios visibles en < 5 s | bandeja cada 5 s en `PanelViewModel` |
| C-RC-03 Solo HTTPS en release | `AndroidManifest.xml` (`usesCleartextTraffic` = `false` en release) |
| C-RC-08 / C-RS-07 Aviso de privacidad aceptado antes de ver datos | `AyudaScreens.kt` → `ConsentimientoScreen` |
| C-RFNN-02 Recuperar contraseña | "¿Olvidaste tu contraseña?" en el login: código al correo o SMS (`RecuperarPasswordScreen`) |
| M-RS-01 Guía rápida · C-RS-04 Reportar una falla | `AyudaScreens.kt` |

Capturas de todas las pantallas: [`docs/capturas/`](docs/capturas/).

### Activar las notificaciones push (Firebase)

1. En la [consola de Firebase](https://console.firebase.google.com) crea el proyecto y agrega
   una app Android con el paquete `mx.eyesofter.app`.
2. Descarga `google-services.json` y ponlo en `app/androidApp/`. Está en `.gitignore`: no contiene
   secretos del servidor, pero es mejor no publicarlo.
3. En el mismo proyecto: *Configuración → Cuentas de servicio → Generar clave privada* y pega el
   JSON en la variable `FIREBASE_SERVICE_ACCOUNT` del servidor en Railway.
4. Compila de nuevo. Al iniciar sesión, la app pide permiso de notificaciones y registra el teléfono.

Sin Firebase la app funciona igual: los avisos se ven en la bandeja (se revisa cada 5 s) y el
servidor los manda por correo si está configurado Resend.

## Publicar el APK (descarga para las familias)

Las familias descargan la app desde la página **Descarga la app** del portal (`/App`), que también
abre el botón *Descarga la app móvil* del escritorio. Esa página apunta siempre al último APK de
**Releases** de este repositorio, así que publicar una versión nueva no requiere tocar el portal.

### Una sola vez: la llave de firma

Android solo instala una actualización si viene firmada con **la misma llave** que la versión
instalada. Créala una vez en tu computadora (necesita Java; `keytool` viene con el JDK) y guárdala
en un lugar seguro, fuera del repositorio (por ejemplo, un gestor de contraseñas):

```powershell
keytool -genkeypair -v -keystore eyesofter.jks -alias eyesofter -keyalg RSA -keysize 2048 -validity 10000
# Te pide una contraseña y unos datos (nombre, organización); el alias es "eyesofter".
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\eyesofter.jks")) | Set-Clipboard
# Ya quedó en el portapapeles en Base64.
```

En GitHub: **Settings → Secrets and variables → Actions → New repository secret**:

| Secreto | Valor |
|---|---|
| `EYESOFTER_KEYSTORE_BASE64` | Lo que quedó en el portapapeles |
| `EYESOFTER_KEYSTORE_PASSWORD` | La contraseña de la llave |
| `EYESOFTER_KEY_ALIAS` | `eyesofter` |
| `GOOGLE_SERVICES_JSON` | (Opcional) Contenido de `google-services.json`, para las notificaciones push |

> Si pierdes la llave no podrás actualizar la app: habría que desinstalarla y volver a instalarla.
> No la subas al repositorio ni la compartas: con ella alguien podría publicar una app falsa
> que se instale encima de la verdadera.

### Cada vez que quieras publicar

**Actions → Publicar APK → Run workflow**, escribe la versión (por ejemplo `1.1`) y espera unos
minutos. El flujo compila la versión de producción (solo HTTPS), la firma, y la publica como
`eyesofter.apk` con su huella SHA-256. El número interno de versión sube solo en cada publicación.

## Pendientes conocidos

- **Firebase**: falta crear el proyecto y agregar `google-services.json` (ver arriba). Al registrar la app Android en Firebase usa el paquete `mx.eyesofter.app`.
- La pausa 20-20-20 solo corre con la app abierta (no hay servicio en segundo plano).
- iOS no se ha compilado (requiere Mac). En iOS no hay `Telefono` ni `Push` todavía.

Verificación de subida mediante conexión SSH (2026-11-04 09:14)
