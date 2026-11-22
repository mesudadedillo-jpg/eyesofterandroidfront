# Seguridad — App móvil EYESOFTER

| Área | Medida |
|---|---|
| Conexión | Solo HTTPS en la versión de release (`usesCleartextTraffic=false`) |
| Inicio de sesión | Mismas cuentas del portal, con verificación en dos pasos cuando la cuenta la tiene |
| En el teléfono | Sin respaldo en la nube ni copia a otro teléfono (`allowBackup=false`, reglas de extracción); lo guardado se borra al cerrar sesión |
| Sesión | El token del personal dura 12 h; si el servidor lo rechaza, la app vuelve al login |

## Reportar una vulnerabilidad

Si encuentras una falla de seguridad, **no abras un issue público**. Usa
*Security → Report a vulnerability* de este repositorio (reporte privado de GitHub) o el formulario
*Reportar una falla* del portal. Responderemos lo antes posible.

## Datos y secretos

- **Todos los datos de este repositorio son ficticios** (nombres, CURP, teléfonos y correos de ejemplo).
  Ningún dato real de alumnos o familias se guarda en el código.
- **No hay secretos en el repositorio.** Llaves de cifrado, secretos de sesión, contraseñas y conexiones
  a la base viven solo en las variables de entorno de Railway.
- Las cuentas demo (contraseña `Eyesofter123!`) existen solo en desarrollo. En producción el portal
  desactiva al arrancar cualquier cuenta demo que conserve esa contraseña.

