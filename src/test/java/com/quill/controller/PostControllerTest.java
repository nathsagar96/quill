package com.quill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.config.TestSecurityConfig;
import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.exception.PostNotFoundException;
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

@WebMvcTest(PostController.class)
@Import(TestSecurityConfig.class)
@DisplayName("PostController")
class PostControllerTest {

    private static final long POST_ID = 10L;
    private static final long AUTHOR_ID = 1L;
    private static final long CATEGORY_ID = 5L;
    private static final long TAG_ID = 7L;
    private static final String USERNAME = "alice";
    private final PostResponse response = new PostResponse(
            POST_ID,
            "Title",
            "Body",
            null,
            AUTHOR_ID,
            Set.of(CATEGORY_ID),
            Set.of(TAG_ID),
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));
    private final PostRequest request = new PostRequest("Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID));

    @Autowired
    private MockMvcTester mockMvc;

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
    }

    @Nested
    @DisplayName("GET /api/posts/{id}")
    class FindPostById {

        @Test
        @WithMockUser
        void returnsPostWhenFound() {
            when(postService.findPostById(POST_ID)).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/posts/{id}", POST_ID))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).findPostById(POST_ID);
        }

        @Test
        @WithMockUser
        void returns404WhenNotFound() {
            when(postService.findPostById(POST_ID)).thenThrow(new PostNotFoundException(POST_ID));

            assertThat(mockMvc.get().uri("/api/posts/{id}", POST_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void allowsAnonymousAccess() {
            assertThat(mockMvc.get().uri("/api/posts/{id}", POST_ID)).hasStatusOk();
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
                            .content("{\"title\":\"Title\",\"body\":\"Body\",\"categoryIds\":[5],\"tagIds\":[7]}"))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).createPost(request, USERNAME);
        }

        @Test
        @WithMockUser
        void returns400WhenCategoryIdsMissing() {
            assertThat(mockMvc.post()
                            .uri("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"body\":\"Body\"}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @WithMockUser
        void returns400WhenCategoryIdsEmpty() {
            assertThat(mockMvc.post()
                            .uri("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T\",\"body\":\"B\",\"categoryIds\":[]}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
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
                            .content("{\"title\":\"Title\",\"body\":\"Body\",\"categoryIds\":[5]}"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{id}")
    class UpdatePost {

        @Test
        @WithMockUser(username = USERNAME)
        void updatesAndReturns200() {
            when(postService.updatePost(POST_ID, request, USERNAME, false)).thenReturn(response);

            assertThat(mockMvc.put()
                            .uri("/api/posts/{id}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"body\":\"Body\",\"categoryIds\":[5],\"tagIds\":[7]}"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(10);
            verify(postService).updatePost(eq(POST_ID), any(PostRequest.class), eq(USERNAME), eq(false));
        }

        @Test
        @WithMockUser(username = USERNAME)
        void returns404WhenNotFound() {
            when(postService.updatePost(POST_ID, request, USERNAME, false))
                    .thenThrow(new PostNotFoundException(POST_ID));

            assertThat(mockMvc.put()
                            .uri("/api/posts/{id}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"body\":\"Body\",\"categoryIds\":[5],\"tagIds\":[7]}"))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.put()
                            .uri("/api/posts/{id}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"body\":\"Body\",\"categoryIds\":[5]}"))
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
