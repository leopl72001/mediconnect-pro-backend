package com.universidad.mediconnect2.auth;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerAndInitializerCoverageTest {
    @Test void mapsSuccessfulRegistrationToPublicResponse() {
        AuthService service = mock(AuthService.class);
        AuthController controller = new AuthController(service);
        UserAccount account = mock(UserAccount.class);
        UUID id = UUID.randomUUID();
        when(service.register("patient@example.test", "password-123", "Ana", "Prueba"))
                .thenReturn(account);
        when(account.getId()).thenReturn(id);
        when(account.getEmail()).thenReturn("patient@example.test");
        when(account.getRole()).thenReturn(UserRole.ROLE_PATIENT);
        var result = controller.register(new AuthController.RegisterRequest(
                "patient@example.test", "password-123", "Ana", "Prueba"));
        assertEquals(id.toString(), result.id());
        assertEquals("patient@example.test", result.email());
        assertEquals("ROLE_PATIENT", result.role());
    }

    @Test void wrapsLoginTokenInBearerResponse() {
        AuthService service = mock(AuthService.class);
        when(service.login("patient@example.test", "password-123")).thenReturn("jwt-ficticio");
        var result = new AuthController(service).login(
                new AuthController.LoginRequest("patient@example.test", "password-123"));
        assertEquals("jwt-ficticio", result.accessToken());
        assertEquals("Bearer", result.tokenType());
    }

    @Test void noBootstrapAdminWhenEitherCredentialIsBlank() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ApplicationArguments args = mock(ApplicationArguments.class);
        new DevAdminInitializer(users, passwordEncoder, "", "12345678").run(args);
        new DevAdminInitializer(users, passwordEncoder, "admin@example.test", "").run(args);
        verifyNoInteractions(users, passwordEncoder);
    }

    @Test void bootstrapAdminRefusesWeakPassword() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DevAdminInitializer initializer = new DevAdminInitializer(
                users, passwordEncoder, "admin@example.test", "1234567");
        assertThrows(IllegalArgumentException.class, () ->
                initializer.run(mock(ApplicationArguments.class)));
        verifyNoInteractions(users, passwordEncoder);
    }

    @Test void doesNotOverwriteAnExistingAdminAccount() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(users.existsByEmailIgnoreCase("admin@example.test")).thenReturn(true);
        new DevAdminInitializer(users, passwordEncoder, " ADMIN@Example.Test ", "Password123!")
                .run(mock(ApplicationArguments.class));
        verify(users, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test void bootstrapsNewAdminUsingHashedPasswordAndCanonicalEmail() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-secret");
        new DevAdminInitializer(users, passwordEncoder, " ADMIN@Example.Test ", "Password123!")
                .run(mock(ApplicationArguments.class));
        verify(users).save(argThat(account ->
                account.getEmail().equals("admin@example.test") &&
                account.getPasswordHash().equals("hashed-secret") &&
                account.getRole() == UserRole.ROLE_ADMIN));
    }
}
