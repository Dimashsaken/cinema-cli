package com.cinebook.repository;

import com.cinebook.domain.User;
import com.cinebook.domain.enums.Role;
import com.cinebook.infra.JsonFileAdapter;
import com.cinebook.infra.JsonUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonUserRepositoryTest {

    private UserRepository repo;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("users.json");
        repo = new JsonUserRepository(file, new JsonFileAdapter());
    }

    @Test
    void save_and_findById() {
        User user = new User("U001", "saken", "hash", "salt", Role.CUSTOMER);
        repo.save(user);

        Optional<User> found = repo.findById("U001");
        assertTrue(found.isPresent());
        assertEquals("saken", found.get().getUsername());
    }

    @Test
    void findByUsername() {
        repo.save(new User("U001", "saken", "hash", "salt", Role.CUSTOMER));

        assertTrue(repo.findByUsername("saken").isPresent());
        assertTrue(repo.findByUsername("SAKEN").isPresent()); // case insensitive
        assertTrue(repo.findByUsername("nope").isEmpty());
    }

    @Test
    void save_updatesExisting() {
        repo.save(new User("U001", "saken", "hash1", "salt1", Role.CUSTOMER));
        repo.save(new User("U001", "saken", "hash2", "salt2", Role.ADMIN));

        assertEquals(1, repo.findAll().size());
        assertEquals(Role.ADMIN, repo.findById("U001").get().getRole());
    }

    @Test
    void deleteById() {
        repo.save(new User("U001", "saken", "hash", "salt", Role.CUSTOMER));
        repo.deleteById("U001");
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void persistsAcrossInstances() {
        Path file = tempDir.resolve("persist.json");
        JsonFileAdapter adapter = new JsonFileAdapter();

        UserRepository repo1 = new JsonUserRepository(file, adapter);
        repo1.save(new User("U001", "saken", "hash", "salt", Role.CUSTOMER));

        UserRepository repo2 = new JsonUserRepository(file, adapter);
        assertEquals(1, repo2.findAll().size());
    }
}
