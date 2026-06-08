package com.quill.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.model.Category;
import com.quill.model.Post;
import com.quill.model.Tag;
import com.quill.model.User;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PostMapper")
class PostMapperTest {

    private final PostMapper mapper = new PostMapper();

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("maps every field including the author's id, category ids, and tag ids")
        void mapsAllFields() {
            var created = Instant.parse("2024-01-01T00:00:00Z");
            var updated = Instant.parse("2024-02-02T00:00:00Z");
            var author = User.builder().id(7L).build();
            var cat = Category.builder().id(1L).build();
            var tag = Tag.builder().id(2L).build();
            var post = Post.builder()
                    .id(42L)
                    .title("Title")
                    .body("Body")
                    .author(author)
                    .categories(Set.of(cat))
                    .tags(Set.of(tag))
                    .createdAt(created)
                    .updatedAt(updated)
                    .build();

            PostResponse response = mapper.toResponse(post);

            assertThat(response.id()).isEqualTo(42L);
            assertThat(response.title()).isEqualTo("Title");
            assertThat(response.body()).isEqualTo("Body");
            assertThat(response.authorId()).isEqualTo(7L);
            assertThat(response.categoryIds()).containsExactly(1L);
            assertThat(response.tagIds()).containsExactly(2L);
            assertThat(response.createdAt()).isEqualTo(created);
            assertThat(response.updatedAt()).isEqualTo(updated);
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("sets all fields from the request and supplied entities")
        void setsAllFields() {
            PostRequest request = new PostRequest("Title", "Body", null, Set.of(1L), Set.of(2L));
            User author = User.builder().id(1L).build();
            var cat = Category.builder().id(1L).name("Tech").build();
            var tag = Tag.builder().id(2L).name("java").build();

            Post entity = mapper.toEntity(request, author, Set.of(cat), Set.of(tag));

            assertThat(entity.getTitle()).isEqualTo("Title");
            assertThat(entity.getBody()).isEqualTo("Body");
            assertThat(entity.getAuthor()).isSameAs(author);
            assertThat(entity.getCategories()).containsExactly(cat);
            assertThat(entity.getTags()).containsExactly(tag);
        }

        @Test
        @DisplayName("leaves id, audit fields, and comments to JPA")
        void doesNotSetPersistedFields() {
            PostRequest request = new PostRequest("Title", "Body", null, Set.of(1L), Set.of());
            User author = User.builder().id(1L).build();
            var cat = Category.builder().id(1L).build();

            Post entity = mapper.toEntity(request, author, Set.of(cat), Set.of());

            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getComments()).isEmpty();
        }
    }
}
