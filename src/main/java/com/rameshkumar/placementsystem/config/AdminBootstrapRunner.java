package com.rameshkumar.placementsystem.config;

import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account from APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD, because public
 * registration only ever creates STUDENT accounts. An existing account is never modified.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String ADMIN_ROLE = "ADMIN";
    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.email:}") String email,
                                @Value("${app.admin.password:}") String password,
                                @Value("${app.admin.name:Placement Admin}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password == null ? "" : password;
        this.name = name == null || name.isBlank() ? "Placement Admin" : name.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password.isEmpty()) {
            if (!userRepository.existsByRole(ADMIN_ROLE)) {
                logger.warn("No ADMIN account exists. Set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to create one on startup.");
            }
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            logger.error("APP_ADMIN_PASSWORD must be at least {} characters. Admin account was not created.", MIN_PASSWORD_LENGTH);
            return;
        }

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
                logger.info("Bootstrap admin {} already exists", email);
            } else {
                // Promoting automatically would hand admin rights to whoever registered this email first.
                logger.warn("Bootstrap admin email {} belongs to an existing {} account and was not promoted", email, user.getRole());
            }
            return;
        }

        User admin = new User();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(ADMIN_ROLE);
        userRepository.save(admin);
        logger.info("Created bootstrap admin account {}", email);
    }
}
