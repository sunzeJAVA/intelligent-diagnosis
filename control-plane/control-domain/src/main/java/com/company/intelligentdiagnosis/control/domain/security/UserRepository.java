package com.company.intelligentdiagnosis.control.domain.security;

import java.util.Optional;

/**
 * 用户仓库接口
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
