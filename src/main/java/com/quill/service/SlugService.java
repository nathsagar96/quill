package com.quill.service;

import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class SlugService {

    public String generateSlug(String source) {
        Objects.requireNonNull(source, "source must not be null");
        if (source.isBlank()) {
            return "";
        }
        return source.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    public String toUniqueSlug(String source, String fallback, Predicate<String> existsChecker) {
        if (source == null) {
            source = fallback;
        }
        String base = generateSlug(source);
        if (base.isBlank()) {
            base = fallback;
        }
        String slug = base;
        int counter = 1;
        while (existsChecker.test(slug)) {
            slug = base + "-" + counter;
            counter++;
        }
        return slug;
    }

    public String resolveSlug(String providedSlug, String source, Predicate<String> existsChecker) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            if (existsChecker.test(providedSlug)) {
                throw new IllegalArgumentException("Slug '" + providedSlug + "' is already in use");
            }
            return providedSlug;
        }
        return toUniqueSlug(source, "post", existsChecker);
    }
}
