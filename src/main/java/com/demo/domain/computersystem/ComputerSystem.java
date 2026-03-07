package com.demo.domain.computersystem;

import com.demo.domain.BaseEntity;
import com.demo.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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

    @Column(nullable = false)
    private String department;

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
