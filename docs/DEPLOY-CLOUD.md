# MediConnect Pro — Preparación de despliegue público

**Estado:** plantillas y CD creados, sin URL ni credenciales públicas verificadas. Aprobación de costes y conexión de cuentas deben hacerlas el dueño del proyecto.

## Servicios y costes

El backend requiere **PostgreSQL administrado**, **MongoDB Atlas** (notas/archivos) y **Redis compatible** (cachés). La plantilla del proyecto ahora apunta a **Render Free** y reutiliza una base gratuita ya existente, sin crear una segunda. Esto no garantiza que todo el despliegue sea gratuito: comprobar cualquier coste antes de aprobar un servicio. La base existente vence el **12 de diciembre de 2026** salvo renovación/migración. No migrar datos ficticios de Docker ni reutilizar contraseñas locales.

## Orden de activación

1. **MongoDB Atlas gratuito identificado:** proyecto `Project 0`, clúster `Cluster0` M0, región AWS `US_EAST_1`, versión 8.0.37. Crear un usuario de aplicación **limitado a `readWrite` en la base `mediconnect`** (no reutilizar el usuario `atlasAdmin`), permitir acceso únicamente del backend o restringir temporalmente según la IP de salida, y configurar `MONGO_URI` como secreto. Las credenciales de aplicación y la conectividad Render todavía no están verificadas.
2. **Redis gratuito preparado:** instancia Render Key Value `mediconnect-pro-cache` (`red-dbrah756e07s73b2s4o0`, plan Free, Oregon, versión Redis compatible 8.1.10), sin persistencia. Obtener su URL de conexión interna desde Render y configurar `REDIS_URL` como secreto del backend, con TLS si lo requiere el proveedor.
3. En Render, **New → Blueprint → Connect GitHub** y seleccionar `leopl72001/mediconnect-pro-backend` (archivo `render.yaml`). **La base PostgreSQL ya existe:** `biblioteca-db`, base `biblioteca_db_w5no`, región Oregon. La plantilla **no crea otra**: configura DB_HOST con el hostname interno y DB_NAME, DB_USERNAME, DB_PASSWORD con los valores de Render. El backend debe estar en la **misma región Oregon** para usar la conectividad interna. Revisar importes y confirmar antes de crear el servicio web.
4. Ingresar `MONGO_URI`, `REDIS_URL`, `JWT_SECRET` (mínimo 64 bytes aleatorios; nuevo y distinto al de Docker), `DEV_ADMIN_EMAIL` y `DEV_ADMIN_PASSWORD` si se necesita cuenta de demo. No pegarlos en chat, tickets, código ni README; usar secretos del proveedor.
5. **Bloqueo comprobado 13/11/2026:** el acceso SQL externo a `biblioteca-db` fue denegado porque su lista de IP permitidas está vacía. Verificar desde el panel de Render o una conexión interna que la base PostgreSQL seleccionada está **vacía** antes de iniciar el perfil `prod`: Flyway ejecuta la primera migración y Hibernate valida su esquema. Esperar despliegue y comprobar `https://<API_REAL>/actuator/health` = `UP`. No conectar a una base previamente poblada sin revisar migraciones.
6. En Render crear segundo Blueprint desde `leopl72001/mediconnect-pro-frontend` (su `render.yaml`). En `BACKEND_ORIGIN` escribir `https://<API_REAL>` sin barra final.
7. Abrir `https://<UI_REAL>/`. Probar registro JWT, especialistas, reserva, SSE y almacenamiento privado con **datos ficticios**. En plan Free puede existir suspensión por inactividad y latencia al despertar; verificar si SSE se reconecta. Nginx sirve SPA y proxy `/api`, por lo que el navegador no envía JWT entre orígenes externos.
8. GitHub Actions en ambos repos publica imágenes a GHCR en cada push a `main`. Si se quiere disparar Render desde GitHub, añadir secreto `RENDER_DEPLOY_HOOK_URL` **en cada repositorio** con el hook correcto. Publicar imagen no significa haber desplegado URL.
9. Opcional SonarCloud: crear dos proyectos y en Actions Secrets poner `SONAR_TOKEN`, `SONAR_PROJECT_KEY`, `SONAR_ORGANIZATION`; esperar dashboard real.

## Evidencia obligatoria

- Links reales de backend health y frontend, capturas de ejecución JWT y SSE.
- Runs verdes de CI, Testcontainers y E2E.
- Reportes JaCoCo y Vitest con % auténtico. La meta de 80% solo cuenta cuando el informe supera 80%.
- Estado de SonarCloud conectado. Sin proyecto/token, el paso de análisis queda saltado, no aprobado.

## Advertencias de diseño

- Nginx frontend requiere `BACKEND_ORIGIN` público correctamente configurado.
- Render no crea automáticamente MongoDB Atlas ni Redis si son externos. **Ya existen Atlas M0 y Render Key Value Free**, pero no están conectados al backend y no equivalen a un despliegue público funcional. La base PostgreSQL de Render sí existe, pero los servicios web aún no están publicados.
- Se eligió una instancia backend; SSE en memoria no garantiza notificaciones entre réplicas distintas.
- Para producción clínica real falta cifrar cola offline IndexedDB, autorización clínica fina, auditoría y almacenamiento de archivos robusto. Solo demo académica ficticia.
- No reemplazar/modificar la versión anterior `leopl72001/mediconnect`.
