package com.demo.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Authenticates every MockMvc request in a {@code @SpringBootTest} as the
 * bootstrap {@code admin} directory user unless the test says otherwise.
 *
 * <p>Every {@code /api/**} route requires HTTP Basic credentials that bind
 * against the embedded LDAP server, so integration tests that are not about
 * authentication would otherwise have to decorate each request. Import this
 * class and they get {@code admin} for free; use {@link #asUser} to override a
 * single request or {@link #anonymous()} to drop the credentials.
 *
 * <p>The default is applied as a plain header (not
 * {@code httpBasic()}) so a per-request override can replace it: MockMvc merges
 * default headers only when the request does not already carry that header.
 */
@TestConfiguration
public class AsAdminMockMvc {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    @Bean
    public MockMvcBuilderCustomizer authenticateAsAdminByDefault() {
        return builder -> builder.defaultRequest(MockMvcRequestBuilders.get("/")
            .header(HttpHeaders.AUTHORIZATION, basic(ADMIN_USERNAME, ADMIN_PASSWORD)));
    }

    /**
     * Sends this request as the given directory user instead of {@code admin}.
     */
    public static RequestPostProcessor asUser(String username, String password) {
        return request -> {
            request.removeHeader(HttpHeaders.AUTHORIZATION);
            request.addHeader(HttpHeaders.AUTHORIZATION, basic(username, password));
            return request;
        };
    }

    /**
     * Sends this request with no credentials at all.
     */
    public static RequestPostProcessor anonymous() {
        return request -> {
            request.removeHeader(HttpHeaders.AUTHORIZATION);
            return request;
        };
    }

    public static String basic(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
