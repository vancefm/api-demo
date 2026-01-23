package com.demo.application.security;

import com.demo.application.security.token.ApiTokenRepository;
import com.demo.application.security.token.ApiTokenService;
import com.demo.application.security.token.CreateTokenResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class ApiTokenServiceTest {

    @Autowired
    ApiTokenService apiTokenService;

    @Autowired
    ApiTokenRepository apiTokenRepository;

    @Test
    public void createTokenStoresHashedSecret() {
        CreateTokenResponse resp = apiTokenService.createToken(1L, "read", 1);
        Assertions.assertNotNull(resp.getTokenId());
        Assertions.assertNotNull(resp.getTokenValue());

        // Ensure repository contains stored entry with tokenId
        var opt = apiTokenRepository.findByTokenId(resp.getTokenId());
        Assertions.assertTrue(opt.isPresent());
        var stored = opt.get();
        Assertions.assertNotNull(stored.getTokenHash());
        // Hash should not equal raw token value
        Assertions.assertNotEquals(stored.getTokenHash(), resp.getTokenValue());
    }
}
