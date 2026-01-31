package com.github.rrin.vulyk.exception;

public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(400, "Validation error", message);
    }
}
