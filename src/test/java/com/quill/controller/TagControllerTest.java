package com.quill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.config.TestSecurityConfig;
import com.quill.dto.request.TagRequest;
import com.quill.dto.response.TagResponse;
import com.quill.exception.TagNotFoundException;
import com.quill.service.TagService;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(TagController.class)
@Import(TestSecurityConfig.class)
@DisplayName("TagController")
class TagControllerTest {

    private static final long TAG_ID = 1L;
    private final TagResponse response = new TagResponse(
            TAG_ID, "java", Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"));

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private TagService tagService;

    @Nested
    @DisplayName("GET /api/tags")
    class FindAllTags {

        @Test
        void returnsListOfTags() {
            when(tagService.findAllTags()).thenReturn(List.of(response));

            assertThat(mockMvc.get().uri("/api/tags"))
                    .hasStatusOk()
                    .bodyJson()
                    .isEqualTo(
                            "[{\"id\":1,\"name\":\"java\",\"createdAt\":\"2024-01-01T00:00:00Z\",\"updatedAt\":\"2024-01-01T00:00:00Z\"}]");
            verify(tagService).findAllTags();
        }

        @Test
        void allowsAnonymousAccess() {
            assertThat(mockMvc.get().uri("/api/tags")).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("GET /api/tags/{id}")
    class FindTagById {

        @Test
        void returnsTagWhenFound() {
            when(tagService.findTagById(TAG_ID)).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/tags/{id}", TAG_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.name")
                    .asString()
                    .isEqualTo("java");
            verify(tagService).findTagById(TAG_ID);
        }

        @Test
        void returns404WhenNotFound() {
            when(tagService.findTagById(TAG_ID)).thenThrow(new TagNotFoundException(TAG_ID));

            assertThat(mockMvc.get().uri("/api/tags/{id}", TAG_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void allowsAnonymousAccess() {
            assertThat(mockMvc.get().uri("/api/tags/{id}", TAG_ID)).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("POST /api/tags")
    class CreateTag {

        @Test
        @WithMockUser
        void createsAndReturns201() {
            var request = new TagRequest("java");
            when(tagService.createTag(request)).thenReturn(response);

            assertThat(mockMvc.post()
                            .uri("/api/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"java\"}"))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(1);
            verify(tagService).createTag(request);
        }

        @Test
        @WithMockUser
        void returns400WhenNameMissing() {
            assertThat(mockMvc.post()
                            .uri("/api/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.post()
                            .uri("/api/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"java\"}"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/tags/{id}")
    class UpdateTag {

        @Test
        @WithMockUser
        void updatesAndReturns200() {
            var request = new TagRequest("updated-java");
            var updatedResponse = new TagResponse(
                    TAG_ID,
                    "updated-java",
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-02-02T00:00:00Z"));
            when(tagService.updateTag(TAG_ID, request)).thenReturn(updatedResponse);

            assertThat(mockMvc.put()
                            .uri("/api/tags/{id}", TAG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"updated-java\"}"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.name")
                    .asString()
                    .isEqualTo("updated-java");
            verify(tagService).updateTag(TAG_ID, request);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.put()
                            .uri("/api/tags/{id}", TAG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\"}"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /api/tags/{id}")
    class DeleteTag {

        @Test
        @WithMockUser(roles = "ADMIN")
        void deletesAndReturns204() {
            assertThat(mockMvc.delete().uri("/api/tags/{id}", TAG_ID)).hasStatus(HttpStatus.NO_CONTENT);
            verify(tagService).deleteTag(TAG_ID);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void returns404WhenNotFound() {
            doThrow(new TagNotFoundException(TAG_ID)).when(tagService).deleteTag(TAG_ID);

            assertThat(mockMvc.delete().uri("/api/tags/{id}", TAG_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @WithMockUser
        void returns403WhenNotAdmin() {
            assertThat(mockMvc.delete().uri("/api/tags/{id}", TAG_ID)).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.delete().uri("/api/tags/{id}", TAG_ID)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
