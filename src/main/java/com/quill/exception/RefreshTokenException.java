package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class RefreshTokenException extends ApplicationException {

    public RefreshTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
