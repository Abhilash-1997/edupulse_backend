package com.school.management.bootstrap;

import com.school.management.constant.UserRole;
import com.school.management.entity.User;

import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin-password}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {

        try {
            boolean superAdminExists = userRepository.existsByRoleAndDeletedAtIsNull(UserRole.SUPER_ADMIN);
            if (!superAdminExists) {

                log.info("Dddddd -----------> password "  +superAdminPassword);

                User superAdmin = User.builder()
                        .name("Super Admin")
                        .email("superadmin@edupulse.com")
                        .passwordHash(passwordEncoder.encode(superAdminPassword))
                        .role(UserRole.SUPER_ADMIN)
                        .isActive(true)
                        .build();

                userRepository.save(superAdmin);

                log.warn("🔐 SUPER_ADMIN created. PLEASE CHANGE PASSWORD IMMEDIATELY!");
            } else {
                log.info("SUPER_ADMIN already exists. Skipping seed.");
            }

        } catch (DataIntegrityViolationException ex) {
            log.info("SUPER_ADMIN already created by another instance.");
        }
    }
}
