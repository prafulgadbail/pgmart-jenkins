package com.pgmart.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the store admin at startup from ADMIN_EMAIL / ADMIN_PASSWORD
 * (.env locally, Secrets Manager in AWS). No password is written in the code.
 */
@Component
public class AdminAccount implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccount.class);

    private final Users users;
    private final String email;
    private final String password;

    public AdminAccount(Users users, @Value("${app.admin-email:}") String email,
                        @Value("${app.admin-password:}") String password) {
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set: no admin account");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters");
        }
        String normalized = AuthController.normalize(email);
        String hash = new BCryptPasswordEncoder().encode(password);
        users.byEmail(normalized).ifPresentOrElse(
                u -> users.makeAdmin(u.id(), hash),
                () -> users.create(normalized, "Store Admin", hash, "ADMIN", true));
        log.info("Admin account ready");
    }
}
