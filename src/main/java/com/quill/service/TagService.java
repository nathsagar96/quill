package com.quill.service;

import com.quill.dto.request.TagRequest;
import com.quill.dto.response.TagResponse;
import com.quill.exception.TagNotFoundException;
import com.quill.mapper.TagMapper;
import com.quill.model.Tag;
import com.quill.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final SlugService slugService;

    public java.util.List<TagResponse> findAllTags() {
        log.debug("Fetching all tags");
        return tagRepository.findAll().stream().map(tagMapper::toResponse).toList();
    }

    public TagResponse findTagById(Long id) {
        log.debug("Fetching tag with id={}", id);
        return tagRepository.findById(id).map(tagMapper::toResponse).orElseThrow(() -> new TagNotFoundException(id));
    }

    @Transactional
    public TagResponse createTag(TagRequest request) {
        log.info("Creating tag: name='{}'", request.name());
        Tag entity = tagMapper.toEntity(request);
        entity.setSlug(slugService.toUniqueSlug(request.name(), "untitled-tag", tagRepository::existsBySlug));
        Tag saved = tagRepository.save(entity);
        log.info("Created tag with id={}", saved.getId());
        return tagMapper.toResponse(saved);
    }

    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        log.info("Updating tag with id={}", id);
        Tag existing = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        String oldSlug = existing.getSlug();
        if (!existing.getName().equals(request.name())) {
            existing.setName(request.name());
            existing.setSlug(slugService.toUniqueSlug(
                    request.name(), "untitled-tag", s -> tagRepository.existsBySlug(s) && !s.equals(oldSlug)));
        }
        log.info("Updated tag with id={}", id);
        return tagMapper.toResponse(existing);
    }

    @Transactional
    public void deleteTag(Long id) {
        log.info("Deleting tag with id={}", id);
        if (!tagRepository.existsById(id)) {
            throw new TagNotFoundException(id);
        }
        tagRepository.deleteById(id);
        log.info("Deleted tag with id={}", id);
    }
}
