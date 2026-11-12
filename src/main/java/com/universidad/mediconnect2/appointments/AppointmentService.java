package com.universidad.mediconnect2.appointments;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.context.ApplicationEventPublisher;
import com.universidad.mediconnect2.notifications.AppointmentChanged;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.universidad.mediconnect2.admin.*;
import com.universidad.mediconnect2.auth.*;
import com.universidad.mediconnect2.shared.*;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final UserAccountRepository users;
    private final DoctorRepository doctors;
    private final OfficeRepository offices;
    private final Clock clock;
    private final MeterRegistry metrics;
    private final ApplicationEventPublisher notifications;

    public AppointmentService(AppointmentRepository appointments,UserAccountRepository users,
            DoctorRepository doctors,OfficeRepository offices,Clock clock,MeterRegistry metrics,
            ApplicationEventPublisher notifications) {
        this.appointments=appointments; this.users=users; this.doctors=doctors;
        this.offices=offices; this.clock=clock; this.metrics=metrics; this.notifications=notifications;
    }

    @Transactional
    @PreAuthorize("hasRole('PATIENT')")
    public Appointment create(String patientEmail,UUID doctorId,UUID officeId,Instant start,Instant end) {
        AppointmentPolicy.validateWindow(start,end,clock.instant());
        UserAccount patient=users.findByEmailIgnoreCase(patientEmail)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado."));
        // Bloqueo SQL para impedir reservas simultaneas del mismo doctor.
        Doctor doctor=doctors.findLockedById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado."));
        Office office=offices.findById(officeId)
                .orElseThrow(() -> new NotFoundException("Consultorio no encontrado."));
        if (!office.isActive()) throw new IllegalArgumentException("Consultorio inactivo.");
        boolean overlap=appointments.existsByDoctor_IdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                doctor.getId(),AppointmentStatus.CANCELLED,end,start);
        if (overlap) throw new ConflictException("El doctor ya tiene una cita que se solapa con ese horario.");
        Appointment saved = appointments.save(new Appointment(patient,doctor,office,start,end));
        // Published only after the transaction commits, via @TransactionalEventListener.
        notifications.publishEvent(new AppointmentChanged(
                saved.getId(), patient.getEmail(), doctor.getUser().getEmail(), saved.getStatus()));
        return saved;
    }

    @Transactional
    @PreAuthorize("hasRole('PATIENT')")
    public Appointment cancel(String patientEmail,UUID appointmentId) {
        Appointment appointment=appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada."));
        if (!appointment.getPatient().getEmail().equalsIgnoreCase(patientEmail))
            throw new NotFoundException("Cita no encontrada.");
        if (appointment.getStatus()!=AppointmentStatus.SCHEDULED)
            throw new IllegalArgumentException("Solo se cancelan citas programadas.");
        try {
            AppointmentPolicy.requireCancellable(appointment.getStartTime(),clock.instant());
        } catch (LateCancellationException e) {
            metrics.counter("mediconnect2.late_cancellations").increment();
            throw e;
        }
        appointment.cancel();
        notifications.publishEvent(new AppointmentChanged(
                appointment.getId(), appointment.getPatient().getEmail(),
                appointment.getDoctor().getUser().getEmail(), appointment.getStatus()));
        return appointment;
    }

    @Transactional(readOnly=true)
    @PreAuthorize("hasRole('PATIENT')")
    public List<Appointment> mine(String patientEmail) {
        return appointments.findByPatient_EmailIgnoreCaseOrderByStartTimeDesc(patientEmail);
    }

    @Transactional(readOnly=true)
    @PreAuthorize("hasRole('DOCTOR')")
    public List<Appointment> doctorSchedule(String doctorEmail) {
        return appointments.findByDoctor_User_EmailIgnoreCaseOrderByStartTimeDesc(doctorEmail);
    }
}
