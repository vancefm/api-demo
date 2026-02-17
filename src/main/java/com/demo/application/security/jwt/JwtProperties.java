package com.demo.application.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtProperties {
    private String keySource;
    private String privateKey; // intentionally left blank in application.yml
    private String privateKeyPath;
    private String kid;
    private String alg = "RS512";
    private Duration ttl = Duration.ofHours(2);
    private Map<String, String> claims;
}
