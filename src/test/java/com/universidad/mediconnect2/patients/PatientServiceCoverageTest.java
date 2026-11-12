package com.universidad.mediconnect2.patients;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.universidad.mediconnect2.auth.UserAccount;
import com.universidad.mediconnect2.auth.UserAccountRepository;
import com.universidad.mediconnect2.auth.UserRole;
import com.universidad.mediconnect2.shared.NotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientServiceCoverageTest {
    private UserAccountRepository users;
    private PatientProfileRepository profiles;
    private PatientService service;
    private final UUID id = UUID.randomUUID();
    private UserAccount patient;

    @BeforeEach void init() {
        users = mock(UserAccountRepository.class);
        profiles = mock(PatientProfileRepository.class);
        service = new PatientService(users, profiles);
        patient = mock(UserAccount.class);
    }

    private void knownUser() {
        when(users.findByEmailIgnoreCase("ana@example.test")).thenReturn(Optional.of(patient));
        when(patient.getId()).thenReturn(id);
        when(patient.getEmail()).thenReturn("ana@example.test");
        when(patient.getFirstName()).thenReturn("Ana");
        when(patient.getLastName()).thenReturn("Paz");
    }

    @Test void missingPatientCannotReadProfile() {
        assertThrows(NotFoundException.class, () -> service.mine("missing@example.test"));
        verifyNoInteractions(profiles);
    }

    @Test void missingPatientCannotUpdateProfile() {
        assertThrows(NotFoundException.class, () ->
                service.update("missing@example.test", LocalDate.of(2000, 1, 1), "3001234567", "Ficticia"));
        verifyNoInteractions(profiles);
    }

    @Test void retrievesFullExistingProfile() {
        knownUser();
        PatientProfile profile = new PatientProfile(patient);
        profile.update(LocalDate.of(1999, 8, 10), " 3001234567 ", " Calle ficticia ");
        when(profiles.findById(id)).thenReturn(Optional.of(profile));
        PatientService.PatientView result = service.mine("ana@example.test");
        assertEquals("ana@example.test", result.email());
        assertEquals("Ana", result.firstName());
        assertEquals("Paz", result.lastName());
        assertEquals(LocalDate.of(1999, 8, 10), result.birthDate());
        assertEquals("3001234567", result.phone());
        assertEquals("Calle ficticia", result.address());
    }

    @Test void createsNewPatientProfileAndTrimsContactDetails() {
        knownUser();
        when(profiles.save(any(PatientProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate birth = LocalDate.of(2001, 2, 3);
        PatientService.PatientView view =
                service.update("ana@example.test", birth, " 3000000000 ", " Calle 1 ");
        assertEquals(birth, view.birthDate());
        assertEquals("3000000000", view.phone());
        assertEquals("Calle 1", view.address());
        verify(profiles).save(any(PatientProfile.class));
    }

    @Test void updatesExistingPatientProfileWithoutCreatingDuplicate() {
        knownUser();
        PatientProfile existing = new PatientProfile(patient);
        when(profiles.findById(id)).thenReturn(Optional.of(existing));
        when(profiles.save(same(existing))).thenReturn(existing);
        PatientService.PatientView view =
                service.update("ana@example.test", null, null, null);
        assertNull(view.birthDate());
        assertNull(view.phone());
        assertNull(view.address());
        verify(profiles).save(same(existing));
    }
}
