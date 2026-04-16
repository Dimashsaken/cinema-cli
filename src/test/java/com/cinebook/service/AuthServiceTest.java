package com.cinebook.service;

import com.cinebook.domain.User;
import com.cinebook.domain.enums.Role;
import com.cinebook.exception.AuthenticationException;
import com.cinebook.exception.InvalidInputException;
import com.cinebook.infra.JsonFileAdapter;
import com.cinebook.infra.JsonUserRepository;
import com.cinebook.infra.PasswordHasher;
import com.cinebook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;
    private UserRepository userRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("users.json");
        userRepository = new JsonUserRepository(file, new JsonFileAdapter());
        authService = new AuthService(userRepository, new PasswordHasher());
    }

    // --- register ---

    @Test
    void register_success() throws Exception {
        User user = authService.register("alice", "pass1234");

        assertNotNull(user.getUserId());
        assertEquals("alice", user.getUsername());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertNotNull(user.getPasswordHash());
        assertNotNull(user.getSalt());
    }

    @Test
    void register_persistsToRepository() throws Exception {
        authService.register("alice", "pass1234");
        assertTrue(userRepository.findByUsername("alice").isPresent());
    }

    @Test
    void register_blankUsername_throws() {
        assertThrows(InvalidInputException.class,
                () -> authService.register("", "pass1234"));
    }

    @Test
    void register_nullUsername_throws() {
        assertThrows(InvalidInputException.class,
                () -> authService.register(null, "pass1234"));
    }

    @Test
    void register_shortPassword_throws() {
        assertThrows(InvalidInputException.class,
                () -> authService.register("alice", "abc"));
    }

    @Test
    void register_nullPassword_throws() {
        assertThrows(InvalidInputException.class,
                () -> authService.register("alice", null));
    }

    @Test
    void register_duplicateUsername_throws() throws Exception {
        authService.register("alice", "pass1234");
        assertThrows(InvalidInputException.class,
                () -> authService.register("alice", "other5678"));
    }

    @Test
    void register_generatesUniqueIds() throws Exception {
        User u1 = authService.register("alice", "pass1234");
        User u2 = authService.register("bob", "pass5678");
        assertNotEquals(u1.getUserId(), u2.getUserId());
    }

    // --- login ---

    @Test
    void login_success() throws Exception {
        authService.register("alice", "pass1234");
        User user = authService.login("alice", "pass1234");

        assertEquals("alice", user.getUsername());
        assertTrue(authService.isLoggedIn());
        assertSame(user, authService.getCurrentUser());
    }

    @Test
    void login_wrongUsername_throws() throws Exception {
        authService.register("alice", "pass1234");
        assertThrows(AuthenticationException.class,
                () -> authService.login("bob", "pass1234"));
    }

    @Test
    void login_wrongPassword_throws() throws Exception {
        authService.register("alice", "pass1234");
        assertThrows(AuthenticationException.class,
                () -> authService.login("alice", "wrongpass"));
    }

    @Test
    void login_sameErrorForBothCases() throws Exception {
        // Should not reveal whether username or password was wrong
        authService.register("alice", "pass1234");

        AuthenticationException wrongUser = assertThrows(AuthenticationException.class,
                () -> authService.login("bob", "pass1234"));
        AuthenticationException wrongPass = assertThrows(AuthenticationException.class,
                () -> authService.login("alice", "wrongpass"));

        assertEquals(wrongUser.getMessage(), wrongPass.getMessage());
    }

    // --- logout ---

    @Test
    void logout_clearsCurrentUser() throws Exception {
        authService.register("alice", "pass1234");
        authService.login("alice", "pass1234");
        assertTrue(authService.isLoggedIn());

        authService.logout();
        assertFalse(authService.isLoggedIn());
        assertNull(authService.getCurrentUser());
    }

    @Test
    void logout_whenNotLoggedIn_noError() {
        assertDoesNotThrow(() -> authService.logout());
    }

    // --- isAdmin ---

    @Test
    void isAdmin_customerUser_false() throws Exception {
        authService.register("alice", "pass1234");
        authService.login("alice", "pass1234");
        assertFalse(authService.isAdmin());
    }

    @Test
    void isAdmin_adminUser_true() throws Exception {
        // Manually insert an admin user
        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hash("admin123", salt);
        userRepository.save(new User("U999", "superadmin", hash, salt, Role.ADMIN));

        authService.login("superadmin", "admin123");
        assertTrue(authService.isAdmin());
    }

    @Test
    void isAdmin_notLoggedIn_false() {
        assertFalse(authService.isAdmin());
    }

    // --- requireLogin / requireAdmin ---

    @Test
    void requireLogin_loggedIn_returnsUser() throws Exception {
        authService.register("alice", "pass1234");
        authService.login("alice", "pass1234");
        assertNotNull(authService.requireLogin());
    }

    @Test
    void requireLogin_notLoggedIn_throws() {
        assertThrows(AuthenticationException.class,
                () -> authService.requireLogin());
    }

    @Test
    void requireAdmin_asAdmin_returnsUser() throws Exception {
        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hash("admin123", salt);
        userRepository.save(new User("U999", "superadmin", hash, salt, Role.ADMIN));

        authService.login("superadmin", "admin123");
        assertNotNull(authService.requireAdmin());
    }

    @Test
    void requireAdmin_asCustomer_throws() throws Exception {
        authService.register("alice", "pass1234");
        authService.login("alice", "pass1234");
        assertThrows(AuthenticationException.class,
                () -> authService.requireAdmin());
    }

    @Test
    void requireAdmin_notLoggedIn_throws() {
        assertThrows(AuthenticationException.class,
                () -> authService.requireAdmin());
    }
}
