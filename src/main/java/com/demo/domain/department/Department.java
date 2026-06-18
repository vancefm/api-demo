package com.demo.domain.department;

import com.demo.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A department that objects (users, computer systems, future entities) can belong to,
 * and that users can be granted membership in. Department membership is the basis for
 * {@code DEPARTMENT}-scoped access control.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
