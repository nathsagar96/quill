package com.quill.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletResponse response;

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setServletPath(path);
        return req;
    }

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("skips /api/auth/** paths")
        void skipsAuthPaths() {
            assertThat(filter.shouldNotFilter(request("POST", "/api/auth/login")))
                    .isTrue();
            assertThat(filter.shouldNotFilter(request("POST", "/api/auth/register")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips GET /api/posts/**")
        void skipsGetPosts() {
            assertThat(filter.shouldNotFilter(request("GET", "/api/posts"))).isTrue();
            assertThat(filter.shouldNotFilter(request("GET", "/api/posts/1"))).isTrue();
            assertThat(filter.shouldNotFilter(request("GET", "/api/posts/slug/hello")))
                    .isTrue();
        }

        @Test
        @DisplayName("does not skip POST /api/posts")
        void doesNotSkipPostPosts() {
            assertThat(filter.shouldNotFilter(request("POST", "/api/posts"))).isFalse();
        }

        @Test
        @DisplayName("does not skip PUT /api/posts")
        void doesNotSkipPutPosts() {
            assertThat(filter.shouldNotFilter(request("PUT", "/api/posts/1"))).isFalse();
        }

        @Test
        @DisplayName("skips GET /api/categories/**")
        void skipsGetCategories() {
            assertThat(filter.shouldNotFilter(request("GET", "/api/categories")))
                    .isTrue();
            assertThat(filter.shouldNotFilter(request("GET", "/api/categories/1")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips GET /api/tags/**")
        void skipsGetTags() {
            assertThat(filter.shouldNotFilter(request("GET", "/api/tags"))).isTrue();
            assertThat(filter.shouldNotFilter(request("GET", "/api/tags/1"))).isTrue();
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("proceeds when no Authorization header")
        void proceedsWithoutAuthHeader() throws Exception {
            var req = request("GET", "/api/users/me");

            filter.doFilter(req, response, chain);

            verify(chain).doFilter(req, response);
        }

        @Test
        @DisplayName("proceeds when Authorization header is not Bearer")
        void proceedsWithNonBearerHeader() throws Exception {
            var req = request("GET", "/api/users/me");
            req.addHeader("Authorization", "Basic token");

            filter.doFilter(req, response, chain);

            verify(chain).doFilter(req, response);
        }

        @Test
        @DisplayName("sets authentication for a valid token")
        void setsAuthenticationForValidToken() throws Exception {
            var req = request("GET", "/api/users/me");
            req.addHeader("Authorization", "Bearer valid-token");

            UserDetails userDetails =
                    User.withUsername("alice").password("pw").roles("USER").build();
            when(jwtService.validateToken("valid-token")).thenReturn("alice");
            when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

            filter.doFilter(req, response, chain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("alice");
            verify(chain).doFilter(req, response);
        }

        @Test
        @DisplayName("returns 401 when token is expired")
        void returns401ForExpiredToken() throws Exception {
            var req = request("GET", "/api/users/me");
            req.addHeader("Authorization", "Bearer expired-token");

            when(jwtService.validateToken("expired-token"))
                    .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

            filter.doFilter(req, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getErrorMessage()).isEqualTo("Token expired");
            verify(chain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("returns 401 when token is invalid")
        void returns401ForInvalidToken() throws Exception {
            var req = request("GET", "/api/users/me");
            req.addHeader("Authorization", "Bearer invalid-token");

            when(jwtService.validateToken("invalid-token")).thenThrow(new JwtException("Invalid token"));

            filter.doFilter(req, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getErrorMessage()).isEqualTo("Invalid token");
            verify(chain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("returns 401 when user is not found")
        void returns401ForUserNotFound() throws Exception {
            var req = request("GET", "/api/users/me");
            req.addHeader("Authorization", "Bearer valid-token");

            when(jwtService.validateToken("valid-token")).thenReturn("unknown");
            when(userDetailsService.loadUserByUsername("unknown"))
                    .thenThrow(new UsernameNotFoundException("not found"));

            filter.doFilter(req, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getErrorMessage()).isEqualTo("Authentication failed");
            verify(chain, never()).doFilter(any(), any());
        }
    }
}
