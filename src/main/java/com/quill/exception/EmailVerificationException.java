package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class EmailVerificationException extends ApplicationException {

    public EmailVerificationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
