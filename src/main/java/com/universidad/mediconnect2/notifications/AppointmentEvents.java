package com.universidad.mediconnect2.notifications;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AppointmentEvents {
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String email) {
        String recipient = key(email);
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        CopyOnWriteArrayList<SseEmitter> list = subscribers.computeIfAbsent(
                recipient, ignored -> new CopyOnWriteArrayList<>());
        // Bound the number of concurrent streams per user in this demo.
        if (list.size() >= 5) {
            SseEmitter oldest = list.remove(0);
            oldest.complete();
        }
        list.add(emitter);
        emitter.onCompletion(() -> remove(recipient, emitter));
        emitter.onTimeout(() -> remove(recipient, emitter));
        emitter.onError(error -> remove(recipient, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected")
                    .data(Map.of("status", "connected"), MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            remove(recipient, emitter);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentChanged(AppointmentChanged event) {
        // A patient and the assigned doctor receive the same minimal notification.
        // Neither the other patients nor anonymous clients can subscribe to their streams.
        EventPayload payload = new EventPayload(
                event.appointmentId(), event.status().name(), Instant.now().toString());
        send(key(event.patientEmail()), payload);
        if (!key(event.patientEmail()).equals(key(event.doctorEmail()))) {
            send(key(event.doctorEmail()), payload);
        }
    }

    @Scheduled(fixedDelay = 20000)
    public void heartbeat() {
        subscribers.forEach((recipient, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception ex) {
                    remove(recipient, emitter);
                    emitter.complete();
                }
            }
        });
    }

    private void send(String recipient, EventPayload payload) {
        for (SseEmitter emitter : subscribers.getOrDefault(recipient, new CopyOnWriteArrayList<>())) {
            try {
                emitter.send(SseEmitter.event().name("appointment-changed")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                remove(recipient, emitter);
                emitter.complete();
            }
        }
    }

    private void remove(String recipient, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = subscribers.get(recipient);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) subscribers.remove(recipient, list);
        }
    }

    private String key(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    public record EventPayload(UUID appointmentId, String status, String timestamp) {}
}
