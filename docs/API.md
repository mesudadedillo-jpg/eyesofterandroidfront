# Contrato de la API — EYESOFTER Suite

> **Este archivo es el acuerdo entre los proyectos** y existe, idéntico, en:
>
> - `eyesofter-backend/docs/API.md` (repositorio `eyesofterandroid`, servidor Ktor)
> - `eyesofter-app/docs/API.md` (repositorio `eyesofterandroidfront`, app móvil)
>
> **Si cambias una ruta, un parámetro o un campo de un DTO, actualiza los proyectos
> y las dos copias de este archivo en el mismo momento.** No hay código compartido
> entre la app y el servidor: si un lado cambia y el otro no, la app deja de entender
> las respuestas y no hay error de compilación que avise.

## Arquitectura (desde 2026-11-20)

```
  App móvil (KMP) ──HTTPS/JSON──┐
                                ├──► Servidor Ktor (Railway) ──┐
  Sistema de escritorio ────────┘     notificaciones push/correo│
                                                                ├──► PostgreSQL (Railway)
  Navegador ──HTTPS──► Portal web ASP.NET (Railway) ────────────┘    UNA sola base
```

- **Una sola base PostgreSQL.** El portal web (`eyesofterweb`, EF Core) es el dueño del
  esquema y de las cuentas (ASP.NET Identity). El servidor Ktor NO crea tablas: usa las del portal.
- **Las mismas cuentas** en la web y en la app. Las crea Dirección en el portal; no hay registro público.
- **El mismo bloqueo**: 3 intentos fallidos → 15 min, en la web y en la app (columnas de Identity).
- **La misma llave de cifrado** (`EYESOFTER_CLAVE_DATOS`, AES-256-GCM) para los datos sensibles (C-RC-04).
- **La misma bandeja de notificaciones.** Lo que crea el portal o el servidor queda en la tabla
  `notificaciones` con `pendiente_envio = true`; el servidor Ktor la entrega por push o correo en
  segundos (bandeja de salida). Así solo Ktor habla con Firebase y Resend.
- Los cambios de estado se ven en el otro lado **al instante** (misma base; C-RC-02 pide < 5 s).

## Dónde vive cada lado del contrato

| Concepto | Servidor (`eyesofter-backend`) | App (`eyesofter-app`) |
|---|---|---|
| Login, consentimiento, reportes | `model/AuthDtos.kt` | `app/shared/.../data/model/AuthDtos.kt` |
| Óptica y clínicas | `model/Optica.kt` | `app/shared/.../domain/model/Optica.kt` |
| Notificaciones | `model/NotificacionDtos.kt` | `app/shared/.../data/model/NotificacionDtos.kt` |
| Tamizajes, casos, pases, citas, comprobantes | `model/TamizajeDtos.kt` | `app/shared/.../data/model/TamizajeDtos.kt` |
| Rutas | `routes/` | `app/shared/.../data/network/EyesApi.kt` |

## URLs base

| Entorno | URL |
|---|---|
| Producción (Railway) | `https://eyesofter-api.up.railway.app` |
| Local, desde la misma computadora | `http://localhost:8080` |
| Local, desde el emulador de Android | `http://10.0.2.2:8080` |

Todas las respuestas son JSON en UTF-8, excepto el PNG del pase y los archivos de comprobantes.
En producción solo HTTPS (TLS 1.2+, lo pone Railway; C-RC-03). La app de release no permite HTTP.

- **Errores:** siempre `{"error": "mensaje en español"}`. Algunos agregan `"codigo"` para que la app
  reaccione sin leer el texto: `CONSENTIMIENTO_PENDIENTE`, `ALUMNO_DESCONOCIDO`.
- **Fechas:** instantes en ISO-8601 UTC (`"2026-11-19T15:04:05.123Z"`); fechas de calendario
  como `"2026-11-19"` (hora de la Ciudad de México).
- **Enums:** en MAYÚSCULAS con guion bajo (`EN_SEGUIMIENTO`, `COMPROBANTE_LENTES`).

### Autenticación

| Quién | Cómo | Rutas |
|---|---|---|
| Nadie | — | `GET /`, `GET /salud`, `POST /api/login`, `GET /opticas/cercanas`, `GET /api/clinicas`, `POST /api/reportes` |
| La app (con sesión) | `Authorization: Bearer <token>` con el `token` del login | Todo lo demás bajo `/api/` |
| Nadie (vinculación) | — | `POST /api/escritorio/vinculaciones`, `POST /api/escritorio/vinculaciones/estado`, `GET/POST /escritorio/vincular` (página web) |
| Equipo de escritorio vinculado | `Authorization: Bearer <token del equipo>` (§13). **No** sirve el token de la app, ni el del equipo en las rutas de la app | Resto de `/api/escritorio/...` |

