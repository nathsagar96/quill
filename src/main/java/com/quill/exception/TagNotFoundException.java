package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class TagNotFoundException extends ApplicationException {

    public TagNotFoundException(Long id) {
        super("Tag not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
