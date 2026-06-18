package com.demo.domain.computersystem;

import com.demo.domain.BaseEntity;
import com.demo.domain.ScopedResource;
import com.demo.domain.department.Department;
import com.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "computer_systems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComputerSystem extends BaseEntity implements ScopedResource {

    @Column(nullable = false, unique = true)
    private String hostname;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String model;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User systemUser;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "computer_system_departments",
            joinColumns = @JoinColumn(name = "computer_system_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> departments = new HashSet<>();

    @Column(nullable = false, unique = true)
    private String macAddress;

    @Column(nullable = false, unique = true)
    private String ipAddress;

    @Column(nullable = false)
    private String networkName;

    /** Audit only — who created the record. Ownership for OWN scope uses {@link #systemUser}. */
    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** A computer system is "owned" by its assigned user (basis for OWN-scoped access). */
    @Override
    public Long getOwnerId() {
        return systemUser != null ? systemUser.getId() : null;
    }

    @Override
    public Set<Long> getDepartmentIds() {
        return departments == null ? Set.of()
                : departments.stream().map(Department::getId).collect(Collectors.toSet());
    }
}
