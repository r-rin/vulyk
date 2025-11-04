package com.github.rrin.vulyk.exception;

public class InvalidCredentials extends BaseException {
    public InvalidCredentials(String message) {
        super(401, "Invalid credentials", message);
    }
}
