package com.github.rrin.vulyk.domain;

public interface Identifiable<ID> {
    ID getId();
    void setId(ID id);
}
