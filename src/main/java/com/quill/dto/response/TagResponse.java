package com.quill.dto.response;

import java.time.Instant;

public record TagResponse(Long id, String name, Instant createdAt, Instant updatedAt) {}
