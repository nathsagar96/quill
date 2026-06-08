package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.dto.CategoryRequest;
import com.quill.dto.CategoryResponse;
import com.quill.exception.CategoryNotFoundException;
import com.quill.mapper.CategoryMapper;
import com.quill.model.Category;
import com.quill.repository.CategoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    private static final Long CATEGORY_ID = 1L;
    private static final Long MISSING_ID = 99L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequest request;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(CATEGORY_ID)
                .name("Technology")
                .slug("technology")
                .description("Tech posts")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        request = new CategoryRequest("Technology", "technology", "Tech posts");
        response = new CategoryResponse(
                CATEGORY_ID,
                "Technology",
                "technology",
                "Tech posts",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Nested
    @DisplayName("findAllCategories")
    class FindAllCategories {

        @Test
        @DisplayName("returns all categories mapped")
        void returnsAll() {
            when(categoryRepository.findAll()).thenReturn(List.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            var result = categoryService.findAllCategories();

            assertThat(result).containsExactly(response);
            verify(categoryRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findCategoryById")
    class FindCategoryById {

        @Test
        @DisplayName("returns mapped response when found")
        void returnsWhenFound() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            var result = categoryService.findCategoryById(CATEGORY_ID);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when missing")
        void throwsWhenMissing() {
            when(categoryRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

            var thrown =
                    assertThrows(CategoryNotFoundException.class, () -> categoryService.findCategoryById(MISSING_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_ID));
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("persists and returns mapped response")
        void createsAndReturns() {
            when(categoryMapper.toEntity(request)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            var result = categoryService.createCategory(request);

            assertThat(result).isEqualTo(response);
            verify(categoryMapper).toEntity(request);
            verify(categoryRepository).save(category);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("updates and returns mapped response")
        void updatesAndReturns() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            var result = categoryService.updateCategory(CATEGORY_ID, request);

            assertThat(category.getName()).isEqualTo("Technology");
            assertThat(category.getSlug()).isEqualTo("technology");
            assertThat(category.getDescription()).isEqualTo("Tech posts");
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when missing")
        void throwsWhenMissing() {
            when(categoryRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

            assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(MISSING_ID, request));
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("deletes when exists")
        void deletesExisting() {
            when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);

            categoryService.deleteCategory(CATEGORY_ID);

            verify(categoryRepository).deleteById(CATEGORY_ID);
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when missing")
        void throwsWhenMissing() {
            when(categoryRepository.existsById(MISSING_ID)).thenReturn(false);

            assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(MISSING_ID));
            verify(categoryRepository, never()).deleteById(any());
        }
    }
}
