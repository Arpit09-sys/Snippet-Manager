package com.snippet.service;

import com.snippet.entity.User;
import com.snippet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Register a new user with BCrypt hashed password
    public User registerUser(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        String trimmedUsername = username.trim();
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Hash raw password with BCrypt before persisting
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(trimmedUsername, encodedPassword);
        return userRepository.save(user);
    }

    // Find user by username
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
