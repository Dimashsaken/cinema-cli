package com.cinebook.service;

import com.cinebook.domain.User;
import com.cinebook.domain.enums.Role;
import com.cinebook.exception.AuthenticationException;
import com.cinebook.exception.InvalidInputException;
import com.cinebook.infra.PasswordHasher;
import com.cinebook.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles user registration, login, and session tracking.
 *
 * <p>Passwords are hashed with a per-user random salt via {@link PasswordHasher}.
 * The currently logged-in user is tracked in memory (single-session CLI model).
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private User currentUser;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Register a new customer account.
     *
     * @throws InvalidInputException if username is blank or already taken
     */
    public User register(String username, String password) throws InvalidInputException {
        if (username == null || username.isBlank()) {
            throw new InvalidInputException("Username cannot be blank");
        }
        if (password == null || password.length() < 4) {
            throw new InvalidInputException("Password must be at least 4 characters");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new InvalidInputException("Username already taken: " + username);
        }

        String salt = passwordHasher.generateSalt();
        String hash = passwordHasher.hash(password, salt);
        String userId = generateUserId();

        User user = new User(userId, username, hash, salt, Role.CUSTOMER);
        userRepository.save(user);
        log.info("Registered new user: {}", username);
        return user;
    }

    /**
     * Authenticate a user by username and password.
     *
     * @throws AuthenticationException if credentials are invalid
     */
    public User login(String username, String password) throws AuthenticationException {
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            throw new AuthenticationException("Invalid username or password");
        }
        User user = found.get();
        if (!passwordHasher.verify(password, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        this.currentUser = user;
        log.info("User logged in: {}", username);
        return user;
    }

    /** Log out the current user. */
    public void logout() {
        if (currentUser != null) {
            log.info("User logged out: {}", currentUser.getUsername());
        }
        this.currentUser = null;
    }

    /** Get the currently logged-in user, or null if not logged in. */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Check if a user is currently logged in. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Check if the current user has ADMIN role. */
    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Require that a user is logged in.
     *
     * @throws AuthenticationException if no user is logged in
     */
    public User requireLogin() throws AuthenticationException {
        if (currentUser == null) {
            throw new AuthenticationException("You must be logged in");
        }
        return currentUser;
    }

    /**
     * Require that the current user is an admin.
     *
     * @throws AuthenticationException if not logged in or not admin
     */
    public User requireAdmin() throws AuthenticationException {
        User user = requireLogin();
        if (!user.isAdmin()) {
            throw new AuthenticationException("Admin access required");
        }
        return user;
    }

    private String generateUserId() {
        int count = userRepository.findAll().size();
        return String.format("U%03d", count + 1);
    }
}
