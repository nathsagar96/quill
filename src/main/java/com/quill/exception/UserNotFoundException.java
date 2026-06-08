package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
