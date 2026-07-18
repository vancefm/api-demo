package com.demo.feature.security.token;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    Optional<ApiToken> findByTokenId(String tokenId);
}