**401** `{"error": "Sesión inválida o vencida. Inicia sesión de nuevo."}` cuando: no hay token, venció
(dura 30 días), lo firmó otro servidor, **la cuenta fue desactivada** en el portal, o **cambió la
contraseña o el rol** desde que se emitió (se compara el `security_stamp` de Identity). La app debe
mandar al login.

Roles (`user.role`): `ADMINISTRADOR` = Dirección, `SUPERVISOR` = Enfermería, `CLIENTE` = Tutor,
`DOCTOR` = Especialista externo, `ALUMNO`. "Personal escolar" = `ADMINISTRADOR` o `SUPERVISOR`.
En el portal se llaman Administrador, Enfermeria, Tutor, Especialista y Alumno.

**Límite por IP:** `POST /api/login` y `POST /api/reportes` aceptan 30 peticiones por minuto por IP;
después **429** hasta el siguiente minuto.

**Bitácora (C-RC-07, C-RFNN-06):** inicios de sesión, fallos, bloqueos, accesos denegados a datos
ajenos y acciones del personal quedan en la tabla `auditoria` con `origen = "app"` o `"escritorio"`
y, cuando aplica, la escuela (`plantel_id`). Dirección la consulta en el portal (Auditoría): ve la de
sus escuelas; el administrador global, toda.

### Varias escuelas (C-RE-01) 🏫

El sistema atiende **varias escuelas** con la misma base. Todo lo de un alumno (tamizajes, casos,
pases, citas, comprobantes, avisos) pertenece a la **escuela del alumno** (`alumnos.plantel_id`).

| Cuenta | Qué escuelas ve |
|---|---|
| Dirección / Enfermería | Solo las que tiene asignadas en el portal (tabla `usuario_planteles`). Sin escuelas, nada |
| Dirección con `administrador_global` | Todas. Es para el equipo que administra EYESOFTER: da de alta escuelas y recibe los reportes de fallas |
| Tutor, alumno, especialista | Sin cambio: lo suyo (sus hijos, su expediente, los pases de su clínica), sea de la escuela que sea |
| Equipo de escritorio | Las de la cuenta que lo vinculó (§13) |

- Un tamizaje, comprobante o caso de **otra escuela** responde **404** (como si no existiera) y queda
  como `ACCESO_DENEGADO` en la bitácora.
- Las alertas a Enfermería (tutor sin cuenta, comprobante nuevo, cita, aviso sin leer, falla de
  sincronización) le llegan **solo a la Enfermería de la escuela del alumno**.
- La **matrícula es única dentro de cada escuela**, no en todo el sistema: dos escuelas pueden tener `A001`.
- El folio de caso `EYS-AAAA-NNNN` sigue siendo único en todo el sistema.

---

## 1. `GET /`

```json
{ "servicio": "eyesofter-backend", "documentacion": "docs/API.md" }
```

## 2. `GET /salud`

```json
{ "status": "ok", "db": true, "version": "a1b2c3d", "push": true, "correo": true, "escritorio": true, "pendientesEnvio": 0 }
```

- `db`: la base respondió en menos de 2 s. Si no, `status` es `"degradado"` (sigue siendo **200**
  para que Railway no reinicie el servicio en bucle mientras la base arranca).
- `version`: commit desplegado en Railway (`dev` en local).
- `push` / `correo`: si están configurados Firebase y Resend. `escritorio`: siempre `true` (la vinculación de equipos ya no depende de una clave).
- `pendientesEnvio`: notificaciones esperando entrega. Si crece y no baja, algo falla con push/correo.
- Railway la usa como *healthcheck* (`railway.json`).

## 3. `POST /api/login`

```json
{ "username": "tutor@eyesofter.mx", "password": "Eyesofter123!" }
```

`username` es el **correo** de la cuenta (también acepta el nombre de usuario de Identity).
No distingue mayúsculas.

| Código | Cuándo | Cuerpo |
|---|---|---|
| 200 | Correcto | `LoginResponseDto` |
| 400 | Falta correo o contraseña, o JSON inválido | `{"error": "Escribe tu correo y tu contraseña"}` |
| 401 | Cuenta inexistente o contraseña incorrecta | `{"error": "Credenciales inválidas"}` |
| 403 | Cuenta desactivada o sin rol | `{"error": "Tu cuenta está desactivada. Comunícate con la escuela."}` |
| 429 | 3.er intento fallido seguido, o cuenta aún bloqueada (C-RFNN-01) | `{"error": "Cuenta bloqueada por intentos fallidos. Intenta de nuevo en 15 min."}` |
| 401 | La cuenta tiene **verificación en dos pasos** y falta el código | `{"error": "Escribe el código de 6 dígitos…", "codigo": "REQUIERE_2FA"}` |
| 401 | El código de dos pasos no es correcto (cuenta como intento fallido) | `{"error": "El código no es correcto…", "codigo": "CODIGO_2FA_INVALIDO"}` |
| 403 | Dirección o Enfermería **sin** verificación en dos pasos (obligatoria con `EXIGIR_2FA_PERSONAL`) | `{"error": "…activa primero la verificación en dos pasos en el portal…", "codigo": "ACTIVAR_2FA"}` |

