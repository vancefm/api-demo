package com.demo.shared.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for application metrics using Micrometer and Spring Boot Actuator.
 * Custom metrics can be registered by implementing MeterBinder or using @Timed annotations.
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfiguration {
    
    /**
     * Enables @Timed annotation support for timing method executions.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    /**
     * Customizes the meter registry with application-specific configuration.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "api-demo", "version", "1.0.0");
    }
}
