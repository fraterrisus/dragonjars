package com.hitchhikerprod.dragonjars.exec;

import java.util.Arrays;

/*
 * make_free_space (cs/132e) iterates over the frobs struct looking for
 *   any segment with frob 2; when it finds one, it calls free_struct_memory()
 *   which looks up the segment and calls free (int 0x21 ah 0x49)
 */

public enum Frob {
    GONE(0), IN_USE(1), FREE(2), FROZEN(0xff);

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
