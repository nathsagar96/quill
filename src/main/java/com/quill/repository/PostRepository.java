package com.quill.repository;

import com.quill.model.Post;
import com.quill.model.PostStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByStatusAndCategoriesId(PostStatus status, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByStatusAndTagsId(PostStatus status, Long tagId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByAuthorIdAndStatusAndCategoriesId(
            Long authorId, PostStatus status, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Page<Post> findByAuthorIdAndStatusAndTagsId(Long authorId, PostStatus status, Long tagId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    Optional<Post> findBySlug(String slug);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    List<Post> findByStatusAndScheduledAtBefore(PostStatus status, Instant before);

    boolean existsBySlug(String slug);

    @Query(value = """
            SELECT p.id FROM posts p
            WHERE p.search_vector @@ websearch_to_tsquery('english', :query)
            AND p.status = 'PUBLISHED'
            ORDER BY ts_rank(p.search_vector, websearch_to_tsquery('english', :query)) DESC
            """, countQuery = """
            SELECT count(*) FROM posts p
            WHERE p.search_vector @@ websearch_to_tsquery('english', :query)
            AND p.status = 'PUBLISHED'
            """, nativeQuery = true)
    Page<Long> searchPostIds(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories", "tags"})
    List<Post> findByIdIn(List<Long> ids);
}
