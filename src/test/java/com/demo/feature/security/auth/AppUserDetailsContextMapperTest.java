package com.demo.feature.security.auth;

import com.demo.feature.user.User;
import com.demo.feature.user.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsContextMapperTest {

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private DirContextOperations ctx;

    private AppUserDetailsContextMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AppUserDetailsContextMapper(userManagementService);
    }

    @Test
    void mapsDirectoryEntryToPrincipalBackedByUsersRow() {
        when(ctx.getStringAttribute("uid")).thenReturn("user1");
        when(ctx.getStringAttribute("mail")).thenReturn("user1@example.com");
        when(ctx.getStringAttribute("givenName")).thenReturn("User");
        when(ctx.getStringAttribute("sn")).thenReturn("One");
        when(userManagementService.findOrProvision("user1", "user1@example.com", "User", "One"))
            .thenReturn(User.builder().id(42L).username("user1").email("user1@example.com").build());

        UserDetails details = mapper.mapUserFromContext(ctx, "user1", List.of());

        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, details);
        assertEquals(42L, principal.id());
        assertEquals("user1", principal.getUsername());
        assertTrue(principal.getAuthorities().isEmpty());
    }

    @Test
    void fallsBackToBindUsernameAndSyntheticEmailWhenAttributesMissing() {
        when(ctx.getStringAttribute("uid")).thenReturn(null);
        when(ctx.getStringAttribute("mail")).thenReturn("");
        when(userManagementService.findOrProvision("bare", "bare@example.com", null, null))
            .thenReturn(User.builder().id(7L).username("bare").email("bare@example.com").build());

        mapper.mapUserFromContext(ctx, "bare", List.of());

        verify(userManagementService).findOrProvision("bare", "bare@example.com", null, null);
    }

    @Test
    void neverWritesToTheDirectory() {
        UserPrincipal principal = new UserPrincipal(1L, "user1");
        DirContextAdapter adapter = mock(DirContextAdapter.class);

        assertThrows(UnsupportedOperationException.class, () -> mapper.mapUserToContext(principal, adapter));
    }
}
