package com.quill.controller;

import com.quill.dto.CommentRequest;
import com.quill.dto.CommentResponse;
import com.quill.exception.CommentNotFoundException;
import com.quill.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            AUTHOR_ID,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));
    private final CommentRequest request = new CommentRequest("Nice post!");

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Nested
    @DisplayName("GET /api/posts/{postId}/comments")
    class FindAllCommentsByPostId {

        @Test
        @WithMockUser
        void returnsPageOfComments() {
            var pageable = PageRequest.of(0, 20);
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
            var pageable = PageRequest.of(0, 20);
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
    @DisplayName("GET /api/comments/{id}")
    class FindCommentById {

        @Test
        @WithMockUser
        void returnsCommentWhenFound() {
            when(commentService.findCommentById(COMMENT_ID)).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/comments/{id}", COMMENT_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(100);
            verify(commentService).findCommentById(COMMENT_ID);
        }

        @Test
        @WithMockUser
        void returns404WhenNotFound() {
            when(commentService.findCommentById(COMMENT_ID)).thenThrow(new CommentNotFoundException(COMMENT_ID));

            assertThat(mockMvc.get().uri("/api/comments/{id}", COMMENT_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.get().uri("/api/comments/{id}", COMMENT_ID)).hasStatus(HttpStatus.UNAUTHORIZED);
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
                            .content("{\"body\":\"Nice post!\"}"))
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
                            .content("{\"body\":\"Nice post!\"}"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
