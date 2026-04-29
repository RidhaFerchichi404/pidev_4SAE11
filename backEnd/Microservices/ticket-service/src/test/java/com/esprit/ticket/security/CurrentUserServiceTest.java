package com.esprit.ticket.security;

import com.esprit.ticket.client.UserServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCurrentUserId_readsJwtEmailAndCachesLookup() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "dev@platform.com")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null, List.of()));
        when(userServiceClient.resolveUserIdByEmail("dev@platform.com")).thenReturn(15L);

        Long first = currentUserService.requireCurrentUserId();
        Long second = currentUserService.requireCurrentUserId();

        assertThat(first).isEqualTo(15L);
        assertThat(second).isEqualTo(15L);
        verify(userServiceClient, times(1)).resolveUserIdByEmail("dev@platform.com");
    }

    @Test
    void requireCurrentEmail_fallsBackToPreferredUsername() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "fallback-user")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null, List.of()));

        assertThat(currentUserService.requireCurrentEmail()).isEqualTo("fallback-user");
    }

    @Test
    void requireCurrentEmail_throwsWhenUnauthenticated() {
        assertThatThrownBy(() -> currentUserService.requireCurrentEmail())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(UNAUTHORIZED);
    }

    @Test
    void requireCurrentEmail_throwsForbiddenWhenJwtHasNoIdentityClaims() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "u1").build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null, List.of()));

        assertThatThrownBy(() -> currentUserService.requireCurrentEmail())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(FORBIDDEN);
    }

    @Test
    void isAdmin_returnsTrueWhenRoleAdminPresent() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", null, "ROLE_USER", "ROLE_ADMIN")
        );

        assertThat(currentUserService.isAdmin()).isTrue();
    }
}
