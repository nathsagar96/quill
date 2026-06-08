package com.quill.repository;

import com.quill.model.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByCategoriesId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByTagsId(Long tagId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
