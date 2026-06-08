package com.quill.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.dto.request.TagRequest;
import com.quill.dto.response.TagResponse;
import com.quill.model.Tag;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TagMapper")
class TagMapperTest {

    private final TagMapper mapper = new TagMapper();

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("maps every field")
        void mapsAllFields() {
            var created = Instant.parse("2024-01-01T00:00:00Z");
            var updated = Instant.parse("2024-02-02T00:00:00Z");
            var entity = Tag.builder()
                    .id(1L)
                    .name("java")
                    .slug("java")
                    .createdAt(created)
                    .updatedAt(updated)
                    .build();

            TagResponse response = mapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("java");
            assertThat(response.slug()).isEqualTo("java");
            assertThat(response.createdAt()).isEqualTo(created);
            assertThat(response.updatedAt()).isEqualTo(updated);
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("sets name from request")
        void setsName() {
            var request = new TagRequest("java");

            Tag entity = mapper.toEntity(request);

            assertThat(entity.getName()).isEqualTo("java");
        }

        @Test
        @DisplayName("leaves id and audit fields to JPA")
        void doesNotSetPersistedFields() {
            var request = new TagRequest("java");

            Tag entity = mapper.toEntity(request);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getSlug()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getPosts()).isEmpty();
        }
    }
}