**Verificación en dos pasos (TOTP):** se activa en el portal (menú de la cuenta). Desde ese momento el
login lleva `"codigo": "123456"` con el código de la app autenticadora (Google/Microsoft Authenticator…).
La app manda primero correo y contraseña; si recibe `REQUIERE_2FA`, muestra el campo del código y repite
el login con `codigo`. Es la misma clave que usa el portal (`asp_net_user_tokens`), así que el mismo
código sirve en los dos. **Es obligatoria para Dirección y Enfermería**; opcional para los demás.

**Duración del token:** 30 días para tutores y alumnos; **12 horas para el personal escolar**
(`JWT_HORAS_PERSONAL`), que ve datos de muchos menores.

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ…",
  "user": {
    "username": "tutor@eyesofter.mx",
    "fullName": "María López Hernández",
    "role": "CLIENTE",
    "id": "b6c7f1fe-737f-43ea-860e-e841d5d10f6e",
    "requiereConsentimiento": false
  }
}
```

- La contraseña se verifica contra el hash PBKDF2-HMAC-SHA512 de Identity (C-RC-05).
- **¿Olvidaste tu contraseña?** (C-RFNN-02): la app abre `{URL_PORTAL}/Cuenta/Olvide` en el navegador;
  el portal manda el enlace al correo registrado.
- `requiereConsentimiento: true` → antes de cualquier otra cosa, mostrar el aviso de privacidad (§4).

## 4. Aviso de privacidad (C-RC-08, C-RS-07) 🔒

| Ruta | Respuesta |
|---|---|
| `GET /api/consentimiento` | `{"versionVigente":"2026-11","aceptado":false,"aceptadoEn":null,"urlAviso":"https://…/Privacidad/Aviso","urlTerminos":"https://…/Privacidad/Terminos"}` |
| `POST /api/consentimiento` | Cuerpo `{"version":"2026-11","acepto":true}` → **204**. **400** si la versión no es la vigente |

Mientras un **tutor** no acepte la versión vigente, sus rutas de datos (`/api/tamizajes…`,
`/api/comprobantes…`) responden **403** `{"error":"Acepta el aviso de privacidad para continuar.","codigo":"CONSENTIMIENTO_PENDIENTE"}`.
Aceptarlo en la web o en la app cuenta para los dos. El personal no necesita aceptarlo.

## 5. Directorio de clínicas (públicas)

### `GET /opticas/cercanas` — para el mapa (M-RFN-06, M-RFN-07)

| Forma | Ejemplo | Resultado |
|---|---|---|
| Por ubicación | `?lat=20.0825&lng=-98.3622` (opcional `&query=`) | **Asociadas primero**, luego por distancia, con `distanciaKm` |
| Por texto | `?query=tulancingo` | Coinciden en nombre, ciudad, dirección o CP, sin importar acentos (`distanciaKm = 0.0`) |

Solo incluye clínicas **activas y con coordenadas**. **400** si falta `lng`, si no son números o están
fuera de rango, o si no viene nada.

```json
[{ "id": 1, "nombre": "Óptica Tulancingo Centro", "direccion": "Av. Juárez 10", "ciudad": "Tulancingo",
   "codigoPostal": "43600", "lat": 20.083, "lng": -98.363, "distanciaKm": 0.12, "telefono": "7751000001",
   "verificada": true, "tipo": "Optica", "horario": "L-V 9 a 18" }]
```

`verificada` = asociada al programa (insignia, M-RFN-07).

**Convenios:** la ruta es pública, pero si la petición trae `Authorization: Bearer <token>` de un **tutor**,
cada clínica trae `"convenio": "20 % en armazones"` con el beneficio que su escuela pactó con esa
clínica para los alumnos (lo registra Dirección en el portal → Convenios). Sin sesión, con otro rol o con
un token vencido, `convenio` es `null` y la respuesta es la misma de siempre (nunca 401).

### `GET /api/clinicas[?query=]` — todo el directorio activo

Incluye las que **no tienen coordenadas** (`lat`/`lng` = `null`). Asociadas primero. Para la lista sin
ubicación (M-RFNN-05) y para revisar una clínica escrita a mano (M-RFNN-06).

```json
[{ "id": 4, "nombre": "Oftalmología Norte", "tipo": "Oftalmologia", "direccion": "Blvd. Norte 100",
   "ciudad": "Tulancingo", "codigoPostal": "43620", "telefono": "7751000004", "horario": null,
   "lat": null, "lng": null, "asociada": false, "convenio": null }]
