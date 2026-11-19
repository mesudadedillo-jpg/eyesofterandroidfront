# Contrato de la API — EYESOFTER Suite

> **Este archivo es el acuerdo entre los dos proyectos** y existe, idéntico, en:
>
> - `eyesofter-backend/docs/API.md` (repositorio `eyesofterandroid`)
> - `eyesofter-app/docs/API.md` (repositorio `eyesofterandroidfront`)
>
> **Si cambias una ruta, un parámetro o un campo de un DTO, actualiza los dos
> proyectos y las dos copias de este archivo en el mismo momento.** Ya no hay
> código compartido entre la app y el servidor: si un lado cambia y el otro no,
> la app deja de entender las respuestas y no hay error de compilación que avise.

## Dónde vive cada lado del contrato

| Concepto | Backend (`eyesofter-backend`) | App (`eyesofter-app`) |
|---|---|---|
| Login (DTOs) | `src/main/kotlin/com/example/eyesofterapp/model/AuthDtos.kt` | `app/shared/src/commonMain/kotlin/com/example/eyesofterapp/data/model/AuthDtos.kt` |
| Óptica | `src/main/kotlin/com/example/eyesofterapp/model/Optica.kt` | `app/shared/src/commonMain/kotlin/com/example/eyesofterapp/domain/model/Optica.kt` |
| Salud | `model/Optica.kt` → `SaludDto` | La app solo revisa que responda 2xx |
| Notificaciones y dispositivos | `model/NotificacionDtos.kt` | **Por crear en la app** (ver "Lo que la app debe agregar") |
| Tamizajes | `model/TamizajeDtos.kt` | **Por crear en la app** (ver "Lo que la app debe agregar") |
| Rutas | `src/main/kotlin/com/example/eyesofterapp/routes/` | `data/network/AuthApi.kt` y `data/network/OpticasApi.kt` |

## URLs base

| Entorno | URL |
|---|---|
| Producción (Railway) | `https://eyesofterandroid-production.up.railway.app` |
| Local, desde la misma computadora | `http://localhost:8080` |
| Local, desde el emulador de Android | `http://10.0.2.2:8080` |

Todas las respuestas son JSON en UTF-8, excepto `GET /`.
Los errores siempre tienen la forma `{"error": "mensaje en español"}`.
Las fechas con hora van en ISO-8601 UTC, por ejemplo `"2026-11-19T15:04:05.123Z"`.

### Autenticación

| Quién | Cómo | Rutas |
|---|---|---|
| Nadie | — | `GET /`, `GET /salud`, `POST /api/login`, `GET /opticas/cercanas` |
| La app (con sesión) | Encabezado `Authorization: Bearer <token>` con el `token` del login | Todas las de `/api/notificaciones`, `/api/dispositivos`, `/api/tamizajes` |
| Sistema de escritorio | Encabezado `X-Api-Key: <ESCRITORIO_API_KEY>` | `POST /api/escritorio/tamizajes` |

Sin token, con token vencido (dura 30 días) o firmado por otro servidor:
**401** `{"error": "Sesión inválida o vencida. Inicia sesión de nuevo."}` → la app debe mandar al login.

Roles: `ADMINISTRADOR` = Dirección, `SUPERVISOR` = Enfermería, `CLIENTE` = Tutor,
`DOCTOR` = Especialista externo, `ALUMNO`. "Personal escolar" = `ADMINISTRADOR` o `SUPERVISOR`.

---

## 1. `GET /`

Prueba rápida de que el servidor está encendido. Responde **texto plano**:

```
Hello, Ktor Server!
```

## 2. `GET /salud`

Indica si el servidor y la base de datos están disponibles.

**200 OK**

```json
{ "status": "ok", "db": false, "version": "a1b2c3d", "push": true, "correo": false, "escritorio": true }
```

- `db` es `false` mientras no haya base de datos conectada. Cuando se conecte MySQL será `true` si responde.
- `version`: commit desplegado en Railway (`dev` en local). Sirve para confirmar qué código está en producción.
- `push` / `correo` / `escritorio`: si el servidor tiene configurados Firebase, Resend y `ESCRITORIO_API_KEY`.
- Railway la usa como *healthcheck* (`railway.json`): si no responde, no da por bueno el despliegue.
- La app la consulta cuando el login falla por timeout o conexión, para distinguir "servidor caído" de "servidor lento".

