package com.universidad.mediconnect2.notifications;

import java.util.UUID;
import com.universidad.mediconnect2.appointments.AppointmentStatus;

/** Minimal event: no clinical data and no patient identity in the SSE payload. */
public record AppointmentChanged(
        UUID appointmentId,
        String patientEmail,
        String doctorEmail,
        AppointmentStatus status) {}
