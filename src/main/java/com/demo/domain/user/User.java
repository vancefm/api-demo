package com.demo.domain.user;

import com.demo.domain.BaseEntity;
import com.demo.domain.ScopedResource;
import com.demo.domain.department.Department;
import com.demo.domain.security.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity representing a user in the system.
 *
 * <p>A user holds one or more {@link Role}s (the "what" — object/field/CRUD policy) and
 * belongs to one or more {@link Department}s (the "where" — their department designation).
 * Department membership scopes both the user's own access (as a principal) and an admin's
 * access to this user object.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity implements ScopedResource {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_departments",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> departments = new HashSet<>();

    @Column(length = 255)
    private String passwordHash;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /** A user owns their own user record (basis for OWN-scoped self access). */
    @Override
    public Long getOwnerId() {
        return getId();
    }

    @Override
    public Set<Long> getDepartmentIds() {
        return departments == null ? Set.of()
                : departments.stream().map(Department::getId).collect(Collectors.toSet());
    }
}
