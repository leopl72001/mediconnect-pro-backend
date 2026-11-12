package com.universidad.mediconnect2.admin;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.universidad.mediconnect2.auth.*;
import com.universidad.mediconnect2.catalog.*;
import com.universidad.mediconnect2.shared.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminControllerCoverageTest {
    OfficeRepository offices;
    DoctorRepository doctors;
    SpecialtyRepository specialties;
    UserAccountRepository users;
    PasswordEncoder passwords;
    AdminController controller;

    @BeforeEach void init() {
        offices = mock(OfficeRepository.class);
        doctors = mock(DoctorRepository.class);
        specialties = mock(SpecialtyRepository.class);
        users = mock(UserAccountRepository.class);
        passwords = mock(PasswordEncoder.class);
        controller = new AdminController(offices, doctors, specialties, users, passwords);
    }

    private AdminController.DoctorRequest doctorRequest(UUID specialtyId) {
        return new AdminController.DoctorRequest(
                " Doctor@Example.TEST ", "test-password-123", " Ana ", " Prueba ",
                "LICENSE-FICTICIA", specialtyId);
    }

    @Test void createsTrimmedOfficePreservingActiveFlag() {
        UUID id = UUID.randomUUID();
        when(offices.save(any(Office.class))).thenAnswer(invocation -> {
            Office office = invocation.getArgument(0);
            assertEquals("Consultorio demo", office.getName());
            assertEquals("Calle ficticia", office.getAddress());
            assertEquals("103", office.getRoomNumber());
            assertFalse(office.isActive());
            Office stored = mock(Office.class);
            when(stored.getId()).thenReturn(id);
            return stored;
        });
        var response = controller.createOffice(new AdminController.OfficeRequest(
                " Consultorio demo ", " Calle ficticia ", " 103 ", false));
        assertEquals(id, response.id());
    }

    @Test void doesNotCreateDoctorWithDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("doctor@example.test")).thenReturn(true);
        assertThrows(ConflictException.class, () -> controller.createDoctor(doctorRequest(UUID.randomUUID())));
        verifyNoInteractions(passwords, specialties);
        verify(doctors, never()).save(any());
    }

    @Test void doesNotCreateDoctorWithDuplicateLicense() {
        when(doctors.existsByMedicalLicenseIgnoreCase("LICENSE-FICTICIA")).thenReturn(true);
        assertThrows(ConflictException.class, () -> controller.createDoctor(doctorRequest(UUID.randomUUID())));
        verifyNoInteractions(passwords, specialties);
        verify(users, never()).save(any());
    }

    @Test void rejectsUnknownMedicalSpecialtyWithoutPersistingUser() {
        UUID specialtyId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> controller.createDoctor(doctorRequest(specialtyId)));
        verify(specialties).findById(specialtyId);
        verifyNoInteractions(passwords);
        verify(users, never()).save(any());
    }

    @Test void createsDoctorWithHashedPasswordAndCanonicalIdentity() {
        UUID specialtyId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Specialty specialty = new Specialty("Medicina general");
        when(specialties.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(passwords.encode("test-password-123")).thenReturn("stored-password-hash");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount u = invocation.getArgument(0);
            assertEquals("doctor@example.test", u.getEmail());
            assertEquals("stored-password-hash", u.getPasswordHash());
            assertEquals("Ana", u.getFirstName());
            assertEquals("Prueba", u.getLastName());
            assertEquals(UserRole.ROLE_DOCTOR, u.getRole());
            return u;
        });
        when(doctors.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor saved = invocation.getArgument(0);
            assertEquals("LICENSE-FICTICIA", saved.getMedicalLicense());
            assertSame(specialty, saved.getSpecialty());
            Doctor persisted = mock(Doctor.class);
            when(persisted.getId()).thenReturn(doctorId);
            return persisted;
        });
        var response = controller.createDoctor(doctorRequest(specialtyId));
        assertEquals(doctorId, response.id());
        verify(passwords).encode("test-password-123");
    }
}