## 3. `POST /api/login`

Inicia sesión.

**Cuerpo (JSON)** — `LoginRequestDto`

```json
{ "username": "admin", "password": "123456" }
```

| Campo | Tipo | Notas |
|---|---|---|
| `username` | String | No distingue mayúsculas/minúsculas |
| `password` | String | Obligatorio, no puede ir vacío |

**Respuestas**

| Código | Cuándo | Cuerpo |
|---|---|---|
| 200 | Credenciales correctas | `LoginResponseDto` (abajo) |
| 400 | `password` vacío | `{"error": "Contraseña requerida"}` |
| 401 | Usuario no existe o contraseña incorrecta | `{"error": "Credenciales inválidas"}` |
| 429 | 3 intentos fallidos seguidos: cuenta bloqueada 15 min (C-RFNN-01) | `{"error": "Cuenta bloqueada por intentos fallidos. Intenta de nuevo en 15 min."}` |
| 400 | El cuerpo no es JSON válido o falta un campo | `{"error": "Cuerpo de la petición inválido: …"}` |

**200 OK** — `LoginResponseDto`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ…",
  "user": {
    "username": "admin",
    "fullName": "Administrador del Sistema",
    "role": "ADMINISTRADOR"
  }
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `token` | String | JWT firmado (HS256), válido 30 días. Se manda tal cual en `Authorization: Bearer …`. La app no necesita leer su contenido |
| `user.username` | String | |
| `user.fullName` | String | |
| `user.role` | String | `ADMINISTRADOR`, `SUPERVISOR`, `CLIENTE`, `DOCTOR` o `ALUMNO` |

## 4. `GET /opticas/cercanas`

Lista de ópticas. Se usa de dos formas:

| Forma | Ejemplo | Resultado |
|---|---|---|
| Por ubicación | `/opticas/cercanas?lat=20.0825&lng=-98.3622` | Todas las ópticas, de la más cercana a la más lejana (fórmula de Haversine), con `distanciaKm` calculada |
| Por texto | `/opticas/cercanas?query=Tulancingo` | Solo las que contienen el texto en nombre, ciudad, dirección o código postal (sin importar mayúsculas ni acentos). `distanciaKm` = `0.0` |
| Ambas | `?lat=…&lng=…&query=CDMX` | Filtra por texto y ordena por distancia |

| Parámetro | Tipo | Reglas |
|---|---|---|
| `lat` | Número decimal con punto | Entre -90 y 90. Va siempre junto con `lng` |
| `lng` | Número decimal con punto | Entre -180 y 180. Va siempre junto con `lat` |
| `query` | Texto | No puede ir vacío |

Cualquier otro parámetro se ignora (la app también envía `ciudad` y `cp` con el mismo texto que `query`).

**200 OK** — lista de `Optica` (puede ser `[]` si nada coincide)

```json
[
  {
    "id": 1,
    "nombre": "Óptica Tulancingo Centro",
    "direccion": "Av. Juárez 102, Centro",
    "ciudad": "Tulancingo",
    "codigoPostal": "43600",
    "lat": 20.0825,
    "lng": -98.3622,
    "distanciaKm": 0.0,
    "telefono": "7757531234",
    "verificada": true
  }
]
```

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Int | |
| `nombre` | String | |
| `direccion` | String | |
| `ciudad` | String | |
| `codigoPostal` | String | |
| `lat`, `lng` | Double | Ubicación de la óptica |
| `distanciaKm` | Double | Redondeada a 2 decimales. `0.0` = sin distancia (la app no la muestra) |
| `telefono` | String | 10 dígitos |
| `verificada` | Boolean | `true` = óptica asociada al programa |

**400 Bad Request** — `{"error": "..."}`

| Caso | Mensaje |
|---|---|
| Sin `lat`/`lng` ni `query` | `Faltan parámetros: envía lat y lng, o query` |
| Solo `lat` o solo `lng` | `Envía lat y lng juntos, por ejemplo ?lat=20.0825&lng=-98.3622` |
| `lat`/`lng` no son números (p. ej. `abc` o `20,08`) | `lat y lng deben ser números con punto decimal, por ejemplo lat=20.0825` |
| Fuera de rango o `NaN` | `Coordenadas fuera de rango: lat debe estar entre -90 y 90, y lng entre -180 y 180` |
| `query` vacío | `El parámetro query no puede estar vacío` |

