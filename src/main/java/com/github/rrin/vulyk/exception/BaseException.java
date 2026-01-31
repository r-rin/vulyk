package com.github.rrin.vulyk.exception;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class BaseException extends RuntimeException {

    private final int status;
    private final String name;
    private final List<String> messages;

    public BaseException(int status, String name, String... messages) {
        super(Arrays.toString(messages));
        this.status = status;
        this.name = name;
        this.messages = List.of(messages);
    }
}
