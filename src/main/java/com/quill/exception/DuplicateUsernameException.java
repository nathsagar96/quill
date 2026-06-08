package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class DuplicateUsernameException extends ApplicationException {

    public DuplicateUsernameException(String username) {
        super("Username already taken: " + username, HttpStatus.CONFLICT);
    }
}
