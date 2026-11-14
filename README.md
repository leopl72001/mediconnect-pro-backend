# MediConnect Pro — Backend (Proyecto de Grado)

## Entrega del proyecto de grado — MediConnect Pro

**MediConnect Pro** es una aplicación académica full-stack para la gestión de citas y registros clínicos de demostración. Implementa flujos diferenciados de **paciente, médico y administrador**, con autenticación JWT y persistencia en PostgreSQL y MongoDB. **No es un sistema clínico certificado ni debe utilizarse con datos de pacientes reales.**

### Enlaces de la solución

| Recurso | Enlace |
| --- | --- |
| Aplicación web desplegada | [Abrir MediConnect Pro](https://mediconnect-pro-frontend.onrender.com) |
| API Spring Boot | [Backend en Render](https://mediconnect-pro-backend.onrender.com) |
| Estado de la API | [Actuator Health](https://mediconnect-pro-backend.onrender.com/actuator/health) |
| Código fuente backend | [GitHub — Java / Spring Boot](https://github.com/leopl72001/mediconnect-pro-backend) |
| Código fuente frontend | [GitHub — Angular](https://github.com/leopl72001/mediconnect-pro-frontend) |

> **Video de sustentación (5–8 minutos):** pendiente de subir. Cuando esté publicado, añadir aquí su URL pública de YouTube o Google Drive. El formulario de entrega también requiere pegar esa URL en su campo correspondiente.

### ¿Qué problema resuelve?

Centraliza en una sola plataforma las funciones principales de una consulta médica de demostración: catálogo de especialistas, reserva y cancelación de citas, agenda del médico, consulta de notas clínicas y prescripciones, perfil del paciente y archivos privados de prueba. Ofrece una interfaz en español con permisos por rol.

### Tecnologías y arquitectura

- **Frontend:** Angular 21, TypeScript, Signals, Tailwind CSS 4, Nginx.
- **Backend:** Java 25, Spring Boot 4, Spring Security, JWT, Maven.
- **Datos relacionales:** PostgreSQL, con migraciones Flyway (usuarios, médicos, consultorios, especialidades, citas y prescripciones).
- **Datos documentales y archivos:** MongoDB Atlas (notas clínicas y GridFS).
- **Caché:** Redis; **notificaciones de agenda:** SSE.
- **Despliegue:** dos servicios Docker independientes en Render, con Nginx como proxy inverso hacia la API Spring Boot.
- **Calidad:** JUnit, JaCoCo, Vitest, Cypress y GitHub Actions.

### Demostración funcional realizada (noviembre de 2026)

Con cuentas y datos **ficticios** se comprobaron manualmente estos recorridos en la aplicación publicada:

1. Registro e inicio de sesión de paciente; autenticación JWT y navegación por rol.
2. Acceso de administrador y creación de médico y consultorio de prueba.
3. Actualización del perfil del paciente con confirmación de guardado en PostgreSQL.
4. Creación y consulta de cita, cancelación y actualización del estado; agenda del médico.
5. Creación de una nota clínica por el médico y consulta de esa nota en la cuenta del paciente.
6. Creación de una prescripción por el médico y consulta por el paciente; el resultado de OpenFDA fue `MANUAL_VALIDATION_REQUIRED`, **no una validación automática aprobada**.
7. Subida y visualización de archivo privado de prueba en MongoDB GridFS.
8. Indicador `SSE en vivo` durante las pruebas de sesiones autenticadas.

Estas son **pruebas funcionales de demostración**; no constituyen auditoría de seguridad, certificación médica ni garantía de disponibilidad.

### Pruebas automatizadas y cobertura reportada

| Suite | Resultado reportado |
| --- | --- |
| Backend JUnit | 95/95 pruebas; JaCoCo 82,36 % líneas y 82,17 % ramas |
| Frontend Vitest | 235/235 pruebas; 95,78 % líneas, 87,86 % ramas, 81,20 % funciones y 92,13 % instrucciones |
| Cypress | Recorridos de autenticación, reservas y privacidad entre cuentas documentados |

Los porcentajes proceden de las últimas ejecuciones documentadas en los repositorios, **no de una nueva ejecución realizada para esta actualización del README**. Las reglas de cobertura ≥80 % están configuradas en CI. **No se afirma contar con una validación real de SonarCloud**.

### Cómo evaluar el sistema

1. Acceder a [la aplicación web](https://mediconnect-pro-frontend.onrender.com).
2. Crear un paciente **ficticio** mediante «Registrarse» o iniciar sesión con una cuenta de prueba existente.
3. Explorar Especialistas, Mis citas, Mi perfil, Historia clínica, Mis recetas y Mis archivos.
4. Para probar administración y agenda médica se necesitan cuentas de esos roles creadas de manera autorizada; **no se publican contraseñas ni tokens**.
5. Para reproducir las pruebas automatizadas o ejecutar el sistema localmente, consultar las instrucciones técnicas más abajo en este README y el repositorio complementario.

### Limitaciones y seguridad

- Render Free suspende instancias por inactividad: el primer acceso puede tardar varios minutos; un despliegue `Live` no implica disponibilidad continua.
- PostgreSQL Free del entorno de demostración tiene fecha de vencimiento anunciada para **12 de diciembre de 2026**: antes de esa fecha se debe respaldar o migrar la información.
- Algunas comprobaciones de OpenFDA requieren revisión manual.
- Las estadísticas avanzadas y el modo demostración pueden usar datos ilustrativos; distinguirlos de los flujos con JWT real.
- La cola offline IndexedDB no está cifrada: utilizar exclusivamente documentos inventados y no subir datos de salud reales.
- El entorno es **académico**; requiere controles adicionales antes de cualquier uso productivo.

### Entrega y video (5–8 minutos)

Para el formulario universitario:

1. **Repositorio GitHub público:** colocar la URL de este repositorio como principal; el otro repositorio está enlazado en la tabla anterior.
2. **Video público:** grabar la pantalla mostrando el problema, la arquitectura, los roles, el registro, la cita y su cancelación, la historia clínica, la receta, el archivo de prueba, los repositorios y el despliegue. Publicarlo en YouTube o Drive con permisos de acceso adecuados y pegar la URL en el campo del formulario.
3. **Verificar antes de enviar:** abrir ambos enlaces en una ventana privada y comprobar que el video sea accesible sin solicitar permisos.

---


> **Versión 2 independiente, Java 25 / Spring Boot 4.0.9.** Partimos de [MediConnect v1](https://github.com/leopl72001/mediconnect), ya entregado y conservado sin modificaciones. El frontend Angular 21+ está en [mediconnect-pro-frontend](https://github.com/leopl72001/mediconnect-pro-frontend). Las fases 6 y 7 se validaron en la interfaz; la fase 8 se probó localmente (archivos privados y cola offline). Fase 9: nuevas pruebas de seguridad y de negocio integradas en GitHub.

**Ruta de desarrollo y criterios del proyecto de grado:** [docs/capstone-roadmap.md](docs/capstone-roadmap.md). **Gobernanza de Cursor AI:** [.cursorrules](.cursorrules). El frontend Angular 21+ se desarrolla en el repositorio independiente enlazado arriba.

---

## Base heredada de MediConnect v1

**Proyecto universitario independiente**, migrado a Java 25 y Spring Boot 4, con Maven, Spring Security, PostgreSQL, MongoDB y Redis.

## Características

- Registro de pacientes y login con **JWT HS512**, BCrypt y roles PATIENT, DOCTOR y ADMIN.
- Perfil de paciente (consulta y actualización de fecha de nacimiento, teléfono y dirección), especialidades, médicos, consultorios y administración protegida.
- Citas transaccionales con bloqueo pesimista del médico, rechazo de solapamientos **409 Conflict**, cancelación con más de 24 h.
- Notas clínicas en MongoDB: solo escribe el médico asignado; el paciente consulta exclusivamente las suyas; auditoría de autor y fecha.
- Prescripciones con consulta a OpenFDA, timeout de 1.5 s, Resilience4j Circuit Breaker y fallback `MANUAL_VALIDATION_REQUIRED`.
- Redis con caché de médicos (TTL 10 minutos) y medicamentos validados (TTL 24 horas).
- DTOs con records, MapStruct, ProblemDetail, Swagger/OpenAPI, Actuator, métricas y virtual threads.
- Docker Compose, Postman, pruebas JUnit/Mockito y CI GitHub Actions.
- **Fase 7:** avisos SSE privados de nuevas citas y cancelaciones, enviados únicamente a paciente y médico asignado después de confirmar la transacción. [Contrato y prueba](docs/FASE7-SSE.md).
- **Fase 8 (validada en entorno local con datos ficticios):** archivos privados de demostración (WebP/PNG/JPEG/PDF, máximo 5 MB) en MongoDB GridFS. Acceso exclusivo del paciente autenticado mediante JWT; validación de firma de archivo, protección contra lectura cruzada, descarga como adjunto. [Contrato de medios](docs/FASE8-MEDIOS.md).

## Opción A: ejecutar sin Docker (DEV)

Requisitos: **JDK 25, Maven 3.9+** y Git.

```powershell
git clone https://github.com/leopl72001/mediconnect-pro-backend.git
cd mediconnect-pro-backend
mvn test
mvn spring-boot:run
```

En `dev`, el servidor usa H2 en memoria y desactiva MongoDB/Redis. Los endpoints de notas clínicas solo están habilitados en los perfiles `docker` y `prod`.

- Health: http://localhost:8081/actuator/health
- Swagger: http://localhost:8081/swagger-ui.html
- Especialidades: http://localhost:8081/api/v1/specialties

`mvn spring-boot:run` permanece activo mientras la API está ejecutándose. Para detenerla, Ctrl+C.

## Opción B: ejecutar todo con Docker Desktop (perfil docker)

```powershell
git clone https://github.com/leopl72001/mediconnect-pro-backend.git
cd mediconnect-pro-backend
Copy-Item .env.example .env
```

**Edita `.env` y cambia las contraseñas, el correo admin y `JWT_SECRET` (64+ bytes) antes de continuar.** No compartas ese archivo ni lo subas a GitHub. Evita caracteres reservados de URL en las credenciales de Mongo, o codifícalos.

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

La aplicación escucha en **http://127.0.0.1:8082**. PostgreSQL usa **5434**, MongoDB **27019** y Redis **6381**, enlazados a localhost para no interferir con MediConnect v1.

El perfil `docker` utiliza `ddl-auto=update` **solo para demostración local**. El perfil `prod` usa Flyway V1 para crear una base nueva y `ddl-auto=validate`. **No conectar este perfil a una base ya poblada sin una migración/baseline evaluados.**

## Flujo rápido

1. `POST /api/v1/auth/register` registra un paciente.
2. `POST /api/v1/auth/login` devuelve `accessToken`.
3. Paciente: `GET /api/v1/patients/me` y `PUT /api/v1/patients/me`.
4. Admin: `POST /api/v1/admin/offices` y `POST /api/v1/admin/doctors`.
5. Paciente: `POST /api/v1/appointments`, `GET /api/v1/appointments/me`, `PATCH /api/v1/appointments/{id}/cancel`.
6. Médico: `GET /api/v1/appointments/doctor/me`, `POST /api/v1/appointments/{id}/clinical-notes` (Docker), `POST /api/v1/appointments/{id}/prescriptions`.
7. Paciente: `GET /api/v1/clinical-notes/me` (Docker), `GET /api/v1/prescriptions/me`.
8. Médico o paciente autenticado: `GET /api/v1/notifications/stream` con JWT en la cabecera `Authorization` para recibir eventos `appointment-changed` por SSE.
9. Paciente: `GET /api/v1/media/me`, `POST /api/v1/media/me` (multipart `file`) y `GET /api/v1/media/me/{id}` para descargar sus propios archivos de prueba; habilitado solo en `docker/prod`.

Importa `postman/MediConnect.postman_collection.json` y completa las variables de credenciales y IDs. Nunca pegues JWT reales en un repositorio público.

## Administrador de desarrollo

En DEV, antes de iniciar el servidor, puedes establecer:

```powershell
$env:DEV_ADMIN_EMAIL='admin@local.test'
$env:DEV_ADMIN_PASSWORD='TuClaveSeguraDePrueba'
mvn spring-boot:run
```

En Docker, las mismas variables se leen del archivo `.env`. No existe registro público de administradores.

## Proyecto de grado: calidad, cloud y defensa

**Arquitectura:** [diagramas y decisiones](docs/ARQUITECTURA-CAPSTONE.md).
**Despliegue:** [configurar Render, MongoDB y Redis externos](docs/DEPLOY-CLOUD.md). La solución académica ya tiene frontend y backend desplegados públicamente en Render; MongoDB Atlas y Redis se utilizan como servicios de datos. El alojamiento gratuito puede suspenderse por inactividad.
**Frontend, pruebas Cypress y defensa:** [mediconnect-pro-frontend](https://github.com/leopl72001/mediconnect-pro-frontend).

- `mvn clean verify` genera `target/site/jacoco/index.html` y `jacoco.xml`; `python3 scripts/report-coverage.py` muestra % real (opcional `--minimum 80`). Última medición integrada de CI: **82,36 % líneas / 82,17 % ramas** y **95/95 pruebas**, incluida validación de límites. La meta de **80 % del backend está aprobada y se exige en CI**; el frontend también tiene reportada cobertura ≥80 % en sus cuatro métricas. Detalle en [auditoría de cierre](docs/ESTADO-FINAL-CAPSTONE.md).
- **Testcontainers:** `mvn -Dtest=SpecialtyPostgresIT,FlywayPostgresIT test` (requiere Docker Desktop); prueba repositorio JPA, esquema Flyway y Hibernate validate en PostgreSQL 17 temporal.
- **CD:** `.github/workflows/publish-cloud.yml` publica imágenes a GHCR en pushes a main; Render solo se activa al configurar el proyecto y sus secretos, y opcionalmente un deploy hook.
- **SonarCloud:** los jobs requieren tokens y proyectos configurados. Un workflow saltado no equivale a un dashboard aprobado.
- **Validación pendiente:** dashboard SonarCloud real y verificación continua de disponibilidad cloud; las coberturas y la URL pública están documentadas en la sección de entrega.

## Calidad y alcance

```powershell
mvn test
```

GitHub Actions ejecuta pruebas de Spring Boot, seguridad y reglas de negocio. En pruebas locales de noviembre de 2026 se verificó el despliegue Docker de PostgreSQL, MongoDB y Redis con salud `UP`, además de autenticación, reservas, seguridad por roles, notas clínicas y recetas. La fase 7 ya se validó visualmente con dos sesiones: la agenda médica se actualizó sola por SSE. La fase 8 incluye pruebas de formatos y privacidad; la subida, descarga, conversión WebP y sincronización offline se comprobaron manualmente con archivos ficticios en Docker. Cypress automatiza además la separación de archivos entre cuentas. Estas pruebas son **evidencia funcional académica**, no una certificación para producción.

## Entrega universitaria: repositorio y video

El código fuente se entrega mediante este repositorio público. Para la explicación oral, sigue el [guion de sustentación de 5 a 7 minutos](docs/video-script.md): incluye las pantallas que debes mostrar, las pruebas representativas y precauciones para no exponer secretos. El [estado de la entrega](docs/plan-de-entrega.md) recoge los resultados comprobados. No se requiere un informe escrito para la entrega indicada.

Documentación adicional: [trazabilidad técnica](docs/requirements-traceability.md), [estado de la entrega](docs/plan-de-entrega.md) y [guion del video](docs/video-script.md).
