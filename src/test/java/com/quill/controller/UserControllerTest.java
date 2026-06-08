package com.quill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.config.TestSecurityConfig;
import com.quill.dto.request.UpdateProfileRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    private static final long USER_ID = 1L;
    private static final String USERNAME = "alice";
    private final AuthorResponse profile =
            new AuthorResponse(USER_ID, USERNAME, "Alice", "A bio", "https://example.com/avatar.jpg");

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("GET /api/users/me")
    class GetProfile {

        @Test
        @WithMockUser(username = USERNAME)
        void returnsProfile() {
            when(userService.getProfile(USERNAME)).thenReturn(profile);

            assertThat(mockMvc.get().uri("/api/users/me"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.username")
                    .asString()
                    .isEqualTo(USERNAME);
            verify(userService).getProfile(USERNAME);
        }

        @Test
        @WithMockUser(username = USERNAME)
        void returnsBio() {
            when(userService.getProfile(USERNAME)).thenReturn(profile);

            assertThat(mockMvc.get().uri("/api/users/me"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.bio")
                    .asString()
                    .isEqualTo("A bio");
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.get().uri("/api/users/me")).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me")
    class UpdateProfile {

        @Test
        @WithMockUser(username = USERNAME)
        void updatesAndReturnsProfile() {
            var request = new UpdateProfileRequest("New Name", "New bio", "https://example.com/new.jpg");
            var updated = new AuthorResponse(USER_ID, USERNAME, "New Name", "New bio", "https://example.com/new.jpg");
            when(userService.updateProfile(USERNAME, request)).thenReturn(updated);

            assertThat(mockMvc.put()
                            .uri("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(
                                    new UpdateProfileRequest("New Name", "New bio", "https://example.com/new.jpg"))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.displayName")
                    .asString()
                    .isEqualTo("New Name");
            verify(userService).updateProfile(USERNAME, request);
        }

        @Test
        @WithMockUser(username = USERNAME)
        void acceptsPartialUpdate() {
            var request = new UpdateProfileRequest(null, "Just bio", null);
            var updated = new AuthorResponse(USER_ID, USERNAME, "Alice", "Just bio", null);
            when(userService.updateProfile(USERNAME, request)).thenReturn(updated);

            assertThat(mockMvc.put()
                            .uri("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new UpdateProfileRequest(null, "Just bio", null))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.bio")
                    .asString()
                    .isEqualTo("Just bio");
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.put()
                            .uri("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new UpdateProfileRequest(null, "test", null))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
