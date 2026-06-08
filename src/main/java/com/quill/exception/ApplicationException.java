package com.quill.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract sealed class ApplicationException extends RuntimeException
        permits DuplicateEmailException,
                DuplicateUsernameException,
                ForbiddenOperationException,
                PostNotFoundException,
                UserNotFoundException {

    private final HttpStatus httpStatus;

    protected ApplicationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
