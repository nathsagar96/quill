package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class CategoryNotFoundException extends ApplicationException {

    public CategoryNotFoundException(Long id) {
        super("Category not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
