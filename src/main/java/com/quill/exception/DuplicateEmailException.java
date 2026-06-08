package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class DuplicateEmailException extends ApplicationException {

    public DuplicateEmailException(String email) {
        super("Email already in use: " + email, HttpStatus.CONFLICT);
    }
}
