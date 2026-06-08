package com.quill.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.dto.request.CategoryRequest;
import com.quill.dto.response.CategoryResponse;
import com.quill.model.Category;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryMapper")
class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("maps every field")
        void mapsAllFields() {
            var created = Instant.parse("2024-01-01T00:00:00Z");
            var updated = Instant.parse("2024-02-02T00:00:00Z");
            var entity = Category.builder()
                    .id(1L)
                    .name("Technology")
                    .slug("technology")
                    .description("Tech posts")
                    .createdAt(created)
                    .updatedAt(updated)
                    .build();

            CategoryResponse response = mapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Technology");
            assertThat(response.slug()).isEqualTo("technology");
            assertThat(response.description()).isEqualTo("Tech posts");
            assertThat(response.createdAt()).isEqualTo(created);
            assertThat(response.updatedAt()).isEqualTo(updated);
        }

        @Test
        @DisplayName("handles null description")
        void handlesNullDescription() {
            var entity = Category.builder()
                    .id(1L)
                    .name("Tech")
                    .slug("tech")
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .build();

            CategoryResponse response = mapper.toResponse(entity);

            assertThat(response.description()).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("sets name, slug, and description from the request")
        void setsFields() {
            var request = new CategoryRequest("Technology", "technology", "Tech posts");

            Category entity = mapper.toEntity(request);

            assertThat(entity.getName()).isEqualTo("Technology");
            assertThat(entity.getSlug()).isEqualTo("technology");
            assertThat(entity.getDescription()).isEqualTo("Tech posts");
        }

        @Test
        @DisplayName("leaves id and audit fields to JPA")
        void doesNotSetPersistedFields() {
            var request = new CategoryRequest("Tech", "tech", null);

            Category entity = mapper.toEntity(request);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getPosts()).isEmpty();
        }
    }
}
