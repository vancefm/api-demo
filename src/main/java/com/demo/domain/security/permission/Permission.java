package com.demo.domain.security.permission;

import com.demo.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a permission in the system.
 * Permissions define what operations can be performed on which resources.
 * Field-level permissions are stored as JSON in the fieldPermissions column.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Permission extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String resourceType;

    @Column(nullable = false, length = 50)
    private String operation;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String fieldPermissions;
}
