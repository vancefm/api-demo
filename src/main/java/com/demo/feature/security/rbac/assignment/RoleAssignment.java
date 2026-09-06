package com.demo.feature.security.rbac.assignment;
import com.demo.feature.security.rbac.role.Role;

import com.demo.feature.department.Department;
import com.demo.feature.user.User;
import com.demo.platform.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A grant: {@code user} holds {@code role} within {@code department}.
 *
 * <p>{@code department == null} makes the grant <em>global</em> — the role's
 * permissions apply to every entity, including ones that have no department
 * (roles, users without departments, …). A department-scoped grant applies only
 * to entities linked to that department.
 *
 * <p>All three foreign keys are {@code ON DELETE CASCADE}: deleting the user,
 * the role, or the department removes the grant, so no feature has to know
 * about assignments to stay consistent (the same reasoning as
 * {@link com.demo.feature.user.UserDepartment}).
 *
 * <p>The unique constraint does not catch two global grants of the same role
 * (SQL treats the NULLs as distinct); {@code RoleAssignmentService} checks for
 * that before inserting.
 */
@Entity
@Table(name = "role_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_role_assignment",
        columnNames = {"user_id", "role_id", "department_id"}),
    indexes = {
        @Index(name = "idx_ra_department", columnList = "department_id"),
        @Index(name = "idx_ra_role", columnList = "role_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoleAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Department department;

    public boolean isGlobal() {
        return department == null;
    }
}
