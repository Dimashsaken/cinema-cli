package com.cinebook.domain;

import com.cinebook.domain.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** A registered user of the system. */
public class User {

    private String userId;
    private String username;
    private String passwordHash;
    private String salt;
    private Role role;

    public User() {}

    public User(String userId, String username, String passwordHash, String salt, Role role) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
    }

    /** Check if this user has the ADMIN role. */
    @JsonIgnore
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
