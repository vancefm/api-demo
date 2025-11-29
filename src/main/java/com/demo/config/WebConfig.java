package com.demo.config;

import com.demo.interceptor.GlobalRateLimitInterceptor;
import org.springframework.beans.factory.ObjectProvider;
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

    /**
     * Constructor with optional rate limit interceptor.
     * Uses ObjectProvider to gracefully handle cases where the interceptor is not available (e.g., in tests).
     */
    public WebConfig(ObjectProvider<GlobalRateLimitInterceptor> rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register rate limiting interceptor if available (not in test context)
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
