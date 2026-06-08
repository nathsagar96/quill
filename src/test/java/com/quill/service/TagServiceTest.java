package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.dto.TagRequest;
import com.quill.dto.TagResponse;
import com.quill.exception.TagNotFoundException;
import com.quill.mapper.TagMapper;
import com.quill.model.Tag;
import com.quill.repository.TagRepository;
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
@DisplayName("TagService")
class TagServiceTest {

    private static final Long TAG_ID = 1L;
    private static final Long MISSING_ID = 99L;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    private Tag tag;
    private TagRequest request;
    private TagResponse response;

    @BeforeEach
    void setUp() {
        tag = Tag.builder()
                .id(TAG_ID)
                .name("java")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        request = new TagRequest("java");
        response = new TagResponse(
                TAG_ID, "java", Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Nested
    @DisplayName("findAllTags")
    class FindAllTags {

        @Test
        @DisplayName("returns all tags mapped")
        void returnsAll() {
            when(tagRepository.findAll()).thenReturn(List.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(response);

            var result = tagService.findAllTags();

            assertThat(result).containsExactly(response);
            verify(tagRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findTagById")
    class FindTagById {

        @Test
        @DisplayName("returns mapped response when found")
        void returnsWhenFound() {
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(response);

            var result = tagService.findTagById(TAG_ID);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws TagNotFoundException when missing")
        void throwsWhenMissing() {
            when(tagRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(TagNotFoundException.class, () -> tagService.findTagById(MISSING_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_ID));
        }
    }

    @Nested
    @DisplayName("createTag")
    class CreateTag {

        @Test
        @DisplayName("persists and returns mapped response")
        void createsAndReturns() {
            when(tagMapper.toEntity(request)).thenReturn(tag);
            when(tagRepository.save(tag)).thenReturn(tag);
            when(tagMapper.toResponse(tag)).thenReturn(response);

            var result = tagService.createTag(request);

            assertThat(result).isEqualTo(response);
            verify(tagMapper).toEntity(request);
            verify(tagRepository).save(tag);
        }
    }

    @Nested
    @DisplayName("updateTag")
    class UpdateTag {

        @Test
        @DisplayName("updates and returns mapped response")
        void updatesAndReturns() {
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(response);

            var result = tagService.updateTag(TAG_ID, request);

            assertThat(tag.getName()).isEqualTo("java");
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws TagNotFoundException when missing")
        void throwsWhenMissing() {
            when(tagRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

            assertThrows(TagNotFoundException.class, () -> tagService.updateTag(MISSING_ID, request));
        }
    }

    @Nested
    @DisplayName("deleteTag")
    class DeleteTag {

        @Test
        @DisplayName("deletes when exists")
        void deletesExisting() {
            when(tagRepository.existsById(TAG_ID)).thenReturn(true);

            tagService.deleteTag(TAG_ID);

            verify(tagRepository).deleteById(TAG_ID);
        }

        @Test
        @DisplayName("throws TagNotFoundException when missing")
        void throwsWhenMissing() {
            when(tagRepository.existsById(MISSING_ID)).thenReturn(false);

            assertThrows(TagNotFoundException.class, () -> tagService.deleteTag(MISSING_ID));
            verify(tagRepository, never()).deleteById(any());
        }
    }
}
