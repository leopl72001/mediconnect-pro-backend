package com.universidad.mediconnect2.clinical;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.universidad.mediconnect2.admin.Doctor;
import com.universidad.mediconnect2.appointments.*;
import com.universidad.mediconnect2.auth.*;
import com.universidad.mediconnect2.shared.NotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClinicalNoteServiceCoverageTest {
    private final UUID appointmentId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-12T12:00:00Z");
    private ClinicalNoteRepository notes;
    private AppointmentRepository appointments;
    private UserAccountRepository users;
    private ClinicalNoteService service;

    @BeforeEach void init() {
        notes = mock(ClinicalNoteRepository.class);
        appointments = mock(AppointmentRepository.class);
        users = mock(UserAccountRepository.class);
        service = new ClinicalNoteService(notes, appointments, users,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test void rejectsNotesForMissingAppointment() {
        assertThrows(NotFoundException.class, () -> service.add(
                "doctor@example.test", appointmentId, "Texto ficticio"));
        verifyNoInteractions(notes);
    }

    private Appointment stubAssignedDoctor() {
        Appointment appointment = mock(Appointment.class);
        Doctor doctor = mock(Doctor.class);
        UserAccount doctorAccount = mock(UserAccount.class);
        when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointment.getDoctor()).thenReturn(doctor);
        when(doctor.getUser()).thenReturn(doctorAccount);
        when(doctorAccount.getEmail()).thenReturn("doctor@example.test");
        return appointment;
    }

    @Test void rejectsOtherDoctorsWithoutSaving() {
        stubAssignedDoctor();
        assertThrows(AccessDeniedException.class, () -> service.add(
                "other@example.test", appointmentId, "Nota ficticia"));
        verifyNoInteractions(notes);
    }

    @Test void rejectsCancelledAppointmentWithoutSaving() {
        Appointment appointment = stubAssignedDoctor();
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);
        assertThrows(IllegalArgumentException.class, () -> service.add(
                "doctor@example.test", appointmentId, "Nota ficticia"));
        verifyNoInteractions(notes);
    }

    @Test void assignedDoctorWritesTrimmedNoteWithCorrectOwnershipAndAuditDate() {
        Appointment appointment = stubAssignedDoctor();
        Doctor doctor = appointment.getDoctor();
        UserAccount patient = mock(UserAccount.class);
        when(appointment.getId()).thenReturn(appointmentId);
        when(appointment.getPatient()).thenReturn(patient);
        when(patient.getId()).thenReturn(patientId);
        when(doctor.getId()).thenReturn(doctorId);
        when(notes.save(any(ClinicalNoteDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        ClinicalNoteDocument result = service.add("DOCTOR@example.test", appointmentId, "  Nota ficticia  ");
        assertEquals(appointmentId.toString(), result.getAppointmentId());
        assertEquals(patientId.toString(), result.getPatientId());
        assertEquals(doctorId.toString(), result.getDoctorId());
        assertEquals("Nota ficticia", result.getContent());
        assertEquals("DOCTOR@example.test", result.getCreatedBy());
        assertEquals(now, result.getCreatedAt());
        verify(notes).save(any(ClinicalNoteDocument.class));
    }

    @Test void patientQueriesOnlyTheirOwnNotes() {
        UserAccount patient = mock(UserAccount.class);
        when(users.findByEmailIgnoreCase("patient@example.test")).thenReturn(Optional.of(patient));
        when(patient.getId()).thenReturn(patientId);
        List<ClinicalNoteDocument> expected = List.of(mock(ClinicalNoteDocument.class));
        when(notes.findByPatientIdOrderByCreatedAtDesc(patientId.toString())).thenReturn(expected);
        assertSame(expected, service.mine("patient@example.test"));
        verify(notes).findByPatientIdOrderByCreatedAtDesc(patientId.toString());
    }

    @Test void unknownPatientCannotReadNotes() {
        assertThrows(NotFoundException.class, () -> service.mine("missing@example.test"));
        verifyNoInteractions(notes);
    }
}
