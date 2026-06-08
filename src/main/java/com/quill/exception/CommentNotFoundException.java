package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class CommentNotFoundException extends ApplicationException {

    public CommentNotFoundException(Long id) {
        super("Comment not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public CommentNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
