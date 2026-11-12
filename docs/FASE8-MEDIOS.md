# Fase 8 — Archivos privados de demostración (MongoDB GridFS)

## API (perfil docker/prod)

| Método | Recurso | Acción |
|---|---|---|
| GET | `/api/v1/media/me` | Lista un máximo de 100 archivos de prueba propios |
| POST | `/api/v1/media/me` | Multipart `file` (WebP/JPEG/PNG/PDF, ≤5 MB), almacenamiento privado en MongoDB GridFS |
| GET | `/api/v1/media/me/{id}` | Descarga el archivo propio como adjunto |

**Autorización:** únicamente JWT con rol `PATIENT`. Cada registro contiene `metadata.owner` derivado del JWT y se verifica de nuevo para descargar. Para un ID ajeno o inexistente la respuesta es 404, sin revelar la existencia del archivo. El paciente no puede leer las cargas de otro paciente; las cuentas médica y administrador no tienen acceso por esta API.

**Protecciones:** tamaño limitado a 5 MB tanto por Spring Multipart como por el servicio; validación por cabecera binaria, sin confiar en la extensión ni `Content-Type` proporcionados; nombre saneado con extensión canónica; descargas `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox` y `Cache-Control: no-store`. Sin endpoints públicos de archivos.

## Cliente Angular

- Imágenes: Canvas reduce lado máximo a 1600 píxeles y guarda WebP calidad 0.8 antes del envío. PDF se almacena sin transformación.
- En estado offline o fallo de red, la imagen procesada se conserva en IndexedDB con el correo de la cuenta y se sincroniza al volver la conexión **solo si hay sesión JWT activa**.
- La cola se puede inspeccionar, reintentar o descartar. Si se recarga la página, la sesión JWT debe iniciarse otra vez (sin guardar el token en el navegador). Los datos diferidos continúan en IndexedDB.
- **Limitación crítica:** IndexedDB conserva los archivos locales sin cifrado con clave de usuario. Es una demostración de resiliencia, **NO es seguro para imágenes clínicas reales**. Para uso real: cifrado en reposo, gestión segura de claves, expiración y borrado verificable, consentimiento, auditoría, antivirus, retención y almacenamiento empresarial como S3 con KMS.
- El almacenamiento del prototipo está en MongoDB GridFS del contenedor persistente; al desplegar en cloud deberá validarse persistencia y capacidad separadas del ciclo de vida de cada instancia.

## Prueba

Revisa la guía del frontend: `docs/prueba-fase8-medios.md`.

Las pruebas unitarias verifican formatos prohibidos, límite de 5 MB y lectura cruzada de cuentas. El éxito de CI no reemplaza la validación manual Docker de subida y recuperación offline.
