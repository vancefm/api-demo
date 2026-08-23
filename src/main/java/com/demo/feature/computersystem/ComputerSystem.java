package com.demo.feature.computersystem;

import com.demo.platform.BaseEntity;
import com.demo.feature.department.Department;
import com.demo.feature.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "computer_systems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComputerSystem extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String hostname;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String model;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User systemUser;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "computer_system_departments",
        joinColumns = @JoinColumn(name = "computer_system_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id"))
    @Builder.Default
    private Set<Department> departments = new HashSet<>();

    @Column(nullable = false, unique = true)
    private String macAddress;

    @Column(nullable = false, unique = true)
    private String ipAddress;

    @Column(nullable = false)
    private String networkName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
