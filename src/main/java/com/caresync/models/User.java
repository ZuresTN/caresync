package com.caresync.models;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String fullName;
    private String username;
    private String passwordHash;
    private Role role = Role.RECEPTIONIST;
    private boolean active = true;
    private boolean passwordMustChange;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role == null ? Role.RECEPTIONIST : role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPasswordMustChange() {
        return passwordMustChange;
    }

    public void setPasswordMustChange(boolean passwordMustChange) {
        this.passwordMustChange = passwordMustChange;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRoleLabel() {
        return role == null ? "" : role.getLabel();
    }

    public String getStatusLabel() {
        return active ? "Active" : "Inactive";
    }

    @Override
    public String toString() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }
}
