package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterWidthTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    void isWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        assertTrue(uut.isWide());
        uut.setWidth(false);
        assertFalse(uut.isWide());
    }
}