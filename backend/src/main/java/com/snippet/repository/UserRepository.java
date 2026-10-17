package com.snippet.repository;

import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by username for authentication and authorization
    Optional<User> findByUsername(String username);

    // Check if username already exists during registration
    boolean existsByUsername(String username);
}
