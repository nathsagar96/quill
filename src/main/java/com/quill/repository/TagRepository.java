package com.quill.repository;

import com.quill.model.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
