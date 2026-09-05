package com.demo.feature.user;

import com.demo.feature.department.Department;
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
 * Join entity linking a {@link User} to a {@link Department}.
 *
 * <p>Both sides carry {@link OnDelete} so Hibernate's schema exporter emits the
 * foreign keys as {@code ON DELETE CASCADE}: deleting a department drops its
 * links across every owner type, and deleting a user drops its own.
 * Dissociation therefore needs no application code on either side, and no owner
 * type can be forgotten when a department is removed.
 *
 * <p>The join row is mapped as an entity rather than left as an implicit
 * {@code @ManyToMany} join table because {@code @OnDelete} on a many-to-many
 * collection only reaches the FK pointing at the owning table; targeting the
 * department side would otherwise require a hand-written DDL string.
 */
@Entity
@Table(name = "user_departments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_department",
        columnNames = {"user_id", "department_id"}),
    indexes = @Index(name = "idx_ud_department", columnList = "department_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserDepartment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Department department;
}