```

Igual que el mapa: con la sesión de un tutor, `convenio` trae el beneficio para sus hijos.

## 6. Notificaciones 🔒

La **misma bandeja** que ve el usuario en el portal.

| Ruta | Respuesta |
|---|---|
| `GET /api/notificaciones[?soloNoLeidas=true]` | `{"noLeidas": 2, "notificaciones": [NotificacionDto…]}` (las 100 más recientes) |
| `POST /api/notificaciones/{id}/leida` | La notificación con `leidaEn` (C-RFN-07; conserva la **primera** fecha). **404** si no es suya (y se registra) |
| `POST /api/notificaciones/leidas` | `{"marcadas": 3}` |

```json
{ "id": "6f1c…", "tipo": "RESULTADO_TAMIZAJE", "titulo": "Resultado del tamizaje visual",
  "mensaje": "Ya está disponible el resultado del tamizaje visual de Ana López. Tócala para verlo.",
  "datos": { "ruta": "tamizaje/12", "tamizajeId": "12", "alumno": "Ana López", "casoId": "5" },
  "creadaEn": "2026-11-19T15:04:05Z", "leidaEn": null }
```

| `tipo` | Para | `datos.ruta` |
|---|---|---|
| `RESULTADO_TAMIZAJE` | Tutor | `tamizaje/{id}` |
| `RECORDATORIO_CASO` | Tutor: caso sin atender (C-RFN-08) | `tamizaje/{id}` |
| `COMPROBANTE_REVISADO` | Tutor | `tamizaje/{id}` |
| `CASO_ACTUALIZADO` | Tutor: atención registrada, caso cerrado… | `tamizaje/{id}` |
| `AVISO` | Cualquiera | `notificacion/{id}` (o `reportes` para Dirección) |
| `ALERTA_ENFERMERIA` | Enfermería: aviso sin leer, tutor sin cuenta, falla del escritorio | `seguimiento` |
| `COMPROBANTE_RECIBIDO` | Enfermería | `comprobantes` |
| `CITA` | Enfermería y el especialista de esa clínica | `seguimiento` |

La app navega a `datos.ruta` al tocarla (M-RC-02) y la marca como leída.
**Sondeo:** con la app abierta, `GET /api/notificaciones` cada 5 s como máximo (C-RC-02); la push
(§7) avisa aunque esté cerrada.

## 7. Dispositivos (token de push) 🔒

| Ruta | Notas |
|---|---|
| `POST /api/dispositivos` | `{"token": "<FCM>", "plataforma": "ANDROID"}` (`ANDROID` o `IOS`) → **204**. Tras el login y en cada `onNewToken`. Si el token era de otra cuenta, cambia de dueño |
| `DELETE /api/dispositivos?token=…` | Al cerrar sesión → **204** |

### Cómo llega el aviso (M-RFN-02, M-RFNN-04, M-RC-01)

1. Quien la crea (escritorio, portal o la propia API) la guarda en la bandeja con `pendiente_envio = true`.
2. El **despachador** de Ktor revisa la bandeja cada `DESPACHO_SEGUNDOS` (3 s) y la entrega en orden:
   **push** (FCM HTTP v1, Android e iOS) → si no llega (sin permiso, sin app, token vencido) **correo** (Resend).
3. Cada intento queda en `entregas_notificacion` (el portal y `/seguimiento` muestran por dónde llegó).
4. Si no hay canales configurados, la notificación queda solo en la bandeja: la app la muestra al iniciar sesión.

La push **no lleva el color del semáforo** (pasa por servidores de Google/Apple). Mensaje FCM:
`notification {title, body}`, `data {notificacionId, tipo, ruta, …datos}`, canal Android `eyesofter_avisos`.

**Recordatorios** (cada hora): resultado sin leer en 3 días → se reenvía como `"Recordatorio: …"`
(hasta 3 veces) y la primera vez se alerta a Enfermería (C-RFNN-04). Caso amarillo/rojo sin atención
ni comprobante aprobado → `RECORDATORIO_CASO` semanal (hasta 3; C-RFN-08).

## 8. Tamizajes 🔒

| Ruta | Quién | Respuesta |
|---|---|---|
| `GET /api/tamizajes` | Tutor: los de sus hijos (con `caso` completo, para guardarlo sin conexión). Alumno: los suyos. Personal: los 200 más recientes (sin `caso`) | `[TamizajeDto…]` |
| `GET /api/tamizajes/{id}` | Tutor del alumno, el alumno o personal | `TamizajeDto` con `caso`. **404** si no le corresponde (se registra, C-RFNN-06) |

```json
{
  "id": "12", "folio": "ESC-2026-0451", "alumnoId": 1, "alumnoNombre": "Ana López", "grado": "3", "grupo": "B",
  "fecha": "2026-11-19", "semaforo": "ROJO", "semaforoTexto": "Atención urgente requerida",
  "observaciones": "Entrecierra los ojos al leer el pizarrón", "campana": "Campaña otoño 2026",
  "caso": {
    "id": 5, "folio": "EYS-2026-0003", "estado": "EN_SEGUIMIENTO", "destinoDefinido": true,
    "clinicaId": 1, "clinicaNombre": "Óptica Tulancingo Centro", "clinicaAsociada": true,
    "pase": {
      "codigo": "K7M2Q9XRT4HW8NBC3PZD6FJYEA", "codigoLegible": "K7M2 Q9XR T4HW 8NBC 3PZD 6FJY EA",
      "estado": "VIGENTE", "creadoEn": "2026-11-19T16:00:00Z", "expiraEn": "2027-01-18T16:00:00Z", "usadoEn": null,
      "clinicaId": 1, "clinicaNombre": "Óptica Tulancingo Centro", "clinicaDireccion": "Av. Juárez 10",
      "clinicaTelefono": "7751000001", "urlValidacion": "https://portal/Pases/Validar?codigo=K7M2…"
    },
    "fechaAtencion": null, "comprobanteAprobado": false,
    "citas": [{ "id": 3, "clinicaNombre": "Óptica Tulancingo Centro", "fechaPreferida": "2026-11-25",
                "horario": "MANANA", "comentario": null, "estado": "SOLICITADA", "creadaEn": "…" }],
    "eventos": [{ "fecha": "2026-11-19", "tipo": "TAMIZAJE", "descripcion": "Tamizaje Snellen (escritorio…)" }]
  }
}
```

- `semaforo`: `VERDE`, `AMARILLO`, `ROJO`. Mostrar **color + `semaforoTexto` + icono** (C-RC-10). Sin diagnóstico (C-RFN-06).
- `caso` = `null` en verde (no requiere seguimiento).
- `caso.estado`: `ABIERTO`, `EN_SEGUIMIENTO`, `CERRADO`.
- `pase.estado`: `VIGENTE`, `USADO`, `CANCELADO` o `EXPIRADO` (vence a los 60 días).
- `eventos[].tipo`: `TAMIZAJE`, `AVISO_TUTOR`, `CITA`, `COMPROBANTE`, `DESTINO`, `ATENCION`, `SEGUIMIENTO`, `CIERRE`, `REAPERTURA`.

**Sin conexión (M-RFNN-01, M-RFNN-03):** la app guarda la última respuesta de `GET /api/tamizajes` y
el PNG del pase; si no hay red, los muestra con el aviso "puede no estar actualizado".

## 9. Destino, pase con QR y citas 🔒

### `POST /api/tamizajes/{id}/destino` (M-RFN-09..11, C-RFN-04..06, C-RFNN-07, M-RFNN-06)

Tutor del alumno (o personal). Uno de los dos:

```json
{ "clinicaId": 1 }
{ "nombreManual": "Óptica Mi Barrio" }
```

| Resultado | Cuándo |
|---|---|
| Pase con QR (`conPase: true`) | Clínica registrada, **asociada y activa** |
| Solo resumen (`conPase: false`) | No asociada, o escrita a mano y **no registrada** (se trata como no asociada) |

- Un nombre escrito a mano que coincide con una clínica del directorio (sin acentos ni mayúsculas) se toma como esa clínica.
- Cambiar de destino **cancela** el pase anterior: solo hay uno vigente.
- **200** `{"conPase": true, "mensaje": "Listo. Muestra este pase…", "tamizaje": TamizajeDto}`.
- **409** si el resultado es verde o el caso está cerrado. **400** si la clínica no existe o está desactivada.
- El `pase` trae `"convenio": "…"` si la escuela del alumno tiene convenio vigente con esa clínica (también
  sale en el PDF del pase y se le muestra al especialista al validar el QR). `null` = sin convenio.

### `GET /api/tamizajes/{id}/pase.png[?tamano=480]`

PNG del QR del pase **vigente** (160–1024 px). El QR contiene `urlValidacion`: el especialista lo
escanea en el portal (Pases → Leer pase), que verifica clínica, vigencia y un solo uso (W-RFNN-03/04).
**404** si no hay pase vigente.

### `POST /api/tamizajes/{id}/citas` (M-RFN-08)

Solo el tutor. `{"clinicaId": 1, "fechaPreferida": "2026-11-25", "horario": "MANANA", "comentario": "Después de clases"}`
→ **201** `{"id": 3, "mensaje": "Solicitud enviada. Llama a … al 7751000001 si necesitas confirmarla antes."}`.
Fecha de hoy a 6 meses. Avisa a Enfermería y al especialista de esa clínica; Enfermería la confirma en el portal.

## 10. Recetas y comprobantes 🔒 (M-RFN-05, C-RFNN-05)

| Ruta | Quién | Notas |
|---|---|---|
| `POST /api/comprobantes` | Tutor del alumno | `multipart/form-data`: `tamizajeId`, `tipo` (`RECETA` o `COMPROBANTE_LENTES`), `archivo`. **201** `ComprobanteDto` |
| `GET /api/comprobantes[?estado=PENDIENTE]` | Tutor: los de sus hijos. Personal: todos | `[ComprobanteDto…]` |
| `GET /api/comprobantes/{id}/archivo` | Tutor del alumno o personal | El archivo con su `Content-Type` |
| `POST /api/comprobantes/{id}/revision` | Personal | `{"aprobado": false, "motivo": "La foto está borrosa"}` (motivo obligatorio al rechazar). **409** si ya se revisó |

Errores al subir: **413** más de 10 MB · **415** no es JPG, PNG, WEBP ni PDF (se revisan los primeros
bytes, no la extensión) · **409** resultado verde o caso cerrado · **404** tamizaje ajeno.
El archivo se guarda **cifrado** en la base (C-RC-04). Al subirlo se avisa a Enfermería; al revisarlo, al tutor.

```json
{ "id": "3f2a…", "tamizajeId": "12", "casoId": 5, "alumno": "Ana López", "tutor": "tutor@eyesofter.mx",
  "tipo": "RECETA", "contentType": "image/jpeg", "tamanoBytes": 482113, "subidoEn": "…",
  "estado": "PENDIENTE", "revisadoEn": null, "motivoRechazo": null }
