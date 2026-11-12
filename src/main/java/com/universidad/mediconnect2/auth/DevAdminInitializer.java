package com.universidad.mediconnect2.auth;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev","docker","prod"})
public class DevAdminInitializer implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final String email;
    private final String password;

    public DevAdminInitializer(UserAccountRepository users, PasswordEncoder passwords,
            @Value("${DEV_ADMIN_EMAIL:}") String email,
            @Value("${DEV_ADMIN_PASSWORD:}") String password) {
        this.users=users; this.passwords=passwords; this.email=email; this.password=password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        if (password.length()<8) throw new IllegalArgumentException("DEV_ADMIN_PASSWORD debe tener 8+ caracteres.");
        String normalized=email.trim().toLowerCase(Locale.ROOT);
        if (!users.existsByEmailIgnoreCase(normalized)) {
            users.save(new UserAccount(normalized, passwords.encode(password),
                    "Administrador", "Local", UserRole.ROLE_ADMIN));
        }
    }
}
