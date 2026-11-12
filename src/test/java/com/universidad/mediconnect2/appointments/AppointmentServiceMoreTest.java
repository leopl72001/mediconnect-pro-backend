package com.universidad.mediconnect2.appointments;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.universidad.mediconnect2.admin.*;
import com.universidad.mediconnect2.auth.*;
import com.universidad.mediconnect2.notifications.AppointmentChanged;
import com.universidad.mediconnect2.shared.LateCancellationException;
import com.universidad.mediconnect2.shared.NotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceMoreTest {
    private final Instant now = Instant.parse("2026-11-03T15:00:00Z");
    private AppointmentRepository appointments;
    private UserAccountRepository users;
    private DoctorRepository doctors;
    private OfficeRepository offices;
    private ApplicationEventPublisher notifications;
    private SimpleMeterRegistry metrics;
    private AppointmentService service;

    @BeforeEach void init() {
        appointments = mock(AppointmentRepository.class);
        users = mock(UserAccountRepository.class);
        doctors = mock(DoctorRepository.class);
        offices = mock(OfficeRepository.class);
        notifications = mock(ApplicationEventPublisher.class);
        metrics = new SimpleMeterRegistry();
        service = new AppointmentService(appointments, users, doctors, offices,
                Clock.fixed(now, ZoneOffset.UTC), metrics, notifications);
    }

    @Test void rejectsPastAndMalformedSlotsBeforeRepositoryAccess() {
        UUID doctorId = UUID.randomUUID(), officeId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> service.create("patient@example.com", doctorId, officeId, now.minusSeconds(1), now.plusSeconds(60)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("patient@example.com", doctorId, officeId, now.plusSeconds(60), now.plusSeconds(60)));
        verifyNoInteractions(users, doctors, offices, appointments, notifications);
    }

    @Test void rejectsUnknownPatientBeforeDoctorLookup() {
        assertThrows(NotFoundException.class, () -> service.create("missing@example.com",
                UUID.randomUUID(), UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(5400)));
        verifyNoInteractions(doctors, offices, appointments, notifications);
    }

    @Test void rejectsMissingDoctorBeforeOfficeLookup() {
        UUID doctorId = UUID.randomUUID();
        when(users.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(mock(UserAccount.class)));
        assertThrows(NotFoundException.class, () -> service.create("patient@example.com",
                doctorId, UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(5400)));
        verifyNoInteractions(offices, appointments, notifications);
    }

    @Test void rejectsInactiveOfficeAndDoesNotPublishEvent() {
        UUID doctorId = UUID.randomUUID(), officeId = UUID.randomUUID();
        when(users.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(mock(UserAccount.class)));
        when(doctors.findLockedById(doctorId)).thenReturn(Optional.of(mock(Doctor.class)));
        when(offices.findById(officeId)).thenReturn(Optional.of(mock(Office.class)));
        assertThrows(IllegalArgumentException.class, () -> service.create("patient@example.com",
                doctorId, officeId, now.plusSeconds(3600), now.plusSeconds(5400)));
        verify(appointments, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test void createsAvailableAppointmentAndPublishesChange() {
        UUID doctorId = UUID.randomUUID(), officeId = UUID.randomUUID();
        UserAccount patient = mock(UserAccount.class), doctorUser = mock(UserAccount.class);
        Doctor doctor = mock(Doctor.class);
        Office office = mock(Office.class);
        when(patient.getEmail()).thenReturn("patient@example.com");
        when(doctorUser.getEmail()).thenReturn("doctor@example.com");
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getUser()).thenReturn(doctorUser);
        when(office.isActive()).thenReturn(true);
        when(users.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctors.findLockedById(doctorId)).thenReturn(Optional.of(doctor));
        when(offices.findById(officeId)).thenReturn(Optional.of(office));
        when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Appointment created = service.create("patient@example.com", doctorId, officeId,
                now.plusSeconds(3600), now.plusSeconds(5400));
        assertEquals(AppointmentStatus.SCHEDULED, created.getStatus());
        assertSame(patient, created.getPatient());
        verify(appointments).save(any(Appointment.class));
        verify(notifications).publishEvent(any(AppointmentChanged.class));
    }

    @Test void onlyOwnerCanCancelAppointment() {
        UUID id = UUID.randomUUID();
        Appointment existing = mock(Appointment.class);
        UserAccount patient = mock(UserAccount.class);
        when(appointments.findById(id)).thenReturn(Optional.of(existing));
        when(existing.getPatient()).thenReturn(patient);
        when(patient.getEmail()).thenReturn("owner@example.com");
        assertThrows(NotFoundException.class, () -> service.cancel("other@example.com", id));
        verify(existing, never()).cancel();
        verifyNoInteractions(notifications);
    }

    @Test void rejectsMissingOrAlreadyCancelledAppointment() {
        UUID missingId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.cancel("patient@example.com", missingId));
        UUID id = UUID.randomUUID();
        Appointment existing = mock(Appointment.class);
        UserAccount patient = mock(UserAccount.class);
        when(appointments.findById(id)).thenReturn(Optional.of(existing));
        when(existing.getPatient()).thenReturn(patient);
        when(patient.getEmail()).thenReturn("patient@example.com");
        when(existing.getStatus()).thenReturn(AppointmentStatus.CANCELLED);
        assertThrows(IllegalArgumentException.class, () -> service.cancel("patient@example.com", id));
        verify(existing, never()).cancel();
    }

    @Test void lateCancellationIsCountedWithoutChangingAppointment() {
        UUID id = UUID.randomUUID();
        Appointment existing = mock(Appointment.class);
        UserAccount patient = mock(UserAccount.class);
        when(appointments.findById(id)).thenReturn(Optional.of(existing));
        when(existing.getPatient()).thenReturn(patient);
        when(patient.getEmail()).thenReturn("patient@example.com");
        when(existing.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(existing.getStartTime()).thenReturn(now.plusSeconds(86400));
        assertThrows(LateCancellationException.class, () -> service.cancel("patient@example.com", id));
        assertEquals(1.0, metrics.counter("mediconnect2.late_cancellations").count());
        verify(existing, never()).cancel();
        verifyNoInteractions(notifications);
    }

    @Test void ownerCancelsFutureAppointmentAndNotificationIsPublished() {
        UUID id = UUID.randomUUID();
        Appointment existing = mock(Appointment.class);
        UserAccount patient = mock(UserAccount.class);
        Doctor doctor = mock(Doctor.class);
        UserAccount doctorUser = mock(UserAccount.class);
        when(appointments.findById(id)).thenReturn(Optional.of(existing));
        when(existing.getPatient()).thenReturn(patient);
        when(patient.getEmail()).thenReturn("patient@example.com");
        when(existing.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(existing.getStartTime()).thenReturn(now.plusSeconds(86401));
        when(existing.getDoctor()).thenReturn(doctor);
        when(doctor.getUser()).thenReturn(doctorUser);
        when(doctorUser.getEmail()).thenReturn("doctor@example.com");
        assertSame(existing, service.cancel("patient@example.com", id));
        verify(existing).cancel();
        verify(notifications).publishEvent(any(AppointmentChanged.class));
    }

    @Test void usesScopedQueriesForPatientAndDoctorViews() {
        List<Appointment> expected = List.of(mock(Appointment.class));
        when(appointments.findByPatient_EmailIgnoreCaseOrderByStartTimeDesc("PATIENT@example.com")).thenReturn(expected);
        when(appointments.findByDoctor_User_EmailIgnoreCaseOrderByStartTimeDesc("DOCTOR@example.com")).thenReturn(expected);
        assertSame(expected, service.mine("PATIENT@example.com"));
        assertSame(expected, service.doctorSchedule("DOCTOR@example.com"));
    }
}
