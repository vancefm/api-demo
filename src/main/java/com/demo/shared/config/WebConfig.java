package com.demo.shared.config;

import com.demo.shared.interceptor.GlobalRateLimitInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering global interceptors.
 * Applies the GlobalRateLimitInterceptor to all API endpoints.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<GlobalRateLimitInterceptor> rateLimitInterceptor;

    private final boolean gatewayEnabled;

    /**
     * Constructor with optional rate limit interceptor.
     * Uses ObjectProvider to gracefully handle cases where the interceptor is not available (e.g., in tests).
     */
    public WebConfig(ObjectProvider<GlobalRateLimitInterceptor> rateLimitInterceptor,
                     @Value("${app.gateway.enabled:true}") boolean gatewayEnabled) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.gatewayEnabled = gatewayEnabled;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register rate limiting interceptor only when gateway is disabled. When the
        // integrated gateway is enabled it will own rate-limiting and related filters.
        if (!gatewayEnabled) {
            rateLimitInterceptor.ifAvailable(interceptor ->
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/api/**")
                        .excludePathPatterns(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/h2-console/**",
                            "/actuator/**"
                        )
            );
        }
    }
}
