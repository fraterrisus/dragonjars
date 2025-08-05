package com.hitchhikerprod.dragonjars.exec;

public record Address(int chunk, int offset) {
    public Address incr(int i) {
        return new Address(chunk, offset + i);
    }

    public Address incr() {
        return incr(1);
    }
}
