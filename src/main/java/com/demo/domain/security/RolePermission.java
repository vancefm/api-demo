package com.demo.domain.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Junction entity mapping roles to permissions (many-to-many relationship).
 */
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public RolePermission() {
    }

    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Role role;
        private Permission permission;

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder permission(Permission permission) {
            this.permission = permission;
            return this;
        }

        public RolePermission build() {
            return new RolePermission(role, permission);
        }
    }
}
