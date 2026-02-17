package com.demo.application.security.token;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_tokens")
@Getter
@Setter
@NoArgsConstructor
public class ApiToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenId;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = true)
    private String scopes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;
}
