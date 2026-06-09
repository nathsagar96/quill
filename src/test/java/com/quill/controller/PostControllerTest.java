package com.quill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.config.TestSecurityConfig;
import com.quill.dto.request.PostRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.dto.response.PostResponse;
import com.quill.exception.PostNotFoundException;
import com.quill.model.PostStatus;
import com.quill.service.PostService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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

@WebMvcTest(PostController.class)
@Import(TestSecurityConfig.class)
@DisplayName("PostController")
class PostControllerTest {

    private static final long POST_ID = 10L;
    private static final long AUTHOR_ID = 1L;
    private static final long CATEGORY_ID = 5L;
    private static final long TAG_ID = 7L;
    private static final String USERNAME = "alice";
    private final Instant now = Instant.parse("2024-01-01T00:00:00Z");
    private final PostResponse response = new PostResponse(
            POST_ID,
            "Title",
            "Body",
            null,
            "title",
            new AuthorResponse(AUTHOR_ID, null, null, null, null),
            Set.of(CATEGORY_ID),
            Set.of(TAG_ID),
            PostStatus.PUBLISHED,
            null,
            null,
            now,
            now);
    private final PostRequest request =
            new PostRequest("Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), null, null);

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private PostService postService;

    @Nested
    @DisplayName("GET /api/posts")
    class FindAllPosts {

        @Test
        @WithMockUser
        void returnsPageOfPosts() {
            var pageable = PageRequest.of(0, 20);
            Page<PostResponse> page = new PageImpl<>(List.of(response), pageable, 1);
            when(postService.findAllPosts(any(PageRequest.class))).thenReturn(page);

            assertThat(mockMvc.get().uri("/api/posts"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .hasSize(1);
            verify(postService).findAllPosts(any(PageRequest.class));
        }

        @Test
        @WithMockUser
        void returnsEmptyPage() {
            var pageable = PageRequest.of(0, 20);
            Page<PostResponse> empty = new PageImpl<>(List.of(), pageable, 0);
            when(postService.findAllPosts(any(PageRequest.class))).thenReturn(empty);

            assertThat(mockMvc.get().uri("/api/posts"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .isEmpty();
        }

        @Test
        void allowsAnonymousAccess() {
            assertThat(mockMvc.get().uri("/api/posts")).hasStatusOk();
        }

        @Test
        @WithMockUser
        void filtersByCategoryId() {
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PostResponse> page = new PageImpl<>(List.of(response), pageable, 1);
            when(postService.findPostsByCategoryId(CATEGORY_ID, pageable)).thenReturn(page);

            assertThat(mockMvc.get().uri("/api/posts?categoryId={id}", CATEGORY_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .hasSize(1);
            verify(postService).findPostsByCategoryId(CATEGORY_ID, pageable);
        }

        @Test
        @WithMockUser
        void filtersByTagId() {
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PostResponse> page = new PageImpl<>(List.of(response), pageable, 1);
            when(postService.findPostsByTagId(TAG_ID, pageable)).thenReturn(page);

            assertThat(mockMvc.get().uri("/api/posts?tagId={id}", TAG_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .hasSize(1);
            verify(postService).findPostsByTagId(TAG_ID, pageable);
        }

        @Test
        @WithMockUser(username = USERNAME)
        void filtersByStatus() {
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PostResponse> page = new PageImpl<>(List.of(response), pageable, 1);
            when(postService.findPostsByStatus(PostStatus.DRAFT, null, null, pageable, USERNAME))
                    .thenReturn(page);

            assertThat(mockMvc.get().uri("/api/posts?status=DRAFT"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content")
                    .asArray()
                    .hasSize(1);
            verify(postService).findPostsByStatus(PostStatus.DRAFT, null, null, pageable, USERNAME);
        }

        @Test
        void statusFilterReturns401WhenAnonymous() {
            assertThat(mockMvc.get().uri("/api/posts?status=DRAFT")).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{id}")
    class FindPostById {

        @Test
        @WithMockUser(username = USERNAME)
        void returnsPostWhenFound() {
            when(postService.findPostById(POST_ID, USERNAME)).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/posts/{id}", POST_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).findPostById(POST_ID, USERNAME);
        }

        @Test
        @WithMockUser(username = USERNAME)
        void returns404WhenNotFound() {
            when(postService.findPostById(POST_ID, USERNAME)).thenThrow(new PostNotFoundException(POST_ID));

            assertThat(mockMvc.get().uri("/api/posts/{id}", POST_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void allowsAnonymousAccess() {
            when(postService.findPostById(eq(POST_ID), any())).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/posts/{id}", POST_ID)).hasStatusOk();
            verify(postService).findPostById(eq(POST_ID), any());
        }
    }

    @Nested
    @DisplayName("GET /api/posts/slug/{slug}")
    class FindPostBySlug {

        private static final String SLUG = "my-post";

        @Test
        @WithMockUser(username = USERNAME)
        void returnsPostWhenFound() {
            when(postService.findPostBySlug(SLUG, USERNAME)).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/posts/slug/{slug}", SLUG))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).findPostBySlug(SLUG, USERNAME);
        }

        @Test
        @WithMockUser(username = USERNAME)
        void returns404WhenNotFound() {
            when(postService.findPostBySlug(SLUG, USERNAME)).thenThrow(new PostNotFoundException("not found"));

            assertThat(mockMvc.get().uri("/api/posts/slug/{slug}", SLUG)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void allowsAnonymousAccess() {
            when(postService.findPostBySlug(eq(SLUG), any())).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/posts/slug/{slug}", SLUG)).hasStatusOk();
            verify(postService).findPostBySlug(eq(SLUG), any());
        }
    }

    @Nested
    @DisplayName("POST /api/posts")
    class CreatePost {

        @Test
        @WithMockUser(username = USERNAME)
        void createsAndReturns201() {
            when(postService.createPost(request, USERNAME)).thenReturn(response);

            assertThat(mockMvc.post()
                            .uri("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).createPost(request, USERNAME);
        }

        @Test
        @WithMockUser
        void returns400WhenBodyMissing() {
            assertThat(mockMvc.post()
                            .uri("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.post()
                            .uri("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(
                                    new PostRequest("Title", "Body", null, Set.of(CATEGORY_ID), null, null, null))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{id}")
    class UpdatePost {

        @Test
        @WithMockUser(username = USERNAME)
        void updatesAndReturns200() {
            when(postService.updatePost(POST_ID, request, USERNAME)).thenReturn(response);

            assertThat(mockMvc.put()
                            .uri("/api/posts/{id}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).updatePost(eq(POST_ID), any(PostRequest.class), eq(USERNAME));
        }

        @Test
        @WithMockUser(username = USERNAME)
        void returns404WhenNotFound() {
            when(postService.updatePost(POST_ID, request, USERNAME)).thenThrow(new PostNotFoundException(POST_ID));

            assertThat(mockMvc.put()
                            .uri("/api/posts/{id}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.put()
                            .uri("/api/posts/{id}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(
                                    new PostRequest("Title", "Body", null, Set.of(CATEGORY_ID), null, null, null))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /api/posts/{id}")
    class DeletePost {

        @Test
        @WithMockUser(roles = "ADMIN")
        void deletesAndReturns204() {
            assertThat(mockMvc.delete().uri("/api/posts/{id}", POST_ID)).hasStatus(HttpStatus.NO_CONTENT);
            verify(postService).deletePost(POST_ID);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void returns404WhenNotFound() {
            doThrow(new PostNotFoundException(POST_ID)).when(postService).deletePost(POST_ID);

            assertThat(mockMvc.delete().uri("/api/posts/{id}", POST_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @WithMockUser
        void returns403WhenNotAdmin() {
            assertThat(mockMvc.delete().uri("/api/posts/{id}", POST_ID)).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.delete().uri("/api/posts/{id}", POST_ID)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
