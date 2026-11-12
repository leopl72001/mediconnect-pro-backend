package com.universidad.mediconnect2.appointments;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.universidad.mediconnect2.shared.LateCancellationException;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentPolicyTest {
    private final Instant now=Instant.parse("2026-11-03T15:00:00Z");

    @Test void acceptsFutureWindow() {
        assertDoesNotThrow(() -> AppointmentPolicy.validateWindow(now.plusSeconds(3600),now.plusSeconds(5400),now));
    }
    @Test void rejectsPastStart() {
        assertThrows(IllegalArgumentException.class,() -> AppointmentPolicy.validateWindow(now.minusSeconds(60),now.plusSeconds(3600),now));
    }
    @Test void rejectsInvalidWindow() {
        assertThrows(IllegalArgumentException.class,() -> AppointmentPolicy.validateWindow(now.plusSeconds(3600),now.plusSeconds(1800),now));
    }
    @Test void rejectsCancellationAtExactly24Hours() {
        assertThrows(LateCancellationException.class,() -> AppointmentPolicy.requireCancellable(now.plusSeconds(86400),now));
    }
    @Test void allowsCancellationAfter24Hours() {
        assertDoesNotThrow(() -> AppointmentPolicy.requireCancellable(now.plusSeconds(86401),now));
    }

    @Test void rejectsMissingStartAndEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentPolicy.validateWindow(null, now.plusSeconds(3600), now));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentPolicy.validateWindow(now.plusSeconds(3600), null, now));
    }

    @Test void rejectsStartExactlyAtCurrentTime() {
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentPolicy.validateWindow(now, now.plusSeconds(1800), now));
    }

    @Test void rejectsCancelWithinTheLastTwentyFourHours() {
        assertThrows(LateCancellationException.class,
                () -> AppointmentPolicy.requireCancellable(now.plusSeconds(86399), now));
    }

}
