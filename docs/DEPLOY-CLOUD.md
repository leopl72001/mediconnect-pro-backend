# MediConnect Pro — Preparación de despliegue público

**Estado:** plantillas y CD creados, sin URL ni credenciales públicas verificadas. Aprobación de costes y conexión de cuentas deben hacerlas el dueño del proyecto.

## Servicios y costes

El backend requiere **PostgreSQL administrado**, **MongoDB Atlas** (notas/archivos) y **Redis compatible** (cachés). Render Starter + PostgreSQL de pago podrían generar cargos. Revisar precios vigentes **antes de crear recursos**. No hay que migrar los datos ficticios de Docker ni reutilizar contraseñas locales.

## Orden de activación

1. Crear una instancia MongoDB Atlas y credenciales específicas del proyecto, permitir tráfico solo del backend, obtener `MONGO_URI` con `authSource` apropiado.
2. Configurar Redis externo con `REDIS_URL`; si el proveedor exige TLS, usar `rediss://` y certificar conexión.
3. En Render, **New → Blueprint → Connect GitHub** y seleccionar `leopl72001/mediconnect-pro-backend` (archivo `render.yaml`). Leer los importes mostrados y aprobar únicamente con autorización del dueño. Render crea Postgres y el servicio Docker.
4. Ingresar `MONGO_URI`, `REDIS_URL`, `JWT_SECRET` (mínimo 64 bytes aleatorios; nuevo y distinto al de Docker), `DEV_ADMIN_EMAIL` y `DEV_ADMIN_PASSWORD` si se necesita cuenta de demo. No pegarlos en chat, tickets, código ni README; usar secretos del proveedor.
5. Esperar despliegue y comprobar `https://<API_REAL>/actuator/health` = `UP`. `prod` ejecuta Flyway sobre una **base nueva**, no sobre una DB Docker ya poblada.
6. En Render crear segundo Blueprint desde `leopl72001/mediconnect-pro-frontend` (su `render.yaml`). En `BACKEND_ORIGIN` escribir `https://<API_REAL>` sin barra final.
7. Abrir `https://<UI_REAL>/`. Probar registro JWT, especialistas, reserva, SSE y almacenamiento privado con **datos ficticios**. Nginx sirve SPA y proxy `/api`, por lo que el navegador no envía JWT entre orígenes externos.
8. GitHub Actions en ambos repos publica imágenes a GHCR en cada push a `main`. Si se quiere disparar Render desde GitHub, añadir secreto `RENDER_DEPLOY_HOOK_URL` **en cada repositorio** con el hook correcto. Publicar imagen no significa haber desplegado URL.
9. Opcional SonarCloud: crear dos proyectos y en Actions Secrets poner `SONAR_TOKEN`, `SONAR_PROJECT_KEY`, `SONAR_ORGANIZATION`; esperar dashboard real.

## Evidencia obligatoria

- Links reales de backend health y frontend, capturas de ejecución JWT y SSE.
- Runs verdes de CI, Testcontainers y E2E.
- Reportes JaCoCo y Vitest con % auténtico. La meta de 80% solo cuenta cuando el informe supera 80%.
- Estado de SonarCloud conectado. Sin proyecto/token, el paso de análisis queda saltado, no aprobado.

## Advertencias de diseño

- Nginx frontend requiere `BACKEND_ORIGIN` público correctamente configurado.
- Render no crea automáticamente MongoDB Atlas ni Redis si son externos.
- Se eligió una instancia backend; SSE en memoria no garantiza notificaciones entre réplicas distintas.
- Para producción clínica real falta cifrar cola offline IndexedDB, autorización clínica fina, auditoría y almacenamiento de archivos robusto. Solo demo académica ficticia.
- No reemplazar/modificar la versión anterior `leopl72001/mediconnect`.
