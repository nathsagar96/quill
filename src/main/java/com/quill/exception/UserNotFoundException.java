package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
