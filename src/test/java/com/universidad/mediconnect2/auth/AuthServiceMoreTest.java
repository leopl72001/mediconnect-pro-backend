package com.universidad.mediconnect2.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.universidad.mediconnect2.shared.ConflictException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceMoreTest {
    private UserAccountRepository users;
    private PasswordEncoder passwords;
    private JwtEncoder jwt;
    private AuthService auth;

    @BeforeEach void init() {
        users = mock(UserAccountRepository.class);
        passwords = mock(PasswordEncoder.class);
        jwt = mock(JwtEncoder.class);
        auth = new AuthService(users, passwords, jwt,
                Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC), 60);
    }

    @Test void registersTrimmedLowercasePatientAndHashedPassword() {
        when(passwords.encode("SafePassword123!")).thenReturn("hash-value");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount saved = auth.register("  TEST@Example.COM ", "SafePassword123!", " Ana ", " López ");
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("hash-value", saved.getPasswordHash());
        assertEquals("Ana", saved.getFirstName());
        assertEquals("López", saved.getLastName());
        assertEquals(UserRole.ROLE_PATIENT, saved.getRole());
        verify(users).existsByEmailIgnoreCase("test@example.com");
        verify(passwords).encode("SafePassword123!");
    }

    @Test void refusesDuplicateEmailBeforeHashingPassword() {
        when(users.existsByEmailIgnoreCase("duplicate@example.com")).thenReturn(true);
        assertThrows(ConflictException.class, () -> auth.register(
                " Duplicate@Example.com ", "SafePassword123!", "Ana", "Test"));
        verifyNoInteractions(passwords, jwt);
        verify(users, never()).save(any());
    }

    @Test void refusesUnknownAccountWithoutInvokingPasswordEncoder() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auth.login(" missing@example.com ", "secret"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(users).findByEmailIgnoreCase("missing@example.com");
        verifyNoInteractions(passwords, jwt);
    }

    @Test void rejectsIncorrectPasswordWithoutIssuingJwt() {
        UserAccount user = new UserAccount("user@example.com", "encoded", "Ana", "Test", UserRole.ROLE_PATIENT);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("wrong-password", "encoded")).thenReturn(false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auth.login("user@example.com", "wrong-password"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwt);
    }
}
