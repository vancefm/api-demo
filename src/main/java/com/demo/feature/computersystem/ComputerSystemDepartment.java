package com.demo.feature.computersystem;

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
 * Join entity linking a {@link ComputerSystem} to a {@link Department}.
 *
 * <p>Both sides carry {@link OnDelete} so Hibernate's schema exporter emits the
 * foreign keys as {@code ON DELETE CASCADE}: deleting a department drops its
 * links across every owner type, and deleting a computer system drops its own.
 * Dissociation therefore needs no application code on either side, and no owner
 * type can be forgotten when a department is removed.
 *
 * <p>The join row is mapped as an entity rather than left as an implicit
 * {@code @ManyToMany} join table because {@code @OnDelete} on a many-to-many
 * collection only reaches the FK pointing at the owning table; targeting the
 * department side would otherwise require a hand-written DDL string.
 */
@Entity
@Table(name = "computer_system_departments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_computer_system_department",
        columnNames = {"computer_system_id", "department_id"}),
    indexes = @Index(name = "idx_csd_department", columnList = "department_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComputerSystemDepartment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "computer_system_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ComputerSystem computerSystem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Department department;
}
