# MediConnect Pro — auditoría de cierre del proyecto de grado

**Actualizado:** 13 de noviembre de 2026. **Alcance:** prototipo académico con cuentas y datos totalmente ficticios.

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
| Pruebas backend | **95/95** | GitHub Actions, JUnit y Testcontainers PostgreSQL |
| Cobertura backend | **82,36 % líneas; 82,17 % ramas** | JaCoCo; **meta 80 % cumplida y obligatoria en CI** |
| Pruebas frontend | **235/235** | Vitest y GitHub Actions, PR #28 |
| Cobertura frontend | **95,78 % líneas; 87,86 % ramas; 81,20 % funciones; 92,13 % instrucciones** | Vitest PR #28; **las cuatro métricas superan el 80 % con puerta CI** |
| Cypress E2E | **4/4 flujos** | Registro, citas, notas y privacidad de archivos; PR #13 verde |
| Docker multi-stage y workflows GitHub Actions | Implementados | Builds e integración automatizada |
| SonarCloud real | **Pendiente** | Sin proyecto/tokens ni dashboard verificado |
| Backend público Render | **Pendiente** | No existe servicio web todavía; MongoDB y Redis cloud pendientes |
| Frontend público Render | **Pendiente** | Necesita URL funcional del backend |
| Defensa | Guion disponible | `docs/DEFENSA-CAPSTONE-15MIN.md` en frontend; video por grabar |

**Precisión:** estas cifras proceden de CI sobre PRs de verificación integrados y no sustituyen una nueva medición de todas las ramas después de actualizaciones futuras.

## Lo que ya se integró en main

Backend: PR #6 (seguridad/formatos medios), #7 (citas y autenticación), #8 (límites de citas), #10 (perfiles, recetas y notas), #11 (catálogo y administración), #12 (SSE y errores), #13 (autenticación y citas), #14 (GridFS), #15 (puerta mínima del 80 %). Frontend: PR #10 (E2E de privacidad), #11 (estado de Angular), #12 (JWT), #13 (sesión), #15–#23 (baterías funcionales), #24 (privacidad y casos límite), #25 (plantillas por rol), #26 (pantallas JWT y offline), #27 (interacciones UI) y #28 (puerta de calidad 80 %). Render fue preparado en PR #5 backend y #9 frontend.

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

1. **Cobertura académica:** cumplida en ambas aplicaciones. JaCoCo exige al menos 80 % de líneas y ramas; Vitest exige al menos 80 % de líneas, ramas, funciones e instrucciones. Los reportes de CI deben seguir adjuntos como evidencia de sustentación.
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

**Conclusión:** el prototipo académico full-stack funciona localmente y las principales rutas de prueba pasan CI. La **meta académica de cobertura 80 % está cumplida**. SonarCloud real, URL pública y pruebas de despliegue siguen pendientes; no declarar producción clínica certificada. No declarar producción clínica certificada.
