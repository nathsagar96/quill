package com.quill.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.TestcontainersConfiguration;
import com.quill.config.JpaConfig;
import com.quill.model.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@DisplayName("TagRepository")
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Test
    @DisplayName("should persist a tag and assign an id")
    void shouldPersistTag() {
        var tag = Tag.builder().name("java").build();

        Tag saved = tagRepository.saveAndFlush(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(tagRepository.findById(saved.getId())).isPresent().get().satisfies(found -> {
            assertThat(found.getName()).isEqualTo("java");
        });
    }

    @Test
    @DisplayName("should find tag by name")
    void shouldFindByName() {
        tagRepository.saveAndFlush(Tag.builder().name("spring").build());

        assertThat(tagRepository.findByName("spring")).isPresent();
        assertThat(tagRepository.findByName("nonexistent")).isNotPresent();
    }
}
