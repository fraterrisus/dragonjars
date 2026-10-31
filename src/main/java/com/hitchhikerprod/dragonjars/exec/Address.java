package com.hitchhikerprod.dragonjars.exec;

public record Address(int segment, int offset) {
    public Address incr(int i) {
        return new Address(segment, offset + i);
    }

    public Address incr() {
        return incr(1);
    }
}
