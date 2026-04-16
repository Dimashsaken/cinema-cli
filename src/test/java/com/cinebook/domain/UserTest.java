package com.cinebook.domain;

import com.cinebook.domain.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void isAdmin_adminRole_true() {
        User user = new User("U001", "admin", "hash", "salt", Role.ADMIN);
        assertTrue(user.isAdmin());
    }

    @Test
    void isAdmin_customerRole_false() {
        User user = new User("U002", "customer", "hash", "salt", Role.CUSTOMER);
        assertFalse(user.isAdmin());
    }

    @Test
    void constructor_setsAllFields() {
        User user = new User("U003", "john", "h", "s", Role.CUSTOMER);
        assertEquals("U003", user.getUserId());
        assertEquals("john", user.getUsername());
        assertEquals("h", user.getPasswordHash());
        assertEquals("s", user.getSalt());
        assertEquals(Role.CUSTOMER, user.getRole());
    }
}