```

## 11. Personal escolar 🔒 (Dirección / Enfermería)

| Ruta | Notas |
|---|---|
| `POST /api/notificaciones` | Aviso: `{"titulo":"Junta","mensaje":"…","usernames":["correo@…"],"rol":"CLIENTE"}` (lista, rol o ambos). Solo llega a cuentas **de sus escuelas**: un correo de otra escuela sale en `noEncontrados`. **201** `{"enviadas": 12, "noEncontrados": []}`. **403** para otros roles |
| `GET /api/notificaciones/seguimiento[?soloNoLeidas=true]` | Resultados enviados a tutores **de sus escuelas**: `[{notificacionId, tutor, alumno, tamizajeId, creadaEn, leidaEn, recordatoriosEnviados, entregadaPor: ["push"]}]` |
| `GET /api/tamizajes`, `GET /api/comprobantes` | El personal recibe solo los de sus escuelas |

## 12. `POST /api/reportes` (C-RS-04) — con o sin sesión

`{"tipo": "FALLA", "mensaje": "La app se cierra al abrir el mapa", "correo": "papa@example.com", "plataforma": "ANDROID"}`

- `tipo`: `FALLA` o `DUDA`. `mensaje`: 10 a 1000 caracteres. `correo` obligatorio sin sesión.
- **201** `{"id": 7}`. Lo recibe el **administrador global** (si aún no hay, toda Dirección) y lo atiende en el portal (Soporte).

## 13. Sistema de escritorio 🔑 (equipo vinculado)

**Ya no hay una clave compartida (`ESCRITORIO_API_KEY` / `X-Api-Key`).** Con ella, cualquiera que
instalara el escritorio y la tuviera podía buscar a todos los alumnos de la escuela. Ahora cada
computadora se **vincula** con la cuenta del portal de alguien de **Dirección o Enfermería**, como
cuando una tele pide "entra a esta página y escribe el código":

1. `POST /api/escritorio/vinculaciones` `{"equipo": "Enfermería PC1"}` → **201**
   `{"solicitud": "<secreto>", "codigo": "K7QM-3RTA", "ruta": "/escritorio/vincular?codigo=K7QM-3RTA", "expiraEnSegundos": 600, "intervaloSegundos": 3}`.
   Límite de 30 por minuto por IP. **503** si hay demasiadas solicitudes abiertas.
2. El escritorio abre `{servidor}{ruta}` en el navegador y muestra el `codigo`. La página pide
   correo, contraseña **y código de dos pasos** de la cuenta del portal (mismo bloqueo de 3 intentos)
   y que se confirme que el código coincide con el de la pantalla. Solo Dirección y Enfermería pueden aprobar (**403** a los demás).
   Queda en la bitácora (`VINCULAR_EQUIPO`).
3. El escritorio pregunta cada `intervaloSegundos`: `POST /api/escritorio/vinculaciones/estado`
   `{"solicitud": "<secreto>"}` → **202** `{"estado":"PENDIENTE"}` · **200**
   `{"estado":"APROBADA","token":"…","usuario":"enfermeria@…","nombre":"…","equipo":"Enfermería PC1"}`
   (el token se entrega **una sola vez**) · **410** `{"codigo":"VINCULACION_EXPIRADA"}` si venció o ya se usó.
4. Desde ahí, todas las rutas de abajo llevan `Authorization: Bearer <token del equipo>`.

El equipo trabaja con las **escuelas de la cuenta que lo aprobó**: solo busca y envía alumnos de ellas.
Las solicitudes se guardan en la tabla `vinculaciones_escritorio` (solo la huella SHA-256 del secreto),
así que funciona aunque haya varias instancias del servidor.

El token dura `ESCRITORIO_DIAS` (180) y va ligado a la cuenta que lo aprobó: deja de servir **al
instante** si esa cuenta se desactiva, cambia su contraseña o su rol (se compara el `security_stamp`).
Así se retira el acceso a un equipo. Las solicitudes duran 10 minutos.

**401** `{"codigo":"EQUIPO_NO_VINCULADO"}` sin token, con uno inválido o revocado (se registra como `ACCESO_DENEGADO`).

### `GET /api/escritorio/equipo`

`{"equipo": "Enfermería PC1", "usuario": "enfermeria@eyesofter.mx", "nombre": "…", "rol": "SUPERVISOR"}` — para "Probar conexión".

### `POST /api/escritorio/tamizajes` (C-RFN-02)

```json
{ "folio": "ESC-2026-0451", "matricula": "PRI2026001", "fecha": "2026-11-19", "semaforo": "ROJO",
  "agudezaOjoDerecho": "20/60", "agudezaOjoIzquierdo": "20/50", "observaciones": "Entrecierra los ojos" }
