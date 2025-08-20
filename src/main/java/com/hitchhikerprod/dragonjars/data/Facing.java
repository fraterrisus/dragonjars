package com.hitchhikerprod.dragonjars.data;

import java.util.Arrays;

public enum Facing {
    NORTH(0), EAST(1), SOUTH(2), WEST(3);

    private int index;

    Facing(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static Facing valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.index() == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid facing value: " + value));
    }
}
