package com.quill.exception;

import org.springframework.http.HttpStatus;

public final class ForbiddenOperationException extends ApplicationException {

    public ForbiddenOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
