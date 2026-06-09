package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class PasswordResetTokenException extends ApplicationException {

    public PasswordResetTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
