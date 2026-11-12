package com.universidad.mediconnect2.appointments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import com.universidad.mediconnect2.admin.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentControllerCoverageTest {
    private AppointmentService service;
    private AppointmentController controller;
    private Authentication patient;
    private Appointment appointment;
    private final UUID appointmentId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID officeId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-12-15T15:00:00Z");
    private final Instant end = Instant.parse("2026-12-15T15:30:00Z");

    @BeforeEach void init() {
        service = mock(AppointmentService.class);
        controller = new AppointmentController(service);
        patient = mock(Authentication.class);
        when(patient.getName()).thenReturn("patient@example.test");
        appointment = mock(Appointment.class);
        Doctor doctor = mock(Doctor.class);
        Office office = mock(Office.class);
        when(appointment.getId()).thenReturn(appointmentId);
        when(appointment.getDoctor()).thenReturn(doctor);
        when(appointment.getOffice()).thenReturn(office);
        when(doctor.getId()).thenReturn(doctorId);
        when(office.getId()).thenReturn(officeId);
        when(appointment.getStartTime()).thenReturn(start);
        when(appointment.getEndTime()).thenReturn(end);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
    }

    @Test void createsAppointmentWithScopedPatientAndCorrectDto() {
        when(service.create("patient@example.test", doctorId, officeId, start, end))
                .thenReturn(appointment);
        var result = controller.create(patient, new AppointmentController.CreateRequest(
                doctorId, officeId, start, end));
        assertEquals(appointmentId, result.id());
        assertEquals(doctorId, result.doctorId());
        assertEquals(officeId, result.officeId());
        assertEquals(start, result.startTime());
        assertEquals(end, result.endTime());
        assertEquals(AppointmentStatus.SCHEDULED, result.status());
    }

    @Test void listsOnlyAuthenticatedPatientsAppointments() {
        when(service.mine("patient@example.test")).thenReturn(List.of(appointment));
        var result = controller.mine(patient);
        assertEquals(1, result.size());
        assertEquals(appointmentId, result.get(0).id());
        verify(service).mine("patient@example.test");
    }

    @Test void getsDoctorScheduleUsingAuthenticatedPrincipal() {
        Authentication doctorAuth = mock(Authentication.class);
        when(doctorAuth.getName()).thenReturn("doctor@example.test");
        when(service.doctorSchedule("doctor@example.test")).thenReturn(List.of(appointment));
        assertEquals(1, controller.doctorSchedule(doctorAuth).size());
        verify(service).doctorSchedule("doctor@example.test");
    }

    @Test void cancellationDelegatesToServiceAndMapsResponse() {
        when(service.cancel("patient@example.test", appointmentId)).thenReturn(appointment);
        var result = controller.cancel(patient, appointmentId);
        assertEquals(appointmentId, result.id());
        verify(service).cancel("patient@example.test", appointmentId);
    }
}
