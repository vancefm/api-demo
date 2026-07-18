package com.demo.feature.security.session;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionStore, Long> {
    Optional<SessionStore> findBySessionId(String sessionId);
}
