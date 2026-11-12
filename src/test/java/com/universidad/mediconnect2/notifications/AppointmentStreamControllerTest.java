package com.universidad.mediconnect2.notifications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentStreamControllerTest {
    @Mock AppointmentEvents events;
    @Mock Authentication authentication;

    @Test void subscribesOnlyUnderTheAuthenticatedUsersIdentity() {
        SseEmitter stream = new SseEmitter();
        when(authentication.getName()).thenReturn("doctor@example.com");
        when(events.subscribe("doctor@example.com")).thenReturn(stream);

        SseEmitter actual = new AppointmentStreamController(events).stream(authentication);

        assertSame(stream, actual);
        verify(events).subscribe("doctor@example.com");
        verifyNoMoreInteractions(events);
    }
}
