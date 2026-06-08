package com.quill.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.TestcontainersConfiguration;
import com.quill.config.JpaConfig;
import com.quill.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@DisplayName("CategoryRepository")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("should persist a category and assign an id")
    void shouldPersistCategory() {
        var category = Category.builder()
                .name("Technology")
                .slug("technology")
                .description("Tech-related posts")
                .build();

        Category saved = categoryRepository.saveAndFlush(category);

        assertThat(saved.getId()).isNotNull();
        assertThat(categoryRepository.findById(saved.getId())).isPresent().get().satisfies(found -> {
            assertThat(found.getName()).isEqualTo("Technology");
            assertThat(found.getSlug()).isEqualTo("technology");
            assertThat(found.getDescription()).isEqualTo("Tech-related posts");
        });
    }

    @Test
    @DisplayName("should find category by name")
    void shouldFindByName() {
        categoryRepository.saveAndFlush(
                Category.builder().name("Science").slug("science").build());

        assertThat(categoryRepository.findByName("Science")).isPresent();
        assertThat(categoryRepository.findByName("Nonexistent")).isNotPresent();
    }

    @Test
    @DisplayName("should find category by slug")
    void shouldFindBySlug() {
        categoryRepository.saveAndFlush(
                Category.builder().name("Health").slug("health").build());

        assertThat(categoryRepository.findBySlug("health")).isPresent();
        assertThat(categoryRepository.findBySlug("missing")).isNotPresent();
    }
}
