package com.quill.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.model.Post;
import com.quill.model.User;
import java.time.Instant;
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
        @DisplayName("maps every field including the author's id")
        void mapsAllFields() {
            var created = Instant.parse("2024-01-01T00:00:00Z");
            var updated = Instant.parse("2024-02-02T00:00:00Z");
            var author = User.builder().id(7L).build();
            var post = Post.builder()
                    .id(42L)
                    .title("Title")
                    .body("Body")
                    .author(author)
                    .createdAt(created)
                    .updatedAt(updated)
                    .build();

            PostResponse response = mapper.toResponse(post);

            assertThat(response.id()).isEqualTo(42L);
            assertThat(response.title()).isEqualTo("Title");
            assertThat(response.body()).isEqualTo("Body");
            assertThat(response.authorId()).isEqualTo(7L);
            assertThat(response.createdAt()).isEqualTo(created);
            assertThat(response.updatedAt()).isEqualTo(updated);
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("sets title, body, and author from the request and supplied user")
        void setsTitleBodyAndAuthor() {
            PostRequest request = new PostRequest("Title", "Body");
            User author = User.builder().id(1L).build();

            Post entity = mapper.toEntity(request, author);

            assertThat(entity.getTitle()).isEqualTo("Title");
            assertThat(entity.getBody()).isEqualTo("Body");
            assertThat(entity.getAuthor()).isSameAs(author);
        }

        @Test
        @DisplayName("leaves id, audit fields, and comments to JPA")
        void doesNotSetPersistedFields() {
            PostRequest request = new PostRequest("Title", "Body");
            User author = User.builder().id(1L).build();

            Post entity = mapper.toEntity(request, author);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getComments()).isEmpty();
        }
    }
}
