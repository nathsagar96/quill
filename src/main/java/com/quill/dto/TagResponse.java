package com.quill.dto;

import java.time.Instant;

public record TagResponse(Long id, String name, Instant createdAt, Instant updatedAt) {}
