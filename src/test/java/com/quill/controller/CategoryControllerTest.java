package com.quill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.config.TestSecurityConfig;
import com.quill.dto.request.CategoryRequest;
import com.quill.dto.response.CategoryResponse;
import com.quill.exception.CategoryNotFoundException;
import com.quill.filter.CacheControlFilter;
import com.quill.service.CategoryService;
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
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CategoryController.class)
@Import({TestSecurityConfig.class, CacheControlFilter.class})
@DisplayName("CategoryController")
class CategoryControllerTest {

    private static final long CATEGORY_ID = 1L;
    private final CategoryResponse response = new CategoryResponse(
            CATEGORY_ID,
            "Technology",
            "technology",
            "Tech posts",
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Nested
    @DisplayName("GET /api/categories")
    class FindAllCategories {

        @Test
        void returnsListOfCategories() {
            when(categoryService.findAllCategories()).thenReturn(List.of(response));

            assertThat(mockMvc.get().uri("/api/categories"))
                    .hasStatusOk()
                    .hasHeader("Cache-Control", "max-age=3600, public")
                    .bodyJson()
                    .isEqualTo(jsonMapper.writeValueAsString(List.of(response)));
            verify(categoryService).findAllCategories();
        }

        @Test
        void allowsAnonymousAccess() {
            assertThat(mockMvc.get().uri("/api/categories"))
                    .hasStatusOk()
                    .hasHeader("Cache-Control", "max-age=3600, public");
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}")
    class FindCategoryById {

        @Test
        void returnsCategoryWhenFound() {
            when(categoryService.findCategoryById(CATEGORY_ID)).thenReturn(response);

            assertThat(mockMvc.get().uri("/api/categories/{id}", CATEGORY_ID))
                    .hasStatusOk()
                    .hasHeader("Cache-Control", "max-age=3600, public")
                    .bodyJson()
                    .extractingPath("$.name")
                    .asString()
                    .isEqualTo("Technology");
            verify(categoryService).findCategoryById(CATEGORY_ID);
        }

        @Test
        void returns404WhenNotFound() {
            when(categoryService.findCategoryById(CATEGORY_ID)).thenThrow(new CategoryNotFoundException(CATEGORY_ID));

            assertThat(mockMvc.get().uri("/api/categories/{id}", CATEGORY_ID)).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        void allowsAnonymousAccess() {
            assertThat(mockMvc.get().uri("/api/categories/{id}", CATEGORY_ID))
                    .hasStatusOk()
                    .hasHeader("Cache-Control", "max-age=3600, public");
        }
    }

    @Nested
    @DisplayName("POST /api/categories")
    class CreateCategory {

        @Test
        @WithMockUser
        void createsAndReturns201() {
            var request = new CategoryRequest("Technology", "Tech posts");
            when(categoryService.createCategory(request)).thenReturn(response);

            assertThat(mockMvc.post()
                            .uri("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new CategoryRequest("Technology", "Tech posts"))))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.id")
                    .asNumber()
                    .isEqualTo(1);
            verify(categoryService).createCategory(request);
        }

        @Test
        @WithMockUser
        void returns400WhenNameMissing() {
            assertThat(mockMvc.post()
                            .uri("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.post()
                            .uri("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new CategoryRequest("Tech", null))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("PUT /api/categories/{id}")
    class UpdateCategory {

        @Test
        @WithMockUser
        void updatesAndReturns200() {
            var request = new CategoryRequest("Updated", null);
            var updatedResponse = new CategoryResponse(
                    CATEGORY_ID,
                    "Updated",
                    "updated",
                    null,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-02-02T00:00:00Z"));
            when(categoryService.updateCategory(CATEGORY_ID, request)).thenReturn(updatedResponse);

            assertThat(mockMvc.put()
                            .uri("/api/categories/{id}", CATEGORY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new CategoryRequest("Updated", null))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.name")
                    .asString()
                    .isEqualTo("Updated");
            verify(categoryService).updateCategory(CATEGORY_ID, request);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.put()
                            .uri("/api/categories/{id}", CATEGORY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new CategoryRequest("X", null))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /api/categories/{id}")
    class DeleteCategory {

        @Test
        @WithMockUser(roles = "ADMIN")
        void deletesAndReturns204() {
            assertThat(mockMvc.delete().uri("/api/categories/{id}", CATEGORY_ID))
                    .hasStatus(HttpStatus.NO_CONTENT);
            verify(categoryService).deleteCategory(CATEGORY_ID);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void returns404WhenNotFound() {
            doThrow(new CategoryNotFoundException(CATEGORY_ID))
                    .when(categoryService)
                    .deleteCategory(CATEGORY_ID);

            assertThat(mockMvc.delete().uri("/api/categories/{id}", CATEGORY_ID))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @WithMockUser
        void returns403WhenNotAdmin() {
            assertThat(mockMvc.delete().uri("/api/categories/{id}", CATEGORY_ID))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void requiresAuthentication() {
            assertThat(mockMvc.delete().uri("/api/categories/{id}", CATEGORY_ID))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
