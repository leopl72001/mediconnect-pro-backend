# MediConnect Pro — auditoría de cierre del proyecto de grado

**Actualizado:** 12 de noviembre de 2026. **Alcance:** prototipo académico con cuentas y datos totalmente ficticios.

## Estado verificado

| Criterio | Resultado | Evidencia / limitación |
|---|---|---|
| Java 25, Spring Boot 4, API REST JWT HS512 y roles | Implementado | `mvn verify`, GitHub Actions |
| Angular 21, Signals, Tailwind y componentes responsive | Implementado | CI Angular, prueba funcional local |
| PostgreSQL, Hibernate/Flyway, cita y cancelación con restricciones | Implementado local | Testcontainers PostgreSQL y Docker Compose |
| MongoDB, notas clínicas y archivos privados GridFS | Implementado local | Cypress (notas, separación de pacientes), pruebas manuales |
| Redis para caché | Implementado local | Docker Compose; no instancia cloud creada |
| SSE para cambios de agenda | Implementado local | Pruebas locales y Cypress |
| WebP, almacenamiento privado y cola offline IndexedDB | Implementado y probado en local | Imagen ficticia pendiente y posterior sincronización a MongoDB |
| Pruebas backend | **95/95** | GitHub Actions PR #8: `mvn verify` |
| Cobertura backend | **82,36 % líneas; 82,17 % ramas** | JaCoCo PR #8; **meta 80 % pendiente** |
| Pruebas frontend | **140/140** | GitHub Actions PR #13: Vitest |
| Cobertura frontend | **70,33 % líneas; 69,77 % ramas** | Vitest PR #13; **meta 80 % pendiente** |
| Cypress E2E | **4/4 flujos** | Registro, citas, notas y privacidad de archivos; PR #13 verde |
| Docker multi-stage y workflows GitHub Actions | Implementados | Builds e integración automatizada |
| SonarCloud real | **Pendiente** | Sin proyecto/tokens ni dashboard verificado |
| Backend público Render | **Pendiente** | No existe servicio web todavía; MongoDB y Redis cloud pendientes |
| Frontend público Render | **Pendiente** | Necesita URL funcional del backend |
| Defensa | Guion disponible | `docs/DEFENSA-CAPSTONE-15MIN.md` en frontend; video por grabar |

**Precisión:** estas cifras proceden de CI sobre PRs de verificación integrados y no sustituyen una nueva medición de todas las ramas después de actualizaciones futuras.

## Lo que ya se integró en main

Backend: PR #6 (seguridad/formatos medios), #7 (citas y autenticación), #8 (límites de citas), #10 (perfiles, recetas y notas), #11 (catálogo y administración), #12 (SSE y errores), #13 (autenticación y citas), #14 (GridFS), #15 (puerta mínima del 80 %). Frontend: PR #10 (E2E de privacidad), #11 (estado de Angular), #12 (JWT), #13 (sesión), #15 (perfil), #16 (WebP), #17 (medios offline), #18 (admin/clínico), #19 (acceso), #20 (verificación), #21 (citas reales), #22 (SSE), #23 (cola IndexedDB), más las plantillas de Render #5 backend y #9 frontend.

## Para ejecutar desde Windows

Backend, PowerShell:
```powershell
cd 'D:\app fullstack\mediconnect-pro-backend'
git pull --ff-only origin main
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://127.0.0.1:8082/actuator/health
```

Frontend, segunda terminal PowerShell:
```powershell
cd 'D:\app fullstack\mediconnect-pro-frontend'
git pull --ff-only origin main
npm ci
npm start
```

Visitar `http://localhost:4200`. Si Node nativo o el proxy local fallan en Windows, seguir las instrucciones de Docker de `mediconnect-pro-frontend/README.md`. **No ejecutar `docker compose down -v`**, porque elimina volúmenes y datos de prueba.

## Bloqueos para cierre total

1. **Cobertura académica:** JaCoCo superó 80 % de líneas y ramas y el CI exige ese mínimo. Falta subir Vitest de 70,33 % de líneas y 69,77 % de ramas al 80 %; no redondear ni declararlo aprobado. Priorizar `AppointmentService`, `AuthService`, controles de medios y componentes Angular sin cubrir.
2. **Render:** hay una base de PostgreSQL 18 gratuita ya existente (`biblioteca-db`, nombre de base `biblioteca_db_w5no`, región Oregon, caducidad indicada **12/12/2026**), pero no hay aplicaciones web ni Key Value/Redis. Se requiere MongoDB Atlas u otro MongoDB accesible, Redis y secretos configurados en Render. `render.yaml` utiliza un plan Free y **no crea otra base**. El despliegue no se puede declarar completado hasta que existan URLs públicas comprobadas.
3. **SonarCloud:** requiere conectar dos proyectos y configurar los secretos del repositorio; no exponer valores en documentación ni capturas.
4. **Defensa:** grabar video de funcionamiento con usuarios ficticios y guardar capturas de CI, cobertura, autorización y pruebas offline.

## Restricciones esenciales

- No introducir historiales, fotografías ni identidades de pacientes reales: la cola IndexedDB no está cifrada y el prototipo no está listo para datos clínicos regulados.
- Las recetas con OpenFDA no sustituyen evaluación clínica.
- SSE actualmente es en memoria por instancia; multi-réplica necesita distribución de eventos.
- El modo demo y las métricas de ejemplo no son datos de negocio reales.
- En producción, realizar auditoría de seguridad, backups, cifrado, antivirus, gestión de retención y monitoreo.

## Checklist de verificación posterior al despliegue

- `https://API_REAL/actuator/health` responde `UP` sin secretos.
- Frontend HTTPS carga y `/api` se enruta al backend.
- Registro/login con un paciente ficticio y JWT real.
- Paciente reserva/cancela, médico ve cambios con SSE.
- Nota clínica y archivo privado llegan a MongoDB; un segundo paciente recibe 404 al descargar el archivo ajeno.
- Offline: el archivo pasa de 1 pendiente a 0 tras reconectar.
- CI, Cypress, cobertura y SonarCloud publicados con los valores auténticos.

**Conclusión:** el prototipo académico full-stack funciona localmente y las principales rutas de prueba pasan CI. El **cierre completo de la rúbrica (80 %, SonarCloud y URL pública)** sigue condicionado a las tareas anteriores. No declarar producción clínica certificada.
