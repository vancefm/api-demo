package com.demo.feature.user;

import com.demo.feature.security.rbac.assignment.RoleAssignment;
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
 *
 * <p>Holds the profile and (through the RBAC feature) the role assignments of a
 * caller. It deliberately holds <em>no credentials</em>: authentication is done
 * by binding against the embedded LDAP directory, and the row is linked to the
 * directory entry by {@code username}. A row may be created through the API or
 * provisioned automatically on a user's first login.
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

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

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

    /**
     * Department:Role grants. Read-only from this side — grants are created and
     * revoked through {@code RoleAssignmentService} and removed by the database
     * ({@code ON DELETE CASCADE}) when the user, role or department goes away.
     */
    @OneToMany(mappedBy = "user")
    @BatchSize(size = 50)
    @Builder.Default
    private Set<RoleAssignment> roleAssignments = new HashSet<>();

}
