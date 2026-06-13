package com.quill.service;

import com.quill.dto.request.CategoryRequest;
import com.quill.dto.response.CategoryResponse;
import com.quill.exception.CategoryNotFoundException;
import com.quill.mapper.CategoryMapper;
import com.quill.model.Category;
import com.quill.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SlugService slugService;

    @Cacheable("categories")
    public java.util.List<CategoryResponse> findAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Cacheable("categories")
    public CategoryResponse findCategoryById(Long id) {
        log.debug("Fetching category with id={}", id);
        return categoryRepository
                .findById(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: name='{}'", request.name());
        Category entity = categoryMapper.toEntity(request);
        entity.setSlug(slugService.toUniqueSlug(request.name(), "untitled-category", categoryRepository::existsBySlug));
        Category saved = categoryRepository.save(entity);
        log.info("Created category with id={}", saved.getId());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with id={}", id);
        Category existing = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        String oldSlug = existing.getSlug();
        if (!existing.getName().equals(request.name())) {
            existing.setName(request.name());
            existing.setSlug(slugService.toUniqueSlug(
                    request.name(),
                    "untitled-category",
                    s -> categoryRepository.existsBySlug(s) && !s.equals(oldSlug)));
        }
        existing.setDescription(request.description());
        log.info("Updated category with id={}", id);
        return categoryMapper.toResponse(existing);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category with id={}", id);
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
        log.info("Deleted category with id={}", id);
    }
}
