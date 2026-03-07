package com.demo.shared.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.demo.application.computersystem.ComputerSystemRepository;

/**
 * Custom metrics provider for computer systems.
 * Registers gauges with Micrometer to track system metrics via Spring Boot Actuator.
 * Access metrics at: /actuator/metrics or /actuator/metrics/app.computersystems.total
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ComputerSystemMetrics {
    
    private final MeterRegistry meterRegistry;
    private final ComputerSystemRepository computerSystemRepository;
    
    /**
     * Registers custom metrics on application startup.
     * This creates gauges that report current values to Micrometer.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerMetrics() {
        log.info("Registering custom computer system metrics");
        
        // Register gauge for total computer systems
        meterRegistry.gauge(
            "app.computersystems.total",
            computerSystemRepository,
            ComputerSystemRepository::count
        );
        
        log.info("Custom metrics registered successfully");
    }
}
