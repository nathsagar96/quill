package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class PostNotFoundException extends ApplicationException {

    public PostNotFoundException(Long id) {
        super("Post not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public PostNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
