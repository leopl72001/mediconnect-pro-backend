package com.universidad.mediconnect2.appointments;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.universidad.mediconnect2.admin.*;
import com.universidad.mediconnect2.auth.*;
import com.universidad.mediconnect2.shared.ConflictException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    @Mock AppointmentRepository appointments;
    @Mock UserAccountRepository users;
    @Mock DoctorRepository doctors;
    @Mock OfficeRepository offices;
    @Mock Clock clock;
    @Mock MeterRegistry metrics;
    @Mock ApplicationEventPublisher notifications;

    @Test void rejectsOverlapBeforeSaving() {
        UUID doctorId=UUID.randomUUID();
        UUID officeId=UUID.randomUUID();
        Instant now=Instant.parse("2026-11-03T15:00:00Z");
        Instant start=now.plusSeconds(3600);
        Instant end=now.plusSeconds(5400);
        UserAccount patient=mock(UserAccount.class);
        Doctor doctor=mock(Doctor.class);
        Office office=mock(Office.class);
        when(clock.instant()).thenReturn(now);
        when(users.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctors.findLockedById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctor.getId()).thenReturn(doctorId);
        when(offices.findById(officeId)).thenReturn(Optional.of(office));
        when(office.isActive()).thenReturn(true);
        when(appointments.existsByDoctor_IdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                doctorId,AppointmentStatus.CANCELLED,end,start)).thenReturn(true);
        AppointmentService service=new AppointmentService(appointments,users,doctors,offices,clock,metrics,notifications);
        assertThrows(ConflictException.class,
                () -> service.create("patient@example.com",doctorId,officeId,start,end));
        verify(appointments,never()).save(any());
    }
}
