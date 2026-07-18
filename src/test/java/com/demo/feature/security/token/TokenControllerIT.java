package com.demo.feature.security.token;

import com.demo.feature.security.role.Role;
import com.demo.feature.user.User;
import com.demo.feature.security.auth.RoleRepository;
import com.demo.feature.user.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TokenControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private com.demo.feature.security.token.ApiTokenRepository apiTokenRepository;

    @Test
    @WithMockUser(roles = {"MY_APP_SUPERADMIN"})
    public void createTokenAsAdmin() throws Exception {
        Role role = roleRepository.findByName("MY_APP_USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("MY_APP_USER");
                    return roleRepository.save(r);
                });

        User u = new User();
        u.setUsername("svcuser");
        u.setEmail("svc@example.com");
        u.setDepartment("dev");
        u.setRole(role);
        userRepository.save(u);

        mockMvc.perform(post("/api/v1/tokens")
                .param("ownerUserId", u.getId().toString())
                .param("scopes", "read")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(status().isOk());

        // repository should have an entry for owner
        var all = apiTokenRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
    }
}
