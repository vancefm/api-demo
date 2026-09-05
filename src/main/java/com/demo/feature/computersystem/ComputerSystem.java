package com.demo.feature.computersystem;

import com.demo.platform.BaseEntity;
import com.demo.feature.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

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

    /**
     * Lazy, with the fetch declared per read path via {@code @EntityGraph} in
     * {@link ComputerSystemRepository} — eager fetching joined the user on every
     * read whether or not the caller needed it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User systemUser;

    /**
     * Department links. Fetching is deliberately driven by {@link BatchSize}
     * rather than an {@code @EntityGraph} on paged queries: a collection-fetching
     * graph combined with pagination makes Hibernate join the collection and
     * paginate in memory (HHH000104), loading the whole result set. Batching
     * keeps a page at one query plus a small constant number of {@code IN}
     * queries. Entity graphs are still used for single-entity reads — see
     * {@link ComputerSystemRepository}.
     */
    @OneToMany(mappedBy = "computerSystem", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<ComputerSystemDepartment> departmentLinks = new HashSet<>();

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
