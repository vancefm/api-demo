package com.demo.feature.department;

import com.demo.platform.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

/**
 * Entity representing a department — a registry of the departments that exist,
 * holding no references to its owners.
 *
 * <p>Owners (users, computer systems, and any future model) associate to
 * departments through their own join entity, each of which declares
 * {@code ON DELETE CASCADE} on its {@code department_id} foreign key. Deleting a
 * department therefore dissociates it everywhere without this feature knowing
 * which models reference it.
 *
 * <p>{@link BatchSize} bounds proxy initialisation: mapping a page of owners
 * touches each distinct department once per batch rather than once per owner.
 */
@Entity
@Table(name = "departments")
@BatchSize(size = 50)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;
}
