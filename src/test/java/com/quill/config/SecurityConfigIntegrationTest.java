package com.quill.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.TestcontainersConfiguration;
import com.quill.model.Post;
import com.quill.model.Role;
import com.quill.model.User;
import com.quill.repository.PostRepository;
import com.quill.repository.UserRepository;
import com.quill.security.CustomUserDetails;
import com.quill.security.JwtService;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("SecurityConfig integration")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private Post post;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .displayName("Test User")
                .build());

        User admin = userRepository.save(User.builder()
                .username("testadmin")
                .email("testadmin@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .displayName("Test Admin")
                .role(Role.ADMIN)
                .build());

        post = postRepository.save(Post.builder()
                .title("Test Post")
                .body("Test body")
                .slug("test-post-" + UUID.randomUUID())
                .author(user)
                .build());

        userToken = jwtService.generateToken(new CustomUserDetails(user));
        adminToken = jwtService.generateToken(new CustomUserDetails(admin));
    }

    @Test
    @DisplayName("permits anonymous access to public GET endpoints")
    void publicEndpointsAllowAnonymous() {
        assertThat(mockMvc.get().uri("/api/posts")).hasStatusOk();
        assertThat(mockMvc.get().uri("/api/categories")).hasStatusOk();
        assertThat(mockMvc.get().uri("/api/tags")).hasStatusOk();
    }

    @Test
    @DisplayName("permits anonymous access to auth endpoints")
    void authEndpointsAllowAnonymous() {
        assertThat(mockMvc.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mockMvc.post()
                        .uri("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("rejects protected endpoint without token")
    void protectedEndpointWithoutToken() {
        assertThat(mockMvc.get().uri("/api/users/me")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("accepts protected endpoint with valid token")
    void protectedEndpointWithValidToken() {
        assertThat(mockMvc.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .hasStatusOk();
    }

    @Test
    @DisplayName("rejects protected endpoint with malformed token")
    void protectedEndpointWithMalformedToken() {
        assertThat(mockMvc.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-jwt-token"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("rejects protected endpoint with token signed with a different key")
    void protectedEndpointWithWrongKey() {
        String wrongSecret = Base64.getEncoder()
                .encodeToString("different-secret-key-that-is-long-enough-for-testing-purposes-only".getBytes());
        var wrongKeyService = new JwtService(new JwtProperties(wrongSecret, Duration.ofHours(1)));
        var user = userRepository.findByUsername("testuser").orElseThrow();
        String wrongToken = wrongKeyService.generateToken(new CustomUserDetails(user));

        assertThat(mockMvc.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongToken))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("rejects admin endpoint with non-admin token")
    void adminEndpointWithNonAdminToken() {
        assertThat(mockMvc.delete().uri("/api/posts/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("accepts admin endpoint with admin token")
    void adminEndpointWithAdminToken() {
        assertThat(mockMvc.delete()
                        .uri("/api/posts/" + post.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .hasStatus(HttpStatus.NO_CONTENT);
    }
}
