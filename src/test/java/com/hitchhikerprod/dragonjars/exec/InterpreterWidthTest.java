package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterWidthTest {
    @Test
    void isWide() {
        final Interpreter uut = new Interpreter(null, List.of(), 0, 0);
        uut.setWidth(true);
        assertTrue(uut.isWide());
        uut.setWidth(false);
        assertFalse(uut.isWide());
    }
}