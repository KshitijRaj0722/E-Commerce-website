package com.guvi.ecommerce.config;

import com.guvi.ecommerce.entity.User;
import com.guvi.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account on startup so the admin module is reachable
 * without editing the database by hand. Disable with app.admin.seed-enabled=false.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-enabled:true}")
    private boolean seedEnabled;

    @Value("${app.admin.email:admin@guvi.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled || userRepository.existsByEmail(adminEmail)) {
            return;
        }
        userRepository.save(User.builder()
                .name("Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .build());
        log.warn("Seeded admin account {} - change ADMIN_PASSWORD before deploying publicly.", adminEmail);
    }
}