> Comportamiento de la app: si esta ruta falla o devuelve `[]`, la app muestra su
> catálogo local de respaldo (`OpticasApi.kt`), que tiene las mismas 8 ópticas.

---

## 5. `GET /api/notificaciones` 🔒

Bandeja del usuario con sesión, de la más nueva a la más vieja. La app la consulta
**al iniciar sesión** para mostrar los pendientes aunque el usuario no haya
permitido las push (M-RFNN-04), y al abrir la pantalla de notificaciones.

| Parámetro | Tipo | Notas |
|---|---|---|
| `soloNoLeidas` | Boolean (opcional) | `true` = solo las no leídas. `noLeidas` siempre cuenta todas |

**200 OK** — `BandejaDto`

```json
{
  "noLeidas": 1,
  "notificaciones": [
    {
      "id": "4cf09cbc-c7c6-48a4-8b28-882d51e0eb1a",
      "tipo": "RESULTADO_TAMIZAJE",
      "titulo": "Resultado del tamizaje visual",
      "mensaje": "Ya está disponible el resultado del tamizaje visual de Ana López. Tócala para verlo.",
      "datos": { "tamizajeId": "6299…", "alumno": "Ana López", "ruta": "tamizaje/6299…" },
      "creadaEn": "2026-11-19T02:24:24.909Z",
      "leidaEn": null
    }
  ]
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `tipo` | String | `RESULTADO_TAMIZAJE` (tutor), `ALERTA_ENFERMERIA` (Enfermería), `AVISO` (aviso libre), `COMPROBANTE_RECIBIDO` (Enfermería), `COMPROBANTE_REVISADO` (tutor) |
| `datos` | Map<String,String> | Siempre trae `ruta`. Puede traer `tamizajeId`, `alumno`, `tutor`, `notificacionId` |
| `datos.ruta` | String | A dónde navegar al tocarla: `tamizaje/<id>`, `seguimiento`, `comprobantes` o `notificacion/<id>` |
| `leidaEn` | String o null | `null` = sin leer |

## 6. Confirmación de lectura 🔒

| Ruta | Qué hace | Respuesta |
|---|---|---|
| `POST /api/notificaciones/{id}/leida` | Registra fecha y hora de lectura (C-RFN-07). Si ya estaba leída, conserva la primera fecha | **200** `NotificacionDto` · **404** si no existe o no es del usuario |
| `POST /api/notificaciones/leidas` | Marca todas las del usuario como leídas | **200** `{"marcadas": 2}` |

La app debe llamar a la primera **cuando el usuario abre la notificación** (desde la
push o desde la bandeja). Mientras no lo haga, el servidor la considera sin leer y
manda recordatorios: si pasan **3 días** sin leerse, la reenvía al tutor (máximo 3
veces, cada 3 días) y la primera vez avisa a Enfermería (C-RFN-08, C-RFNN-04).

## 7. Dispositivos (token de push) 🔒

| Ruta | Cuándo la llama la app | Respuesta |
|---|---|---|
| `POST /api/dispositivos` | Después del login, y cada vez que Firebase entrega un token nuevo (`onNewToken`) | **204** · **400** si `plataforma` no es `ANDROID`/`IOS` o el token está vacío |
| `DELETE /api/dispositivos?token=<token>` | Al cerrar sesión | **204** (aunque no existiera) |

Cuerpo de `POST` — `RegistroDispositivoDto`:

```json
{ "token": "<token de Firebase Cloud Messaging>", "plataforma": "ANDROID" }
```

Si el mismo teléfono se usa con otro usuario, el token pasa al nuevo usuario.
Si Firebase dice que un token ya no existe (app desinstalada), el servidor lo borra solo.

### Cómo llega la push al teléfono

Mensaje de Firebase Cloud Messaging con:

- `notification.title` / `notification.body`: los mismos `titulo` y `mensaje` de la bandeja
  (con "Recordatorio: " antepuesto al título si es un reenvío). **No incluyen el resultado
  del semáforo**, solo avisan que hay algo nuevo.
- `data`: `notificacionId`, `tipo` y todo lo que hay en `datos` (incluida `ruta`).
- Android: prioridad alta y canal **`eyesofter_avisos`** (la app debe crear ese `NotificationChannel`).

Al tocarla, la app abre `data.ruta` y llama a §6 con `data.notificacionId`.
Así el tutor llega al resultado en 1 toque (M-RC-02).

Si la push no llega (sin dispositivos registrados o Firebase falla), el servidor
intenta por **correo** al `email` del usuario. Si tampoco hay correo, queda en la bandeja.

## 8. Tamizajes 🔒

| Ruta | Quién | Respuesta |
|---|---|---|
| `GET /api/tamizajes` | Tutor: los de sus hijos. Personal escolar: todos. Otros roles: `[]` | **200** lista de `TamizajeDto` |
| `GET /api/tamizajes/{id}` | El tutor dueño o el personal escolar | **200** `TamizajeDto` · **404** si no existe o no le corresponde (el intento se registra en la bitácora) |

`TamizajeDto`:

```json
{
  "id": "6299…",
  "folio": "F-2026-0001",
  "alumnoNombre": "Ana López",
  "grado": "3",
  "grupo": "B",
  "fecha": "2026-11-18",
  "semaforo": "AMARILLO",
  "semaforoTexto": "En observación",
  "observaciones": "Usa lentes desde 2025"
}
```

| `semaforo` | `semaforoTexto` |
|---|---|
| `VERDE` | Saludable |
| `AMARILLO` | En observación |
| `ROJO` | Atención urgente requerida |

## 9. Rutas del personal escolar 🔒 (Dirección / Enfermería)

Las usará el portal web. Cualquier otro rol recibe **403**.

**`POST /api/notificaciones`** — enviar un aviso libre. Cuerpo `AvisoEntradaDto`:

```json
{ "titulo": "Junta de padres", "mensaje": "El viernes a las 10:00", "rol": "CLIENTE", "usernames": ["tutor1"] }
```

Se envía a la unión de `usernames` y de todos los usuarios con `rol` (sin duplicar).
**201** `{"enviadas": 12, "noEncontrados": []}` · **400** si falta `titulo`/`mensaje` o no hay `usernames` ni `rol`.

**`GET /api/notificaciones/seguimiento`** (`?soloNoLeidas=true` opcional) — qué familias ya
leyeron su resultado. **200** lista de:

```json
{
  "notificacionId": "4cf0…", "tutor": "cliente", "alumno": "Ana López", "tamizajeId": "6299…",
  "creadaEn": "2026-11-19T02:24:24.909Z", "leidaEn": null,
  "recordatoriosEnviados": 1, "entregadaPor": ["push"]
}
```

`entregadaPor` vacío = no llegó ni por push ni por correo; solo está en la bandeja.

## 10. `POST /api/escritorio/tamizajes` 🔑 (sistema de escritorio)

El sistema de escritorio EYESOFTER manda cada resultado aquí (C-RFN-02) con
`X-Api-Key`. El servidor lo guarda y avisa al tutor en segundos (M-RFN-02, M-RC-01).

Cuerpo `TamizajeEntradaDto`:

```json
{
  "folio": "F-2026-0001",
  "alumnoNombre": "Ana López",
  "grado": "3",
  "grupo": "B",
  "tutorUsername": "cliente",
  "fecha": "2026-11-18",
  "semaforo": "AMARILLO",
  "observaciones": "Usa lentes desde 2025"
}
```

| Código | Cuándo | Cuerpo |
|---|---|---|
| 201 | Tamizaje nuevo; se notificó al tutor | `{"tamizajeId": "…", "notificacionId": "…", "duplicado": false}` |
| 200 | Ese `folio` ya se había recibido (reintento); **no** se vuelve a notificar | `{"tamizajeId": "…", "notificacionId": null, "duplicado": true}` |
| 400 | Falta `folio`/`alumnoNombre`, `semaforo` no es VERDE/AMARILLO/ROJO, `fecha` no es AAAA-MM-DD, o el tutor no existe o no es `CLIENTE` | `{"error": "…"}` |
| 401 | `X-Api-Key` falta o es incorrecta | `{"error": "X-Api-Key inválida"}` |
| 503 | El servidor no tiene `ESCRITORIO_API_KEY` configurada | `{"error": "…"}` |

Como el folio evita duplicados, el escritorio puede reintentar sin miedo si no recibe respuesta (C-RFNN-03).

## 11. Recetas y comprobantes 🔒

El tutor fotografía la receta o el comprobante de lentes (M-RFN-05) y Enfermería lo valida.

**`POST /api/comprobantes`** (solo `CLIENTE`) — `multipart/form-data` con:

| Campo | Valor |
|---|---|
| `tamizajeId` | Id de un tamizaje del propio tutor |
| `tipo` | `RECETA` o `COMPROBANTE_LENTES` |
| `archivo` | La foto (JPG, PNG, WEBP) o PDF. **Máximo 10 MB**. El tipo se reconoce por el contenido, no por el nombre |

| Código | Cuándo |
|---|---|
| 201 | Guardado; Enfermería recibe una notificación `COMPROBANTE_RECIBIDO`. Cuerpo: `ComprobanteDto` |
| 400 | Falta `archivo` o `tipo` inválido |
| 403 | Quien sube no es tutor |
| 404 | El tamizaje no existe o no es suyo |
| 413 | Más de 10 MB → `{"error": "El archivo pesa más de 10 MB…"}` |
| 415 | No es imagen ni PDF → `{"error": "Solo se aceptan fotos (JPG, PNG, WEBP) o PDF"}` |

`ComprobanteDto`:

```json
{
  "id": "…", "tamizajeId": "…", "tutor": "cliente", "tipo": "RECETA",
  "contentType": "image/jpeg", "tamanoBytes": 345678, "subidoEn": "2026-11-19T…Z",
  "estado": "PENDIENTE", "revisadoEn": null, "motivoRechazo": null
}
```

`estado`: `PENDIENTE`, `APROBADO` o `RECHAZADO`.

| Ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/comprobantes?estado=PENDIENTE` | Tutor: los suyos. Personal escolar: todos | Lista de `ComprobanteDto` (`estado` opcional) |
| `GET /api/comprobantes/{id}/archivo` | Dueño o personal escolar | El archivo tal cual, con su `Content-Type` · 404 si no le corresponde |
| `POST /api/comprobantes/{id}/revision` | Personal escolar | Cuerpo `{"aprobado": false, "motivo": "La foto está borrosa"}`. El motivo es obligatorio al rechazar. Avisa al tutor (`COMPROBANTE_REVISADO`). **409** si ya estaba revisado |

