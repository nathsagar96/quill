package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SlugService")
class SlugServiceTest {

    private final SlugService slugService = new SlugService();

    @Nested
    @DisplayName("generateSlug")
    class GenerateSlug {

        @Test
        @DisplayName("lowercases and replaces spaces with hyphens")
        void lowercasesAndHyphenates() {
            assertThat(slugService.generateSlug("Hello World")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("strips non-alphanumeric characters except hyphens and spaces")
        void stripsSpecialCharacters() {
            assertThat(slugService.generateSlug("Hello, World! #2024")).isEqualTo("hello-world-2024");
        }

        @Test
        @DisplayName("collapses multiple hyphens into one")
        void collapsesHyphens() {
            assertThat(slugService.generateSlug("foo---bar")).isEqualTo("foo-bar");
        }

        @Test
        @DisplayName("trims leading and trailing hyphens")
        void trimsHyphens() {
            assertThat(slugService.generateSlug("-hello-world-")).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("throws NullPointerException for null input")
        void throwsForNull() {
            assertThatThrownBy(() -> slugService.generateSlug(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("source must not be null");
        }

        @Test
        @DisplayName("returns empty string for blank input")
        void returnsEmptyForBlank() {
            assertThat(slugService.generateSlug("   ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("toUniqueSlug")
    class ToUniqueSlug {

        @Test
        @DisplayName("returns the base slug when it does not exist")
        void returnsBaseWhenUnique() {
            String result = slugService.toUniqueSlug("Hello World", "fallback", _ -> false);
            assertThat(result).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("appends a counter suffix when the slug already exists")
        void appendsCounterWhenTaken() {
            AtomicInteger call = new AtomicInteger(0);
            Predicate<String> exists = _ -> {
                int n = call.getAndIncrement();
                return n < 2;
            };
            String result = slugService.toUniqueSlug("test", "fallback", exists);
            assertThat(result).isEqualTo("test-2");
        }

        @Test
        @DisplayName("uses the fallback when the source is blank")
        void usesFallbackForBlankSource() {
            String result = slugService.toUniqueSlug("", "fallback", _ -> false);
            assertThat(result).isEqualTo("fallback");
        }

        @Test
        @DisplayName("uses the fallback when the source is null")
        void usesFallbackForNullSource() {
            String result = slugService.toUniqueSlug(null, "fallback", _ -> false);
            assertThat(result).isEqualTo("fallback");
        }

        @Test
        @DisplayName("appends counter to fallback when fallback slug is taken")
        void fallbackWithCounter() {
            AtomicInteger call = new AtomicInteger(0);
            Predicate<String> exists = _ -> call.getAndIncrement() == 0;
            String result = slugService.toUniqueSlug("", "fallback", exists);
            assertThat(result).isEqualTo("fallback-1");
        }
    }

    @Nested
    @DisplayName("resolveSlug")
    class ResolveSlug {

        @Test
        @DisplayName("returns the provided slug when it is non-blank and unique")
        void returnsProvidedSlugWhenUnique() {
            String result = slugService.resolveSlug("custom-slug", "title", _ -> false);
            assertThat(result).isEqualTo("custom-slug");
        }

        @Test
        @DisplayName("throws when the provided slug already exists")
        void throwsWhenProvidedSlugExists() {
            var thrown = assertThrows(
                    IllegalArgumentException.class,
                    () -> slugService.resolveSlug("taken", "title", s -> s.equals("taken")));
            assertThat(thrown).hasMessageContaining("taken");
        }

        @Test
        @DisplayName("falls back to toUniqueSlug when no slug is provided")
        void fallsBackWhenSlugNull() {
            String result = slugService.resolveSlug(null, "Hello World", _ -> false);
            assertThat(result).isEqualTo("hello-world");
        }

        @Test
        @DisplayName("falls back to toUniqueSlug when provided slug is blank")
        void fallsBackWhenSlugBlank() {
            String result = slugService.resolveSlug("  ", "Hello World", _ -> false);
            assertThat(result).isEqualTo("hello-world");
        }
    }
}
