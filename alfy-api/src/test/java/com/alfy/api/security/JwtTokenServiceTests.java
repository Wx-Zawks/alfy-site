package com.alfy.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTests {

    private final JwtTokenService tokenService = new JwtTokenService(
            new ObjectMapper(), "test-signing-secret-must-have-at-least-32-characters", Duration.ofMinutes(5));

    @Test
    void issuesAndParsesAdminIdentity() {
        String token = tokenService.issueAccessToken(new AdminPrincipal(7L, "editor", "content_admin"));

        assertThat(tokenService.parseAccessToken(token))
                .isEqualTo(new AdminPrincipal(7L, "editor", "content_admin"));
    }

    @Test
    void rejectsModifiedToken() {
        String token = tokenService.issueAccessToken(new AdminPrincipal(7L, "editor", "content_admin"));
        String modified = token.substring(0, token.length() - 1) + "x";

        assertThatThrownBy(() -> tokenService.parseAccessToken(modified))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
