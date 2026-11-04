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
| Rutas | `src/main/kotlin/com/example/eyesofterapp/routes/` | `data/network/AuthApi.kt` y `data/network/OpticasApi.kt` |

## URLs base

| Entorno | URL |
|---|---|
| Producción (Railway) | `https://eyesofterandroid-production.up.railway.app` |
| Local, desde la misma computadora | `http://localhost:8080` |
| Local, desde el emulador de Android | `http://10.0.2.2:8080` |

Todas las respuestas son JSON en UTF-8, excepto `GET /`.
Los errores siempre tienen la forma `{"error": "mensaje en español"}`.

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
{ "status": "ok", "db": false }
```

- `db` es `false` mientras no haya base de datos conectada. Cuando se conecte MySQL será `true` si responde.
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
| 500 | El cuerpo no es JSON válido o falta un campo | `{"error": "<detalle>"}` |

**200 OK** — `LoginResponseDto`

```json
{
  "token": "jwt-mock-token-1793763817502",
  "user": {
    "username": "admin",
    "fullName": "Administrador del Sistema",
    "role": "ADMINISTRADOR"
  }
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `token` | String | **Simulado**: todavía no es un JWT real ni se valida en ninguna ruta |
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

## Historial de cambios del contrato

| Fecha | Cambio |
|---|---|
| 2026-11-03 | Separación en dos proyectos. Se implementan `/salud` y `/opticas/cercanas` en el servidor; se documentan errores, parámetros y campos. |
