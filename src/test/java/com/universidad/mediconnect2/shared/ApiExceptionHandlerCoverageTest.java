package com.universidad.mediconnect2.shared;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiExceptionHandlerCoverageTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test void mapsDuplicateRecordsToConflict409() {
        var result = handler.conflict(new ConflictException("correo duplicado"));
        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals("correo duplicado", result.getDetail());
    }

    @Test void mapsMissingRecordsToNotFound404() {
        var result = handler.notFound(new NotFoundException("Cita no encontrada."));
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Cita no encontrada.", result.getDetail());
    }

    @Test void returnsBadRequestForIllegalInputAndLateCancellation() {
        var invalid = handler.badRequest(new IllegalArgumentException("hora inválida"));
        var late = handler.badRequest(new LateCancellationException("cancelación tardía"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), invalid.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.value(), late.getStatus());
        assertEquals("hora inválida", invalid.getDetail());
        assertEquals("cancelación tardía", late.getDetail());
    }

    @Test void mapsValidationErrorsToFieldSpecificProblemDetail() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "", false, null, null, "obligatorio")));
        var result = handler.invalidBody(exception);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("email: obligatorio", result.getDetail());
    }

    @Test void usesGenericBodyErrorWhenNoFieldErrorsArePresent() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of());
        assertEquals("Solicitud invalida", handler.invalidBody(exception).getDetail());
    }
}
