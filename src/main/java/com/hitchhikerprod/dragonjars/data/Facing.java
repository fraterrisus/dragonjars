package com.hitchhikerprod.dragonjars.data;

import java.util.Arrays;
import java.util.function.Function;

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

    public Facing turnRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public Facing turnLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
        };
    }

    public enum Delta {
        LEFT(Facing::turnLeft),
        RIGHT(Facing::turnRight),
        NONE(Function.identity());

        private final Function<Facing, Facing> mogrifier;

        Delta(Function<Facing, Facing> mogrifier) {
            this.mogrifier = mogrifier;
        }

        public Facing apply(Facing base) {
            return mogrifier.apply(base);
        }
    }
}
