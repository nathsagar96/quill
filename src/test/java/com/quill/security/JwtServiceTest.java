package com.quill.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quill.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-testing-only".getBytes());

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
        userDetails =
                User.withUsername("alice").password("ignored").roles("USER").build();
    }

    @Test
    @DisplayName("generateToken produces a non-null non-empty token")
    void generatesToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("validateToken returns the subject for a valid token")
    void validatesValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.validateToken(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("validateToken throws JwtException for a tampered token")
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(userDetails);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.validateToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("validateToken throws JwtException for a token signed with a different key")
    void rejectsWrongKey() {
        String wrongSecret = Base64.getEncoder()
                .encodeToString("different-key-that-is-also-long-enough-for-hmac-sha-256-testing".getBytes());
        var wrongJwtService = new JwtService(new JwtProperties(wrongSecret, Duration.ofHours(1)));
        String tokenWithWrongKey = wrongJwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.validateToken(tokenWithWrongKey)).isInstanceOf(JwtException.class);
    }
}
