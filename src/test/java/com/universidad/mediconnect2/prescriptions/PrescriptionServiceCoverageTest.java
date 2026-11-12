package com.universidad.mediconnect2.prescriptions;

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
import com.universidad.mediconnect2.auth.UserAccount;
import com.universidad.mediconnect2.shared.NotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PrescriptionServiceCoverageTest {
    private final UUID appointmentId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-12T12:00:00Z");
    private PrescriptionRepository prescriptions;
    private AppointmentRepository appointments;
    private OpenFdaService openFda;
    private PrescriptionService service;

    @BeforeEach void init() {
        prescriptions = mock(PrescriptionRepository.class);
        appointments = mock(AppointmentRepository.class);
        openFda = mock(OpenFdaService.class);
        service = new PrescriptionService(prescriptions, appointments, openFda,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private Appointment authorizedAppointment() {
        Appointment appointment = mock(Appointment.class);
        Doctor doctor = mock(Doctor.class);
        UserAccount user = mock(UserAccount.class);
        when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointment.getDoctor()).thenReturn(doctor);
        when(doctor.getUser()).thenReturn(user);
        when(user.getEmail()).thenReturn("assigned@example.test");
        return appointment;
    }

    @Test void missingAppointmentDoesNotCallDrugProvider() {
        assertThrows(NotFoundException.class, () -> service.create("assigned@example.test",
                appointmentId, List.of(new PrescriptionService.Line("Drug", "10 mg", "daily"))));
        verifyNoInteractions(openFda, prescriptions);
    }

    @Test void patientCanOnlyQueryPrescriptionsByOwnEmail() {
        List<Prescription> expected = List.of(mock(Prescription.class));
        when(prescriptions.findByAppointment_Patient_EmailIgnoreCaseOrderByCreatedAtDesc(
                "patient@example.test")).thenReturn(expected);
        assertSame(expected, service.mine("patient@example.test"));
        verify(prescriptions).findByAppointment_Patient_EmailIgnoreCaseOrderByCreatedAtDesc("patient@example.test");
    }

    @Test void assignedDoctorCreatesAndTrimsPrescriptionLines() {
        Appointment appointment = authorizedAppointment();
        when(openFda.validate("  Aspirina  ")).thenReturn(ValidationStatus.VALIDATED);
        when(prescriptions.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
        Prescription saved = service.create("ASSIGNED@example.test", appointmentId,
                List.of(new PrescriptionService.Line("  Aspirina  ", "  10 mg ", " Cada día ")));
        assertSame(appointment, saved.getAppointment());
        assertEquals("ASSIGNED@example.test", saved.getCreatedBy());
        assertEquals(now, saved.getCreatedAt());
        assertEquals(1, saved.getItems().size());
        assertEquals("Aspirina", saved.getItems().get(0).getMedication());
        assertEquals("10 mg", saved.getItems().get(0).getDosage());
        assertEquals("Cada día", saved.getItems().get(0).getFrequency());
        assertEquals(ValidationStatus.VALIDATED, saved.getItems().get(0).getValidationStatus());
        verify(openFda).validate("  Aspirina  ");
    }

    @Test void preservesManualValidationStatusWithoutPretendingDrugIsValidated() {
        authorizedAppointment();
        when(openFda.validate("NombreFicticio")).thenReturn(ValidationStatus.MANUAL_VALIDATION_REQUIRED);
        when(prescriptions.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
        Prescription saved = service.create("assigned@example.test", appointmentId,
                List.of(new PrescriptionService.Line("NombreFicticio", "1 unidad", "diario")));
        assertEquals(ValidationStatus.MANUAL_VALIDATION_REQUIRED,
                saved.getItems().get(0).getValidationStatus());
    }

    @Test void emptyLinesCreatePrescriptionWithoutCallingExternalValidation() {
        authorizedAppointment();
        when(prescriptions.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
        Prescription saved = service.create("assigned@example.test", appointmentId, List.of());
        assertTrue(saved.getItems().isEmpty());
        verifyNoInteractions(openFda);
    }
}
