package com.airline.service;

import com.airline.config.AppProperties;
import com.airline.domain.RoleName;
import com.airline.domain.UserAccount;
import com.airline.repository.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties properties;

    public AdminBootstrap(UserAccountRepository users, PasswordEncoder encoder, AppProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.getBootstrap().getEmail().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        PasswordPolicy.check(properties.getBootstrap().getPassword());
        UserAccount admin = new UserAccount();
        admin.setEmail(email);
        admin.setFullName(properties.getBootstrap().getName());
        admin.setPasswordHash(encoder.encode(properties.getBootstrap().getPassword()));
        admin.setActive(true);
        admin.setMustChangePassword(true);
        admin.getRoles().add(RoleName.ADMIN);
        users.save(admin);
    }
}
