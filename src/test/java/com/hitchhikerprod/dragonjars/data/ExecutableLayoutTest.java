package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ExecutableLayoutTest {
    @Test
    void recognizesGogExecutableHash() {
        assertSame(ExecutableLayout.GOG, ExecutableLayout.forSha256(ExecutableLayout.GOG_SHA256));
    }

    @Test
    void preservesOriginalLayoutForUnknownExecutables() {
        assertSame(ExecutableLayout.ORIGINAL, ExecutableLayout.forSha256("unknown"));
    }
}
