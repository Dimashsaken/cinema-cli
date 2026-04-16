package com.cinebook.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
    }

    @Test
    void generateSalt_returns32CharHex() {
        String salt = hasher.generateSalt();
        assertEquals(32, salt.length());
        assertTrue(salt.matches("[0-9a-f]{32}"));
    }

    @Test
    void generateSalt_unique() {
        String s1 = hasher.generateSalt();
        String s2 = hasher.generateSalt();
        assertNotEquals(s1, s2);
    }

    @Test
    void hash_deterministic_sameSalt() {
        String salt = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6";
        String h1 = hasher.hash("password123", salt);
        String h2 = hasher.hash("password123", salt);
        assertEquals(h1, h2);
    }

    @Test
    void hash_differentPasswords_differentHashes() {
        String salt = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6";
        String h1 = hasher.hash("password123", salt);
        String h2 = hasher.hash("password456", salt);
        assertNotEquals(h1, h2);
    }

    @Test
    void hash_differentSalts_differentHashes() {
        String h1 = hasher.hash("password123", "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6");
        String h2 = hasher.hash("password123", "f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1");
        assertNotEquals(h1, h2);
    }

    @Test
    void hash_returns64CharHex() {
        String salt = hasher.generateSalt();
        String hash = hasher.hash("test", salt);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void verify_correctPassword_true() {
        String salt = hasher.generateSalt();
        String hash = hasher.hash("myPassword", salt);
        assertTrue(hasher.verify("myPassword", salt, hash));
    }

    @Test
    void verify_wrongPassword_false() {
        String salt = hasher.generateSalt();
        String hash = hasher.hash("myPassword", salt);
        assertFalse(hasher.verify("wrongPassword", salt, hash));
    }

    @Test
    void verify_seedUserCredentials() {
        // Verify the seed data hashes match what was generated for users.json
        String customerHash = hasher.hash("password123", "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6");
        assertEquals("361eb939f2318b1b039f3ce7dca7ad8066d1389f8d6d49b501a5d72ab5bb0eaf", customerHash);

        String adminHash = hasher.hash("admin123", "f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1");
        assertEquals("695f67c012e6546d6897134b4b28345dc0fcdc23aa91d3d1f48e1d9c03087b07", adminHash);
    }
}
