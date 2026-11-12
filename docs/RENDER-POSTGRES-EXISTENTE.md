# Conectar MediConnect Pro a PostgreSQL existente en Render

Este proyecto usa **Java 25, Spring Boot 4, PostgreSQL, MongoDB y Redis**. El perfil `prod` ya lee `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`. Spring Boot utiliza Flyway para crear el esquema en una base de datos **vacía**.

## Base PostgreSQL que ya está creada

- Servicio Render: `biblioteca-db` (PostgreSQL 18, plan gratuito).
- Nombre real de la base de datos: `biblioteca_db_w5no`.
- Nombre de usuario: `biblioteca_db_w5no_user`.
- Región: Oregon, EE. UU.
- Caducidad indicada por Render: **12 de diciembre de 2026**. Para conservar los datos después de esa fecha hace falta un plan de pago o migrarlos previamente. No usar para información clínica real.

## Configuración de variables de entorno

En el servicio web de Render, usa **Environment**. Obtén `DB_HOST` de la sección **Internal Database URL / Hostname** de la base existente. La base y el backend deben estar en la misma región y contar con conectividad interna.

| Variable | Valor / origen |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `8081` |
| `DB_HOST` | Hostname interno de `biblioteca-db` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `biblioteca_db_w5no` |
| `DB_USERNAME` | `biblioteca_db_w5no_user` |
| `DB_PASSWORD` | Contraseña de PostgreSQL desde Render; nunca subir a GitHub |
| `MONGO_URI` | URI de MongoDB Atlas, con credenciales privadas |
| `REDIS_URL` | URL del servicio Redis, con TLS si lo exige el proveedor |
| `JWT_SECRET` | Clave aleatoria nueva de 64 bytes o más |

No es suficiente configurar solo PostgreSQL: MongoDB Atlas y Redis son dependencias necesarias del perfil `prod`. No se ha creado ni desplegado un servicio web y no se ha verificado todavía ninguna conexión JDBC.

## Render Blueprint

El archivo `render.yaml` está preparado para **reutilizar PostgreSQL existente**. No contiene una sección `databases` que cree una segunda instancia. Su servicio web usa plan `free`; verificar en el panel la elegibilidad y restricciones vigentes antes de aprobar. Los secretos están marcados `sync: false` para ingresarlos en Render.

**No autorizar un despliegue hasta tener MongoDB, Redis y JWT_SECRET listos.** Si se usa Blueprint, Render puede solicitar todas las variables `sync: false` en el flujo de creación.

## Comprobaciones después de configurar

1. Revisar **Logs** del servicio web, sin publicar secretos.
2. Comprobar `/actuator/health` (esperado: `UP`).
3. Confirmar ejecución de Flyway con la base vacía; `ddl-auto=validate` valida el esquema.
4. Hacer pruebas funcionales con **datos ficticios**, sin pacientes reales.
5. Guardar evidencias y planificar exportación o migración antes del vencimiento del plan Free.
