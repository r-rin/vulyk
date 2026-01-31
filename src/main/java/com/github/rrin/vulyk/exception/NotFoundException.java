package com.github.rrin.vulyk.exception;

public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(404, "Not Found", message);
    }
}
