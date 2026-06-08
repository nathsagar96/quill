package com.quill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.config.TestSecurityConfig;
import com.quill.dto.request.CommentRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.dto.response.CommentResponse;
import com.quill.exception.ForbiddenOperationException;
import com.quill.service.CommentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CommentController.class)
@Import(TestSecurityConfig.class)
@DisplayName("CommentController")
class CommentControllerTest {

    private static final long COMMENT_ID = 100L;
    private static final long POST_ID = 10L;
    private static final long AUTHOR_ID = 1L;
    private static final String USERNAME = "alice";
    private final CommentResponse response = new CommentResponse(
            COMMENT_ID,
            "Nice post!",
            POST_ID,
            new AuthorResponse(AUTHOR_ID, null, null, null, null),
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));
    private final CommentRequest request = new CommentRequest("Nice post!");

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private CommentService commentService;

    @Nested
    @DisplayName("GET /api/posts/{postId}/comments")
    class FindAllCommentsByPostId {

        @Test
        @WithMockUser
        void returnsPageOfComments() {
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<CommentResponse> page = new PageImpl<>(List.of(response), pageable, 1);
            when(commentService.findAllCommentsByPostId(POST_ID, pageable)).thenReturn(page);

            assertThat(mockMvc.get().uri("/api/posts/{postId}/comments", POST_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .hasSize(1);
            verify(commentService).findAllCommentsByPostId(POST_ID, pageable);
        }

        @Test
        @WithMockUser
        void returnsEmptyPage() {
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<CommentResponse> empty = new PageImpl<>(List.of(), pageable, 0);
            when(commentService.findAllCommentsByPostId(POST_ID, pageable)).thenReturn(empty);

            assertThat(mockMvc.get().uri("/api/posts/{postId}/comments", POST_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .isEmpty();
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.get().uri("/api/posts/{postId}/comments", POST_ID))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /api/posts/{postId}/comments")
    class CreateComment {

        @Test
        @WithMockUser(username = USERNAME)
        void createsAndReturns201() {
            when(commentService.createComment(request, POST_ID, USERNAME)).thenReturn(response);

            assertThat(mockMvc.post()
                            .uri("/api/posts/{postId}/comments", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(100);
            verify(commentService).createComment(request, POST_ID, USERNAME);
        }

        @Test
        @WithMockUser
        void returns400WhenBodyMissing() {
            assertThat(mockMvc.post()
                            .uri("/api/posts/{postId}/comments", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.post()
                            .uri("/api/posts/{postId}/comments", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{postId}/comments/{id}")
    class UpdateComment {

        @Test
        @WithMockUser(username = USERNAME)
        void updatesAndReturns200() {
            var updateRequest = new CommentRequest("Updated body");
            var updatedResponse = new CommentResponse(
                    COMMENT_ID,
                    "Updated body",
                    POST_ID,
                    new AuthorResponse(AUTHOR_ID, null, null, null, null),
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T01:00:00Z"));
            when(commentService.updateComment(POST_ID, COMMENT_ID, updateRequest, USERNAME))
                    .thenReturn(updatedResponse);

            assertThat(mockMvc.put()
                            .uri("/api/posts/{postId}/comments/{id}", POST_ID, COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(updateRequest)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.body")
                    .asString()
                    .isEqualTo("Updated body");
            verify(commentService).updateComment(POST_ID, COMMENT_ID, updateRequest, USERNAME);
        }

        @Test
        @WithMockUser
        void returns400WhenBodyMissing() {
            assertThat(mockMvc.put()
                            .uri("/api/posts/{postId}/comments/{id}", POST_ID, COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @WithMockUser(username = "bob")
        void returns403WhenNotOwner() {
            when(commentService.updateComment(POST_ID, COMMENT_ID, request, "bob"))
                    .thenThrow(new ForbiddenOperationException("not owner"));

            assertThat(mockMvc.put()
                            .uri("/api/posts/{postId}/comments/{id}", POST_ID, COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.put()
                            .uri("/api/posts/{postId}/comments/{id}", POST_ID, COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /api/posts/{postId}/comments/{id}")
    class DeleteComment {

        @Test
        @WithMockUser(roles = "ADMIN")
        void deletesAndReturns204() {
            assertThat(mockMvc.delete().uri("/api/posts/{postId}/comments/{id}", POST_ID, COMMENT_ID))
                    .hasStatus(HttpStatus.NO_CONTENT);
            verify(commentService).deleteComment(POST_ID, COMMENT_ID);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.delete().uri("/api/posts/{postId}/comments/{id}", POST_ID, COMMENT_ID))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
