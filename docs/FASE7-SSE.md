# Fase 7 — Avisos SSE autenticados de citas

MediConnect Pro envía avisos en tiempo real cuando un paciente **crea o cancela una cita**.

## Contrato

`GET /api/v1/notifications/stream`
- Requiere JWT válido por `Authorization: Bearer <token>`.
- Solo roles `PATIENT` y `DOCTOR`; ningún token viaja en parámetros de URL.
- Devuelve `Content-Type: text/event-stream`.
- Evento `connected`: indica que el canal SSE quedó conectado.
- Evento `appointment-changed`: datos mínimos `{"appointmentId":"UUID","status":"SCHEDULED|CANCELLED","timestamp":"ISO"}`, sin datos clínicos ni identidad de otros pacientes.
- Heartbeat cada ~20 s y cierre automático a los 5 minutos. Angular reconecta y vuelve a autenticar.

## Flujo

1. El paciente reserva una cita en PostgreSQL. `AppointmentService` publica un evento interno.
2. `@TransactionalEventListener(phase=AFTER_COMMIT)` procesa solo cambios confirmados.
3. El servicio envía SSE al correo del paciente y al correo del médico asignado.
4. Angular escucha el stream con `fetch`, pasando JWT en cabecera. Ante un evento, actualiza la agenda consultando la API, que conserva la autorización.
5. Lo mismo ocurre cuando el paciente cancela una cita.

## Seguridad / limitaciones

- El servidor solo registra emisores en memoria del proceso Java. En despliegues con múltiples réplicas se necesitará un bus Redis Pub/Sub o equivalente para distribuir eventos. **No afirmar funcionamiento multinstancia hasta implementar el bus.**
- Se envía un identificador de cita, su estado y una marca temporal, **sin datos médicos sensibles**.
- Al cerrar sesión, el cliente cierra el stream. Si un JWT vence, el servidor lo rechaza en la siguiente reconexión.
- Los mensajes SSE son señales de invalidación, no la fuente de verdad: el cliente vuelve a hacer `GET /api/v1/appointments/me` o `GET /api/v1/appointments/doctor/me`.
- Esta solución **no sustituye** notificaciones push fuera de la aplicación.
- Las pruebas unitarias y CI no equivalen a una prueba manual entre dos sesiones del navegador.

## Validación manual de usuario

1. Mantener Angular abierto como **médico** en una ventana del navegador; confirmar etiqueta `SSE en vivo`.
2. Abrir otro navegador en modo privado como **paciente**, también con etiqueta `SSE en vivo`.
3. El paciente reserva una cita futura con ese médico. La agenda del médico debe mostrarla **sin recargar**, y aparecer un aviso.
4. El paciente cancela esa cita (con más de 24 h de anticipación). El médico ve el estado actualizado **sin recargar**.
5. Detener/reiniciar backend para comprobar que el indicador muestra `SSE reconectando` y regresa a `SSE en vivo` después de levantar la API.

**No usar datos médicos ni credenciales reales para pruebas.**
