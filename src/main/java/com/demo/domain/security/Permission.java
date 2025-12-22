package com.demo.domain.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a permission in the system.
 * Permissions define what operations can be performed on which resources.
 * Field-level permissions are stored as JSON in the fieldPermissions column.
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String resourceType;

    @Column(nullable = false, length = 50)
    private String operation;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String fieldPermissions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Permission() {
    }

    public Permission(String resourceType, String operation, String scope, String fieldPermissions) {
        this.resourceType = resourceType;
        this.operation = operation;
        this.scope = scope;
        this.fieldPermissions = fieldPermissions;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getFieldPermissions() {
        return fieldPermissions;
    }

    public void setFieldPermissions(String fieldPermissions) {
        this.fieldPermissions = fieldPermissions;
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

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourceType;
        private String operation;
        private String scope;
        private String fieldPermissions;

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder fieldPermissions(String fieldPermissions) {
            this.fieldPermissions = fieldPermissions;
            return this;
        }

        public Permission build() {
            return new Permission(resourceType, operation, scope, fieldPermissions);
        }
    }
}
