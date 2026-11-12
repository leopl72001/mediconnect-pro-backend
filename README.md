# MediConnect Pro — Backend (Proyecto de Grado)

> **Versión 2 independiente, Java 25 / Spring Boot 4.0.9.** Partimos de [MediConnect v1](https://github.com/leopl72001/mediconnect), ya entregado y conservado sin modificaciones. El frontend Angular 21+ está en [mediconnect-pro-frontend](https://github.com/leopl72001/mediconnect-pro-frontend). Las fases 6 y 7 están validadas en la interfaz: autenticación real y avisos SSE sin refrescar. La fase 8 añade archivos de prueba privados y cola offline.

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
- **Fase 8 (en pruebas):** archivos privados de demostración (WebP/PNG/JPEG/PDF, máximo 5 MB) en MongoDB GridFS. Acceso exclusivo del paciente autenticado mediante JWT; validación de firma de archivo, protección contra lectura cruzada, descarga como adjunto. [Contrato de medios](docs/FASE8-MEDIOS.md).

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
**Despliegue:** [configurar Render, MongoDB y Redis externos](docs/DEPLOY-CLOUD.md).
**Frontend, pruebas Cypress y defensa:** [mediconnect-pro-frontend](https://github.com/leopl72001/mediconnect-pro-frontend).

- `mvn clean verify` genera `target/site/jacoco/index.html` y `jacoco.xml`; `python3 scripts/report-coverage.py` muestra % real (opcional `--minimum 80`). Última medición en CI: **42.73 % líneas / 34.39 % ramas**. La meta de **80 % todavía no está aprobada**.
- **Testcontainers:** `mvn -Dtest=SpecialtyPostgresIT,FlywayPostgresIT test` (requiere Docker Desktop); prueba repositorio JPA, esquema Flyway y Hibernate validate en PostgreSQL 17 temporal.
- **CD:** `.github/workflows/publish-cloud.yml` publica imágenes a GHCR en pushes a main; Render solo se activa al configurar el proyecto y sus secretos, y opcionalmente un deploy hook.
- **SonarCloud:** los jobs requieren tokens y proyectos configurados. Un workflow saltado no equivale a un dashboard aprobado.
- **Meta pendiente:** cobertura ≥80 %, tres Cypress verdes, URL pública funcional y dashboard SonarCloud verificado.

## Calidad y alcance

```powershell
mvn test
```

GitHub Actions ejecuta pruebas de Spring Boot, seguridad y reglas de negocio. En pruebas locales de noviembre de 2026 se verificó el despliegue Docker de PostgreSQL, MongoDB y Redis con salud `UP`, además de autenticación, reservas, seguridad por roles, notas clínicas y recetas. La fase 7 ya se validó visualmente con dos sesiones: la agenda médica se actualizó sola por SSE. La fase 8 incorpora pruebas unitarias de formatos y privacidad; la prueba funcional de subida y sincronización offline sigue pendiente. Estas pruebas son **evidencia funcional académica**, no una certificación para producción.

## Entrega universitaria: repositorio y video

El código fuente se entrega mediante este repositorio público. Para la explicación oral, sigue el [guion de sustentación de 5 a 7 minutos](docs/video-script.md): incluye las pantallas que debes mostrar, las pruebas representativas y precauciones para no exponer secretos. El [estado de la entrega](docs/plan-de-entrega.md) recoge los resultados comprobados. No se requiere un informe escrito para la entrega indicada.

Documentación adicional: [trazabilidad técnica](docs/requirements-traceability.md), [estado de la entrega](docs/plan-de-entrega.md) y [guion del video](docs/video-script.md).
