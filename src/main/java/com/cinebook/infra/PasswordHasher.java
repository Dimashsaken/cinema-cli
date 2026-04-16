package com.cinebook.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Salted SHA-256 password hashing.
 *
 * <p>Each user gets a unique random salt stored alongside their hash.
 * To verify a password, re-hash with the stored salt and compare.
 */
public class PasswordHasher {

    private static final int SALT_LENGTH_BYTES = 16;

    /** Generate a random 16-byte salt, returned as a 32-char hex string. */
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hash a password with the given salt.
     *
     * @param password plaintext password
     * @param saltHex  hex-encoded salt
     * @return hex-encoded SHA-256 hash of (salt bytes + password UTF-8 bytes)
     */
    public String hash(String password, String saltHex) {
        try {
            byte[] salt = hexToBytes(saltHex);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in every JVM
            throw new AssertionError("SHA-256 not available", e);
        }
    }

    /**
     * Verify a password against a stored hash and salt.
     *
     * @return true if the password matches
     */
    public boolean verify(String password, String saltHex, String expectedHash) {
        return hash(password, saltHex).equals(expectedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
