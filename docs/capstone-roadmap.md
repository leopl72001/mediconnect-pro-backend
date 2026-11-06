# MediConnect Pro — Proyecto de grado Full-Stack

> **Estado inicial:** copia independiente de [MediConnect v1](https://github.com/leopl72001/mediconnect). El backend sigue en **Java 21 / Spring Boot 3.5.7**; aún no se ha migrado a Java 25 / Spring Boot 4 ni existe el frontend Angular.

## Objetivo del proyecto

Desarrollar una plataforma de gestión de citas médicas con backend Spring Boot 4 / Java 25, frontend Angular 21+ zoneless con Signals y Tailwind, autenticación JWT, PostgreSQL, integración en tiempo real, carga segura de medios, soporte offline, automatización de pruebas y despliegue en una URL pública.

## Repositorios

- Backend: https://github.com/leopl72001/mediconnect-pro-backend
- Frontend: `mediconnect-pro-frontend` (**por crear**, separado del backend)
- Versión 1 entregada: https://github.com/leopl72001/mediconnect (**no modificar**)

## Plan de ejecución y evidencia

| Fase | Objetivos | Evidencia de terminado |
|---|---|---|
| 0. Separación segura | Copia independiente, reglas de IA, documentación | Repo diferente y commits propios |
| 1. Migración backend | JDK 25, Spring Boot 4 y actualización de dependencias incompatibles | Maven verify + health + Docker con Java 25 |
| 2. Contrato y frontend | OpenAPI coherente; Angular 21+ zoneless, Signals, Tailwind, UI responsiva | SPA navegable con login JWT y flujos reales |
| 3. Funciones avanzadas | WebSocket/SSE, subir imágenes con WebP y almacenamiento privado; offline + sincronización diferida | Demostraciones funcionales y pruebas |
| 4. Calidad | TDD documentado, JaCoCo >=80% en lógica de negocio, Vitest >=80%, Testcontainers y tres flujos E2E de Cypress | Reportes reproducibles, CI verde |
| 5. DevOps y cloud | Dockerfiles multi-stage, Compose, lint y análisis, registry, CI/CD y despliegue automatizado | URL pública activa, deploy verificable |
| 6. Defensa y entregables | Diagramas, decisiones de arquitectura, READMEs, SonarQube/SonarCloud y bitácora de IA | Defensa de 15 min preparada |

## Arquitectura objetivo

```text
Angular SPA (Zoneless + Signals + Tailwind)
    | HTTPS REST + JWT
    | SSE/WebSocket para notificaciones y estado de citas
Spring Boot 4 API (Java 25)
    | PostgreSQL: usuarios, agendas, citas, prescripciones
    | MongoDB: notas clínicas (si se conserva de la versión 1)
    | Redis: caché / estados temporales
    | Storage privado: imágenes/archivos y URLs firmadas
    | OpenFDA: información pública sobre medicamentos
```

**Decisión provisional:** conservamos PostgreSQL/MongoDB/Redis existentes donde siguen aportando valor; el requisito mínimo exige PostgreSQL. No se deben almacenar contraseñas, tokens ni datos de pacientes en el repositorio. La solución requiere autorización robusta, auditoría y límites a archivos antes de exponerla públicamente.

## Criterios de evaluación y seguimiento

- Full-Stack + JWT: **25 puntos** — pendiente de frontend integrado.
- Calidad y pruebas >=80%: **25 puntos** — pendiente de cobertura y E2E.
- DevOps, registro y despliegue: **20 puntos** — pendiente de CD y URL pública.
- IA y limpieza: **15 puntos** — reglas iniciales en `.cursorrules`; bitácora pendiente.
- Defensa y revisión técnica: **15 puntos** — guion y ejercicio de code review pendientes.

**Importante:** el repositorio copiado no satisface todavía la rúbrica. No describir funcionalidades planificadas como si ya estuvieran construidas.

## Guion de defensa (15 minutos)

1. Arquitectura y diseño: 3 min (diagrama, entidades, decisiones).
2. Demostración desde URL pública: 5 min (flujo de paciente, médico, tiempo real y offline).
3. Deep dive: 4 min (concurrencia de reservas, real-time o pruebas de integración).
4. Preguntas / revisión aleatoria de código: 3 min.

## Próxima acción inmediata

1. Abrir **solo** `mediconnect-pro-backend` en VS Code (carpeta separada de `mediconnect`).
2. Confirmar rama `main` y remotos Git con `git remote -v`.
3. Identificar y resolver incompatibilidades Spring Boot 4 / Java 25, en una rama de migración; ejecutar `mvn verify` antes y después.
4. Crear `mediconnect-pro-frontend` y añadir sus propias reglas de Cursor antes de implementar Angular.
