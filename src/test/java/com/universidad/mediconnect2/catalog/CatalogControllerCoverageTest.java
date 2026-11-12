package com.universidad.mediconnect2.catalog;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.universidad.mediconnect2.admin.*;
import com.universidad.mediconnect2.auth.UserAccount;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogControllerCoverageTest {
    SpecialtyRepository specialties;
    DoctorRepository doctors;
    OfficeRepository offices;
    SpecialtyMapper mapper;
    CatalogController controller;

    @BeforeEach void init() {
        specialties = mock(SpecialtyRepository.class);
        doctors = mock(DoctorRepository.class);
        offices = mock(OfficeRepository.class);
        mapper = mock(SpecialtyMapper.class);
        controller = new CatalogController(specialties, doctors, offices, mapper);
    }

    @Test void returnsOnlySpecialtiesProvidedByRepositoryAndMapper() {
        Specialty specialty = new Specialty("Cardiología");
        UUID id = UUID.randomUUID();
        var view = new CatalogController.SpecialtyView(id, "Cardiología");
        when(specialties.findAll()).thenReturn(List.of(specialty));
        when(mapper.toView(specialty)).thenReturn(view);
        assertEquals(List.of(view), controller.specialties());
        verify(mapper).toView(specialty);
    }

    @Test void listsAllDoctorsWithoutSpecialtyFilter() {
        Doctor doctor = mock(Doctor.class);
        UserAccount account = mock(UserAccount.class);
        Specialty specialty = mock(Specialty.class);
        UUID id = UUID.randomUUID(), specId = UUID.randomUUID();
        when(doctors.findAll()).thenReturn(List.of(doctor));
        when(doctor.getId()).thenReturn(id);
        when(doctor.getUser()).thenReturn(account);
        when(account.getFirstName()).thenReturn("Ana");
        when(account.getLastName()).thenReturn("Prueba");
        when(doctor.getSpecialty()).thenReturn(specialty);
        when(specialty.getId()).thenReturn(specId);
        when(specialty.getName()).thenReturn("Medicina general");
        var result = controller.doctors(null);
        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
        assertEquals("Ana", result.get(0).firstName());
        assertEquals("Prueba", result.get(0).lastName());
        assertEquals(specId, result.get(0).specialtyId());
        assertEquals("Medicina general", result.get(0).specialtyName());
        verify(doctors).findAll();
        verify(doctors, never()).findBySpecialty_Id(any());
    }

    @Test void filtersDoctorsByRequestedSpecialtyAtRepositoryLayer() {
        UUID id = UUID.randomUUID();
        when(doctors.findBySpecialty_Id(id)).thenReturn(List.of());
        assertTrue(controller.doctors(id).isEmpty());
        verify(doctors).findBySpecialty_Id(id);
        verify(doctors, never()).findAll();
    }

    @Test void convertsOfficeEntitiesToPublicDTOs() {
        UUID id = UUID.randomUUID();
        Office office = mock(Office.class);
        when(office.getId()).thenReturn(id);
        when(office.getName()).thenReturn("Consultorio ficticio");
        when(office.getAddress()).thenReturn("Calle ficticia");
        when(office.getRoomNumber()).thenReturn("102");
        when(office.isActive()).thenReturn(true);
        when(offices.findAll()).thenReturn(List.of(office));
        var result = controller.offices();
        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
        assertEquals("Consultorio ficticio", result.get(0).name());
        assertEquals("Calle ficticia", result.get(0).address());
        assertEquals("102", result.get(0).roomNumber());
        assertTrue(result.get(0).active());
    }

    @Test void returnsEmptyListsWhenCatalogContainsNoRecords() {
        assertTrue(controller.specialties().isEmpty());
        assertTrue(controller.offices().isEmpty());
        assertTrue(controller.doctors(null).isEmpty());
    }
}
