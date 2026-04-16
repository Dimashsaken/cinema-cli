package com.cinebook.repository;

import com.cinebook.domain.User;
import java.util.Optional;

/** Repository for users. Concrete implementation in infra package. */
public interface UserRepository extends Repository<User> {

    /** Find a user by username. */
    Optional<User> findByUsername(String username);
}
