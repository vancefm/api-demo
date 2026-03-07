package com.demo.shared.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Global rate limiting interceptor that applies rate limiting to all API endpoints.
 * Uses Resilience4j's RateLimiter with a token bucket algorithm.
 *
 * Rate limit configuration is defined in application.yml under resilience4j.ratelimiter.instances.global-api
 * 
 * Note: This component is only created when RateLimiterRegistry bean is available (e.g., not in WebMvcTest)
 */
@Component
@ConditionalOnBean(RateLimiterRegistry.class)
@Slf4j
public class GlobalRateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public GlobalRateLimitInterceptor(RateLimiterRegistry rateLimiterRegistry) {
        // Get the global rate limiter instance configured in application.yml
        this.rateLimiter = rateLimiterRegistry.rateLimiter("global-api");
        log.info("Global rate limiting initialized successfully");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Skip rate limiting for certain paths (health checks, UI, docs)
        if (shouldSkipRateLimit(request.getRequestURI())) {
            return true;
        }

        // Attempt to acquire a permit from the rate limiter
        if (rateLimiter.acquirePermission()) {
            // Request allowed - add metrics headers
            addRateLimitHeaders(response);
            log.debug("Request allowed - Available permits: {}", 
                    rateLimiter.getMetrics().getNumberOfWaitingThreads());
            return true;
        } else {
            // Rate limit exceeded - reject request with 429
            log.warn("Rate limit exceeded for IP: {}", getClientIp(request));
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\": 429, \"message\": \"Too Many Requests\", " +
                    "\"details\": \"Rate limit exceeded. Please try again later.\"}"
            );
            return false;
        }
    }

    /**
     * Determines if a request path should be excluded from rate limiting.
     * Swagger UI, API docs, H2 console, and health checks are excluded.
     */
    private boolean shouldSkipRateLimit(String uri) {
        return uri.contains("/swagger-ui") ||
               uri.contains("/v3/api-docs") ||
               uri.contains("/h2-console") ||
               uri.contains("/actuator");
    }

    /**
     * Adds rate limit information to response headers for client awareness.
     */
    private void addRateLimitHeaders(HttpServletResponse response) {
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        response.addHeader("X-Rate-Limit-Available-Permits", 
                String.valueOf(metrics.getNumberOfWaitingThreads()));
    }

    /**
     * Extracts client IP from request, handling proxies and load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}
