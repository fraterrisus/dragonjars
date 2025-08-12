package com.hitchhikerprod.dragonjars.exec;

import java.util.Arrays;

public enum Frob {
    EMPTY(0), CLEAN(1), DIRTY(2), FROZEN(0xff);

    private final int value;

    Frob(int value) {
        this.value = value;
    }

    public static Frob of(int value) {
        return Arrays.stream(Frob.values())
                .filter(f -> f.value() == value)
                .findFirst().orElse(null);
    }

    public int value() {
        return value;
    }
}