---

## Estado de la app (`eyesofter-app`) frente a este contrato

| Punto | Estado |
|---|---|
| Guardar el `token` y mandarlo en `Authorization: Bearer …`; ante 401 volver al login | ✅ `data/network/EyesApi.kt`, `data/session/SesionGuardada.kt` |
| `GET /api/notificaciones` al iniciar sesión y cada minuto con la app abierta | ✅ `ui/PanelViewModel.kt` |
| Al abrir una notificación: navegar a `datos.ruta` y `POST /api/notificaciones/{id}/leida` | ✅ `composeApp/navigation/AppNavHost.kt` |
| Resultado con semáforo de color **más** `semaforoTexto` e icono | ✅ `composeApp/screens/TamizajeScreen.kt` |
| Subir receta/comprobante (§11) con cámara o archivo | ✅ `TamizajeScreen.kt` + `androidApp/TelefonoAndroid.kt` |
| Enfermería: seguimiento, revisión de comprobantes y avisos (§9, §11) | ✅ `composeApp/screens/PersonalScreens.kt` |
| Mostrar el 429 del login (cuenta bloqueada) | ✅ `ui/AuthViewModel.kt` |
| **Firebase Cloud Messaging**: permiso de notificaciones, canal `eyesofter_avisos`, `POST /api/dispositivos` tras el login y en `onNewToken`, `DELETE /api/dispositivos` al salir | ⏳ Pendiente (falta el proyecto de Firebase) |

---

## Historial de cambios del contrato

| Fecha | Cambio |
|---|---|
| 2026-11-03 | Separación en dos proyectos. Se implementan `/salud` y `/opticas/cercanas` en el servidor; se documentan errores, parámetros y campos. |
| 2026-11-19 (b) | `/salud` agrega `version`, `push`, `correo`, `escritorio`. Login responde 429 tras 3 fallos. Nueva §11: recetas y comprobantes. CORS permite `DELETE`. |
| 2026-11-19 | El `token` del login ahora es un JWT real. JSON inválido en el login responde 400 (antes 500). Nuevas rutas §5-§10: notificaciones, confirmación de lectura, dispositivos, tamizajes, avisos, seguimiento y recepción desde el escritorio. |
