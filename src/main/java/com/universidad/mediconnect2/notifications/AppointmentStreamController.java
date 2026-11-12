package com.universidad.mediconnect2.notifications;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE is authenticated via the Authorization: Bearer header, never a token in a URL. */
@RestController
@RequestMapping("/api/v1/notifications")
public class AppointmentStreamController {
    private final AppointmentEvents events;

    public AppointmentStreamController(AppointmentEvents events) {
        this.events = events;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public SseEmitter stream(Authentication authentication) {
        return events.subscribe(authentication.getName());
    }
}
