package com.demo.application.computersystem;

import com.demo.domain.computersystem.ComputerSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ComputerSystemRepositoryTest {

    @Autowired
    private ComputerSystemRepository repository;

    private ComputerSystem testSystem;

    @BeforeEach
    void setUp() {
        testSystem = ComputerSystem.builder()
            .hostname("TEST-SERVER")
            .ipAddress("192.168.1.100")
            .macAddress("00:1A:2B:3C:4D:5E")
            .manufacturer("Dell")
            .model("PowerEdge R750")
            .systemUser("admin")
            .department("IT")
            .networkName("VLAN-001")
            .build();
    }

    @Test
    void testSaveComputerSystem() {
        ComputerSystem saved = repository.save(testSystem);
        assertNotNull(saved.getId());
        assertEquals("TEST-SERVER", saved.getHostname());
    }

    @Test
    void testFindById() {
        ComputerSystem saved = repository.save(testSystem);
        var found = repository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void testFindByHostname() {
        repository.save(testSystem);
        var found = repository.findByHostname("TEST-SERVER");
        
        assertTrue(found.isPresent());
        assertEquals("TEST-SERVER", found.get().getHostname());
    }

    @Test
    void testFindAll() {
        repository.save(testSystem);
        Page<ComputerSystem> results = repository.findAll(PageRequest.of(0, 10));
        
        assertFalse(results.isEmpty());
        assertTrue(results.getContent().stream()
            .anyMatch(cs -> cs.getHostname().equals("TEST-SERVER")));
    }

    @Test
    void testUpdate() {
        ComputerSystem saved = repository.save(testSystem);
        saved.setHostname("UPDATED-SERVER");
        repository.save(saved);
        
        var updated = repository.findById(saved.getId()).get();
        assertEquals("UPDATED-SERVER", updated.getHostname());
    }

    @Test
    void testDeleteById() {
        ComputerSystem saved = repository.save(testSystem);
        repository.deleteById(saved.getId());
        
        var found = repository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void testDuplicateHostnameConstraint() {
        repository.save(testSystem);
        
        ComputerSystem duplicate = ComputerSystem.builder()
            .hostname("TEST-SERVER")
            .ipAddress("192.168.1.101")
            .macAddress("00:1A:2B:3C:4D:5F")
            .manufacturer("Dell")
            .model("PowerEdge R750")
            .systemUser("admin")
            .department("IT")
            .networkName("VLAN-001")
            .build();
        
        assertThrows(Exception.class, () -> repository.save(duplicate));
    }
}
