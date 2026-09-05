package com.demo.feature.department;

import com.demo.feature.user.User;
import com.demo.feature.user.UserDepartment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentLinksTest {

    private Department it;
    private Department hr;
    private User owner;
    private Set<UserDepartment> links;

    @BeforeEach
    void setUp() {
        it = Department.builder().id(1L).name("IT").build();
        hr = Department.builder().id(2L).name("HR").build();
        owner = User.builder().id(10L).username("john.doe").email("john@example.com").build();
        links = new HashSet<>();
    }

    private void replaceWith(Department... desired) {
        DepartmentLinks.replace(links, UserDepartment::getDepartment, List.of(desired),
            department -> UserDepartment.builder().user(owner).department(department).build());
    }

    private Set<Long> linkedIds() {
        Set<Long> ids = new HashSet<>();
        links.forEach(link -> ids.add(link.getDepartment().getId()));
        return ids;
    }

    @Test
    void addsLinksForNewlyAssignedDepartments() {
        replaceWith(it, hr);

        assertEquals(Set.of(1L, 2L), linkedIds());
        links.forEach(link -> assertSame(owner, link.getUser()));
    }

    @Test
    void removesLinksForDepartmentsNoLongerAssigned() {
        replaceWith(it, hr);

        replaceWith(hr);

        assertEquals(Set.of(2L), linkedIds());
    }

    @Test
    void emptyDesiredClearsEveryLink() {
        replaceWith(it, hr);

        replaceWith();

        assertTrue(links.isEmpty());
    }

    /**
     * The reason for diffing rather than clear-and-rebuild: a surviving
     * association keeps its identity, so its audit timestamps — and any
     * attributes the join entity grows later — are preserved across an update.
     */
    @Test
    void leavesSurvivingLinksUntouched() {
        replaceWith(it);
        UserDepartment original = links.iterator().next();

        replaceWith(it, hr);

        UserDepartment survivor = links.stream()
            .filter(link -> link.getDepartment().getId().equals(1L))
            .findFirst()
            .orElseThrow();
        assertSame(original, survivor);
    }
}