```

| Código | Cuándo |
|---|---|
| **201** | Nuevo: `{"tamizajeId":"12","casoId":5,"casoFolio":"EYS-2026-0003","tutoresAvisados":1,"duplicado":false}` |
| **200** | Ese `folio` ya se había recibido: `{"tamizajeId":"12","casoId":5,"duplicado":true}`. No se vuelve a notificar |
| **400** | Falta folio o matrícula, semáforo distinto de `VERDE/AMARILLO/ROJO`, fecha futura o mal escrita, agudeza que no es `20/NN`, observaciones de más de 500 |
| **422** | `{"error":"No hay ningún alumno con la matrícula … en las escuelas de este equipo","codigo":"ALUMNO_DESCONOCIDO"}` — darlo de alta en el portal y reenviar |
| **409** | `{"codigo":"MATRICULA_AMBIGUA"}` — la matrícula existe en más de una escuela del equipo (p. ej. lo vinculó el administrador global). Vincúlalo con una cuenta de una sola escuela |

En una sola transacción: guarda el tamizaje cifrado, lo asigna a la **campaña vigente** del plantel,
**abre caso** si es amarillo o rojo (o actualiza el caso abierto del alumno: uno por alumno), escribe
la línea de tiempo y deja el aviso al tutor. Si el tutor no tiene cuenta, alerta a Enfermería.

**Reintentos (C-RFNN-03):** el escritorio guarda en cola lo que no pudo enviar y reintenta con espera
creciente; como el folio es idempotente, reenviar nunca duplica. Si sigue fallando (p. ej. 30 min):

### `POST /api/escritorio/incidencias`

`{"mensaje": "Sin conexión a internet", "pendientes": 12, "desde": "2026-11-19T08:00:00Z", "equipo": "Enfermería PC1"}`
→ **201** `{"enfermeriaAvisada": 2}`. Enfermería recibe `ALERTA_ENFERMERIA` "Falla de sincronización".

### `GET /api/escritorio/alumnos?q=`

**Búsqueda**, no listado: `q` obligatorio de al menos 3 caracteres (si no, **400**); `%` y `_` se buscan
literalmente. Devuelve como máximo 20: `[{"matricula","nombre","apellidos","nivel","grado","grupo"}]`.
Cada búsqueda queda en la bitácora con la cuenta que vinculó el equipo (`BUSCAR_ALUMNOS`).

---

## Reglas que comparten el portal y el servidor

Si cambias una, cámbiala en los dos (`eyesofterweb/Servicios/Expedientes.cs` y `repos/Expedientes.kt`).

| Regla | Valor |
|---|---|
| Bloqueo de cuenta | 3 fallos seguidos → 15 min (`access_failed_count`, `lockout_end`) |
| Alcance por escuela | Personal: `usuario_planteles`; `administrador_global` = todas (`Seguridad/AlcanceEscuela.cs` y `Usuario.alcance`) |
| Matrícula | Única por escuela (`plantel_id`, `matricula`) |
| Convenio vigente | `activo` y (`vigente_hasta` nulo o ≥ hoy) para la escuela del alumno y la clínica |
| Folio de caso | `EYS-AAAA-NNNN` |
| Código del pase | 26 caracteres base32 sin letras confundibles (128 bits), un solo uso, 60 días |
| Pase solo para | Clínica registrada, asociada y activa; lo valida un especialista de ESA clínica |
| Un caso abierto por alumno | Un nuevo amarillo/rojo actualiza el caso abierto |
| Cerrar un caso | Requiere atención registrada o comprobante aprobado (W-RFN-04; solo en el portal) |
| Cifrado | AES-256-GCM: texto `"v1:" + Base64(nonce‖cifrado‖tag)`, archivos `"E1"` + lo mismo |
| Textos del semáforo | Verde "Saludable", Amarillo "En observación", Rojo "Atención urgente requerida" |
| Versión del aviso de privacidad | `VERSION_AVISO_PRIVACIDAD` (servidor) = `Eyesofter__VersionAvisoPrivacidad` (portal) |

---

## Historial de cambios del contrato

| Fecha | Cambio |
|---|---|
| 2026-11-21 (c) | **Seguridad.** `LoginRequestDto` agrega `codigo` (verificación en dos pasos); nuevos `REQUIERE_2FA`, `CODIGO_2FA_INVALIDO` (401) y `ACTIVAR_2FA` (403). La página de vinculación del escritorio pide el código. Token del personal: 12 h. Cabecera HSTS. |
| 2026-11-21 (b) | **Varias escuelas.** El personal solo ve lo de sus escuelas (`usuario_planteles`; `administrador_global` ve todas); lo de otra escuela es 404. Alertas a la Enfermería de la escuela del alumno. Avisos y seguimiento por escuela. Reportes de fallas al administrador global. El escritorio trabaja con las escuelas de quien lo vinculó; matrícula única por escuela y nuevo **409** `MATRICULA_AMBIGUA`. Vinculaciones en la base. **Convenios:** `convenio` en `Optica`, `ClinicaDto` (con sesión de tutor) y `PaseDto`. |
| 2026-11-21 | **Escritorio: adiós a `ESCRITORIO_API_KEY`.** Cada equipo se vincula en el navegador con una cuenta de Dirección o Enfermería (`/api/escritorio/vinculaciones`, `/escritorio/vincular`) y usa `Authorization: Bearer <token del equipo>`. `GET /api/escritorio/alumnos` exige `q` de 3+ caracteres, devuelve máx. 20 y ya no trae `usaLentes`. Nueva `GET /api/escritorio/equipo`. La bitácora registra quién envía y quién busca. |
| 2026-11-20 | **Base PostgreSQL compartida con el portal.** Login con el correo y las cuentas de Identity (bloqueo compartido, 403 cuenta desactivada; el token deja de servir si se desactiva la cuenta o cambia la contraseña). `UserDto` agrega `id` y `requiereConsentimiento`. Nuevas: `/api/consentimiento`, `/api/clinicas`, destino/pase.png/citas (§9), `/api/reportes`, `/api/escritorio/incidencias` y `/alumnos`. `TamizajeDto` agrega `alumnoId`, `campana` y `caso` (pase, citas, línea de tiempo); ids numéricos en texto. **El escritorio manda `matricula` y agudezas en lugar de `alumnoNombre`/`tutorUsername`.** Comprobantes agregan `casoId` y `alumno`; `tutor` es el correo. `/opticas/cercanas` sale de la base: asociadas primero, agrega `tipo` y `horario`. `/salud` revisa la base y agrega `pendientesEnvio`. `GET /` responde JSON. |
| 2026-11-19 (b) | `/salud` agrega `version`, `push`, `correo`, `escritorio`. Login responde 429 tras 3 fallos. Recetas y comprobantes. CORS permite `DELETE`. |
| 2026-11-19 | El `token` del login es un JWT real. Notificaciones, confirmación de lectura, dispositivos, tamizajes, avisos, seguimiento y recepción desde el escritorio. |
| 2026-11-03 | Separación en dos proyectos. `/salud` y `/opticas/cercanas` en el servidor. |
