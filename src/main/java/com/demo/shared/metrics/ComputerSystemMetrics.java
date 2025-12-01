package com.demo.shared.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.demo.application.computersystem.ComputerSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom metrics provider for computer systems.
 * Registers gauges with Micrometer to track system metrics via Spring Boot Actuator.
 * Access metrics at: /actuator/metrics or /actuator/metrics/app.computersystems.total
 */
@Component
public class ComputerSystemMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(ComputerSystemMetrics.class);
    
    private final MeterRegistry meterRegistry;
    private final ComputerSystemRepository computerSystemRepository;
    
    public ComputerSystemMetrics(MeterRegistry meterRegistry, 
                                 ComputerSystemRepository computerSystemRepository) {
        this.meterRegistry = meterRegistry;
        this.computerSystemRepository = computerSystemRepository;
    }
    
    /**
     * Registers custom metrics on application startup.
     * This creates gauges that report current values to Micrometer.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerMetrics() {
        logger.info("Registering custom computer system metrics");
        
        // Register gauge for total computer systems
        meterRegistry.gauge(
            "app.computersystems.total",
            computerSystemRepository,
            ComputerSystemRepository::count
        );
        
        logger.info("Custom metrics registered successfully");
    }
}
