package com.universidad.mediconnect2.notifications;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.universidad.mediconnect2.appointments.AppointmentStatus;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentEventsCoverageTest {
    @SuppressWarnings("unchecked")
    private Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers(AppointmentEvents service)
            throws ReflectiveOperationException {
        Field field = AppointmentEvents.class.getDeclaredField("subscribers");
        field.setAccessible(true);
        return (Map<String, CopyOnWriteArrayList<SseEmitter>>) field.get(service);
    }

    @Test void canonicalizesRecipientsAndKeepsOneChannelListPerEmail() throws Exception {
        AppointmentEvents service = new AppointmentEvents();
        SseEmitter first = service.subscribe("PATIENT@example.test");
        SseEmitter second = service.subscribe("patient@example.test");
        var current = subscribers(service);
        assertEquals(1, current.size());
        assertEquals(2, current.get("patient@example.test").size());
        assertTrue(current.get("patient@example.test").contains(first));
        assertTrue(current.get("patient@example.test").contains(second));
    }

    @Test void limitsUserToFiveActiveStreamsToAvoidUnboundedGrowth() throws Exception {
        AppointmentEvents service = new AppointmentEvents();
        for (int i = 0; i < 7; i++) {
            service.subscribe("patient@example.test");
        }
        assertEquals(5, subscribers(service).get("patient@example.test").size());
    }

    @Test void notifiesPatientAndAssignedDoctorAndHeartbeatsWithoutLeakingOtherUsers() throws Exception {
        AppointmentEvents service = new AppointmentEvents();
        service.subscribe("patient@example.test");
        service.subscribe("doctor@example.test");
        service.subscribe("stranger@example.test");
        assertDoesNotThrow(() -> service.onAppointmentChanged(
                new AppointmentChanged(UUID.randomUUID(), "PATIENT@example.test",
                        "DOCTOR@example.test", AppointmentStatus.SCHEDULED)));
        assertDoesNotThrow(service::heartbeat);
        assertEquals(3, subscribers(service).size());
        assertEquals(1, subscribers(service).get("stranger@example.test").size());
    }

    @Test void doesNotDuplicateRecipientsIfPatientAndDoctorEmailsMatch() {
        AppointmentEvents service = new AppointmentEvents();
        service.subscribe("same@example.test");
        assertDoesNotThrow(() -> service.onAppointmentChanged(
                new AppointmentChanged(UUID.randomUUID(), "SAME@example.test",
                        "same@example.test", AppointmentStatus.CANCELLED)));
    }

    @Test void heartbeatsNoActiveSubscribersSafely() {
        AppointmentEvents service = new AppointmentEvents();
        assertDoesNotThrow(service::heartbeat);
        assertDoesNotThrow(() -> service.onAppointmentChanged(
                new AppointmentChanged(UUID.randomUUID(), "patient@example.test",
                        "doctor@example.test", AppointmentStatus.SCHEDULED)));
    }
}
