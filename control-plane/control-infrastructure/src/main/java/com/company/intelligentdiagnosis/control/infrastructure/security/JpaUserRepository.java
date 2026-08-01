package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.control.domain.security.User;
import com.company.intelligentdiagnosis.control.domain.security.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public JpaUserRepository(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity();
        entity.setUsername(user.username());
        entity.setPassword(user.password());
        entity.setRole(user.role());
        entity.setEnabled(user.enabled());
        jpaRepository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
            .map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    private User toDomain(UserEntity entity) {
        return new User(
            entity.getUsername(),
            entity.getPassword(),
            entity.getRole(),
            entity.isEnabled()
        );
    }
}
