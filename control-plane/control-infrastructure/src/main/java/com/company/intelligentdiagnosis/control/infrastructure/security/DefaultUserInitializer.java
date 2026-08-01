package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.control.domain.security.User;
import com.company.intelligentdiagnosis.control.domain.security.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 默认管理员账号初始化
 * 当数据库中不存在 admin 用户时自动创建
 */
@Component
public class DefaultUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        User admin = new User(
            "admin",
            passwordEncoder.encode("admin"),
            "ADMIN",
            true
        );
        userRepository.save(admin);
        log.warn("Created default admin user. Please change the password in production.");
    }
}
