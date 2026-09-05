package com.demo.feature.user;

import com.demo.platform.BaseEntity;
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

/**
 * Entity representing a user in the system.
 * Users have a role that determines their permissions.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = true, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /**
     * Department links. Fetching is deliberately driven by {@link BatchSize}
     * rather than an {@code @EntityGraph} on paged queries: a collection-fetching
     * graph combined with pagination makes Hibernate join the collection and
     * paginate in memory (HHH000104), loading the whole result set. Batching
     * keeps a page at one query plus a small constant number of {@code IN}
     * queries. Entity graphs are still used for single-entity reads — see
     * {@link UserRepository}.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<UserDepartment> departmentLinks = new HashSet<>();

}
